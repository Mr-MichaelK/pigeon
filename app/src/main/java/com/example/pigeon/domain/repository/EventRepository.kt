package com.example.pigeon.domain.repository

import com.example.pigeon.data.local.entities.EventEntity
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getAllEvents(): Flow<List<EventEntity>>
    fun getUnresolvedEvents(): Flow<List<EventEntity>>
    suspend fun createEvent(event: EventEntity)
    suspend fun resolveEvent(eventId: String)
    suspend fun searchEvents(query: String): List<EventEntity>
    suspend fun populateMockData()
}
