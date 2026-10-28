package app.viajor.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.TripInput
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchTest {
    private var counter = 0
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    private val db: ViajorDatabase
    private val trips: SqlTripRepository
    private val itinerary: SqlItineraryRepository
    private val search: SqlSearchRepository

    init {
        ViajorDatabase.Schema.create(driver)
        db = ViajorDatabase(driver)
        val stamper = ChangeStamper("dev") { 1L }
        val ids = IdGenerator { "id-${++counter}" }
        trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
        itinerary = SqlItineraryRepository(db, stamper, ids, EmptyCoroutineContext)
        search = SqlSearchRepository(db, EmptyCoroutineContext)
    }

    private suspend fun trip(title: String): String =
        (trips.createTrip(TripInput(title, null, null)) as CreateTripResult.Created).tripId

    private suspend fun activity(tripId: String, title: String, notes: String? = null): String {
        val day = db.dayQueries.selectDaysForTrip(tripId).executeAsList().first().id
        return (itinerary.addActivity(tripId, day, ActivityDraft(title, notes)) as SaveActivityResult.Saved).activityId
    }

    @Test
    fun findsByPrefixIgnoringCaseAndAccents() = runTest {
        val t = trip("Lisbon")
        activity(t, "Café da Manhã", "pastéis de nata")
        assertEquals(1, search.search("cafe").size)
        assertEquals(1, search.search("CAFÉ").size)
        assertEquals(1, search.search("past").size)
        assertEquals(1, search.search("nata past").size) // every term must match
        assertEquals(0, search.search("museu").size)
    }

    @Test
    fun findsPlacesAndActivitiesAndCanLimitToOneTrip() = runTest {
        val a = trip("Lisbon")
        val b = trip("Porto")
        itinerary.addPlace(a, PlaceDraft("Alfama viewpoint", address = "Rua Norte"))
        activity(b, "Alfama walk")
        val all = search.search("alfama")
        assertEquals(setOf(SearchKind.PLACE, SearchKind.ACTIVITY), all.map { it.kind }.toSet())
        assertEquals(listOf(SearchKind.PLACE), search.search("alfama", a).map { it.kind })
        assertEquals(listOf(b), search.search("alfama", b).map { it.tripId })
    }

    @Test
    fun softDeletedItemsAndTripsAreNotReturned() = runTest {
        val t = trip("Lisbon")
        val act = activity(t, "Fado night")
        val place = (itinerary.addPlace(t, PlaceDraft("Fado museum")) as SavePlaceResult.Saved).placeId
        assertEquals(2, search.search("fado").size)
        itinerary.deleteActivity(act)
        itinerary.deletePlace(place)
        assertEquals(0, search.search("fado").size)
        itinerary.restoreActivity(act)
        assertEquals(1, search.search("fado").size)
        trips.deleteTrip(t)
        assertEquals(0, search.search("fado").size)
    }

    @Test
    fun editsUpdateTheIndex() = runTest {
        val t = trip("Lisbon")
        val act = activity(t, "Tram 28")
        assertEquals(1, search.search("tram").size)
        itinerary.updateActivity(act, ActivityDraft("Funicular ride", "old notes"))
        assertEquals(0, search.search("tram").size)
        assertEquals(1, search.search("funicular").size)
        assertEquals(1, search.search("notes").size)
    }

    @Test
    fun hostileQueryTextNeverThrows() = runTest {
        val t = trip("Lisbon")
        activity(t, "Tram 28 to the castle")
        for (text in listOf("\"", "\"unterminated", "tram OR", "NOT", "NEAR/2", "tram*", "(", ")", "'; DROP TABLE trip; --", "a:b", "-tram", "^tram", "", "   ", "%%%", "\u0000")) {
            search.search(text) // must not throw
        }
        assertEquals(0, search.search("tram OR castle").size) // OR is just a word here, not an operator
        assertTrue(search.search("\"\"").isEmpty())
        assertEquals(1, db.tripQueries.selectActive().executeAsList().size) // nothing was dropped
    }

    @Test
    fun queryBuilderProducesLowercasePrefixTermsAndLimitsTheirNumber() {
        assertEquals("cafe* da* manhã*", SearchQuery.toMatch("Cafe, da (Manhã)!"))
        assertEquals("tram* or*", SearchQuery.toMatch("tram OR"))
        assertNull(SearchQuery.toMatch("  \"-* "))
        assertEquals(SearchQuery.MAX_TOKENS, SearchQuery.toMatch("a b c d e f g h i j k")!!.split(" ").size)
    }
}
