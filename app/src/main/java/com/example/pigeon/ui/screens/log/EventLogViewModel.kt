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
    ALL, UNRESOLVED, NEARBY
}

@HiltViewModel
class EventLogViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventLogUiState(isLoading = true))
    val uiState: StateFlow<EventLogUiState> = _uiState.asStateFlow()

    init {
        // Populate mock data on first launch (logic can be refined)
        viewModelScope.launch {
            eventRepository.populateMockData()
            loadEvents()
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.flatMapLatest { state ->
                getEventsUseCase(state.selectedFilter, state.searchQuery)
            }.collect { filteredEvents ->
                _uiState.update { it.copy(events = filteredEvents, isLoading = false) }
            }
        }
    }

    fun onFilterSelected(filter: EventFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun onResolveEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.resolveEvent(eventId)
        }
    }
}
