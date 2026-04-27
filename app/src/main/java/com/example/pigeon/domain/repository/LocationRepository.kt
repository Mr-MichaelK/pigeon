package com.example.pigeon.domain.repository

import com.example.pigeon.domain.model.LocationQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.android.geometry.LatLng

interface LocationRepository {
    val userLocation: Flow<LatLng?>
    val gpsAccuracy: Flow<Float?>
    val locationQuality: Flow<LocationQuality>
    /** Wall-clock millis at which "locating" started, or null when LOCKED. */
    val locatingSinceMs: StateFlow<Long?>
    fun updateLocation(location: LatLng)
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
