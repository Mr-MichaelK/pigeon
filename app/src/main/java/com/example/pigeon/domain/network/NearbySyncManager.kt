package com.example.pigeon.domain.network

import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.proto.PigeonEvent
import kotlinx.coroutines.flow.StateFlow

interface NearbySyncManager {
    val status: StateFlow<ConnectionStatus>
    val nearbyPeers: StateFlow<List<Peer>>

    fun togglePowerState(newState: MeshPowerState)
    fun broadcastIncident(event: PigeonEvent)
    fun syncDeltas()
    fun stop()
    
    /** Debug hook: Simulation of a peer discovery and connection flow */
    fun simulateNearbyPeerFound()
}
