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
    override val userLocation: kotlinx.coroutines.flow.StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow<Float?>(null)
    override val gpsAccuracy: Flow<Float?> = _gpsAccuracy.asStateFlow()

    private val _locationQuality = MutableStateFlow(LocationQuality.NO_FIX)
    override val locationQuality: Flow<LocationQuality> = _locationQuality.asStateFlow()

    // Locating-timer source of truth. Lives on the singleton repo so it
    // survives MapViewModel recreation (bottom-nav re-entry rebuilds the
    // navBackStackEntry-scoped ViewModel). Set to wall-clock millis the first
    // time quality drops below LOCKED, cleared back to null on LOCKED.
    private val _locatingSinceMs = MutableStateFlow<Long?>(null)
    override val locatingSinceMs: kotlinx.coroutines.flow.StateFlow<Long?> = _locatingSinceMs.asStateFlow()

    private fun updateLocatingTimer(quality: LocationQuality) {
        if (quality == LocationQuality.LOCKED) {
            _locatingSinceMs.value = null
        } else if (_locatingSinceMs.value == null) {
            _locatingSinceMs.value = System.currentTimeMillis()
        }
    }

    /**
     * Drive the LocationQuality state machine, but suppress LOCKED→COARSE
     * downgrades that come from fallback paths (stale-fused / GPS_PROVIDER).
     *
     * Without this gate, every fallback fix at 30–50 m accuracy would call
     * classify() and demote LOCKED → COARSE, then a sub-15 m GPS poke would
     * promote back, producing the every-few-seconds quality flicker the user
     * sees on phones with mixed GPS/WiFi positioning.
     *
     * Rule: a fix may downgrade quality only if it passed the primary
     * accuracy gate (≤ ACCURACY_THRESHOLD_M = 20 m), OR if the primary gate
     * hasn't fired for QUALITY_DOWNGRADE_TIMEOUT_MS (30 s) — at which point
     * the user has genuinely been without high-accuracy positioning long
     * enough that the indicator should reflect it.
     */
    private fun maybeUpdateQuality(accuracyMeters: Float, primaryGated: Boolean) {
        if (primaryGated) lastPrimaryGateElapsed = SystemClock.elapsedRealtime()
        val sincePrimary = SystemClock.elapsedRealtime() - lastPrimaryGateElapsed
        val allowDowngrade = sincePrimary >= QUALITY_DOWNGRADE_TIMEOUT_MS
        if (primaryGated || allowDowngrade) {
            _locationQuality.value = classify(accuracyMeters, _locationQuality.value)
            updateLocatingTimer(_locationQuality.value)
        } else {
            Log.d(TAG, "Quality update suppressed — fallback fix acc=${accuracyMeters}m, " +
                    "${sincePrimary}ms since last primary-gate fix " +
                    "(need ≥${QUALITY_DOWNGRADE_TIMEOUT_MS}ms to allow downgrade).")
        }
    }

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

    // Monotonic timestamp of the last fix that passed the PRIMARY accuracy
    // gate (≤ ACCURACY_THRESHOLD_M). Distinct from lastValidFusedElapsed,
    // which is also bumped by stale-fallback acceptances. Quality downgrades
    // out of LOCKED are suppressed until QUALITY_DOWNGRADE_TIMEOUT_MS has
    // elapsed since this timestamp — see maybeUpdateQuality().
    private var lastPrimaryGateElapsed = 0L

    override fun updateLocation(location: LatLng) {
        _userLocation.value = location
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "startLocationUpdates: ACCESS_FINE_LOCATION not granted — skipping.")
            return
        }
        if (fusedCallback != null) {
            Log.d(TAG, "startLocationUpdates: already running — skipping.")
            return
        }
        Log.d(TAG, "startLocationUpdates: subscribing to fused + GPS providers.")
        // Engage the locating-elapsed timer immediately so the UI indicator
        // starts ticking even when the fused/GPS providers stay silent.
        updateLocatingTimer(_locationQuality.value)

        // ── Seed from last known location ────────────────────────────────────
        // Only seed from a demonstrably fresh, accurate, non-mock cached fix.
        // Wall-clock time.time has no freshness contract; use the monotonic
        // elapsedRealtimeNanos field which is filled by the hardware driver.
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Log.d(TAG, "Seed: lastLocation returned null — no cached fix available.")
                return@addOnSuccessListener
            }
            if (_userLocation.value != null) return@addOnSuccessListener
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
                maybeUpdateQuality(location.accuracy, primaryGated = location.accuracy <= ACCURACY_THRESHOLD_M)
            } else {
                Log.d(TAG, "Seed rejected — age=${ageMs}ms acc=${location.accuracy}m " +
                        "(limits: age≤${SEED_MAX_AGE_MS}ms acc≤${SEED_MAX_ACCURACY_M}m).")
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Seed: lastLocation failed — ${e.message}")
        }

        // ── Fused location request ────────────────────────────────────────────
        // Lessons from the M32 silent-callback bug:
        //
        // No setMinUpdateDistanceMeters — earlier we used 2 m to suppress the
        //   stationary-vibration of mixed GPS/network fixes, but Samsung's FLP
        //   shim treats the boot-time cached lastLocation as the displacement
        //   reference. Stationary user → never moves 2 m → ZERO callbacks
        //   delivered for the entire session. Map-dot stability is now handled
        //   downstream by the accuracy filter + classify() hysteresis instead.
        //
        // No setGranularity(FINE) — FINE blocks the network/WiFi fallback the
        //   fused provider would otherwise mix in while GPS warms up (which is
        //   ~96 s on this chipset). With weak satellite reception that meant
        //   the user saw nothing for minutes. classify() handles the COARSE↔
        //   LOCKED flip properly now, so we can let the OS mix sources.
        //
        // PRIORITY_HIGH_ACCURACY stays — we still want GPS as the source of
        // truth once it's available; it just isn't the *only* allowed source.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()

        fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location == null) {
                    Log.d(TAG, "Fused callback fired with null location.")
                    return
                }

                // Reject developer/test mock injections.
                if (isMockLocation(location)) {
                    Log.w(TAG, "Fused update rejected — mock location.")
                    return
                }

                // Always surface accuracy so the UI indicator stays live.
                _gpsAccuracy.value = location.accuracy

                // Reject stale fixes regardless of accuracy.
                if (!isFresh(location)) {
                    val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
                    Log.d(TAG, "Fused fix dropped by isFresh — acc=${location.accuracy}m age=${ageMs}ms " +
                            "(limits: acc≤${FIX_MAX_ACCURACY_M}m age≤${FIX_MAX_AGE_MS}ms).")
                    return
                }

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
                maybeUpdateQuality(location.accuracy, primaryGated = passesAccuracyGate)
            }
        }

        fusedClient.requestLocationUpdates(request, fusedCallback!!, Looper.getMainLooper())
            .addOnSuccessListener { Log.d(TAG, "Fused requestLocationUpdates: subscription confirmed.") }
            .addOnFailureListener { e -> Log.w(TAG, "Fused requestLocationUpdates failed — ${e.message}") }
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
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.w(TAG, "startGpsFallback: GPS_PROVIDER disabled — fallback not registered.")
            return
        }
        Log.d(TAG, "startGpsFallback: subscribing to GPS_PROVIDER.")

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
                maybeUpdateQuality(location.accuracy, primaryGated = location.accuracy <= ACCURACY_THRESHOLD_M)
            } else {
                val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
                Log.d(TAG, "GPS fallback skipped — fusedSilent=${fusedSilentMs}ms acc=${location.accuracy}m age=${ageMs}ms.")
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
        if (fusedCallback == null && gpsListener == null) return
        Log.d(TAG, "stopLocationUpdates: tearing down fused + GPS subscriptions.")
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

    /**
     * Classify with hysteresis around the LOCKED↔COARSE boundary.
     *
     * The Helio G85's fused provider hovers around the 15 m mark while stationary
     * with a clear sky, sending fixes that bounce 13 m → 17 m → 14 m → 18 m. Without
     * hysteresis the UI quality indicator flickers LOCKED↔COARSE every few seconds.
     *
     * Rule: enter LOCKED only when accuracy ≤ LOCKED_THRESHOLD_M (15 m). Once LOCKED,
     * stay LOCKED until accuracy degrades past LOCKED_RELEASE_M (25 m). NO_FIX exit
     * also requires the strict 15 m gate so a stale-fallback coarse fix doesn't
     * "promote" the device into LOCKED on first acceptance.
     */
    private fun classify(accuracyMeters: Float, current: LocationQuality): LocationQuality {
        if (accuracyMeters > FIX_MAX_ACCURACY_M) return LocationQuality.NO_FIX
        return when (current) {
            LocationQuality.LOCKED ->
                if (accuracyMeters <= LOCKED_RELEASE_M) LocationQuality.LOCKED
                else LocationQuality.COARSE
            else ->
                if (accuracyMeters <= LOCKED_THRESHOLD_M) LocationQuality.LOCKED
                else LocationQuality.COARSE
        }
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

        // Quality classification thresholds (hysteresis).
        // Enter LOCKED only when accuracy ≤ LOCKED_THRESHOLD_M (15 m).
        // Once LOCKED, stay LOCKED until accuracy degrades past LOCKED_RELEASE_M
        // (25 m) — the 10 m gap absorbs the natural ±3-5 m wobble of the Helio
        // G85 GPS chipset and prevents the visible LOCKED↔COARSE flicker.
        private const val LOCKED_THRESHOLD_M = 15f
        private const val LOCKED_RELEASE_M = 25f

        // LOCKED stickiness against fallback paths. A stale-fallback fused
        // fix or GPS_PROVIDER fallback fix at 30–50 m must not demote LOCKED
        // unless we've been without a primary-gate-quality fix for at least
        // this long. Tuned long enough to ride out a 10–20 s tunnel/indoors
        // dip, short enough that the indicator does eventually reflect a
        // sustained loss of high-accuracy positioning.
        private const val QUALITY_DOWNGRADE_TIMEOUT_MS = 30_000L
    }
}
