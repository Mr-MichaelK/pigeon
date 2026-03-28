package com.example.pigeon.domain.repository

import kotlinx.coroutines.flow.Flow
import org.maplibre.android.geometry.LatLng

/**
 * Repository for managing and observing the user's real-time location.
 */
interface LocationRepository {
    val userLocation: Flow<LatLng?>
    fun updateLocation(location: LatLng)
}
