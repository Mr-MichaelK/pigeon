package com.example.pigeon.data.repository

import com.example.pigeon.data.local.dao.EventDao
import com.example.pigeon.data.local.entities.EventEntity
import com.example.pigeon.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val mockDataGenerator: MockDataGenerator
) : EventRepository {

    override fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()

    override fun getUnresolvedEvents(): Flow<List<EventEntity>> = eventDao.getUnresolvedEvents()

    override suspend fun createEvent(event: EventEntity) {
        eventDao.insertEvent(event)
    }

    override suspend fun resolveEvent(eventId: String) {
        eventDao.updateEventStatus(eventId, true)
    }
    
    override suspend fun searchEvents(query: String): List<EventEntity> {
        return eventDao.searchEvents(query)
    }

    override suspend fun populateMockData() {
        val mockEvents = mockDataGenerator.generateMockEvents()
        eventDao.insertEvents(mockEvents)
    }
}
