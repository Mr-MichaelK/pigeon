package com.example.pigeon.ui.screens.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.data.service.MeshServiceController
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.domain.network.ConnectionStatus
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject

data class RadarUiState(
    val powerState: MeshPowerState = MeshPowerState.OFF,
    val activePeers: List<Peer> = emptyList(),
    val historicalPeers: List<Peer> = emptyList(),
    val isScanning: Boolean = false,
    // Local user's current fix. Drives the radar's bearing/distance origin —
    // a peer is only drawable on the dial when both this and the peer's
    // PeerInfo lat/lng are present.
    val userLocation: LatLng? = null
)

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val nearbySyncManager: NearbySyncManager,
    private val meshController: MeshServiceController,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            nearbySyncManager.nearbyPeers.collectLatest { peers ->
                _uiState.update { it.copy(activePeers = peers) }
            }
        }

        viewModelScope.launch {
            nearbySyncManager.status.collectLatest { status ->
                val mappedState = when (status) {
                    is ConnectionStatus.ACTIVE -> MeshPowerState.ACTIVE
                    is ConnectionStatus.PASSIVE -> MeshPowerState.PASSIVE
                    else -> MeshPowerState.OFF
                }
                _uiState.update { it.copy(
                    powerState = mappedState,
                    isScanning = status != ConnectionStatus.OFF
                ) }
            }
        }

        viewModelScope.launch {
            locationRepository.userLocation.collectLatest { loc ->
                _uiState.update { it.copy(userLocation = loc) }
            }
        }
    }

    fun setPowerState(state: MeshPowerState) {
        // Power-state changes go through the service so the radio's lifetime is
        // owned by a foreground component (survives Doze and backgrounding).
        // The UI still observes nearbySyncManager.status for state syncing.
        meshController.setPowerState(state, isSticky = true)
    }

    fun triggerDebugDiscovery() {
        nearbySyncManager.simulateNearbyPeerFound()
    }
}
