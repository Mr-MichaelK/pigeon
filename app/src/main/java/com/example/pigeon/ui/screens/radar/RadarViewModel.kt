package com.example.pigeon.ui.screens.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.ConnectionType
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.domain.network.ConnectionStatus
import com.example.pigeon.domain.network.NearbySyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadarUiState(
    val powerState: MeshPowerState = MeshPowerState.OFF,
    val activePeers: List<Peer> = emptyList(),
    val historicalPeers: List<Peer> = emptyList(),
    val isScanning: Boolean = false
)

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val nearbySyncManager: NearbySyncManager
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
    }

    fun setPowerState(state: MeshPowerState) {
        // We only call the manager; the UI will sync via the status Flow collected in init {}
        nearbySyncManager.togglePowerState(state, isSticky = true)
    }

    fun triggerDebugDiscovery() {
        nearbySyncManager.simulateNearbyPeerFound()
    }

    override fun onCleared() {
        super.onCleared()
        nearbySyncManager.stop()
    }
}
