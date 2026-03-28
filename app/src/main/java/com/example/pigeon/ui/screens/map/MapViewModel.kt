package com.example.pigeon.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.model.MapMetadata
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.domain.repository.LocationRepository
import com.example.pigeon.domain.network.NearbySyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import org.maplibre.android.geometry.LatLng
import com.example.pigeon.ui.util.LocationUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
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
    val selectedEvent: Event? = null,
    val isWithinRadius: Boolean = false,
    val distanceMeters: Double? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository,
    private val nearbySyncManager: NearbySyncManager
) : ViewModel() {

    private val _metadata = MutableStateFlow(
        MapMetadata(
            latitude = 33.8938,
            longitude = 35.5018,
            zoom = 12.0
        )
    )

    private val _selectedEvent = MutableStateFlow<Event?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        _metadata,
        eventRepository.getAllEvents(),
        locationRepository.userLocation,
        _selectedEvent
    ) { metadata, events, userLoc, selectedEvent ->
        val distance = if (userLoc != null && selectedEvent != null) {
            LocationUtils.calculateDistance(userLoc.latitude, userLoc.longitude, selectedEvent.latitude, selectedEvent.longitude)
        } else null
        
        val isQualified = distance != null && distance <= 500.0
        
        MapUiState(
            metadata = metadata,
            events = events,
            selectedEvent = selectedEvent,
            isWithinRadius = isQualified,
            distanceMeters = distance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        // Mock data population disabled for persistent operation
    }

    fun onMapMoved(latitude: Double, longitude: Double, zoom: Double) {
        _metadata.value = _metadata.value.copy(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom
        )
    }

    fun onResolveEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.resolveEvent(eventId)
            _selectedEvent.value = null // Dismiss sheet on resolve
        }
    }

    fun updateLocation(location: LatLng) {
        locationRepository.updateLocation(location)
    }

    fun onEventSelected(event: Event?) {
        _selectedEvent.value = event
    }
}
