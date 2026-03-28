package com.example.pigeon.domain.model

enum class EventType {
    FIRE, MEDICAL, SUPPLIES, CONFLICT, CUSTOM, SOS
}

/**
 * Domain model representing a mesh event (incident or report).
 */
data class Event(
    val eventId: String,
    val creatorDeviceId: String,
    val eventType: EventType,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isResolved: Boolean,
    val ttl: Long
) {
    /**
     * Checks if this event is within a specific radius of a LatLng coordinate.
     */
    fun isWithinRange(userLat: Double, userLon: Double, radius: Double = 500.0): Boolean {
        // Haversine implementation from LocationUtils logic (avoiding circular dependency)
        val r = 6371000.0
        val phi1 = userLat * Math.PI / 180.0
        val phi2 = this.latitude * Math.PI / 180.0
        val deltaPhi = (this.latitude - userLat) * Math.PI / 180.0
        val deltaLambda = (this.longitude - userLon) * Math.PI / 180.0

        val a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0)
        val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
        return r * c <= radius
    }
}
