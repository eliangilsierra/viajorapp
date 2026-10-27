package app.viajor.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.FractionalIndex
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ItineraryQueriesTest {
    // Foreign keys are off by default in SQLite; the production drivers must enable them too.
    private fun database(): ViajorDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        ViajorDatabase.Schema.create(driver)
        return ViajorDatabase(driver).also {
            it.tripQueries.insertTrip("t1", "Lisbon", null, "2027-03-01", "2027-03-05", "h1", "dev")
            it.dayQueries.insertDay("d1", "t1", 1, "2027-03-01", null, "h1", "dev")
        }
    }

    private fun ViajorDatabase.activity(
        id: String,
        title: String = id,
        dayId: String? = "d1",
        start: String? = null,
        end: String? = null,
        zone: String? = null,
        sortKey: String,
    ) = activityQueries.insertActivity(
        id, "t1", dayId, null, title, null, "activity", start, end, zone, sortKey, "user", "h1", "dev",
    )

    @Test
    fun activitiesComeBackInSortKeyOrderAndAMoveTouchesOneRow() {
        val db = database()
        val a = FractionalIndex.between(null, null)
        val c = FractionalIndex.between(a, null)
        db.activity("a", sortKey = a)
        db.activity("c", sortKey = c)
        val b = FractionalIndex.between(a, c)
        db.activity("b", sortKey = b)
        assertEquals(listOf("a", "b", "c"), db.activityQueries.selectForDay("d1").executeAsList().map { it.id })

        // Move "a" after "c": only that row changes.
        db.activityQueries.updateSortKey(FractionalIndex.between(c, null), "h2", "dev", "a")
        val rows = db.activityQueries.selectForDay("d1").executeAsList()
        assertEquals(listOf("b", "c", "a"), rows.map { it.id })
        assertEquals(listOf("h1", "h1", "h2"), rows.map { it.updated_at_hlc })
    }

    @Test
    fun activityWithoutDayIsAnUnscheduledIdea() {
        val db = database()
        db.activity("idea", dayId = null, sortKey = "V")
        assertEquals(listOf("idea"), db.activityQueries.selectUnscheduled("t1").executeAsList().map { it.id })
        assertEquals(emptyList(), db.activityQueries.selectForDay("d1").executeAsList())
    }

    @Test
    fun softDeleteHidesAndRestoreBringsBack() {
        val db = database()
        db.activity("a", sortKey = "V")
        db.activityQueries.softDeleteActivity(5L, "h2", "dev", "a")
        assertEquals(emptyList(), db.activityQueries.selectForDay("d1").executeAsList())
        db.activityQueries.restoreActivity("h3", "dev", "a")
        assertEquals(1, db.activityQueries.selectForDay("d1").executeAsList().size)
    }

    @Test
    fun completionIsStoredAndCleared() {
        val db = database()
        db.activity("a", sortKey = "V")
        db.activityQueries.setCompleted(9L, "h2", "dev", "a")
        assertNotNull(db.activityQueries.selectForDay("d1").executeAsOne().completed_at)
        db.activityQueries.setCompleted(null, "h3", "dev", "a")
        assertNull(db.activityQueries.selectForDay("d1").executeAsOne().completed_at)
    }

    @Test
    fun overnightActivityKeepsItsZoneAndLaterEndDate() {
        val db = database()
        db.activity("night", start = "2027-03-01T22:00", end = "2027-03-02T06:00", zone = "Europe/Lisbon", sortKey = "V")
        val row = db.activityQueries.selectForDay("d1").executeAsOne()
        assertEquals("Europe/Lisbon", row.zone_id)
        assertEquals("2027-03-02T06:00", row.end_local)
    }

    @Test
    fun databaseRejectsInvalidActivities() {
        val db = database()
        assertFails { db.activity("e", title = "", sortKey = "V") }
        assertFails { db.activity("r", start = "2027-03-01T10:00", end = "2027-03-01T09:00", zone = "Europe/Lisbon", sortKey = "V") }
        assertFails { db.activity("nz", start = "2027-03-01T10:00", zone = null, sortKey = "V") }
        assertFails { db.activity("nd", dayId = "missing-day", sortKey = "V") }
        assertFails {
            db.activityQueries.insertActivity("k", "t1", null, null, "Bad kind", null, "spaceship", null, null, null, "V", "user", "h", "d")
        }
    }

    @Test
    fun databaseRejectsInvalidPlacesAndDays() {
        val db = database()
        db.placeQueries.insertPlace("p1", "t1", "Alfama", null, 38.71, -9.13, null, "user", "h", "d")
        assertEquals(listOf("Alfama"), db.placeQueries.selectPlacesForTrip("t1").executeAsList().map { it.name })
        assertFails { db.placeQueries.insertPlace("p2", "t1", "Nowhere", null, 91.0, 0.0, null, "user", "h", "d") }
        assertFails { db.placeQueries.insertPlace("p3", "t1", "Half", null, 10.0, null, null, "user", "h", "d") }
        assertFails { db.placeQueries.insertPlace("p4", "missing-trip", "Orphan", null, null, null, null, "user", "h", "d") }
        assertFails { db.dayQueries.insertDay("d2", "t1", 1, null, null, "h", "d") } // duplicate day index
        assertFails { db.dayQueries.insertDay("d3", "t1", 0, null, null, "h", "d") }
    }
}
