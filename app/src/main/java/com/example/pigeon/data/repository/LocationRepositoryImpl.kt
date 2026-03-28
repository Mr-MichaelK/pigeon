package com.example.pigeon.data.repository

import com.example.pigeon.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor() : LocationRepository {
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    override val userLocation: Flow<LatLng?> = _userLocation.asStateFlow()

    override fun updateLocation(location: LatLng) {
        _userLocation.value = location
    }
}
