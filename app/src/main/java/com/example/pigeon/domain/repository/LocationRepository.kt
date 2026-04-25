package com.example.pigeon.domain.repository

import com.example.pigeon.domain.model.LocationQuality
import kotlinx.coroutines.flow.Flow
import org.maplibre.android.geometry.LatLng

interface LocationRepository {
    val userLocation: Flow<LatLng?>
    val gpsAccuracy: Flow<Float?>
    val locationQuality: Flow<LocationQuality>
    fun updateLocation(location: LatLng)
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
