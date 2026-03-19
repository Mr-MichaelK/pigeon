package com.example.pigeon.domain.model

enum class ConnectionType {
    BLE, WIFI_DIRECT, LORA
}

data class Peer(
    val deviceId: String,
    val callsign: String,
    val connectionType: ConnectionType,
    val rssi: Int, // Signal strength in dBm (e.g., -60)
    val physicalDistance: Float, // Calculated distance in meters
    val normalizedDistance: Float, // 0.0 to 1.0 for UI rendering
    val syncProgress: Float, // 0.0 to 1.0
    val lastSeen: Long, // Timestamp
    var isConnected: Boolean = false
)
