package app.viajor.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.db.ViajorDatabase
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationTest {
    // The exact v1 schema (data/src/commonMain/sqldelight/databases/1.db, generated from the v1 sources).
    private val v1 = """
        CREATE TABLE trip (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL CHECK (length(title) > 0),
            description TEXT,
            start_date TEXT,
            end_date TEXT,
            archived_at INTEGER,
            deleted_at INTEGER,
            updated_at_hlc TEXT NOT NULL,
            updated_by TEXT NOT NULL,
            CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date)
        )
    """.trimIndent()

    @Test
    fun upgradingAPopulatedV1DatabaseKeepsTheTripAndAddsTheItineraryTables() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        driver.execute(null, v1, 0)
        driver.execute(null, "CREATE INDEX trip_active ON trip(deleted_at, archived_at)", 0)
        driver.execute(
            null,
            "INSERT INTO trip(id, title, start_date, updated_at_hlc, updated_by) VALUES ('t1', 'Old trip', '2026-05-01', 'h', 'd')",
            0,
        )

        assertEquals(3L, ViajorDatabase.Schema.version)
        ViajorDatabase.Schema.migrate(driver, 1L, 3L)

        val db = ViajorDatabase(driver)
        assertEquals(listOf("Old trip"), db.tripQueries.selectActive().executeAsList().map { it.title })
        db.dayQueries.insertDay("d1", "t1", 1, null, null, "h", "d")
        db.activityQueries.insertActivity("a1", "t1", "d1", null, "Walk", null, "activity", null, null, null, "V", "user", "h", "d")
        assertEquals(1, db.activityQueries.selectForDay("d1").executeAsList().size)

        val tables = driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
            { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(0)!!
                QueryResult.Value(names)
            },
            0,
        ).value
        assertEquals(
            listOf("activity", "place", "search_index", "trip", "trip_day"),
            tables.filter { !it.startsWith("sqlite_") && !it.startsWith("search_index_") },
        )
    }

    @Test
    fun upgradingAPopulatedV2DatabaseMakesExistingRowsSearchable() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        driver.execute(null, v1, 0)
        driver.execute(null, "CREATE INDEX trip_active ON trip(deleted_at, archived_at)", 0)
        driver.execute(null, "INSERT INTO trip(id, title, updated_at_hlc, updated_by) VALUES ('t1', 'Old trip', 'h', 'd')", 0)
        ViajorDatabase.Schema.migrate(driver, 1L, 2L)
        driver.execute(null, "INSERT INTO place(id, trip_id, name, address, updated_at_hlc, updated_by) VALUES ('p1', 't1', 'Alfama viewpoint', 'Rua Norte', 'h', 'd')", 0)
        driver.execute(null, "INSERT INTO trip_day(id, trip_id, day_index, updated_at_hlc, updated_by) VALUES ('d1', 't1', 1, 'h', 'd')", 0)
        driver.execute(
            null,
            "INSERT INTO activity(id, trip_id, day_id, title, notes, sort_key, updated_at_hlc, updated_by) VALUES ('a1', 't1', 'd1', 'Café da manhã', 'pastéis', 'V', 'h', 'd')",
            0,
        )

        ViajorDatabase.Schema.migrate(driver, 2L, 3L)

        val db = ViajorDatabase(driver)
        assertEquals(listOf("p1"), db.searchQueries.search("alfama*", null, 10).executeAsList().map { it.ref_id })
        assertEquals(listOf("a1"), db.searchQueries.search("cafe* past*", null, 10).executeAsList().map { it.ref_id })
    }
}
