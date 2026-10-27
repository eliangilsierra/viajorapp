package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TravelEstimateTest {
    private val paris = GeoPoint(48.8566, 2.3522)
    private val london = GeoPoint(51.5074, -0.1278)

    @Test
    fun parisToLondonIsAbout344Km() {
        val km = TravelEstimate.distanceMeters(paris, london) / 1000.0
        assertTrue(km in 340.0..348.0, "was $km")
    }

    @Test
    fun distanceIsSymmetricAndZeroForSamePoint() {
        assertEquals(0.0, TravelEstimate.distanceMeters(paris, paris), 1e-6)
        assertEquals(
            TravelEstimate.distanceMeters(paris, london),
            TravelEstimate.distanceMeters(london, paris),
            1e-6,
        )
    }

    @Test
    fun antipodalPointsDoNotProduceNaN() {
        val d = TravelEstimate.distanceMeters(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0))
        assertTrue(d.isFinite() && d > 20_000_000.0)
    }

    @Test
    fun walkingEstimateIsLabeledEstimate() {
        val leg = TravelEstimate.walking(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.01))
        assertEquals(LegSource.ESTIMATE, leg.source)
        // 0.01 degrees of longitude at the equator is about 1,112 m; at 1.4 m/s about 794 s.
        assertTrue(leg.distanceMeters!! in 1_100L..1_125L)
        assertTrue(leg.durationSeconds!! in 780L..810L)
    }

    @Test
    fun missingCoordinatesGiveUnavailableNotAGuess() {
        val leg = TravelEstimate.walking(null, paris)
        assertEquals(LegSource.UNAVAILABLE, leg.source)
        assertNull(leg.distanceMeters)
        assertNull(leg.durationSeconds)
    }

    @Test
    fun invalidInputsAreRejected() {
        assertFailsWith<IllegalArgumentException> { GeoPoint(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoPoint(0.0, 181.0) }
        assertFailsWith<IllegalArgumentException> { TravelEstimate.walking(paris, london, 0.0) }
    }
}
