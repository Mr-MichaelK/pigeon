package com.example.pigeon.domain.usecase

import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

enum class EventFilter {
    ALL, UNRESOLVED
}

class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(
        filter: EventFilter = EventFilter.ALL,
        query: String = ""
    ): Flow<List<Event>> {
        return eventRepository.getAllEvents().combine(
            kotlinx.coroutines.flow.flowOf(filter)
        ) { events, currentFilter ->
            when (currentFilter) {
                EventFilter.ALL -> events
                EventFilter.UNRESOLVED -> events.filter { !it.isResolved }
            }
        }.combine(kotlinx.coroutines.flow.flowOf(query)) { events, currentQuery ->
            if (currentQuery.isBlank()) {
                events
            } else {
                events.filter {
                    it.title.contains(currentQuery, ignoreCase = true) ||
                    it.description.contains(currentQuery, ignoreCase = true)
                }
            }
        }
    }
}
