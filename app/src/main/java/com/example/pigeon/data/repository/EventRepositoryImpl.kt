package com.example.pigeon.data.repository

import com.example.pigeon.data.local.dao.EventDao
import com.example.pigeon.data.local.entities.toDomain
import com.example.pigeon.data.local.entities.toEntity
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.repository.EventRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val mockDataGenerator: MockDataGenerator
) : EventRepository {

    override fun getAllEvents(): Flow<List<Event>> = 
        eventDao.getAllEvents().map { entities -> entities.map { it.toDomain() } }

    override fun getUnresolvedEvents(): Flow<List<Event>> = 
        eventDao.getUnresolvedEvents().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createEvent(event: Event) {
        eventDao.insertEvent(event.toEntity())
    }

    override suspend fun resolveEvent(eventId: String) {
        eventDao.updateEventStatus(eventId, true)
    }
    
    override suspend fun searchEvents(query: String): List<Event> {
        return eventDao.searchEvents(query).map { it.toDomain() }
    }

    override suspend fun populateMockData() {
        val mockEntities = mockDataGenerator.generateMockEvents()
        eventDao.insertEvents(mockEntities)
    }
}
