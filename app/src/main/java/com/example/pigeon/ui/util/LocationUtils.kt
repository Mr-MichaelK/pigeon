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
