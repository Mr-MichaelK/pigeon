package com.example.pigeon.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.MapMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isLoading: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onMapMoved(latitude: Double, longitude: Double, zoom: Double) {
        _uiState.value = _uiState.value.copy(
            metadata = _uiState.value.metadata.copy(
                latitude = latitude,
                longitude = longitude,
                zoom = zoom
            )
        )
    }
}
