package com.example.pigeon.domain.network

import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.proto.VerificationMessage
import kotlinx.coroutines.flow.StateFlow

interface NearbySyncManager {
    val status: StateFlow<ConnectionStatus>
    val nearbyPeers: StateFlow<List<Peer>>

    fun togglePowerState(newState: MeshPowerState, isSticky: Boolean = false)
    fun broadcastIncident(event: PigeonEvent)
    fun broadcastVerification(verification: VerificationMessage)
    fun startProximityWave()
    fun syncDeltas()
    fun stop()
    
    /** Debug hook: Simulation of a peer discovery and connection flow */
    fun simulateNearbyPeerFound()
}
