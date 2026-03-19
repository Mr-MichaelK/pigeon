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
import com.example.pigeon.proto.PigeonEvent
import com.google.android.gms.nearby.Nearby
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbySyncManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
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

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()
                Log.d(TAG, "Payload received from $endpointId. Size: ${data?.size ?: 0}")
                // TODO: Handle Protobuf parsing here
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
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

                    // Active device sends a "Hello" payload
                    if (_status.value == ConnectionStatus.ACTIVE) {
                        Log.d(TAG, "Sending Hello payload to $endpointId")
                        val helloPayload = Payload.fromBytes("Hello from $SERVICE_ID".toByteArray(Charsets.UTF_8))
                        if (endpointId == MOCK_PEER_ID) {
                            simulateMockPayload(endpointId)
                        } else {
                            connectionsClient.sendPayload(endpointId, helloPayload)
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
            processEndpointFound(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            _nearbyPeers.update { currentPeers ->
                currentPeers.filterNot { it.deviceId == endpointId }
            }
        }
    }

    private fun processEndpointFound(endpointId: String, endpointName: String) {
        Log.d(TAG, "Endpoint found: $endpointId ($endpointName)")
        val newPeer = Peer(
            deviceId = endpointId,
            callsign = endpointName,
            connectionType = ConnectionType.BLE,
            rssi = -50,
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

    /** Helper to simulate incoming payload from a mock peer */
    private fun simulateMockPayload(endpointId: String) {
        debugScope.launch {
            delay(2000)
            Log.d(TAG, "Simulating mock payload from $endpointId")
            
            val mockEvent = PigeonEvent.newBuilder()
                .setEventId("MOCK_EVT_999")
                .setEventType(com.example.pigeon.proto.EventType.MEDICAL)
                .setDescription("Test Mock Event: Oxygen requested at extraction point.")
                .setTimestamp(System.currentTimeMillis())
                .setCreatorDeviceId(MOCK_PEER_ID)
                .build()
            
            val payload = Payload.fromBytes(mockEvent.toByteArray())
            payloadCallback.onPayloadReceived(endpointId, payload)
        }
    }

    override fun simulateNearbyPeerFound() {
        Log.d(TAG, "Manual simulation triggered for $MOCK_PEER_ID")
        processEndpointFound(MOCK_PEER_ID, MOCK_PEER_NAME)
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

    override fun togglePowerState(newState: MeshPowerState) {
        Log.d(TAG, "Toggling PowerState to: $newState")
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
        // TODO: Convert to PigeonPayload and send to endpoints
    }

    override fun syncDeltas() {
        Log.d(TAG, "Initiating opportunistic sync deltas...")
        // TODO: Map current Room DB to SyncManifest and broadcast payload
    }

    override fun stop() {
        Log.d(TAG, "Stopping NearbySyncManager to clear radio resources.")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _nearbyPeers.value = emptyList()
    }
}
