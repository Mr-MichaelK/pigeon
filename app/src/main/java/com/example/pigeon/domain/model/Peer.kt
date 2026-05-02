package com.example.pigeon.domain.model

enum class ConnectionType {
    BLE, WIFI_DIRECT, LORA
}

data class Peer(
    val deviceId: String,
    val callsign: String,
    val connectionType: ConnectionType,
    val syncProgress: Float,
    val lastSeen: Long,
    var isConnected: Boolean = false,
    // Last known geographic position reported by the peer over a connected
    // Nearby link (PeerInfo proto message). Null until the peer has both
    // (a) acquired a LOCKED fix on their side and (b) sent us a PeerInfo.
    // Drives radar bearing/distance — peers without lat/lng aren't drawn on
    // the dial, only listed below.
    val latitude: Double? = null,
    val longitude: Double? = null
)
