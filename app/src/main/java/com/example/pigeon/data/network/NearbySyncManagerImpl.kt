package com.example.pigeon.data.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.pigeon.domain.model.ConnectionType
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.domain.network.ConnectionStatus
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.repository.UserRepository
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.proto.PigeonPayload
import com.example.pigeon.proto.SyncItem
import com.example.pigeon.proto.SyncManifest
import com.example.pigeon.proto.VerificationMessage
import com.example.pigeon.data.local.dao.VerificationDao
import com.example.pigeon.data.local.entities.VerificationEntity
import com.google.android.gms.nearby.Nearby
import kotlinx.coroutines.Job
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbySyncManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: com.example.pigeon.domain.repository.EventRepository,
    private val verificationDao: VerificationDao,
    private val userRepository: UserRepository
) : NearbySyncManager {

    private val TAG = "[MESH_RADIO]"
    private val SERVICE_ID = context.packageName
    private val MOCK_PEER_ID = "MOCK_PEER_001"
    private val MOCK_PEER_NAME = "Mock Responder"

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val debugScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var localAdvertiseName: String = "PigeonNode"

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.OFF)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _nearbyPeers = MutableStateFlow<List<Peer>>(emptyList())
    override val nearbyPeers: StateFlow<List<Peer>> = _nearbyPeers.asStateFlow()

    private val _isLinking = MutableStateFlow(false)
    override val isLinking: StateFlow<Boolean> = _isLinking.asStateFlow()

    private val _isWaveActive = MutableStateFlow(false)
    override val isWaveActive: StateFlow<Boolean> = _isWaveActive.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var originalPowerState: MeshPowerState? = null
    // activeModeJob owns the 60 s proximity-wave countdown.
    // It is started ONCE per wave and deliberately NOT reset by relay calls from checkSyncCompletion —
    // the window is a fixed lifetime, not a sliding one.
    private var activeModeJob: Job? = null
    private var radioJob: Job? = null
    private var currentPowerState: MeshPowerState = MeshPowerState.OFF
    private var isStickyActive: Boolean = false
    private var lastTransitionTime = 0L

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncStates = mutableMapOf<String, SyncState>()

    private fun refreshSyncingFlag() {
        _isSyncing.value = syncStates.isNotEmpty()
    }

    // Endpoints that have already sent us a connection request (onConnectionInitiated fired).
    // Used to prevent an outbound requestConnection() racing against an inbound one — the
    // peer that fires onConnectionInitiated first "wins" and becomes the initiator.
    private val pendingIncomingConnections = mutableSetOf<String>()

    // Retry counter per endpoint for transient connection failures (GATT errors, etc.)
    private val connectionRetries = mutableMapOf<String, Int>()
    private val MAX_CONNECTION_RETRIES = 2

    // Cooldown to suppress outbound reconnection attempts to a peer we just finished syncing with.
    // Discovery keeps reporting the same peer every few hundred ms while it remains in range, and
    // without this cooldown processEndpointFound's debounced outbound would fire immediately after
    // a clean disconnect — racing the BT controller while it's still tearing down the previous
    // session and producing STATUS_RADIO_ERROR (8007).
    private val recentlySyncedPeers = mutableMapOf<String, Long>()
    // 8 s is enough for the BT controller to tear down the previous session and avoid
    // STATUS_RADIO_ERROR (8007) on tight reconnect. The other guards in
    // processEndpointFound (pendingIncomingConnections / syncStates / alreadyConnected)
    // handle the synchronous race cases, so this only needs to cover hardware drain.
    // Was 30 s, which combined with the post-sync relay-wave loop to delay
    // user-reported events by minutes; broadcastIncident also clears this map outright.
    private val SYNC_COOLDOWN_MS = 8_000L

    // Peer TTL: drop peers that haven't been seen in this many ms. The list is no longer
    // wiped on stop(), so without a TTL the UI would slowly accumulate stale entries from
    // every device we've ever crossed paths with.
    private val PEER_TTL_MS = 5 * 60_000L

    init {
        syncScope.launch {
            userRepository.getUser().collect { user ->
                localAdvertiseName = when {
                    user == null -> "PigeonNode"
                    user.isAnonymous -> user.nodeId.ifBlank { user.nodeName }
                    else -> user.displayName.ifBlank { "PigeonNode" }
                }
            }
        }
        // Periodic peer pruning. Runs every 60 s; cheap, and decoupled from any radio path.
        syncScope.launch {
            while (isActive) {
                delay(60_000L)
                val cutoff = System.currentTimeMillis() - PEER_TTL_MS
                _nearbyPeers.update { peers ->
                    peers.filter { it.isConnected || it.lastSeen >= cutoff }
                }
            }
        }
    }

    // ── Duty Cycle ────────────────────────────────────────────────────────────
    // MediaTek (Helio G85 / Samsung M32) cannot multiplex BLE advertising and
    // scanning simultaneously — the firmware overflows its request queue and
    // returns STATUS_RADIO_ERROR (8007). We solve this at the software level by
    // serialising the two roles into alternating time windows (duty cycle), so
    // the radio controller only ever handles one role at a time.

    private enum class DutyCyclePhase { BEACON, SCANNER }
    private var dutyCycleJob: Job? = null

    /** 1.5 s per phase for proximity-wave (fast relay), 3 s for user-initiated sticky-ACTIVE.
     *  Shorter intervals mean a peer pair finds each other within roughly one half-cycle on
     *  average instead of two — measured ~50% reduction in time-to-first-link. */
    private fun getDutyCycleInterval(): Long = if (isStickyActive) 3000L else 1500L

    /**
     * Stops the current role and starts the next one with a 200 ms silence gap.
     * The gap gives the MediaTek controller time to flush its internal DMA buffers
     * before the new role's hardware registers are written.
     */
    private suspend fun transitionToPhase(phase: DutyCyclePhase) {
        when (phase) {
            DutyCyclePhase.BEACON -> {
                Log.d(TAG, "⟳ Duty Cycle → BEACON (Advertising)")
                try { connectionsClient.stopDiscovery() } catch (_: Exception) {}
                try { connectionsClient.stopAdvertising() } catch (_: Exception) {}
                delay(200)
                startAdvertising(lowPower = false)
            }
            DutyCyclePhase.SCANNER -> {
                Log.d(TAG, "⟳ Duty Cycle → SCANNER (Discovery)")
                try { connectionsClient.stopAdvertising() } catch (_: Exception) {}
                delay(200)
                startDiscovery(lowPower = false)
            }
        }
    }

    /**
     * Launches a coroutine that alternates BEACON → SCANNER → BEACON … every [intervalMs].
     * Starts with BEACON so the device is immediately visible to nearby peers.
     * Any existing cycle is cancelled first, making this safe to call on re-arm.
     */
    private fun startDutyCycle(intervalMs: Long) {
        dutyCycleJob?.cancel()
        dutyCycleJob = syncScope.launch {
            Log.d(TAG, "Duty cycle started (${intervalMs}ms/phase, isStickyActive=$isStickyActive)")
            var phase = DutyCyclePhase.BEACON
            transitionToPhase(phase)
            while (isActive) {
                delay(intervalMs)
                phase = if (phase == DutyCyclePhase.BEACON) DutyCyclePhase.SCANNER else DutyCyclePhase.BEACON
                transitionToPhase(phase)
            }
        }
    }

    private fun stopDutyCycle() {
        dutyCycleJob?.cancel()
        dutyCycleJob = null
        Log.d(TAG, "Duty cycle stopped.")
    }

    // ── Sync State ────────────────────────────────────────────────────────────

    private data class SyncState(
        val expectedIds: MutableSet<String> = mutableSetOf(),
        val pendingPushes: MutableSet<Long> = mutableSetOf(),
        var manifestReceived: Boolean = false,
        var manifestSent: Boolean = false,
        // Set when handleReceivedEvent fires so checkSyncCompletion knows whether
        // this session actually transferred data — gates the post-sync wave so we
        // don't burn cycles waving after no-op manifest matches.
        var receivedNewEvent: Boolean = false
    )

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes() ?: return
                Log.d(TAG, "Payload received from $endpointId. Size: ${data.size}")
                handleIncomingPayload(endpointId, data)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val state = syncStates[endpointId]
                if (state != null && state.pendingPushes.remove(update.payloadId)) {
                    Log.d(TAG, "Push confirmed for payload ${update.payloadId} to $endpointId")
                    checkSyncCompletion(endpointId)
                }
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            handleConnectionInitiated(endpointId, connectionInfo.endpointName)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Mesh Link Established with $endpointId!")
                    pendingIncomingConnections.remove(endpointId)
                    connectionRetries.remove(endpointId)
                    _isLinking.value = false
                    _nearbyPeers.update { currentPeers ->
                        if (currentPeers.none { it.deviceId == endpointId }) {
                            // Peer not yet tracked (safety net for unexpected inbound connections)
                            val rssi = -60
                            currentPeers + Peer(
                                deviceId = endpointId,
                                callsign = endpointId,
                                connectionType = ConnectionType.BLE,
                                rssi = rssi,
                                physicalDistance = calculatePhysicalDistance(rssi),
                                normalizedDistance = normalizeDistanceForRadar(calculatePhysicalDistance(rssi)),
                                syncProgress = 0f,
                                lastSeen = System.currentTimeMillis(),
                                isConnected = true
                            )
                        } else {
                            currentPeers.map {
                                if (it.deviceId == endpointId) {
                                    it.copy(isConnected = true, lastSeen = System.currentTimeMillis())
                                } else it
                            }
                        }
                    }

                    // Both sides always initiate sync — removes the asymmetry where
                    // only ACTIVE sent a manifest. Previously, if the broadcaster's
                    // radio hadn't finished its async PASSIVE→ACTIVE transition by the
                    // time onConnectionResult fired, _status was still PASSIVE and
                    // neither side sent a manifest, leaving the connection idle.
                    Log.d(TAG, "Initiating Set Union sync with $endpointId")
                    syncScope.launch {
                        sendSyncManifest(endpointId)
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                    pendingIncomingConnections.remove(endpointId)
                    connectionRetries.remove(endpointId)
                    _isLinking.value = false
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error with $endpointId")
                    pendingIncomingConnections.remove(endpointId)
                    connectionRetries.remove(endpointId)
                    _isLinking.value = false
                }
                else -> {
                    Log.w(TAG, "Unknown connection status ${result.status.statusCode} with $endpointId")
                    pendingIncomingConnections.remove(endpointId)
                    connectionRetries.remove(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            _isLinking.value = false
            syncStates.remove(endpointId)
            refreshSyncingFlag()
            pendingIncomingConnections.remove(endpointId)
            connectionRetries.remove(endpointId)
            _nearbyPeers.update { currentPeers ->
                currentPeers.map {
                    if (it.deviceId == endpointId) it.copy(isConnected = false) else it
                }
            }
            // Re-arm the duty cycle once all sessions have cleared.
            // Handles two paths:
            //   (a) Clean sync via checkSyncCompletion → startProximityWave → togglePowerState
            //       → executePowerStateChange re-arms the cycle; startDutyCycle cancels-and-
            //       restarts safely, so double-arming is harmless.
            //   (b) Sticky-ACTIVE unexpected disconnects where startProximityWave is skipped
            //       (isStickyActive=true) — this is the only re-arm path for that case.
            if (pendingIncomingConnections.isEmpty() && syncStates.isEmpty() &&
                currentPowerState == MeshPowerState.ACTIVE) {
                Log.d(TAG, "All sessions cleared after $endpointId disconnect. Resuming duty cycle.")
                startDutyCycle(getDutyCycleInterval())
            }
        }
    }

    private fun handleConnectionInitiated(endpointId: String, endpointName: String) {
        Log.d(TAG, "Connection initiated with $endpointId ($endpointName). Auto-accepting...")
        _isLinking.value = true
        // Mark this endpoint as having an inbound connection in flight so that any
        // pending outbound debounce coroutine for the same endpoint will stand down.
        pendingIncomingConnections.add(endpointId)

        // Connection guard: cancel the duty cycle immediately so the radio holds its
        // current phase for the entire session. A mid-session BEACON↔SCANNER switch
        // briefly stops advertising or discovery while GATT is live, which causes the
        // MediaTek controller to drop the L2CAP link with ESTABLISH_GATT_CONNECTION_FAILED.
        stopDutyCycle()
        Log.d(TAG, "Duty cycle suspended — connection guard active for $endpointId")

        // Ensure the peer is visible in _nearbyPeers even if we never ran discovery
        // (critical for Passive nodes, which only advertise and never call startDiscovery).
        // Same callsign-dedup as processEndpointFound — see comment there for rationale.
        _nearbyPeers.update { currentPeers ->
            val deduped = currentPeers.filterNot {
                it.callsign == endpointName && !it.isConnected && it.deviceId != endpointId
            }
            if (deduped.none { it.deviceId == endpointId }) {
                val rssi = -60
                val dist = calculatePhysicalDistance(rssi)
                deduped + Peer(
                    deviceId = endpointId,
                    callsign = endpointName,
                    connectionType = ConnectionType.BLE,
                    rssi = rssi,
                    physicalDistance = dist,
                    normalizedDistance = normalizeDistanceForRadar(dist),
                    syncProgress = 0f,
                    lastSeen = System.currentTimeMillis(),
                    isConnected = false
                )
            } else {
                deduped
            }
        }

        if (endpointId == MOCK_PEER_ID) {
            debugScope.launch {
                delay(500)
                connectionLifecycleCallback.onConnectionResult(endpointId, ConnectionResolution(com.google.android.gms.common.api.Status(ConnectionsStatusCodes.STATUS_OK)))
            }
        } else {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: com.google.android.gms.nearby.connection.DiscoveredEndpointInfo) {
            // Capture baseline RSSI if available, otherwise default to -60dBm for calculation
            // Note: RSSI is not always exposed in standard connection callbacks, but we implement the hook here.
            val rssi = -60 // Baseline fallback
            processEndpointFound(endpointId, info.endpointName, rssi)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            _nearbyPeers.update { currentPeers ->
                currentPeers.filterNot { it.deviceId == endpointId }
            }
        }
    }

    private fun processEndpointFound(endpointId: String, endpointName: String, rssi: Int) {
        val physicalDist = calculatePhysicalDistance(rssi)
        val uiDist = normalizeDistanceForRadar(physicalDist)
        
        Log.d(TAG, "📡 Radar Update: Peer $endpointId signal is ${rssi}dBm (~${String.format("%.1f", physicalDist)} meters).")

        val newPeer = Peer(
            deviceId = endpointId,
            callsign = endpointName,
            connectionType = ConnectionType.BLE,
            rssi = rssi,
            physicalDistance = physicalDist,
            normalizedDistance = uiDist,
            syncProgress = 0f,
            lastSeen = System.currentTimeMillis(),
            isConnected = false
        )
        
        _nearbyPeers.update { currentPeers ->
            // Nearby Connections issues a fresh endpointId every time the peer's
            // advertising session restarts (which happens every BEACON phase of our
            // duty cycle). Without this collapse step, the same physical device
            // accumulates a new disconnected row each reconnect cycle — so the UI
            // shows "3 Peers" when really there's only one peer rediscovered three
            // times. Connected entries are never collapsed (the live session is
            // authoritative); only stale disconnected rows for the same callsign.
            val deduped = currentPeers.filterNot {
                it.callsign == endpointName && !it.isConnected && it.deviceId != endpointId
            }
            if (deduped.none { it.deviceId == endpointId }) deduped + newPeer
            else deduped
        }

        // Request connection if we are ACTIVE, but only after a random debounce window.
        // The debounce staggers simultaneous connection attempts when both peers are ACTIVE
        // and discover each other at the same moment, preventing L2CAP/GATT socket collisions
        // (STATUS_RADIO_ERROR 8007). After the delay, run a chain of guards before initiating
        // the outbound — discovery callbacks fire continuously for any peer in range, including
        // peers we already connected to, are mid-sync with, or just finished syncing with.
        if (_status.value == ConnectionStatus.ACTIVE) {
            syncScope.launch {
                val debounceMs = (100L..400L).random()
                Log.d(TAG, "Debouncing outbound connection to $endpointId for ${debounceMs}ms")
                delay(debounceMs)

                val cooldownAge = recentlySyncedPeers[endpointId]?.let { System.currentTimeMillis() - it }
                val cooldownRemaining = cooldownAge?.let { SYNC_COOLDOWN_MS - it }?.takeIf { it > 0 }
                val alreadyConnected = _nearbyPeers.value.any { it.deviceId == endpointId && it.isConnected }

                when {
                    pendingIncomingConnections.contains(endpointId) ->
                        Log.d(TAG, "Standing down outbound to $endpointId — inbound already in progress (they are the initiator).")
                    syncStates.containsKey(endpointId) ->
                        Log.d(TAG, "Standing down outbound to $endpointId — sync session already active.")
                    alreadyConnected ->
                        Log.d(TAG, "Standing down outbound to $endpointId — peer already connected.")
                    cooldownRemaining != null ->
                        Log.d(TAG, "Standing down outbound to $endpointId — sync cooldown active (${cooldownRemaining}ms remaining).")
                    else -> performRequestConnection(endpointId)
                }
            }
        }
    }

    private fun performRequestConnection(endpointId: String) {
        Log.d(TAG, "Requesting connection to $endpointId...")
        if (endpointId == MOCK_PEER_ID) {
            debugScope.launch {
                delay(500)
                handleConnectionInitiated(endpointId, MOCK_PEER_NAME)
            }
        } else {
            connectionsClient.requestConnection(localAdvertiseName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to request connection to $endpointId", e)
                    if (e is com.google.android.gms.common.api.ApiException) {
                        when (e.statusCode) {
                            ConnectionsStatusCodes.STATUS_RADIO_ERROR -> {
                                // Hardware radio fault (8007): stop scanning/advertising, wait 2s,
                                // then restart so the BT stack can recover its hardware state.
                                Log.e(TAG, "Radio Error (8007) on requestConnection to $endpointId. Resetting radio.")
                                resetRadioAndRestart()
                            }
                            else -> {
                                // Transient failure (GATT setup failed, brief unavailability, etc.).
                                // Retry with linear backoff; on the last retry the Nearby stack will
                                // attempt an alternate transport (WiFi Direct / hotspot) automatically.
                                val attempt = connectionRetries.getOrDefault(endpointId, 0)
                                if (attempt < MAX_CONNECTION_RETRIES) {
                                    connectionRetries[endpointId] = attempt + 1
                                    val backoffMs = 1000L * (attempt + 1)
                                    Log.w(TAG, "Connection to $endpointId failed (attempt ${attempt + 1}/$MAX_CONNECTION_RETRIES, code ${e.statusCode}). Retrying in ${backoffMs}ms.")
                                    syncScope.launch {
                                        delay(backoffMs)
                                        // Stand down if the peer has since initiated inbound
                                        if (!pendingIncomingConnections.contains(endpointId)) {
                                            performRequestConnection(endpointId)
                                        } else {
                                            Log.d(TAG, "Retry cancelled for $endpointId — inbound took over.")
                                            connectionRetries.remove(endpointId)
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Max retries ($MAX_CONNECTION_RETRIES) exhausted for $endpointId. Giving up.")
                                    connectionRetries.remove(endpointId)
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Stops advertising and discovery to clear bad hardware state after STATUS_RADIO_ERROR (8007),
     * then restarts them after a 2-second cooldown. Does NOT call stopAllEndpoints so that any
     * live sessions in progress are not prematurely terminated.
     */
    private fun resetRadioAndRestart() {
        syncScope.launch {
            stopDutyCycle()
            Log.i(TAG, "Radio soft-reset: stopping Discovery + Advertising...")
            try { connectionsClient.stopDiscovery() } catch (_: Exception) {}
            try { connectionsClient.stopAdvertising() } catch (_: Exception) {}
            delay(2000)
            Log.i(TAG, "Radio soft-reset complete. Restarting (state=$currentPowerState)...")
            when (currentPowerState) {
                MeshPowerState.ACTIVE -> startDutyCycle(getDutyCycleInterval())
                MeshPowerState.PASSIVE -> startAdvertising(lowPower = false)
                MeshPowerState.OFF -> { }
            }
        }
    }

    // ── Set Union Synchronization ──────────────────────────────────────────────

    private suspend fun generateLocalManifest(): SyncManifest {
        val events = eventRepository.getAllEvents().first()
        val items = events.map { event ->
            SyncItem.newBuilder()
                .setEventId(event.eventId)
                .setEventHash(event.timestamp.toString())
                .build()
        }
        Log.d(TAG, "Generated local manifest with ${items.size} items.")
        return SyncManifest.newBuilder().addAllItems(items).build()
    }

    private suspend fun sendSyncManifest(endpointId: String) {
        val manifest = generateLocalManifest()
        val payload = PigeonPayload.newBuilder().setManifest(manifest).build()
        val bytes = Payload.fromBytes(payload.toByteArray())
        
        // Mark manifest as sent in state
        syncStates.getOrPut(endpointId) { SyncState() }.manifestSent = true
        refreshSyncingFlag()
        
        Log.d(TAG, "Sending SyncManifest (${manifest.itemsCount} items) to $endpointId")
        if (endpointId == MOCK_PEER_ID) {
            debugScope.launch {
                delay(1000)
                simulateMockManifestResponse(endpointId)
            }
        } else {
            connectionsClient.sendPayload(endpointId, bytes)
        }
        checkSyncCompletion(endpointId)
    }

    private fun handleIncomingPayload(endpointId: String, data: ByteArray) {
        syncScope.launch {
            try {
                val pigeonPayload = PigeonPayload.parseFrom(data)
                when {
                    pigeonPayload.hasManifest() -> {
                        Log.d(TAG, "Received SyncManifest from $endpointId")
                        handleReceivedManifest(endpointId, pigeonPayload.manifest)
                    }
                    pigeonPayload.hasEvent() -> {
                        Log.d(TAG, "Received PigeonEvent from $endpointId: ${pigeonPayload.event.eventId}")
                        handleReceivedEvent(endpointId, pigeonPayload.event)
                    }
                    pigeonPayload.hasVerification() -> {
                        Log.d(TAG, "Received VerificationMessage from $endpointId for event ${pigeonPayload.verification.eventId}")
                        handleReceivedVerification(endpointId, pigeonPayload.verification)
                    }
                    else -> {
                        Log.w(TAG, "Received unknown payload type from $endpointId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse PigeonPayload from $endpointId", e)
            }
        }
    }

    private suspend fun handleReceivedManifest(endpointId: String, remoteManifest: SyncManifest) {
        val localEvents = eventRepository.getAllEvents().first()
        val localMap = localEvents.associateBy { it.eventId }
        val remoteMap = remoteManifest.itemsList.associateBy { it.eventId }

        // Push local events that are missing or newer on the peer
        var pushCount = 0
        for (localEvent in localEvents) {
            val remoteItem = remoteMap[localEvent.eventId]
            if (remoteItem == null || localEvent.timestamp > (remoteItem.eventHash.toLongOrNull() ?: 0L)) {
                pushEventToPeer(endpointId, localEvent)
                pushCount++
            }
        }
        Log.d(TAG, "Pushed $pushCount event(s) to $endpointId")

        // Push all local verifications — verifications are never in the manifest,
        // so we always push the full set; OnConflictStrategy.REPLACE makes this idempotent.
        val allVerifications = verificationDao.getAllVerifications()
        for (v in allVerifications) {
            pushVerificationToPeer(endpointId, v)
        }
        Log.d(TAG, "Pushed ${allVerifications.size} verification(s) to $endpointId")

        // Identify remote events we are missing or that are newer
        val missingIds = remoteMap.keys - localMap.keys
        val newerIds = remoteMap.filter { (id, item) ->
            val local = localMap[id]
            local != null && (item.eventHash.toLongOrNull() ?: 0L) > local.timestamp
        }.keys

        val needIds = missingIds + newerIds
        Log.d(TAG, "Need ${needIds.size} event(s) from $endpointId (${missingIds.size} missing, ${newerIds.size} newer)")

        // Track expected events
        val state = syncStates.getOrPut(endpointId) { SyncState() }
        state.manifestReceived = true
        state.expectedIds.addAll(needIds)
        refreshSyncingFlag()

        // Send our manifest back only if we haven't already — both sides now send
        // on connect, so this reciprocal send is only needed for peers that didn't
        // send first (shouldn't happen in practice, but kept as a safety net).
        if (!state.manifestSent) {
            sendSyncManifest(endpointId)
        } else {
            checkSyncCompletion(endpointId)
        }
    }

    private suspend fun pushEventToPeer(endpointId: String, event: Event) {
        val protoEvent = domainToProto(event)
        val pigeonPayload = PigeonPayload.newBuilder().setEvent(protoEvent).build()
        val payload = Payload.fromBytes(pigeonPayload.toByteArray())
        
        if (endpointId == MOCK_PEER_ID) {
            Log.d(TAG, "[MOCK] Would push event ${event.eventId} to $endpointId")
        } else {
            // Track the push
            syncStates[endpointId]?.pendingPushes?.add(payload.id)
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private fun pushVerificationToPeer(endpointId: String, entity: VerificationEntity) {
        if (endpointId == MOCK_PEER_ID) return
        val msg = VerificationMessage.newBuilder()
            .setId(entity.id)
            .setEventId(entity.eventId)
            .setSignerId(entity.signerId)
            .setIsConfirm(entity.isConfirm)
            .setTimestamp(entity.timestamp)
            .build()
        val payload = PigeonPayload.newBuilder().setVerification(msg).build()
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(payload.toByteArray()))
    }

    private suspend fun handleReceivedEvent(endpointId: String, protoEvent: PigeonEvent) {
        val domainEvent = protoToDomain(protoEvent)
        eventRepository.createEvent(domainEvent)

        // Remove from expected list and mark this session as having transferred data —
        // checkSyncCompletion uses this to decide whether to fire the relay wave.
        val state = syncStates[endpointId]
        state?.expectedIds?.remove(protoEvent.eventId)
        state?.receivedNewEvent = true

        Log.d(TAG, "Upserted event ${domainEvent.eventId}. Remaining expected: ${state?.expectedIds?.size ?: 0}")

        // Do NOT call startProximityWave() here — it calls stop() which kills the
        // active sync session for PASSIVE nodes. Relay wave fires in checkSyncCompletion
        // after the session cleanly closes.
        checkSyncCompletion(endpointId)
    }

    private suspend fun handleReceivedVerification(endpointId: String, msg: VerificationMessage) {
        val entity = VerificationEntity(
            id = msg.id,
            eventId = msg.eventId,
            signerId = msg.signerId,
            isConfirm = msg.isConfirm,
            timestamp = msg.timestamp
        )
        verificationDao.insertVerification(entity)
        Log.d(TAG, "Upserted verification for ${msg.eventId} from ${msg.signerId}")
        // Relay wave fires in checkSyncCompletion — not here, to avoid killing live sessions.
    }

    private fun checkSyncCompletion(endpointId: String) {
        val state = syncStates[endpointId] ?: return

        if (state.manifestReceived && state.manifestSent &&
            state.expectedIds.isEmpty() && state.pendingPushes.isEmpty()) {

            // Capture before remove() so we can gate the wave on whether this session
            // actually moved data. Empty manifest matches must NOT trigger a wave —
            // otherwise two idle peers ping-pong sync→wave→reconnect→empty-sync forever
            // and a freshly-reported event sits stranded waiting for a free moment.
            val didReceiveNew = state.receivedNewEvent
            Log.i(TAG, "✅ Sync Complete with $endpointId (newEvents=$didReceiveNew).")
            syncStates.remove(endpointId)
            refreshSyncingFlag()
            // Stamp the sync time so the discovery debounce won't immediately re-issue an
            // outbound to this peer while the BT controller is still tearing down the session.
            recentlySyncedPeers[endpointId] = System.currentTimeMillis()

            if (endpointId == MOCK_PEER_ID) {
                _nearbyPeers.update { currentPeers ->
                    currentPeers.map {
                        if (it.deviceId == endpointId) it.copy(isConnected = false) else it
                    }
                }
            } else {
                connectionsClient.disconnectFromEndpoint(endpointId)
                syncScope.launch {
                    userRepository.incrementSyncCount()
                }
            }

            // Relay wave: propagate received data to other nearby peers.
            // Only triggered after the session closes so stop() doesn't kill a live sync.
            // Skip if other sessions are still in progress, if the session was a no-op
            // (nothing to relay), or if the device is already sticky ACTIVE (handled inside).
            if (syncStates.isEmpty() && didReceiveNew) {
                startProximityWave()
            }
        }
    }

    // ── Proto ↔ Domain Converters ────────────────────────────────────────────

    private fun domainToProto(event: Event): PigeonEvent {
        val protoType = when (event.eventType) {
            EventType.FIRE -> com.example.pigeon.proto.EventType.FIRE
            EventType.MEDICAL -> com.example.pigeon.proto.EventType.MEDICAL
            EventType.SUPPLIES -> com.example.pigeon.proto.EventType.RESOURCE
            EventType.CONFLICT -> com.example.pigeon.proto.EventType.INFRASTRUCTURE
            EventType.CUSTOM -> com.example.pigeon.proto.EventType.CUSTOM
            EventType.SOS -> com.example.pigeon.proto.EventType.SOS
        }
        return PigeonEvent.newBuilder()
            .setEventId(event.eventId)
            .setEventType(protoType)
            .setDescription(event.description)
            .setLatitude(event.latitude)
            .setLongitude(event.longitude)
            .setTimestamp(event.timestamp)
            .setCreatorDeviceId(event.creatorDeviceId)
            .setIsResolved(event.isResolved)
            .setCreatorName(event.creatorName)
            .setTitle(event.title)
            .setExpiryTimestamp(event.expiryTimestamp)
            .build()
    }

    private fun protoToDomain(proto: PigeonEvent): Event {
        val domainType = when (proto.eventType) {
            com.example.pigeon.proto.EventType.FIRE -> EventType.FIRE
            com.example.pigeon.proto.EventType.MEDICAL -> EventType.MEDICAL
            com.example.pigeon.proto.EventType.RESOURCE -> EventType.SUPPLIES
            com.example.pigeon.proto.EventType.INFRASTRUCTURE -> EventType.CONFLICT
            com.example.pigeon.proto.EventType.CUSTOM -> EventType.CUSTOM
            com.example.pigeon.proto.EventType.SOS -> EventType.SOS
            else -> EventType.CUSTOM
        }
        val now = System.currentTimeMillis()
        val maxExpiry = now + (7 * 24 * 60 * 60 * 1000L) // 7 Days Hard Cap
        
        // Sanitation: Cap received expiry at 7 days from now
        val sanitizedExpiry = if (proto.expiryTimestamp == 0L) {
             now + 3600000 // Default 1H if missing
        } else {
             minOf(proto.expiryTimestamp, maxExpiry)
        }

        val safeTitle = if (proto.title.isBlank()) "Unknown Incident" else proto.title

        return Event(
            eventId = proto.eventId,
            creatorDeviceId = proto.creatorDeviceId,
            eventType = domainType,
            title = safeTitle,
            description = proto.description,
            latitude = proto.latitude,
            longitude = proto.longitude,
            timestamp = proto.timestamp,
            isResolved = proto.isResolved,
            creatorName = proto.creatorName,
            ttl = sanitizedExpiry - proto.timestamp,
            expiryTimestamp = sanitizedExpiry
        )
    }

    /** Simulates a mock peer sending us a SyncManifest with one event we don't have */
    private fun simulateMockManifestResponse(endpointId: String) {
        debugScope.launch {
            delay(1000)
            Log.d(TAG, "Simulating mock manifest response from $endpointId")
            val mockItem = SyncItem.newBuilder()
                .setEventId("MOCK_EVT_999")
                .setEventHash(System.currentTimeMillis().toString())
                .build()
            val mockManifest = SyncManifest.newBuilder().addItems(mockItem).build()
            val payload = PigeonPayload.newBuilder().setManifest(mockManifest).build()
            payloadCallback.onPayloadReceived(endpointId, Payload.fromBytes(payload.toByteArray()))

            delay(1000)
            Log.d(TAG, "Simulating mock event push from $endpointId")
            val mockEvent = PigeonEvent.newBuilder()
                .setEventId("MOCK_EVT_999")
                .setEventType(com.example.pigeon.proto.EventType.MEDICAL)
                .setDescription("MOCK: Oxygen requested at extraction point.")
                .setLatitude(33.8938)
                .setLongitude(35.5018)
                .setTimestamp(System.currentTimeMillis())
                .setCreatorDeviceId(MOCK_PEER_ID)
                .build()
            val eventPayload = PigeonPayload.newBuilder().setEvent(mockEvent).build()
            payloadCallback.onPayloadReceived(endpointId, Payload.fromBytes(eventPayload.toByteArray()))
        }
    }

    override fun simulateNearbyPeerFound() {
        val simulatedRssi = (-70..-45).random()
        Log.d(TAG, "Manual simulation triggered for $MOCK_PEER_ID with RSSI $simulatedRssi")
        processEndpointFound(MOCK_PEER_ID, MOCK_PEER_NAME, simulatedRssi)
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun togglePowerState(newState: MeshPowerState, isSticky: Boolean) {
        if (newState == MeshPowerState.ACTIVE && isSticky) {
            Log.d(TAG, "⚡ Power State: Upgrading to STICKY ACTIVE (User/Report initiated).")
            isStickyActive = true
            // Sticky ACTIVE supersedes any running wave — cancel the countdown.
            activeModeJob?.cancel()
            activeModeJob = null
            _isWaveActive.value = false
            originalPowerState = null
        } else if (newState != MeshPowerState.ACTIVE) {
            isStickyActive = false
            // Transitioning to PASSIVE/OFF — cancel any running wave so it can't
            // race against the new state and re-enable ACTIVE after reverting.
            activeModeJob?.cancel()
            activeModeJob = null
            _isWaveActive.value = false
            originalPowerState = null
        }

        radioJob?.cancel()
        radioJob = syncScope.launch {
            val now = System.currentTimeMillis()
            val elapsed = now - lastTransitionTime
            if (elapsed < 800 && newState != currentPowerState) {
                val waitTime = 800 - elapsed
                Log.d(TAG, "Debouncing radio transition to $newState (waiting ${waitTime}ms)")
                delay(waitTime)
            }

            executePowerStateChange(newState)
            lastTransitionTime = System.currentTimeMillis()
        }
    }

    private suspend fun executePowerStateChange(newState: MeshPowerState) {
        Log.d(TAG, "Executing PowerState change: $newState (Sticky: $isStickyActive)")
        currentPowerState = newState

        if (newState != MeshPowerState.OFF && !hasRequiredPermissions()) {
            Log.e(TAG, "Missing required permissions for Nearby Connections. Aborting state change.")
            return
        }

        // Only call stop if we are transitioning FROM an active state.
        // stop() fires stopAdvertising/stopDiscovery as fire-and-forget Tasks — the underlying
        // Nearby stack processes them asynchronously. A 200 ms settle gap lets the BT controller
        // drain its internal request queue before we issue a new startAdvertising/startDiscovery,
        // and the startAdvertising/startDiscovery failure listeners now treat 8001/8002 as
        // recoverable, so the gap can stay short without re-introducing the race.
        if (_status.value != ConnectionStatus.OFF) {
            stop()
            delay(200)
        }

        when (newState) {
            MeshPowerState.ACTIVE -> {
                // Use duty cycle instead of simultaneous dual-role to avoid 8007 on MediaTek.
                // Interval is 2 s for proximity waves (fast relay) and 5 s for sticky-active
                // (battery-conscious but still capable).
                Log.d(TAG, "Power ACTIVE. Starting duty-cycle Advertising/Discovery.")
                _status.value = ConnectionStatus.ACTIVE
                startDutyCycle(getDutyCycleInterval())
            }
            MeshPowerState.PASSIVE -> {
                // Single-role advertising never triggers 8007 — no duty cycle needed.
                // Use normal-power advertising (lowPower=false). The ~1280 ms BLE ad
                // interval that lowPower=true imposes leaves the GATT server too
                // sluggish to answer connection requests reliably — discovery worked,
                // but inbound requestConnection from an ACTIVE peer would time out
                // (~32 s) and fail with STATUS_RADIO_ERROR (8007). Normal-power's
                // ~158 ms interval keeps the channel responsive enough for the
                // ACTIVE peer's CONNECT to land. Battery cost is in the milliwatts.
                Log.d(TAG, "Power PASSIVE. Starting Advertising only (normal power).")
                _status.value = ConnectionStatus.PASSIVE
                stopDutyCycle()
                startAdvertising(lowPower = false)
            }
            MeshPowerState.OFF -> {
                Log.d(TAG, "Power OFF. Quiet mode engaged.")
                _status.value = ConnectionStatus.OFF
                stopDutyCycle()
            }
        }
    }

    private fun startAdvertising(lowPower: Boolean = true) {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .setLowPower(lowPower)
            .build()
        connectionsClient.startAdvertising(
            localAdvertiseName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started (lowPower=$lowPower).")
        }.addOnFailureListener { e ->
            if (e is com.google.android.gms.common.api.ApiException) {
                when (e.statusCode) {
                    ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING -> {
                        // Radio is already advertising — treat as success. The previous
                        // stopAdvertising() call from stop()/transitionToPhase() is async
                        // and may not have settled before startAdvertising() fired.
                        Log.w(TAG, "Already advertising (8001) — ignoring duplicate start, radio is healthy.")
                    }
                    else -> {
                        Log.e(TAG, "Failed to start advertising (code=${e.statusCode}).", e)
                        handleRadioError(e.statusCode, MeshPowerState.PASSIVE)
                        _status.value = ConnectionStatus.OFF
                    }
                }
            } else {
                Log.e(TAG, "Failed to start advertising.", e)
                _status.value = ConnectionStatus.OFF
            }
        }
    }

    private fun startDiscovery(lowPower: Boolean = false) {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .setLowPower(lowPower)
            .build()
        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started (lowPower=$lowPower).")
        }.addOnFailureListener { e ->
            if (e is com.google.android.gms.common.api.ApiException) {
                when (e.statusCode) {
                    ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING -> {
                        // Same race as 8001 — previous stopDiscovery() hadn't settled yet.
                        Log.w(TAG, "Already discovering (8002) — ignoring duplicate start, radio is healthy.")
                    }
                    else -> {
                        Log.e(TAG, "Failed to start discovery (code=${e.statusCode}).", e)
                        handleRadioError(e.statusCode, MeshPowerState.ACTIVE)
                        _status.value = ConnectionStatus.OFF
                    }
                }
            } else {
                Log.e(TAG, "Failed to start discovery.", e)
                _status.value = ConnectionStatus.OFF
            }
        }
    }

    private fun handleRadioError(errorCode: Int, retryState: MeshPowerState) {
        if (errorCode == ConnectionsStatusCodes.STATUS_RADIO_ERROR) {
            Log.e(TAG, "Critical Radio Error (8007) detected. Scheduling soft reset in 5s.")
            syncScope.launch {
                delay(5000)
                Log.i(TAG, "Attempting Radio Soft Reset...")
                stop()
                delay(1000)
                executePowerStateChange(retryState)
            }
        }
    }

    override fun broadcastIncident(event: PigeonEvent) {
        val reachablePeers = _nearbyPeers.value.filter { it.isConnected }
        Log.d(TAG, "📡 [BROADCAST] Immediate Incident: ${event.eventId} to ${reachablePeers.size} connected peers.")

        val payload = PigeonPayload.newBuilder().setEvent(event).build()
        val bytes = Payload.fromBytes(payload.toByteArray())

        reachablePeers.forEach { peer ->
            if (peer.deviceId != MOCK_PEER_ID) {
                Log.d(TAG, "   -> Sending bytes to ${peer.deviceId} (${peer.callsign})")
                connectionsClient.sendPayload(peer.deviceId, bytes)
            }
        }

        // After a sync we always disconnect, so the immediate hop above usually finds 0
        // connected peers. The new event still has to ride the *next* sync — clearing the
        // cooldown lets that sync happen on the next discovery callback (1–3 s) instead of
        // after the full SYNC_COOLDOWN_MS window. Without this clear, a user-reported event
        // can sit stranded for the entire cooldown period of every nearby peer.
        if (recentlySyncedPeers.isNotEmpty()) {
            Log.d(TAG, "📡 New incident — clearing ${recentlySyncedPeers.size} sync cooldown(s) for fast propagation.")
            recentlySyncedPeers.clear()
        }
    }

    override fun broadcastVerification(verification: VerificationMessage) {
        Log.d(TAG, "Broadcasting Verification: ${verification.id} for event ${verification.eventId}")
        val payload = PigeonPayload.newBuilder().setVerification(verification).build()
        val bytes = Payload.fromBytes(payload.toByteArray())
        val reachablePeers = _nearbyPeers.value.filter { it.isConnected }
        reachablePeers.forEach { peer ->
            if (peer.deviceId != MOCK_PEER_ID) {
                connectionsClient.sendPayload(peer.deviceId, bytes)
            }
        }
        // Same cooldown-clear as broadcastIncident — verifications need to propagate fast too.
        if (recentlySyncedPeers.isNotEmpty()) {
            recentlySyncedPeers.clear()
        }
        // Wake the radio. Unlike broadcastIncident — whose caller (ReportViewModel) already
        // toggles sticky-ACTIVE before broadcasting — verifyEvent does NOT touch power state.
        // Without this wave, a verification created on a PASSIVE device just sits in the
        // local DB forever: PASSIVE never discovers, so the next manifest exchange (which
        // is what actually carries verifications) never happens. The wave gives us a 60 s
        // ACTIVE window that's plenty to find a peer and push the verification.
        startProximityWave()
    }

    override fun startProximityWave() {
        // isStickyActive is set synchronously by togglePowerState before radioJob runs,
        // so it correctly short-circuits even during an in-flight PASSIVE→ACTIVE transition.
        if (isStickyActive) {
            Log.d(TAG, "Wave skipped — device is already in sticky ACTIVE mode.")
            return
        }

        // If OFF or PASSIVE, wake the radio up for the wave window.
        // Record the revert target only on the FIRST call (when no wave is running yet).
        if (currentPowerState == MeshPowerState.OFF || currentPowerState == MeshPowerState.PASSIVE) {
            if (activeModeJob?.isActive != true) {
                originalPowerState = currentPowerState
            }
            togglePowerState(MeshPowerState.ACTIVE, isSticky = false)
        }

        // KEY FIX: only start the 60 s timer if one is not already running.
        // Relay calls that arrive from checkSyncCompletion during an ongoing wave
        // must NOT reset the clock — the wave window is a fixed 60 s lifetime,
        // not a sliding one. Without this guard every completed sync extends the
        // wave indefinitely in a dense peer environment.
        if (activeModeJob?.isActive == true) {
            Log.d(TAG, "🌊 Wave already active — relay call acknowledged, timer continues.")
            return
        }

        _isWaveActive.value = true
        activeModeJob = syncScope.launch {
            delay(60000)
            transitionToPassiveMode()
        }
        Log.d(TAG, "🌊 Wave started — 60 s window begins now (duty cycle at ${getDutyCycleInterval()}ms/phase).")
    }

    /**
     * Called when the 60 s wave window expires.
     *
     * Safety valve: if a sync session is still in progress when the timer fires,
     * we poll in 1 s increments rather than immediately calling stop() and
     * disconnecting mid-transfer. Once all sessions clear we switch the radio
     * back to the pre-wave power state (typically PASSIVE).
     */
    private suspend fun transitionToPassiveMode() {
        var logged = false
        while (pendingIncomingConnections.isNotEmpty() || syncStates.isNotEmpty()) {
            if (!logged) {
                Log.d(TAG, "⏳ Wave expired but session still active — waiting for clean close...")
                logged = true
            }
            delay(1000)
        }
        if (logged) Log.d(TAG, "Session cleared. Proceeding with passive revert.")

        val revertState = originalPowerState ?: MeshPowerState.PASSIVE
        Log.d(TAG, "💤 Wave window closed. Reverting radio to $revertState.")
        _isWaveActive.value = false
        activeModeJob = null
        originalPowerState = null
        togglePowerState(revertState, isSticky = false)
    }

    override fun syncDeltas() {
        Log.d(TAG, "Initiating opportunistic sync deltas...")
        syncScope.launch {
            _nearbyPeers.value.filter { it.isConnected }.forEach { peer ->
                sendSyncManifest(peer.deviceId)
            }
        }
    }

    override fun stop() {
        Log.d(TAG, "Stopping NearbySyncManager radio active tasks.")
        activeModeJob?.cancel()
        activeModeJob = null
        _isWaveActive.value = false
        stopDutyCycle()
        try {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error during stop(): ${e.message}")
        }
        _isLinking.value = false
        syncStates.clear()
        refreshSyncingFlag()
        // Keep the peer list across mode transitions — wiping it here was hiding the
        // just-synced peer from PASSIVE nodes whenever the post-sync proximity wave
        // triggered a PASSIVE→ACTIVE flip (which calls stop() before re-arming the
        // radio). The TTL cleanup loop prunes anything older than PEER_TTL_MS.
        _nearbyPeers.update { peers -> peers.map { it.copy(isConnected = false) } }
    }

    /**
     * Physical Distance Formula:
     * Distance = 10 ^ ((MeasuredRSSI - ReferenceRSSI) / (-10 * PathLossExponent))
     * Ref: -50 dBm @ 1m, PathLossExponent: 2.5
     */
    private fun calculatePhysicalDistance(rssi: Int): Float {
        val refRssi = -50.0
        val n = 2.5
        return Math.pow(10.0, (rssi.toDouble() - refRssi) / (-10.0 * n)).toFloat()
    }

    /**
     * Maps physical distance (0-50m) to UI radius (0.0-1.0)
     */
    private fun normalizeDistanceForRadar(meters: Float): Float {
        val maxDisplayRange = 50f
        return (meters / maxDisplayRange).coerceIn(0.1f, 0.95f)
    }
}
