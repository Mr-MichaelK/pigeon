package com.example.pigeon.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import com.example.pigeon.domain.repository.LocationRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    override val userLocation: Flow<LatLng?> = _userLocation.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow<Float?>(null)
    override val gpsAccuracy: Flow<Float?> = _gpsAccuracy.asStateFlow()

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var fusedCallback: LocationCallback? = null
    private var gpsListener: LocationListener? = null
    // Tracks when fused last gave us a valid (fresh+accurate) fix
    private var lastValidFusedElapsed = 0L

    override fun updateLocation(location: LatLng) {
        _userLocation.value = location
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        stopLocationUpdates()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()

        fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                // Always update the accuracy so the warm-up indicator reflects reality
                _gpsAccuracy.value = location.accuracy
                if (isFresh(location)) {
                    lastValidFusedElapsed = SystemClock.elapsedRealtime()
                    _userLocation.value = LatLng(location.latitude, location.longitude)
                }
            }
        }

        fusedClient.requestLocationUpdates(request, fusedCallback!!, Looper.getMainLooper())
        startGpsFallback()
    }

    @SuppressLint("MissingPermission")
    private fun startGpsFallback() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return

        gpsListener = LocationListener { location: Location ->
            _gpsAccuracy.value = location.accuracy
            // Only use raw GPS if fused has been silent/stale for > 5 seconds
            val fusedSilentMs = SystemClock.elapsedRealtime() - lastValidFusedElapsed
            if (fusedSilentMs > 5_000L && isFresh(location)) {
                _userLocation.value = LatLng(location.latitude, location.longitude)
            }
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2_000L,
            1f,
            gpsListener!!,
            Looper.getMainLooper()
        )
    }

    override fun stopLocationUpdates() {
        fusedCallback?.let { fusedClient.removeLocationUpdates(it) }
        fusedCallback = null
        gpsListener?.let { locationManager.removeUpdates(it) }
        gpsListener = null
    }

    // Discard if accuracy > 50 m or location is older than 10 seconds
    private fun isFresh(location: Location): Boolean =
        location.accuracy <= 50f && (System.currentTimeMillis() - location.time) <= 10_000L

    private fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
