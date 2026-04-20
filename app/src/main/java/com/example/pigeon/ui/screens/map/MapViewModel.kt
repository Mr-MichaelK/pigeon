package com.example.pigeon.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.MapMetadata
import com.example.pigeon.domain.model.TrustScore
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.domain.repository.LocationRepository
import com.example.pigeon.domain.repository.VerificationRepository
import com.example.pigeon.domain.network.NearbySyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import org.maplibre.android.geometry.LatLng
import com.example.pigeon.ui.util.LocationUtils
import kotlinx.coroutines.flow.*
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
    val trustScores: Map<String, TrustScore> = emptyMap(),
    val selectedEvent: Event? = null,
    val isWithinRadius: Boolean = false,
    val distanceMeters: Double? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository,
    private val verificationRepository: VerificationRepository,
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

    private val _pendingCameraTarget = MutableStateFlow<LatLng?>(null)
    val pendingCameraTarget: StateFlow<LatLng?> = _pendingCameraTarget.asStateFlow()

    private val _isWizardLoading = MutableStateFlow(false)
    val isWizardLoading: StateFlow<Boolean> = _isWizardLoading

    fun setWizardLoading(isLoading: Boolean) {
        _isWizardLoading.value = isLoading
    }

    val meshStatus: StateFlow<com.example.pigeon.domain.network.ConnectionStatus> = nearbySyncManager.status
    val peerCount: StateFlow<Int> = nearbySyncManager.nearbyPeers
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MapUiState> = combine(
        _metadata,
        eventRepository.getAllEvents(),
        locationRepository.userLocation,
        _selectedEvent
    ) { metadata, events, userLoc, selectedEvent ->
        metadata to events to userLoc to selectedEvent
    }.flatMapLatest { (triple, selectedEvent) ->
        val (pair, userLoc) = triple
        val (metadata, events) = pair
        
        // Combine all event trust scores into a single flow
        val trustFlows = events.map { event ->
            verificationRepository.getTrustScore(event.eventId).map { score ->
                event.eventId to score
            }
        }
        
        if (trustFlows.isEmpty()) {
            flowOf(emptyMap<String, TrustScore>()).map { trustMap ->
                buildMapUiState(metadata, events, trustMap, userLoc, selectedEvent)
            }
        } else {
            combine(trustFlows) { arrays ->
                arrays.toMap()
            }.map { trustMap ->
                buildMapUiState(metadata, events, trustMap, userLoc, selectedEvent)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    private fun buildMapUiState(
        metadata: MapMetadata,
        events: List<Event>,
        trustMap: Map<String, TrustScore>,
        userLoc: LatLng?,
        selectedEvent: Event?
    ): MapUiState {
        val distance = if (userLoc != null && selectedEvent != null) {
            LocationUtils.calculateDistance(userLoc.latitude, userLoc.longitude, selectedEvent.latitude, selectedEvent.longitude)
        } else null
        
        val isQualified = distance != null && distance <= 500.0
        
        return MapUiState(
            metadata = metadata,
            events = events,
            trustScores = trustMap,
            selectedEvent = selectedEvent,
            isWithinRadius = isQualified,
            distanceMeters = distance
        )
    }

    init {
        // Task 8.6: Auto-activate mesh to PASSIVE (Advertising) on startup
        // This ensures the device is visible to others immediately,
        // without overriding an existing user-initiated ACTIVE state.
        if (nearbySyncManager.status.value == com.example.pigeon.domain.network.ConnectionStatus.OFF) {
            nearbySyncManager.togglePowerState(com.example.pigeon.domain.model.MeshPowerState.PASSIVE, isSticky = false)
        }
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

    fun onEventSelectedById(eventId: String) {
        val event = uiState.value.events.find { it.eventId == eventId }
        _selectedEvent.value = event
    }

    fun consumeDeepLinkEventId(eventId: String) {
        // Wait for events to be available and then select + signal camera
        viewModelScope.launch {
            // Retry briefly in case events haven't loaded yet
            var tries = 0
            var event = uiState.value.events.find { it.eventId == eventId }
            while (event == null && tries < 10) {
                kotlinx.coroutines.delay(200)
                event = uiState.value.events.find { it.eventId == eventId }
                tries++
            }
            if (event != null) {
                _selectedEvent.value = event
                _pendingCameraTarget.value = LatLng(event.latitude, event.longitude)
            }
        }
    }

    fun clearPendingCameraTarget() {
        _pendingCameraTarget.value = null
    }
}
