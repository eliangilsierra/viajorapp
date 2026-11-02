package app.viajor.domain

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutingTest {
    private val lisbon = GeoPoint(38.7223, -9.1393)
    private val sintra = GeoPoint(38.8029, -9.3817)

    /** Runs a suspend block whose fakes never really suspend, so no coroutines library is needed. */
    private fun <T> run(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        return outcome!!.getOrThrow()
    }

    private class Fake(private val answer: () -> RouteResponse) : RoutingProvider {
        override val id = "fake"
        var calls = 0

        override suspend fun route(request: RouteRequest): RouteResponse {
            calls++
            return answer()
        }
    }

    @Test
    fun aProviderRouteIsLabelledRoutedWithItsOwnNumbers() {
        val geometry = listOf(lisbon, sintra)
        val provider = Fake { RouteResponse.Found(RouteResult(28_400, 2_100, geometry)) }
        val r = run { LegResolver(provider).resolve(lisbon, sintra, TravelMode.DRIVING) }
        assertEquals(TravelLeg(LegSource.ROUTED, 28_400, 2_100), r.leg)
        assertEquals(geometry, r.geometry)
        assertNull(r.reason)
        assertEquals(1, provider.calls)
    }

    @Test
    fun aProviderErrorFallsBackToALabelledEstimateWithAReason() {
        val r = run { LegResolver(Fake { RouteResponse.Unavailable("timed out") }).resolve(lisbon, sintra, TravelMode.WALKING) }
        assertEquals(LegSource.ESTIMATE, r.leg.source)
        assertEquals("timed out", r.reason)
        assertTrue(r.geometry.isEmpty())
        val meters = assertNotNull(r.leg.distanceMeters)
        assertTrue(meters in 20_000..30_000, "straight line Lisbon-Sintra is about 23 km, got $meters")
        assertEquals(rounded(meters / 1.4), r.leg.durationSeconds)
    }

    private fun rounded(x: Double): Long = kotlin.math.round(x).toLong()

    @Test
    fun aThrowingProviderNeverReachesTheCaller() {
        val r = run { LegResolver(Fake { throw IllegalStateException("secret internal detail") }).resolve(lisbon, sintra, TravelMode.CYCLING) }
        assertEquals(LegSource.ESTIMATE, r.leg.source)
        assertEquals("the routing provider failed", r.reason)
    }

    @Test
    fun cancellationIsNotSwallowed() {
        assertFailsWith<CancellationException> {
            run { LegResolver(Fake { throw CancellationException("stop") }).resolve(lisbon, sintra, TravelMode.WALKING) }
        }
    }

    @Test
    fun noRouteFoundIsAnEstimateNotARoute() {
        val r = run { LegResolver(Fake { RouteResponse.NotFound }).resolve(lisbon, sintra, TravelMode.DRIVING) }
        assertEquals(LegSource.ESTIMATE, r.leg.source)
        assertTrue(r.reason!!.contains("straight-line"))
    }

    @Test
    fun missingCoordinatesAreUnavailableAndTheProviderIsNotCalled() {
        val provider = Fake { RouteResponse.Found(RouteResult(1, 1)) }
        val r = run { LegResolver(provider).resolve(lisbon, null, TravelMode.WALKING) }
        assertEquals(TravelLeg(LegSource.UNAVAILABLE, null, null), r.leg)
        assertEquals(0, provider.calls)
    }

    @Test
    fun withoutAProviderTheLegIsAnEstimate() {
        val r = run { LegResolver(null).resolve(lisbon, sintra, TravelMode.TRANSIT) }
        assertEquals(LegSource.ESTIMATE, r.leg.source)
        assertEquals("no routing provider is configured", r.reason)
    }

    @Test
    fun aRoutedLegCanOnlyComeFromAProviderFoundResponse() {
        val answers = listOf<() -> RouteResponse>(
            { RouteResponse.NotFound },
            { RouteResponse.Unavailable("x") },
            { throw RuntimeException("boom") },
        )
        for (answer in answers) {
            val r = run { LegResolver(Fake(answer)).resolve(lisbon, sintra, TravelMode.DRIVING) }
            assertTrue(r.leg.source != LegSource.ROUTED)
        }
        assertTrue(run { LegResolver(null).resolve(lisbon, sintra, TravelMode.DRIVING) }.leg.source != LegSource.ROUTED)
    }

    @Test
    fun theSamePointGivesAZeroLengthEstimateAndSpeedsMustBePositive() {
        val r = run { LegResolver(null).resolve(lisbon, lisbon, TravelMode.WALKING) }
        assertEquals(TravelLeg(LegSource.ESTIMATE, 0, 0), r.leg)
        assertFailsWith<IllegalArgumentException> { LegResolver(null, LegResolver.DEFAULT_SPEEDS + (TravelMode.WALKING to 0.0)) }
        assertFailsWith<IllegalArgumentException> { RouteResult(-1, 0) }
    }
}
