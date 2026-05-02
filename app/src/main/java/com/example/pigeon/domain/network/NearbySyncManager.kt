package com.example.pigeon.domain.network

import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.proto.VerificationMessage
import kotlinx.coroutines.flow.StateFlow

interface NearbySyncManager {
    val status: StateFlow<ConnectionStatus>
    val nearbyPeers: StateFlow<List<Peer>>
    /** True while a connection handshake is in-flight (onConnectionInitiated fired, result not yet received) */
    val isLinking: StateFlow<Boolean>
    /** True while a timed proximity wave (relay or post-report) is counting down its 60 s window. */
    val isWaveActive: StateFlow<Boolean>
    /** True while at least one Set Union sync session is in progress. Reflects manifest/payload exchange,
     *  not just the underlying socket — used by the UI to show a "SYNCING WITH PEER" indicator on both
     *  ACTIVE and PASSIVE sides. */
    val isSyncing: StateFlow<Boolean>

    fun togglePowerState(newState: MeshPowerState, isSticky: Boolean = false)
    fun broadcastIncident(event: PigeonEvent)
    fun broadcastVerification(verification: VerificationMessage)
    fun startProximityWave()
    fun syncDeltas()
    fun stop()

    /** Debug hook: Simulation of a peer discovery and connection flow */
    fun simulateNearbyPeerFound()
}
