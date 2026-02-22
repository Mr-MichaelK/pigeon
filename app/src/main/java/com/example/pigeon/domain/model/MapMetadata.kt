package com.example.pigeon.domain.model

/**
 * Domain model for current viewport/location data.
 */
data class MapMetadata(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val meshStatus: String = "Passive",
    val lastSyncMinutes: Int = 0
)
