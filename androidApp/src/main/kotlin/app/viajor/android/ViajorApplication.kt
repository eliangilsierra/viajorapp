package app.viajor.android

import android.app.Application
import android.content.Context
import app.viajor.data.ChangeStamper
import app.viajor.data.DatabaseDriverFactory
import app.viajor.data.SqlBackupService
import app.viajor.data.SqlItineraryRepository
import app.viajor.data.SqlTripRepository
import app.viajor.data.db.ViajorDatabase
import app.viajor.ui.AppDependencies
import kotlinx.datetime.TimeZone
import java.util.UUID

/** Composition root: builds the database and repositories once for the process. No DI framework is used. */
class ViajorApplication : Application() {
    val dependencies: AppDependencies by lazy {
        val database = ViajorDatabase(DatabaseDriverFactory(this).create())
        val stamper = ChangeStamper(deviceId()) { System.currentTimeMillis() }
        AppDependencies(
            trips = SqlTripRepository(database, stamper),
            itinerary = SqlItineraryRepository(database, stamper),
            backup = SqlBackupService(database, stamper),
            timeZoneId = TimeZone.currentSystemDefault().id,
        )
    }

    /** A random id for this install, used only to tag changes (ADR-0002). It is not sent anywhere. */
    private fun deviceId(): String {
        val prefs = getSharedPreferences("viajor", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }
}
