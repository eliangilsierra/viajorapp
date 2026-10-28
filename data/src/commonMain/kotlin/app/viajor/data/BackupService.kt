package app.viajor.data

import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.BackupActivity
import app.viajor.domain.BackupDay
import app.viajor.domain.BackupDocument
import app.viajor.domain.BackupPlace
import app.viajor.domain.BackupTrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** How many records a restore added, for the confirmation message. */
data class RestoreSummary(val trips: Int, val days: Int, val places: Int, val activities: Int)

interface BackupService {
    /** Everything the user has, as a document ready for `BackupCodec.encode`. Deleted items are not included. */
    suspend fun export(nowMillis: Long): BackupDocument

    /**
     * Adds the trips of [document] as new trips with new ids. Existing data is never changed or replaced.
     * The whole restore is one transaction: if anything fails nothing is written.
     */
    suspend fun restore(document: BackupDocument): RestoreSummary

    /** Erases every trip, day, place, activity and the search index. This cannot be undone. */
    suspend fun deleteAllData()
}

class SqlBackupService(
    private val db: ViajorDatabase,
    private val stamper: ChangeStamper,
    private val ids: IdGenerator = IdGenerator.Random,
    private val io: CoroutineContext = Dispatchers.Default,
) : BackupService {
    override suspend fun export(nowMillis: Long): BackupDocument = withContext(io) {
        db.transactionWithResult {
            val trips = db.tripQueries.selectAllForBackup().executeAsList().map { trip ->
                BackupTrip(
                    id = trip.id,
                    title = trip.title,
                    description = trip.description,
                    startDate = trip.start_date,
                    endDate = trip.end_date,
                    archivedAt = trip.archived_at,
                    deletedAt = null,
                    updatedAtHlc = trip.updated_at_hlc,
                    updatedBy = trip.updated_by,
                    days = db.dayQueries.selectDaysForTrip(trip.id).executeAsList().map {
                        BackupDay(it.id, it.day_index.toInt(), it.date, it.title, null, it.updated_at_hlc, it.updated_by)
                    },
                    places = db.placeQueries.selectPlacesForTrip(trip.id).executeAsList().map {
                        BackupPlace(it.id, it.name, it.address, it.lat, it.lon, it.notes, it.source, null, it.updated_at_hlc, it.updated_by)
                    },
                    activities = db.activityQueries.selectActivitiesForTrip(trip.id).executeAsList().map {
                        BackupActivity(
                            it.id, it.day_id, it.place_id, it.title, it.notes, it.kind, it.start_local, it.end_local, it.zone_id,
                            it.sort_key, it.completed_at, it.source, null, it.updated_at_hlc, it.updated_by,
                        )
                    },
                )
            }
            BackupDocument(exportedAtMillis = nowMillis, trips = trips)
        }
    }

    override suspend fun restore(document: BackupDocument): RestoreSummary = withContext(io) {
        db.transactionWithResult {
            var days = 0
            var places = 0
            var activities = 0
            for (trip in document.trips) {
                val stamp = stamper.next()
                val tripId = ids.newId()
                db.tripQueries.insertTrip(tripId, trip.title, trip.description, trip.startDate, trip.endDate, stamp.hlc, stamp.deviceId)
                if (trip.archivedAt != null) db.tripQueries.setArchived(trip.archivedAt, stamp.hlc, stamp.deviceId, tripId)

                val dayIds = HashMap<String, String>()
                for (day in trip.days) {
                    val id = ids.newId().also { dayIds[day.id] = it }
                    db.dayQueries.insertDay(id, tripId, day.dayIndex.toLong(), day.date, day.title, stamp.hlc, stamp.deviceId)
                    days++
                }
                val placeIds = HashMap<String, String>()
                for (place in trip.places) {
                    val id = ids.newId().also { placeIds[place.id] = it }
                    db.placeQueries.insertPlace(id, tripId, place.name, place.address, place.lat, place.lon, place.notes, place.source, stamp.hlc, stamp.deviceId)
                    places++
                }
                for (a in trip.activities) {
                    val id = ids.newId()
                    db.activityQueries.insertActivity(
                        id, tripId, a.dayId?.let(dayIds::getValue), a.placeId?.let(placeIds::getValue), a.title, a.notes, a.kind,
                        a.startLocal, a.endLocal, a.zoneId, a.sortKey, a.source, stamp.hlc, stamp.deviceId,
                    )
                    if (a.completedAt != null) db.activityQueries.setCompleted(a.completedAt, stamp.hlc, stamp.deviceId, id)
                    activities++
                }
            }
            RestoreSummary(document.trips.size, days, places, activities)
        }
    }

    override suspend fun deleteAllData() = withContext(io) {
        db.transaction {
            // Children first: foreign keys are on.
            db.activityQueries.deleteAllActivities()
            db.placeQueries.deleteAllPlaces()
            db.dayQueries.deleteAllDays()
            db.tripQueries.deleteAllTrips()
            db.searchQueries.clearIndex()
        }
    }
}
