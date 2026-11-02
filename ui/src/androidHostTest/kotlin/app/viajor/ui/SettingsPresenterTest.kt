package app.viajor.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.ChangeStamper
import app.viajor.data.IdGenerator
import app.viajor.data.SqlBackupService
import app.viajor.data.SqlTripRepository
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.TripInput
import app.viajor.ui.presenter.SettingsMessage
import app.viajor.ui.presenter.SettingsPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPresenterTest {
    private class FakeFiles : FileAccess {
        var saved: Triple<String, String, String>? = null
        var toPick: String? = null
        var cancelSave = false

        override suspend fun saveText(suggestedName: String, mimeType: String, text: String): Boolean {
            if (cancelSave) return false
            saved = Triple(suggestedName, mimeType, text)
            return true
        }

        override suspend fun pickText(mimeTypes: List<String>): String? = toPick
    }

    private var counter = 0
    private val db: ViajorDatabase
    private val trips: SqlTripRepository
    private val backup: SqlBackupService
    private val files = FakeFiles()

    init {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        ViajorDatabase.Schema.create(driver)
        db = ViajorDatabase(driver)
        val stamper = ChangeStamper("dev") { 1L }
        val ids = IdGenerator { "id-${++counter}" }
        trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
        backup = SqlBackupService(db, stamper, ids, EmptyCoroutineContext)
    }

    private fun TestScope.presenter() =
        SettingsPresenter(backup, files, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), nowMillis = { 1_800_000_000_000L })

    @Test
    fun exportWritesAValidBackupFileNamedWithTheDate() = runTest {
        trips.createTrip(TripInput("Lisbon", null, null))
        val p = presenter()
        p.exportBackup()
        assertEquals(SettingsMessage.Exported, p.state.value.message)
        val (name, mime, text) = files.saved!!
        assertEquals("viajorapp-backup-2027-01-15.json", name)
        assertEquals("application/json", mime)
        assertTrue("\"Lisbon\"" in text)
        assertEquals(false, p.state.value.busy)
    }

    @Test
    fun aCancelledSaveOrPickChangesNothing() = runTest {
        files.cancelSave = true
        val p = presenter()
        p.exportBackup()
        assertEquals(SettingsMessage.Cancelled, p.state.value.message)
        files.toPick = null
        p.restoreBackup()
        assertEquals(SettingsMessage.Cancelled, p.state.value.message)
    }

    @Test
    fun restoreReadsABackupAndAddsItsTrips() = runTest {
        trips.createTrip(TripInput("Original", null, null))
        val p = presenter()
        p.exportBackup()
        files.toPick = files.saved!!.third
        p.restoreBackup()
        val message = assertIs<SettingsMessage.Restored>(p.state.value.message)
        assertEquals(1, message.summary.trips)
        assertEquals(2, trips.observeTrips().first().size)
    }

    @Test
    fun aBadFileIsRejectedAndNothingIsChanged() = runTest {
        trips.createTrip(TripInput("Original", null, null))
        val p = presenter()
        for (bad in listOf("not json at all", "", "{\"format\":\"other\"}")) {
            files.toPick = bad
            p.restoreBackup()
            assertEquals(SettingsMessage.NotABackup, p.state.value.message, bad)
        }
        assertEquals(1, trips.observeTrips().first().size)
    }

    @Test
    fun deleteAllErasesEverything() = runTest {
        trips.createTrip(TripInput("Lisbon", null, null))
        val p = presenter()
        p.deleteAllData()
        assertEquals(SettingsMessage.Erased, p.state.value.message)
        assertEquals(emptyList(), trips.observeTrips().first())
        p.clearMessage()
        assertEquals(null, p.state.value.message)
    }
}
