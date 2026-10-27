package app.viajor.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** How a travel leg was obtained. The UI must never present [ESTIMATE] as a routed path (ADR-0004). */
enum class LegSource { ROUTED, ESTIMATE, UNAVAILABLE }

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}

data class TravelLeg(
    val source: LegSource,
    val distanceMeters: Long?,
    val durationSeconds: Long?,
)

object TravelEstimate {
    const val EARTH_RADIUS_METERS = 6_371_008.8
    const val DEFAULT_WALKING_SPEED_MPS = 1.4

    /** Great-circle distance in meters (haversine). */
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val dLat = lat2 - lat1
        val dLon = (to.longitude - from.longitude).toRadians()
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(max(0.0, h))))
    }

    /** A straight-line walking estimate; always [LegSource.ESTIMATE]. */
    fun walking(
        from: GeoPoint?,
        to: GeoPoint?,
        speedMetersPerSecond: Double = DEFAULT_WALKING_SPEED_MPS,
    ): TravelLeg {
        require(speedMetersPerSecond > 0) { "speed must be positive" }
        if (from == null || to == null) return TravelLeg(LegSource.UNAVAILABLE, null, null)
        val meters = distanceMeters(from, to)
        return TravelLeg(
            source = LegSource.ESTIMATE,
            distanceMeters = meters.roundToLong(),
            durationSeconds = (meters / speedMetersPerSecond).roundToLong(),
        )
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
}
