package com.example.pigeon.domain.repository

import kotlinx.coroutines.flow.Flow
import org.maplibre.android.geometry.LatLng

interface LocationRepository {
    val userLocation: Flow<LatLng?>
    val gpsAccuracy: Flow<Float?>
    fun updateLocation(location: LatLng)
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
