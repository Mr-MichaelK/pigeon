package com.example.pigeon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String,
    val creatorDeviceId: String,
    val eventType: EventType,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isResolved: Boolean = false,
    val ttl: Long
)

fun EventEntity.toDomain(): Event = Event(
    eventId = eventId,
    creatorDeviceId = creatorDeviceId,
    eventType = eventType,
    title = title,
    description = description,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    isResolved = isResolved,
    ttl = ttl
)

fun Event.toEntity(): EventEntity = EventEntity(
    eventId = eventId,
    creatorDeviceId = creatorDeviceId,
    eventType = eventType,
    title = title,
    description = description,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    isResolved = isResolved,
    ttl = ttl
)
