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
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.proto.PigeonPayload
import com.example.pigeon.proto.SyncItem
import com.example.pigeon.proto.SyncManifest
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
    private val eventRepository: EventRepository
) : NearbySyncManager {

    private val TAG = "NearbySyncManager"
    private val SERVICE_ID = context.packageName
    private val MOCK_PEER_ID = "MOCK_PEER_001"
    private val MOCK_PEER_NAME = "Mock Responder"

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val debugScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.OFF)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _nearbyPeers = MutableStateFlow<List<Peer>>(emptyList())
    override val nearbyPeers: StateFlow<List<Peer>> = _nearbyPeers.asStateFlow()

    private var originalPowerState: MeshPowerState? = null
    private var wakeUpJob: Job? = null
    private var currentPowerState: MeshPowerState = MeshPowerState.OFF
    private var isStickyActive: Boolean = false

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncStates = mutableMapOf<String, SyncState>()

    private data class SyncState(
        val expectedIds: MutableSet<String> = mutableSetOf(),
        val pendingPushes: MutableSet<Long> = mutableSetOf(),
        var manifestReceived: Boolean = false,
        var manifestSent: Boolean = false
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
                    _nearbyPeers.update { currentPeers ->
                        currentPeers.map { 
                            if (it.deviceId == endpointId) it.copy(isConnected = true) else it 
                        }
                    }

                    // Active node initiates sync by sending its manifest
                    if (_status.value == ConnectionStatus.ACTIVE) {
                        Log.d(TAG, "Initiating Set Union sync with $endpointId")
                        syncScope.launch {
                            sendSyncManifest(endpointId)
                        }
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error with $endpointId")
                }
                else -> {
                    Log.w(TAG, "Unknown connection status ${result.status.statusCode} with $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            syncStates.remove(endpointId)
            _nearbyPeers.update { currentPeers ->
                currentPeers.map { 
                    if (it.deviceId == endpointId) it.copy(isConnected = false) else it 
                }
            }
        }
    }

    private fun handleConnectionInitiated(endpointId: String, endpointName: String) {
        Log.d(TAG, "Connection initiated with $endpointId ($endpointName). Auto-accepting...")
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
            if (currentPeers.none { it.deviceId == endpointId }) {
                currentPeers + newPeer
            } else {
                currentPeers
            }
        }

        // Immediately request connection if we are ACTIVE (initiator)
        if (_status.value == ConnectionStatus.ACTIVE) {
            performRequestConnection(endpointId)
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
            connectionsClient.requestConnection("PigeonNode", endpointId, connectionLifecycleCallback)
                .addOnFailureListener { Log.e(TAG, "Failed to request connection to $endpointId", it) }
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

        // Send our manifest back so the peer can also push what we need
        if (_status.value != ConnectionStatus.ACTIVE) {
            sendSyncManifest(endpointId)
        }
        checkSyncCompletion(endpointId)
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

    private suspend fun handleReceivedEvent(endpointId: String, protoEvent: PigeonEvent) {
        val domainEvent = protoToDomain(protoEvent)
        eventRepository.createEvent(domainEvent)
        
        // Remove from expected list
        val state = syncStates[endpointId]
        state?.expectedIds?.remove(protoEvent.eventId)
        
        Log.d(TAG, "Upserted event ${domainEvent.eventId}. Remaining expected: ${state?.expectedIds?.size ?: 0}")
        
        // Propagation Wave: relay received data onward
        startProximityWave()
        
        checkSyncCompletion(endpointId)
    }

    private fun checkSyncCompletion(endpointId: String) {
        val state = syncStates[endpointId] ?: return
        
        if (state.manifestReceived && state.manifestSent && 
            state.expectedIds.isEmpty() && state.pendingPushes.isEmpty()) {
            
            Log.i(TAG, "✅ Sync Complete with $endpointId. Racing to sleep...")
            
            if (endpointId == MOCK_PEER_ID) {
                // For mock, just update the flow
                _nearbyPeers.update { currentPeers ->
                    currentPeers.map { 
                        if (it.deviceId == endpointId) it.copy(isConnected = false) else it 
                    }
                }
            } else {
                connectionsClient.disconnectFromEndpoint(endpointId)
            }
            syncStates.remove(endpointId)
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
        return Event(
            eventId = proto.eventId,
            creatorDeviceId = proto.creatorDeviceId,
            eventType = domainType,
            title = proto.description.take(50),
            description = proto.description,
            latitude = proto.latitude,
            longitude = proto.longitude,
            timestamp = proto.timestamp,
            isResolved = proto.isResolved,
            ttl = 0L
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
            wakeUpJob?.cancel()
            wakeUpJob = null
            originalPowerState = null
        } else if (newState != MeshPowerState.ACTIVE) {
            isStickyActive = false
        }

        Log.d(TAG, "Toggling PowerState to: $newState (Sticky: $isStickyActive)")
        currentPowerState = newState
        if (newState != MeshPowerState.OFF && !hasRequiredPermissions()) {
            Log.e(TAG, "Missing required permissions for Nearby Connections. Aborting state change.")
            return
        }

        stop() // Ensure clean state before transitioning

        when (newState) {
            MeshPowerState.ACTIVE -> {
                Log.d(TAG, "Power ACTIVE. Starting Discovery.")
                _status.value = ConnectionStatus.ACTIVE
                startDiscovery()
            }
            MeshPowerState.PASSIVE -> {
                Log.d(TAG, "Power PASSIVE. Starting Advertising.")
                _status.value = ConnectionStatus.PASSIVE
                startAdvertising()
            }
            MeshPowerState.OFF -> {
                Log.d(TAG, "Power OFF. Stop completed.")
                _status.value = ConnectionStatus.OFF
            }
        }
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            "PigeonNode", SERVICE_ID, connectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully.")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising.", e)
            _status.value = ConnectionStatus.OFF
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully.")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery.", e)
            _status.value = ConnectionStatus.OFF
        }
    }

    override fun broadcastIncident(event: PigeonEvent) {
        Log.d(TAG, "Broadcasting Immediate Incident: ${event.eventId}")
        val payload = PigeonPayload.newBuilder().setEvent(event).build()
        val bytes = Payload.fromBytes(payload.toByteArray())
        _nearbyPeers.value.filter { it.isConnected }.forEach { peer ->
            if (peer.deviceId != MOCK_PEER_ID) {
                connectionsClient.sendPayload(peer.deviceId, bytes)
            }
        }
    }

    override fun startProximityWave() {
        Log.d(TAG, "🌊 Power State: Switching to TEMPORARY ACTIVE (Relay wave).")

        if (isStickyActive && currentPowerState == MeshPowerState.ACTIVE) {
            Log.d(TAG, "Wave active but already STICKY. No timer required.")
            return
        }
        
        // If mesh is OFF or PASSIVE, wake it up
        if (currentPowerState == MeshPowerState.OFF || currentPowerState == MeshPowerState.PASSIVE) {
            if (wakeUpJob == null) {
                originalPowerState = currentPowerState
            }
            togglePowerState(MeshPowerState.ACTIVE, isSticky = false)
        }

        // Reset/Start 60s timer
        wakeUpJob?.cancel()
        wakeUpJob = syncScope.launch {
            delay(60000)
            val revertState = originalPowerState ?: MeshPowerState.PASSIVE
            Log.d(TAG, "💤 Race to Sleep: Window expired, returning to $revertState.")
            togglePowerState(revertState, isSticky = false)
            wakeUpJob = null
            originalPowerState = null
        }
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
        Log.d(TAG, "Stopping NearbySyncManager to clear radio resources.")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _nearbyPeers.value = emptyList()
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
