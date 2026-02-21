package com.example.pigeon.domain.repository

import com.example.pigeon.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getAllEvents(): Flow<List<Event>>
    fun getUnresolvedEvents(): Flow<List<Event>>
    suspend fun createEvent(event: Event)
    suspend fun resolveEvent(eventId: String)
    suspend fun searchEvents(query: String): List<Event>
    suspend fun populateMockData()
    suspend fun clearAllEvents()
}
