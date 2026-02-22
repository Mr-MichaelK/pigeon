package com.example.pigeon.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.MapMetadata
import com.example.pigeon.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val metadata: MapMetadata = MapMetadata(
        latitude = 33.8938, // Default to Lebanon (Beirut)
        longitude = 35.5018,
        zoom = 12.0,
        meshStatus = "Passive",
        lastSyncMinutes = 5
    ),
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _metadata = MutableStateFlow(
        MapMetadata(
            latitude = 33.8938,
            longitude = 35.5018,
            zoom = 12.0
        )
    )

    val uiState: StateFlow<MapUiState> = combine(
        _metadata,
        eventRepository.getAllEvents()
    ) { metadata, events ->
        MapUiState(
            metadata = metadata,
            events = events
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        viewModelScope.launch {
            // Populate mock data if database is empty or for demo purposes
            eventRepository.populateMockData()
        }
    }

    fun onMapMoved(latitude: Double, longitude: Double, zoom: Double) {
        _metadata.value = _metadata.value.copy(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom
        )
    }
}
