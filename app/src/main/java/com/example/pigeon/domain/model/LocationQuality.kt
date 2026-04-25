package com.example.pigeon.domain.model

/**
 * Tiered quality signal for the user's position fix.
 *
 * Accuracy is used as a proxy because FusedLocationProviderClient does not
 * expose whether the underlying source was GPS, WiFi, or cell — but GPS fixes
 * are consistently <= 30m while WiFi/cell fixes sit in the 50-1000m range.
 */
enum class LocationQuality {
    /** No fix received yet, or all fixes rejected as stale/inaccurate. */
    NO_FIX,

    /** Rough fix (accuracy > 30m). Likely network- or coarse-GPS-derived. */
    COARSE,

    /** Tight GPS lock (accuracy <= 30m). Suitable for tactical verification. */
    LOCKED
}
