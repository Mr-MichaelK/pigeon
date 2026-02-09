package com.example.pigeon.ui.screens.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.ConnectionType
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class RadarUiState(
    val powerState: MeshPowerState = MeshPowerState.PASSIVE,
    val activePeers: List<Peer> = emptyList(),
    val historicalPeers: List<Peer> = emptyList(),
    val isScanning: Boolean = false
)

@HiltViewModel
class RadarViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    private var scanJob: kotlinx.coroutines.Job? = null

    fun setPowerState(state: MeshPowerState) {
        val previousState = _uiState.value.powerState
        _uiState.update { it.copy(powerState = state) }
        
        if (state == MeshPowerState.OFF) {
            stopScanning()
            _uiState.update { it.copy(activePeers = emptyList(), isScanning = false) }
        } else if (previousState == MeshPowerState.OFF) {
            startScanning()
        }
    }

    private fun startScanning() {
        if (scanJob?.isActive == true) return
        
        scanJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(isScanning = true) }
                
                // Simulate random peer updates
                val mockPeers = generateMockPeers(Random.nextInt(0, 5))
                val currentHistory = _uiState.value.historicalPeers.toMutableList()
                if (currentHistory.size < 5 && Random.nextBoolean()) {
                     currentHistory.add(generateMockPeer("OLD-${Random.nextInt(100, 999)}"))
                }

                _uiState.update { 
                    it.copy(
                        activePeers = mockPeers,
                        historicalPeers = currentHistory
                    ) 
                }
                delay(3000)
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }

    private fun generateMockPeers(count: Int): List<Peer> {
        val peers = mutableListOf<Peer>()
        val callsigns = listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot")
        
        for (i in 0 until count) {
            peers.add(generateMockPeer(id = "NODE-${Random.nextInt(1000, 9999)}", name = callsigns.random()))
        }
        return peers
    }

    private fun generateMockPeer(id: String, name: String = "Unknown"): Peer {
        return Peer(
            deviceId = id,
            callsign = name,
            connectionType = ConnectionType.values().random(),
            rssi = Random.nextInt(-90, -40),
            syncProgress = if (Random.nextBoolean()) Random.nextFloat() else 1.0f,
            lastSeen = System.currentTimeMillis()
        )
    }
}
