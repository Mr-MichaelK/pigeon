package com.example.pigeon.domain.usecase

import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.repository.EventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetEventsUseCaseTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var getEventsUseCase: GetEventsUseCase

    @Before
    fun setup() {
        eventRepository = mock()
        getEventsUseCase = GetEventsUseCase(eventRepository)
    }

    @Test
    fun `invoke with ALL filter returns all events`() = runTest {
        val events = listOf(
            createEvent("1", isResolved = false),
            createEvent("2", isResolved = true)
        )
        whenever(eventRepository.getAllEvents()).thenReturn(flowOf(events))

        val result = getEventsUseCase(EventFilter.ALL).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `invoke with UNRESOLVED filter returns only unresolved events`() = runTest {
        val events = listOf(
            createEvent("1", isResolved = false),
            createEvent("2", isResolved = true)
        )
        whenever(eventRepository.getAllEvents()).thenReturn(flowOf(events))

        val result = getEventsUseCase(EventFilter.UNRESOLVED).first()

        assertEquals(1, result.size)
        assertEquals("1", result[0].eventId)
    }

    @Test
    fun `invoke with search query returns matching events`() = runTest {
        val events = listOf(
            createEvent("1", title = "Water Issue"),
            createEvent("2", title = "Medical Need")
        )
        whenever(eventRepository.getAllEvents()).thenReturn(flowOf(events))

        val result = getEventsUseCase(query = "water").first()

        assertEquals(1, result.size)
        assertEquals("1", result[0].eventId)
    }

    private fun createEvent(
        id: String,
        isResolved: Boolean = false,
        title: String = "Title",
        description: String = "Description"
    ) = Event(
        eventId = id,
        creatorDeviceId = "device",
        eventType = EventType.MEDICAL,
        title = title,
        description = description,
        latitude = 0.0,
        longitude = 0.0,
        timestamp = System.currentTimeMillis(),
        isResolved = isResolved,
        ttl = 1000
    )
}
