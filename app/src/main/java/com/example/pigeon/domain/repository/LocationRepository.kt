package com.example.pigeon.domain.repository

import com.example.pigeon.domain.model.LocationQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.android.geometry.LatLng

interface LocationRepository {
    /**
     * Exposed as StateFlow (not bare Flow) so callers that need the current
     * value synchronously — e.g. NearbySyncManagerImpl on a connection-success
     * callback, deciding whether to broadcast a PeerInfo — can read .value
     * without a coroutine. Existing flow-collecting call sites work unchanged.
     */
    val userLocation: StateFlow<LatLng?>
    val gpsAccuracy: Flow<Float?>
    val locationQuality: Flow<LocationQuality>
    /** Wall-clock millis at which "locating" started, or null when LOCKED. */
    val locatingSinceMs: StateFlow<Long?>
    fun updateLocation(location: LatLng)
    fun startLocationUpdates()
    fun stopLocationUpdates()
}
