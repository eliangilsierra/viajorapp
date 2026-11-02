package app.viajor.domain

import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToLong

enum class TravelMode { WALKING, CYCLING, DRIVING, TRANSIT }

data class RouteRequest(val from: GeoPoint, val to: GeoPoint, val mode: TravelMode)

/** A path a routing provider actually computed. [geometry] may be empty when only totals are known. */
data class RouteResult(val distanceMeters: Long, val durationSeconds: Long, val geometry: List<GeoPoint> = emptyList()) {
    init {
        require(distanceMeters >= 0 && durationSeconds >= 0) { "distance and duration must not be negative" }
    }
}

/** What a provider says. Adapters turn their own errors and timeouts into [Unavailable]; they do not throw. */
sealed interface RouteResponse {
    data class Found(val route: RouteResult) : RouteResponse

    /** The provider answered that no route exists between the points for this mode. */
    data object NotFound : RouteResponse

    /** The provider could not be used (offline, timeout, quota, bad answer). [reason] is safe to show. */
    data class Unavailable(val reason: String) : RouteResponse
}

/** Provider-neutral routing contract (ADR-0004). Adapters live outside `:domain`. */
interface RoutingProvider {
    val id: String

    suspend fun route(request: RouteRequest): RouteResponse
}

/** A leg plus where it came from. [reason] explains an ESTIMATE or UNAVAILABLE result for the UI. */
data class ResolvedLeg(val leg: TravelLeg, val geometry: List<GeoPoint>, val reason: String?)

/**
 * Turns a pair of places into a leg whose label is always honest: ROUTED only when a provider returned a
 * route, otherwise a labelled straight-line ESTIMATE, or UNAVAILABLE without coordinates (ADR-0004).
 */
class LegResolver(private val provider: RoutingProvider?, private val estimateSpeeds: Map<TravelMode, Double> = DEFAULT_SPEEDS) {
    suspend fun resolve(from: GeoPoint?, to: GeoPoint?, mode: TravelMode): ResolvedLeg {
        if (from == null || to == null) {
            return ResolvedLeg(TravelLeg(LegSource.UNAVAILABLE, null, null), emptyList(), "a place has no coordinates")
        }
        if (provider == null) return estimate(from, to, mode, "no routing provider is configured")

        val response = try {
            provider.route(RouteRequest(from, to, mode))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RouteResponse.Unavailable("the routing provider failed")
        }
        return when (response) {
            is RouteResponse.Found -> ResolvedLeg(
                TravelLeg(LegSource.ROUTED, response.route.distanceMeters, response.route.durationSeconds),
                response.route.geometry,
                null,
            )
            RouteResponse.NotFound -> estimate(from, to, mode, "no route was found; showing a straight-line estimate")
            is RouteResponse.Unavailable -> estimate(from, to, mode, response.reason)
        }
    }

    private fun estimate(from: GeoPoint, to: GeoPoint, mode: TravelMode, reason: String): ResolvedLeg {
        val speed = estimateSpeeds.getValue(mode)
        val meters = TravelEstimate.distanceMeters(from, to)
        val leg = TravelLeg(LegSource.ESTIMATE, meters.roundToLong(), (meters / speed).roundToLong())
        return ResolvedLeg(leg, emptyList(), reason)
    }

    companion object {
        /** Rough straight-line speeds in metres per second. They are guesses and the result is labelled as an estimate. */
        val DEFAULT_SPEEDS: Map<TravelMode, Double> = mapOf(
            TravelMode.WALKING to 1.4,
            TravelMode.CYCLING to 4.5,
            TravelMode.DRIVING to 11.0,
            TravelMode.TRANSIT to 8.0,
        )
    }

    init {
        require(TravelMode.entries.all { (estimateSpeeds[it] ?: 0.0) > 0 }) { "every travel mode needs a positive speed" }
    }
}
