package com.example.pigeon.ui.util

import org.maplibre.android.geometry.LatLng
import kotlin.math.*

/**
 * Utility for geographic calculations using the Haversine formula.
 */
object LocationUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the distance in meters between two geographic coordinates.
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val phi1 = lat1 * PI / 180.0
        val phi2 = lat2 * PI / 180.0
        val deltaPhi = (lat2 - lat1) * PI / 180.0
        val deltaLambda = (lon2 - lon1) * PI / 180.0

        val a = sin(deltaPhi / 2.0).pow(2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2.0).pow(2)
        
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Forward-azimuth (initial bearing) from point 1 to point 2, in degrees
     * clockwise from true north (0 = N, 90 = E, 180 = S, 270 = W).
     *
     * Used by the radar dial to place each peer at their real direction
     * relative to the local user. The result is normalized into [0, 360).
     */
    fun calculateBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val phi1 = lat1 * PI / 180.0
        val phi2 = lat2 * PI / 180.0
        val deltaLambda = (lon2 - lon1) * PI / 180.0
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return ((theta * 180.0 / PI) + 360.0) % 360.0
    }

    /**
     * Checks if a user is within a specified radius (in meters) of an event.
     */
    fun isWithinRange(
        userLocation: LatLng?,
        eventLat: Double,
        eventLon: Double,
        radius: Double = 500.0
    ): Boolean {
        if (userLocation == null) return false
        val distance = calculateDistance(
            userLocation.latitude, userLocation.longitude,
            eventLat, eventLon
        )
        return distance <= radius
    }
}
