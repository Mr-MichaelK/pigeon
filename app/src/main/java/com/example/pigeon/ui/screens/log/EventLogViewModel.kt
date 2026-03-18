package com.example.pigeon.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.domain.usecase.EventFilter
import com.example.pigeon.domain.usecase.GetEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventLogUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: EventFilter = EventFilter.ALL,
    val searchQuery: String = ""
)

enum class EventFilter {
    ALL, UNRESOLVED
}

@HiltViewModel
class EventLogViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(EventFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EventLogUiState> = combine(
        _selectedFilter,
        _searchQuery
    ) { filter, query ->
        filter to query
    }.flatMapLatest { (filter, query) ->
        getEventsUseCase(filter, query).map { events ->
            EventLogUiState(
                events = events,
                isLoading = false,
                selectedFilter = filter,
                searchQuery = query
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventLogUiState(isLoading = true)
    )

    init {
        // Initial state is reactive via uiState definition
    }

    fun onFilterSelected(filter: EventFilter) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    fun onResolveEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.resolveEvent(eventId)
        }
    }
}
