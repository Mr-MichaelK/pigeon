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
        val mockNames = listOf("Ghost-7", "Viper-2", "Echo-9", "Alpha-5", "Bravo-3", "Shadow-1")
        val baseLat = 33.8938 // Beirut roughly
        val baseLon = 35.5018

        // Generate 5 mock events
        for (i in 1..5) {
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
                    creatorName = mockNames.random(),
                    ttl = 72 * 60 * 60 * 1000
                )
            )
        }
        return events
    }

    private fun generateTitle(type: EventType): String {
        return when (type) {
            EventType.FIRE -> "Fire Hazard Warning"
            EventType.MEDICAL -> "Medical Assistance"
            EventType.SUPPLIES -> "Package Supply Spotted"
            EventType.CONFLICT -> "Conflict Alert"
            EventType.SOS -> "SOS SIGNAL"
            EventType.CUSTOM -> "POI Recorded"
        }
    }

    private fun generateDescription(type: EventType): String {
        return when (type) {
            EventType.FIRE -> "Containment required in sector 3."
            EventType.MEDICAL -> "Casualties reported near checkpoint."
            EventType.SUPPLIES -> "Rations and water surplus spotted."
            EventType.CONFLICT -> "Ongoing engagement in central sector."
            EventType.SOS -> "CRITICAL EMERGENCY - IMMEDIATE ASSISTANCE REQUIRED."
            EventType.CUSTOM -> "Manual observation logged."
        }
    }
}
