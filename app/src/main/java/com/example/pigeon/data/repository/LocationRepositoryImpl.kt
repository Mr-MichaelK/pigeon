package com.example.pigeon.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.pigeon.domain.model.LocationQuality
import com.example.pigeon.domain.repository.LocationRepository
import com.google.android.gms.location.Granularity
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

    private val TAG = "[GPS]"

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    override val userLocation: Flow<LatLng?> = _userLocation.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow<Float?>(null)
    override val gpsAccuracy: Flow<Float?> = _gpsAccuracy.asStateFlow()

    private val _locationQuality = MutableStateFlow(LocationQuality.NO_FIX)
    override val locationQuality: Flow<LocationQuality> = _locationQuality.asStateFlow()

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var fusedCallback: LocationCallback? = null
    private var gpsListener: LocationListener? = null

    // Monotonic timestamp of the last fix accepted by the accuracy filter.
    // Used by the GPS fallback to detect fused silence and by the accuracy
    // filter itself for the stale-location timeout path.
    private var lastAcceptedElapsed = 0L

    // Monotonic timestamp of the last fix that passed isFresh() AND the
    // accuracy filter — used to decide when the GPS fallback should take over.
    private var lastValidFusedElapsed = 0L

    override fun updateLocation(location: LatLng) {
        _userLocation.value = location
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        stopLocationUpdates()

        // ── Seed from last known location ────────────────────────────────────
        // Only seed from a demonstrably fresh, accurate, non-mock cached fix.
        // Wall-clock time.time has no freshness contract; use the monotonic
        // elapsedRealtimeNanos field which is filled by the hardware driver.
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location == null || _userLocation.value != null) return@addOnSuccessListener
            if (isMockLocation(location)) {
                Log.w(TAG, "Seed rejected — mock location discarded.")
                return@addOnSuccessListener
            }
            val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
            if (ageMs in 0..SEED_MAX_AGE_MS && location.accuracy <= SEED_MAX_ACCURACY_M) {
                Log.d(TAG, "Seeding from cached fix (age=${ageMs}ms, acc=${location.accuracy}m)")
                lastAcceptedElapsed = SystemClock.elapsedRealtime()
                _gpsAccuracy.value = location.accuracy
                _userLocation.value = LatLng(location.latitude, location.longitude)
                _locationQuality.value = classify(location.accuracy)
            }
        }

        // ── Fused location request ────────────────────────────────────────────
        // Key settings for Samsung M32 / Helio G85:
        //
        // PRIORITY_HIGH_ACCURACY — instructs the provider to use GPS satellites.
        //
        // GRANULARITY_FINE — forces fine (GPS-grade) output even when both
        //   COARSE and FINE permissions are present. The default
        //   GRANULARITY_PERMISSION_LEVEL lets the MediaTek fused provider
        //   mix GPS and network sources freely, causing the coarse↔fine flip.
        //
        // setMinUpdateDistanceMeters(2f) — suppresses updates while stationary.
        //   Without this, GPS and network fixes return slightly different
        //   coordinates for the same physical point, making the map dot vibrate.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(2.0f)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()

        fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // Reject developer/test mock injections.
                if (isMockLocation(location)) {
                    Log.w(TAG, "Fused update rejected — mock location.")
                    return
                }

                // Always surface accuracy so the UI indicator stays live.
                _gpsAccuracy.value = location.accuracy

                // Reject stale fixes regardless of accuracy.
                if (!isFresh(location)) return

                // ── Accuracy filter ───────────────────────────────────────────
                // Primary gate: accept only fixes ≤ ACCURACY_THRESHOLD_M (20 m).
                // Fallback gate: if we have not accepted ANY fix for more than
                //   STALE_LOCATION_TIMEOUT_MS (10 s), accept the best available
                //   fix rather than showing nothing at all.
                //
                // This prevents network positioning fixes (typically 50-150 m on
                // MediaTek) from replacing a good GPS fix and causing map jumps,
                // while still providing a coarse position during GPS warm-up or
                // when the device is indoors.
                val timeSinceLastAccepted = SystemClock.elapsedRealtime() - lastAcceptedElapsed
                val passesAccuracyGate = location.accuracy <= ACCURACY_THRESHOLD_M
                val passesStaleGate = timeSinceLastAccepted >= STALE_LOCATION_TIMEOUT_MS

                if (!passesAccuracyGate && !passesStaleGate) {
                    Log.d(TAG, "Fused fix skipped — acc=${location.accuracy}m > threshold, " +
                            "last accepted ${timeSinceLastAccepted}ms ago.")
                    return
                }

                Log.d(TAG, "Fused fix accepted — acc=${location.accuracy}m " +
                        "(gate=${if (passesAccuracyGate) "accuracy" else "stale-fallback"})")
                lastAcceptedElapsed = SystemClock.elapsedRealtime()
                lastValidFusedElapsed = SystemClock.elapsedRealtime()
                _userLocation.value = LatLng(location.latitude, location.longitude)
                _locationQuality.value = classify(location.accuracy)
            }
        }

        fusedClient.requestLocationUpdates(request, fusedCallback!!, Looper.getMainLooper())
        startGpsFallback()
    }

    /**
     * Raw GPS_PROVIDER fallback.
     *
     * Takes over when the fused client has been silent (or delivering only
     * poor-accuracy fixes that were rejected by the accuracy filter) for more
     * than 5 s. Applies the same mock-rejection guard; accuracy is intentionally
     * not filtered here — when the fallback activates we prefer any real GPS fix
     * over no fix at all.
     */
    @SuppressLint("MissingPermission")
    private fun startGpsFallback() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return

        gpsListener = LocationListener { location: Location ->
            if (isMockLocation(location)) {
                Log.w(TAG, "GPS fallback rejected — mock location.")
                return@LocationListener
            }
            _gpsAccuracy.value = location.accuracy
            val fusedSilentMs = SystemClock.elapsedRealtime() - lastValidFusedElapsed
            if (fusedSilentMs > 5_000L && isFresh(location)) {
                Log.d(TAG, "GPS fallback accepted — fused silent ${fusedSilentMs}ms, acc=${location.accuracy}m")
                lastAcceptedElapsed = SystemClock.elapsedRealtime()
                _userLocation.value = LatLng(location.latitude, location.longitude)
                _locationQuality.value = classify(location.accuracy)
            }
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2_000L,
            2f,   // match the fused displacement gate so both sources have the same minimum movement
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

    /**
     * Returns true if the fix from a mock/test provider should be rejected.
     *
     * Uses the non-deprecated API on SDK 31+ (isMock) and falls back to the
     * deprecated isFromMockProvider on older releases.
     */
    private fun isMockLocation(location: Location): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    /**
     * Freshness check using monotonic clock (elapsedRealtimeNanos).
     * Wall-clock time.time can drift with NTP sync; monotonic time cannot.
     * The outer accuracy envelope (FIX_MAX_ACCURACY_M = 75 m) rejects
     * completely unusable fixes; the tight accuracy filter above handles
     * the GPS↔network flip on a per-update basis.
     */
    private fun isFresh(location: Location): Boolean {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        return location.accuracy <= FIX_MAX_ACCURACY_M && ageMs in 0..FIX_MAX_AGE_MS
    }

    private fun classify(accuracyMeters: Float): LocationQuality = when {
        accuracyMeters <= LOCKED_THRESHOLD_M -> LocationQuality.LOCKED
        accuracyMeters <= FIX_MAX_ACCURACY_M -> LocationQuality.COARSE
        else -> LocationQuality.NO_FIX
    }

    private fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    companion object {
        // Seed (lastLocation cache): accept up to 30 s old, up to 150 m accuracy —
        // loose enough to give the map an initial dot quickly on cold start.
        private const val SEED_MAX_AGE_MS = 30_000L
        private const val SEED_MAX_ACCURACY_M = 150f

        // Live fixes — outer freshness envelope used by isFresh().
        private const val FIX_MAX_AGE_MS = 15_000L
        private const val FIX_MAX_ACCURACY_M = 75f

        // Accuracy filter — tight primary gate.
        // Fixes worse than 20 m are typically cell-tower/WiFi positioning,
        // not GPS, and cause the map dot to jump on Helio G85 devices.
        private const val ACCURACY_THRESHOLD_M = 20f

        // If no fix has been accepted for this long, allow a coarser fix through
        // rather than leaving the user with a frozen/missing location dot.
        private const val STALE_LOCATION_TIMEOUT_MS = 10_000L

        // Quality classification thresholds.
        // LOCKED requires genuine GPS quality (≤15 m); anything between 15–75 m
        // is COARSE (marginal GPS or stale-fallback network fix).
        private const val LOCKED_THRESHOLD_M = 15f
    }
}
