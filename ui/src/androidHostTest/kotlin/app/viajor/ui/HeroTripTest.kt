package app.viajor.ui

import app.viajor.data.TripRecord
import app.viajor.data.TripSummary
import app.viajor.ui.presenter.HeroKind
import app.viajor.ui.presenter.pickHeroTrip
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeroTripTest {
    private fun trip(id: String, start: String?, end: String?) =
        TripSummary(TripRecord(id, id, null, start?.let(LocalDate::parse), end?.let(LocalDate::parse), false), 1, 0, 0)

    private val today = LocalDate.parse("2027-03-10")

    @Test
    fun aTripHappeningTodayWins() {
        val trips = listOf(trip("later", "2027-05-01", "2027-05-04"), trip("now", "2027-03-09", "2027-03-12"))
        val hero = pickHeroTrip(trips, today)!!
        assertEquals("now", hero.summary.trip.id)
        assertEquals(HeroKind.HAPPENING_NOW, hero.kind)
    }

    @Test
    fun otherwiseTheSoonestUpcomingTripIsNext() {
        val trips = listOf(trip("past", "2027-01-01", "2027-01-03"), trip("june", "2027-06-01", "2027-06-05"), trip("april", "2027-04-02", "2027-04-04"))
        val hero = pickHeroTrip(trips, today)!!
        assertEquals("april", hero.summary.trip.id)
        assertEquals(HeroKind.NEXT, hero.kind)
    }

    @Test
    fun pastAndUndatedTripsAreNeverLabelledNextOrNow() {
        assertNull(pickHeroTrip(listOf(trip("past", "2027-01-01", "2027-01-03"), trip("someday", null, null)), today))
        assertNull(pickHeroTrip(emptyList(), today))
    }
}
