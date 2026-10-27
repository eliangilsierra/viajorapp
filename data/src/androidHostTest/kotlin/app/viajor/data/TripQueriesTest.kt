package app.viajor.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.db.ViajorDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TripQueriesTest {
    private fun database(): ViajorDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ViajorDatabase.Schema.create(driver)
        return ViajorDatabase(driver)
    }

    @Test
    fun insertedTripIsListedAndSoftDeleteHidesIt() {
        val queries = database().tripQueries
        queries.insertTrip("t1", "Lisbon", null, "2027-03-01", "2027-03-05", "hlc-1", "device-a")
        assertEquals(listOf("Lisbon"), queries.selectActive().executeAsList().map { it.title })

        queries.softDelete(1_000L, "hlc-2", "device-a", "t1")
        assertEquals(emptyList(), queries.selectActive().executeAsList())

        queries.restore("hlc-3", "device-a", "t1")
        assertEquals(1, queries.selectActive().executeAsList().size)
    }

    @Test
    fun undatedTripsAreAllowedAndSortAfterDatedOnes() {
        val queries = database().tripQueries
        queries.insertTrip("a", "Someday", null, null, null, "h", "d")
        queries.insertTrip("b", "Dated", null, "2027-01-01", null, "h", "d")
        assertEquals(listOf("Dated", "Someday"), queries.selectActive().executeAsList().map { it.title })
    }

    @Test
    fun databaseRejectsEmptyTitleAndReversedDates() {
        val queries = database().tripQueries
        assertFails { queries.insertTrip("x", "", null, null, null, "h", "d") }
        assertFails { queries.insertTrip("y", "Bad", null, "2027-02-02", "2027-02-01", "h", "d") }
    }
}
