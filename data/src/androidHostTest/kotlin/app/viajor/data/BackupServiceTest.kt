package app.viajor.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.BackupActivity
import app.viajor.domain.BackupCodec
import app.viajor.domain.BackupDocument
import app.viajor.domain.BackupTrip
import app.viajor.domain.Provenance
import app.viajor.domain.TripInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupServiceTest {
    private class Store(prefix: String) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        val db: ViajorDatabase
        val trips: SqlTripRepository
        val itinerary: SqlItineraryRepository
        val backup: SqlBackupService
        val search: SqlSearchRepository
        private var counter = 0

        init {
            ViajorDatabase.Schema.create(driver)
            db = ViajorDatabase(driver)
            val stamper = ChangeStamper("dev-$prefix") { 1_000L }
            val ids = IdGenerator { "$prefix-${++counter}" }
            trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
            itinerary = SqlItineraryRepository(db, stamper, ids, EmptyCoroutineContext)
            backup = SqlBackupService(db, stamper, ids, EmptyCoroutineContext)
            search = SqlSearchRepository(db, EmptyCoroutineContext)
        }

        fun count(table: String): Long = driver.executeQuery(
            null, "SELECT COUNT(*) FROM $table", { c -> QueryResult.Value(if (c.next().value) c.getLong(0) ?: 0L else 0L) }, 0,
        ).value
    }

    private suspend fun populate(s: Store): String {
        val tripId = (s.trips.createTrip(TripInput("Lisbon", LocalDate.parse("2027-03-01"), LocalDate.parse("2027-03-02")), "Spring") as CreateTripResult.Created).tripId
        val days = s.itinerary.observeDays(tripId).first()
        val place = (s.itinerary.addPlace(tripId, PlaceDraft("Alfama", "Rua Norte", 38.71, -9.13, "old town"), Provenance.GEOCODER) as SavePlaceResult.Saved).placeId
        val a = (s.itinerary.addActivity(
            tripId, days[0].id,
            ActivityDraft("Walk", "bring water", ActivityKind.ACTIVITY, LocalDateTime.parse("2027-03-01T10:00"), LocalDateTime.parse("2027-03-01T12:00"), "Europe/Lisbon", place),
        ) as SaveActivityResult.Saved).activityId
        s.itinerary.addActivity(tripId, days[0].id, ActivityDraft("Lunch", kind = ActivityKind.MEAL))
        s.itinerary.addActivity(tripId, null, ActivityDraft("Fado idea"), Provenance.AI)
        s.itinerary.setCompleted(a, true)
        val gone = (s.itinerary.addActivity(tripId, days[1].id, ActivityDraft("Deleted one")) as SaveActivityResult.Saved).activityId
        s.itinerary.deleteActivity(gone)
        return tripId
    }

    /** The comparable shape of everything in a store, without ids. */
    private suspend fun shape(s: Store): List<String> = s.backup.export(0).trips.sortedBy { it.title }.map { trip ->
        val dayDate = trip.days.associate { it.id to it.date }
        val placeName = trip.places.associate { it.id to it.name }
        "${trip.title}|${trip.description}|${trip.startDate}|${trip.endDate}|days=${trip.days.map { it.dayIndex to it.date }}" +
            "|places=${trip.places.map { listOf(it.name, it.address, it.lat, it.lon, it.notes, it.source) }}" +
            "|acts=${trip.activities.map { listOf(dayDate[it.dayId], placeName[it.placeId], it.title, it.notes, it.kind, it.startLocal, it.endLocal, it.zoneId, it.sortKey, it.completedAt != null, it.source) }}"
    }

    @Test
    fun aFullRoundTripThroughTheJsonFileRestoresEverything() = runTest {
        val source = Store("a")
        populate(source)
        val text = BackupCodec.encode(source.backup.export(5L))
        val target = Store("b")
        val summary = target.backup.restore(BackupCodec.decode(text))

        assertEquals(RestoreSummary(trips = 1, days = 2, places = 1, activities = 3), summary)
        assertEquals(shape(source), shape(target))
        // Deleted items never leave the device.
        assertTrue("Deleted one" !in text)
        // Order and the activity-to-place link survived.
        val restoredDay = target.itinerary.observeDays(target.trips.observeTrips().first().single().id).first().first()
        assertEquals(listOf("Walk", "Lunch"), target.itinerary.observeActivities(restoredDay.id).first().map { it.title })
        assertEquals("Alfama", target.itinerary.observePlaces(restoredDay.tripId).first().single().name)
    }

    @Test
    fun restoringNeverTouchesExistingDataAndRestoredTripsGetNewIds() = runTest {
        val s = Store("a")
        val original = populate(s)
        val doc = s.backup.export(1L)
        s.backup.restore(doc)
        s.backup.restore(doc)
        val all = s.trips.observeTrips().first()
        assertEquals(3, all.size)
        assertEquals(3, all.map { it.id }.toSet().size)
        assertTrue(original in all.map { it.id })
        assertEquals(1, s.itinerary.observePlaces(original).first().size)
    }

    @Test
    fun aBrokenDocumentWritesNothing() = runTest {
        val s = Store("a")
        val broken = BackupDocument(
            trips = listOf(
                BackupTrip(
                    id = "t", title = "Half written", updatedAtHlc = "h", updatedBy = "d",
                    activities = listOf(BackupActivity("a", dayId = "missing-day", title = "x", sortKey = "V", updatedAtHlc = "h", updatedBy = "d")),
                ),
            ),
        )
        assertFailsWith<NoSuchElementException> { s.backup.restore(broken) }
        assertEquals(0L, s.count("trip"))
        assertEquals(0L, s.count("activity"))
    }

    @Test
    fun deleteAllDataEmptiesEveryTableIncludingTheSearchIndex() = runTest {
        val s = Store("a")
        populate(s)
        assertTrue(s.count("search_index") > 0)
        assertEquals(1, s.search.search("alfama").size)

        s.backup.deleteAllData()

        for (table in listOf("trip", "trip_day", "place", "activity", "search_index")) assertEquals(0L, s.count(table), table)
        assertEquals(emptyList(), s.search.search("alfama"))
        // The app keeps working afterwards.
        s.trips.createTrip(TripInput("Fresh start", null, null))
        assertEquals(listOf("Fresh start"), s.trips.observeTrips().first().map { it.title })
    }

    @Test
    fun theExportContainsNoDeviceSecretsOrKeys() = runTest {
        val s = Store("a")
        populate(s)
        // BackupCodec.encode runs the secret guard and would throw if anything key-shaped were present.
        val text = BackupCodec.encode(s.backup.export(1L))
        assertTrue("api" !in text.lowercase().replace("apiary", ""))
    }
}
