package app.viajor.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.Provenance
import app.viajor.domain.TripError
import app.viajor.domain.TripInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoriesTest {
    private var clock = 1_000L
    private var counter = 0
    private val db: ViajorDatabase
    private val trips: SqlTripRepository
    private val itinerary: SqlItineraryRepository

    init {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        ViajorDatabase.Schema.create(driver)
        db = ViajorDatabase(driver)
        val stamper = ChangeStamper("device-a") { clock++ }
        val ids = IdGenerator { "id-${++counter}" }
        trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
        itinerary = SqlItineraryRepository(db, stamper, ids, EmptyCoroutineContext)
    }

    private fun d(s: String) = LocalDate.parse(s)
    private fun t(s: String) = LocalDateTime.parse(s)
    private fun input(title: String = "Lisbon", start: String? = "2027-03-01", end: String? = "2027-03-03") =
        TripInput(title, start?.let(::d), end?.let(::d))

    private suspend fun createTrip(i: TripInput = input()): String =
        assertIs<CreateTripResult.Created>(trips.createTrip(i)).tripId

    @Test
    fun creatingATripWritesItsDaysAndStampsInOneGo() = runTest {
        val id = createTrip()
        val days = itinerary.observeDays(id).first()
        assertEquals(listOf(1, 2, 3), days.map { it.index })
        assertEquals(listOf("2027-03-01", "2027-03-02", "2027-03-03"), days.map { it.date.toString() })
        val row = db.tripQueries.selectById(id).executeAsOne()
        assertEquals("device-a", row.updated_by)
        assertTrue(row.updated_at_hlc.isNotEmpty())
        assertEquals(listOf("Lisbon"), trips.observeTrips().first().map { it.title })
    }

    @Test
    fun invalidInputWritesNothing() = runTest {
        val r = trips.createTrip(TripInput("  ", d("2027-03-05"), d("2027-03-01")))
        assertEquals(CreateTripResult.Invalid(listOf(TripError.EMPTY_TITLE, TripError.END_BEFORE_START)), r)
        assertEquals(emptyList(), trips.observeTrips().first())
        assertEquals(0L, db.dayQueries.selectAllDaysForTrip("id-1").executeAsList().size.toLong())
    }

    @Test
    fun undatedTripsStartWithOneDayAndGrowOnRequest() = runTest {
        val id = createTrip(input(start = null, end = null))
        assertEquals(1, itinerary.observeDays(id).first().size)
        assertTrue(trips.addDay(id))
        assertTrue(trips.addDay(id))
        assertEquals(listOf(1, 2, 3), itinerary.observeDays(id).first().map { it.index })
        val dated = createTrip(input("Dated"))
        assertEquals(false, trips.addDay(dated)) // dated trips grow by changing their end date
    }

    @Test
    fun activitiesAreAddedInOrderAndReorderedWithOneRowChange() = runTest {
        val id = createTrip()
        val day = itinerary.observeDays(id).first().first().id
        val a = (itinerary.addActivity(id, day, ActivityDraft("A")) as SaveActivityResult.Saved).activityId
        val b = (itinerary.addActivity(id, day, ActivityDraft("B")) as SaveActivityResult.Saved).activityId
        val c = (itinerary.addActivity(id, day, ActivityDraft("C")) as SaveActivityResult.Saved).activityId
        assertEquals(listOf("A", "B", "C"), itinerary.observeActivities(day).first().map { it.title })

        val before = db.activityQueries.selectForDay(day).executeAsList().associate { it.id to it.updated_at_hlc }
        itinerary.moveActivity(c, day, 0)
        assertEquals(listOf("C", "A", "B"), itinerary.observeActivities(day).first().map { it.title })
        val after = db.activityQueries.selectForDay(day).executeAsList().associate { it.id to it.updated_at_hlc }
        assertEquals(listOf(c), after.filter { (k, v) -> before[k] != v }.keys.toList())

        itinerary.moveActivity(a, day, 99) // clamped to the end
        assertEquals(listOf("C", "B", "A"), itinerary.observeActivities(day).first().map { it.title })
        assertTrue(b.isNotEmpty())
    }

    @Test
    fun anActivityCanMoveToAnotherDayOrBecomeAnUnscheduledIdea() = runTest {
        val id = createTrip()
        val days = itinerary.observeDays(id).first()
        val a = (itinerary.addActivity(id, days[0].id, ActivityDraft("A")) as SaveActivityResult.Saved).activityId
        itinerary.moveActivity(a, days[1].id, 0)
        assertEquals(emptyList(), itinerary.observeActivities(days[0].id).first())
        assertEquals(listOf("A"), itinerary.observeActivities(days[1].id).first().map { it.title })
        itinerary.moveActivity(a, null, 0)
        assertEquals(listOf("A"), itinerary.observeUnscheduled(id).first().map { it.title })
        assertEquals(SaveActivityResult.Invalid(listOf(ActivityError.DAY_NOT_FOUND)), itinerary.moveActivity(a, "nope", 0))
    }

    @Test
    fun activityValidationReportsEveryProblemAndSavesNothing() = runTest {
        val id = createTrip()
        val day = itinerary.observeDays(id).first().first().id
        val bad = ActivityDraft(" ", start = t("2027-03-01T10:00"), end = t("2027-03-01T09:00"), zoneId = null)
        val r = assertIs<SaveActivityResult.Invalid>(itinerary.addActivity(id, day, bad))
        assertEquals(listOf(ActivityError.EMPTY_TITLE, ActivityError.END_BEFORE_START, ActivityError.ZONE_REQUIRED), r.errors)
        val unknownZone = ActivityDraft("X", start = t("2027-03-01T10:00"), zoneId = "Mars/Olympus")
        assertEquals(listOf(ActivityError.UNKNOWN_ZONE), assertIs<SaveActivityResult.Invalid>(itinerary.addActivity(id, day, unknownZone)).errors)
        assertEquals(emptyList(), itinerary.observeActivities(day).first())
        assertEquals(SaveActivityResult.NotFound, itinerary.addActivity("missing", null, ActivityDraft("X")))
    }

    @Test
    fun timesKeepTheirZoneAndMinutePrecision() = runTest {
        val id = createTrip()
        val day = itinerary.observeDays(id).first().first().id
        val draft = ActivityDraft("Fado", start = t("2027-03-01T22:00"), end = t("2027-03-02T01:30"), zoneId = "Europe/Lisbon")
        val saved = (itinerary.addActivity(id, day, draft) as SaveActivityResult.Saved).activityId
        val row = itinerary.observeActivities(day).first().single { it.id == saved }
        assertEquals(t("2027-03-01T22:00"), row.start)
        assertEquals(t("2027-03-02T01:30"), row.end)
        assertEquals("Europe/Lisbon", row.zoneId)
        itinerary.updateActivity(saved, draft.copy(title = "Fado night", notes = "  book  "))
        val updated = itinerary.observeActivities(day).first().single()
        assertEquals("Fado night", updated.title)
        assertEquals("book", updated.notes)
    }

    @Test
    fun completionDeleteAndRestoreRoundTrip() = runTest {
        val id = createTrip()
        val day = itinerary.observeDays(id).first().first().id
        val a = (itinerary.addActivity(id, day, ActivityDraft("A")) as SaveActivityResult.Saved).activityId
        itinerary.setCompleted(a, true)
        assertTrue(itinerary.observeActivities(day).first().single().completed)
        itinerary.setCompleted(a, false)
        assertEquals(false, itinerary.observeActivities(day).first().single().completed)
        itinerary.deleteActivity(a)
        assertEquals(emptyList(), itinerary.observeActivities(day).first())
        itinerary.restoreActivity(a)
        assertEquals(1, itinerary.observeActivities(day).first().size)
    }

    @Test
    fun aDeletedTripDisappearsAndComesBack() = runTest {
        val id = createTrip()
        trips.deleteTrip(id)
        assertEquals(emptyList(), trips.observeTrips().first())
        assertNull(trips.observeTrip(id).first())
        assertEquals(UpdateTripResult.NotFound, trips.updateTrip(id, input()))
        trips.restoreTrip(id)
        assertEquals(1, trips.observeTrips().first().size)
        trips.setArchived(id, true)
        assertEquals(emptyList(), trips.observeTrips().first())
    }

    @Test
    fun shorteningNeverLosesContentWithoutConfirmation() = runTest {
        val id = createTrip() // 3 days
        val days = itinerary.observeDays(id).first()
        itinerary.addActivity(id, days[2].id, ActivityDraft("Keep me"))
        itinerary.addActivity(id, days[0].id, ActivityDraft("Day one"))

        val first = trips.updateTrip(id, input(end = "2027-03-02"))
        assertEquals(UpdateTripResult.NeedsConfirmation(listOf(3)), first)
        assertEquals(3, itinerary.observeDays(id).first().size) // nothing changed

        assertEquals(UpdateTripResult.Updated, trips.updateTrip(id, input(end = "2027-03-02"), confirmDropDays = true))
        assertEquals(2, itinerary.observeDays(id).first().size)
        assertEquals(listOf("Keep me"), itinerary.observeUnscheduled(id).first().map { it.title })
        assertEquals(listOf("Day one"), itinerary.observeActivities(days[0].id).first().map { it.title })
    }

    @Test
    fun growingAfterShrinkingRevivesTheDroppedDayInsteadOfClashing() = runTest {
        val id = createTrip()
        assertEquals(UpdateTripResult.Updated, trips.updateTrip(id, input(end = "2027-03-01"))) // empty days drop freely
        assertEquals(1, itinerary.observeDays(id).first().size)
        assertEquals(UpdateTripResult.Updated, trips.updateTrip(id, input(end = "2027-03-04")))
        assertEquals(listOf("2027-03-01", "2027-03-02", "2027-03-03", "2027-03-04"), itinerary.observeDays(id).first().map { it.date.toString() })
    }

    @Test
    fun shiftingTheStartDatesMovesDaysButKeepsTheirActivities() = runTest {
        val id = createTrip()
        val first = itinerary.observeDays(id).first().first()
        itinerary.addActivity(id, first.id, ActivityDraft("Stays on day 1"))
        assertEquals(UpdateTripResult.Updated, trips.updateTrip(id, input(start = "2027-04-10", end = "2027-04-12")))
        val days = itinerary.observeDays(id).first()
        assertEquals("2027-04-10", days.first().date.toString())
        assertEquals(first.id, days.first().id)
        assertEquals(1, itinerary.observeActivities(first.id).first().size)
    }

    @Test
    fun placesAreValidatedAndActivitiesCanReferenceThem() = runTest {
        val id = createTrip()
        val day = itinerary.observeDays(id).first().first().id
        assertEquals(
            listOf(PlaceError.EMPTY_NAME, PlaceError.ONLY_ONE_COORDINATE),
            assertIs<SavePlaceResult.Invalid>(itinerary.addPlace(id, PlaceDraft(" ", latitude = 10.0))).errors,
        )
        assertEquals(
            listOf(PlaceError.COORDINATE_OUT_OF_RANGE),
            assertIs<SavePlaceResult.Invalid>(itinerary.addPlace(id, PlaceDraft("X", latitude = 95.0, longitude = 0.0))).errors,
        )
        val place = (itinerary.addPlace(id, PlaceDraft("Alfama", latitude = 38.71, longitude = -9.13), Provenance.GEOCODER) as SavePlaceResult.Saved).placeId
        assertEquals(Provenance.GEOCODER, itinerary.observePlaces(id).first().single().provenance)
        val ok = itinerary.addActivity(id, day, ActivityDraft("Walk", placeId = place))
        assertIs<SaveActivityResult.Saved>(ok)
        val other = createTrip(input("Other"))
        assertEquals(
            SaveActivityResult.Invalid(listOf(ActivityError.PLACE_NOT_FOUND)),
            itinerary.addActivity(other, null, ActivityDraft("Walk", placeId = place)),
        )
    }

    @Test
    fun readsReEmitAfterAWrite() = runTest {
        val id = createTrip()
        val day = itinerary.observeDays(id).first().first().id
        val seen = mutableListOf<List<String>>()
        val job = launch { itinerary.observeActivities(day).take(2).toList().forEach { seen += it.map { a -> a.title } } }
        testScheduler.advanceUntilIdle()
        itinerary.addActivity(id, day, ActivityDraft("Late"))
        job.join()
        assertEquals(listOf(emptyList(), listOf("Late")), seen)
    }

    @Test
    fun stampsIncreaseWithEveryWrite() = runTest {
        val id = createTrip()
        val first = db.tripQueries.selectById(id).executeAsOne().updated_at_hlc
        trips.updateTrip(id, input("Renamed"))
        val second = db.tripQueries.selectById(id).executeAsOne().updated_at_hlc
        assertTrue(second > first, "$second should sort after $first")
    }
}
