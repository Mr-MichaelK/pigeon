package com.example.pigeon.ui.screens.map

import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.repository.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val eventRepository: EventRepository = mock()
    private lateinit var viewModel: MapViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(eventRepository.getAllEvents()).thenReturn(flowOf(emptyList()))
        viewModel = MapViewModel(eventRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reportEvent calls repository createEvent with mapped data`() = runTest(testDispatcher) {
        val type = EventType.FIRE
        val title = "Test Fire"
        val desc = "Test Description"
        val ttl = 3600000L // 1h

        viewModel.reportEvent(type, title, desc, ttl)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that createEvent was called with an event containing the right fields
        verify(eventRepository).createEvent(argThat {
            this.eventType == type &&
            this.title == title &&
            this.description == desc &&
            this.ttl == ttl
        })
    }
}
