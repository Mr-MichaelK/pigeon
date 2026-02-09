package com.example.pigeon.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.data.local.entities.EventEntity
import com.example.pigeon.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventLogUiState(
    val events: List<EventEntity> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: EventFilter = EventFilter.ALL,
    val searchQuery: String = ""
)

enum class EventFilter {
    ALL, UNRESOLVED, NEARBY
}

@HiltViewModel
class EventLogViewModel @Inject constructor(
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
            combine(
                eventRepository.getAllEvents(),
                _uiState.map { it.selectedFilter },
                _uiState.map { it.searchQuery }
            ) { events, filter, query ->
                var filtered = events
                
                // Apply Filter
                if (filter == EventFilter.UNRESOLVED) {
                    filtered = filtered.filter { !it.isResolved }
                }
                // Nearby filter would need location logic, skipping for now layout
                
                // Apply Search
                if (query.isNotEmpty()) {
                    filtered = filtered.filter { 
                        it.title.contains(query, ignoreCase = true) || 
                        it.description.contains(query, ignoreCase = true) 
                    }
                }
                
                filtered
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
