package com.example.pigeon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EventType {
    WATER, CONFLICT, MEDICAL, SOS, FIRE_HAZARD
}

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val creatorDeviceId: String,
    val eventType: EventType,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isResolved: Boolean = false,
    val ttl: Long // Time To Live in milliseconds
)
