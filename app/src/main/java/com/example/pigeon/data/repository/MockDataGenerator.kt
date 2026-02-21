package com.example.pigeon.data.repository

import com.example.pigeon.data.local.entities.EventEntity
import com.example.pigeon.domain.model.EventType
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class MockDataGenerator @Inject constructor() {

    fun generateMockEvents(): List<EventEntity> {
        val events = mutableListOf<EventEntity>()
        val connectionDeviceIds = listOf("NODE-A1B2", "NODE-C3D4", "NODE-E5F6", "NODE-G7H8")
        val baseLat = 33.8938 // Beirut roughly
        val baseLon = 35.5018

        // Generate 15 mock events
        for (i in 1..15) {
            val type = EventType.values().random()
            val isResolved = Random.nextBoolean()
            // Random time within last 48 hours
            val timestamp = System.currentTimeMillis() - Random.nextLong(0, 48 * 60 * 60 * 1000)
            
            events.add(
                EventEntity(
                    eventId = UUID.randomUUID().toString(),
                    creatorDeviceId = connectionDeviceIds.random(),
                    eventType = type,
                    title = generateTitle(type),
                    description = generateDescription(type),
                    latitude = baseLat + (Random.nextDouble() - 0.5) * 0.05,
                    longitude = baseLon + (Random.nextDouble() - 0.5) * 0.05,
                    timestamp = timestamp,
                    isResolved = isResolved,
                    ttl = 72 * 60 * 60 * 1000 // 72 hours
                )
            )
        }
        return events
    }

    private fun generateTitle(type: EventType): String {
        return when (type) {
            EventType.WATER -> "Water Supply Issue"
            EventType.CONFLICT -> "Conflict Reported"
            EventType.MEDICAL -> "Medical Assistance Needed"
            EventType.SOS -> "SOS Signal Detected"
            EventType.FIRE_HAZARD -> "Fire Hazard Warning"
        }
    }

    private fun generateDescription(type: EventType): String {
        return when (type) {
            EventType.WATER -> "Clean water source reported contaminated."
            EventType.CONFLICT -> "Avoid sector 4 due to ongoing activity."
            EventType.MEDICAL -> "Injured civilian requires immediate transport."
            EventType.SOS -> "Weak signal detected from sector 7."
            EventType.FIRE_HAZARD -> "Dry brush fire reported near checkpoint."
        }
    }
}
