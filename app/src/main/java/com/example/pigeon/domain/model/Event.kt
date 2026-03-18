package com.example.pigeon.domain.model

enum class EventType {
    FIRE, MEDICAL, SUPPLIES, CONFLICT, CUSTOM
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
)
