package app.viajor.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.FractionalIndex
import app.viajor.domain.Provenance
import app.viajor.domain.ZonedTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.coroutines.CoroutineContext

interface ItineraryRepository {
    fun observeDays(tripId: String): Flow<List<DayRecord>>

    fun observeActivities(dayId: String): Flow<List<ActivityRecord>>

    /** Activities of a trip that are not on any day (ideas). */
    fun observeUnscheduled(tripId: String): Flow<List<ActivityRecord>>

    fun observePlaces(tripId: String): Flow<List<PlaceRecord>>

    /** Adds at the end of the day, or of the unscheduled list when [dayId] is null. */
    suspend fun addActivity(tripId: String, dayId: String?, draft: ActivityDraft, provenance: Provenance = Provenance.USER): SaveActivityResult

    suspend fun updateActivity(id: String, draft: ActivityDraft): SaveActivityResult

    /** Moves an activity to [toIndex] (0-based, clamped) among the activities of [toDayId] (null = unscheduled). */
    suspend fun moveActivity(id: String, toDayId: String?, toIndex: Int): SaveActivityResult

    suspend fun setCompleted(id: String, completed: Boolean)

    suspend fun deleteActivity(id: String)

    suspend fun restoreActivity(id: String)

    suspend fun addPlace(tripId: String, draft: PlaceDraft, provenance: Provenance = Provenance.USER): SavePlaceResult

    suspend fun updatePlace(id: String, draft: PlaceDraft): SavePlaceResult

    suspend fun deletePlace(id: String)
}

class SqlItineraryRepository(
    private val db: ViajorDatabase,
    private val stamper: ChangeStamper,
    private val ids: IdGenerator = IdGenerator.Random,
    private val io: CoroutineContext = Dispatchers.Default,
) : ItineraryRepository {
    override fun observeDays(tripId: String): Flow<List<DayRecord>> =
        db.dayQueries.selectDaysForTrip(tripId).asFlow().mapToList(io).map { rows ->
            rows.map { DayRecord(it.id, it.trip_id, it.day_index.toInt(), it.date?.let(LocalDate::parse), it.title) }
        }

    override fun observeActivities(dayId: String): Flow<List<ActivityRecord>> =
        db.activityQueries.selectForDay(dayId).asFlow().mapToList(io).map { rows -> rows.map { it.toRecord() } }

    override fun observeUnscheduled(tripId: String): Flow<List<ActivityRecord>> =
        db.activityQueries.selectUnscheduled(tripId).asFlow().mapToList(io).map { rows -> rows.map { it.toRecord() } }

    override fun observePlaces(tripId: String): Flow<List<PlaceRecord>> =
        db.placeQueries.selectPlacesForTrip(tripId).asFlow().mapToList(io).map { rows -> rows.map { it.toRecord() } }

    override suspend fun addActivity(tripId: String, dayId: String?, draft: ActivityDraft, provenance: Provenance): SaveActivityResult {
        val errors = validate(draft).toMutableList()
        return withContext(io) {
            db.transactionWithResult {
                val trip = db.tripQueries.selectById(tripId).executeAsOneOrNull()
                if (trip == null || trip.deleted_at != null) return@transactionWithResult SaveActivityResult.NotFound
                if (dayId != null) {
                    val day = db.dayQueries.selectDayById(dayId).executeAsOneOrNull()
                    if (day == null || day.deleted_at != null || day.trip_id != tripId) errors += ActivityError.DAY_NOT_FOUND
                }
                if (draft.placeId != null && !placeBelongsTo(draft.placeId, tripId)) errors += ActivityError.PLACE_NOT_FOUND
                if (errors.isNotEmpty()) return@transactionWithResult SaveActivityResult.Invalid(errors)

                val stamp = stamper.next()
                val last = if (dayId != null) {
                    db.activityQueries.lastSortKeyForDay(dayId).executeAsOneOrNull()
                } else {
                    db.activityQueries.lastSortKeyUnscheduled(tripId).executeAsOneOrNull()
                }
                val id = ids.newId()
                db.activityQueries.insertActivity(
                    id, tripId, dayId, draft.placeId, draft.title.trim(), draft.notes?.trim()?.ifEmpty { null }, draft.kind.dbValue,
                    draft.start?.format(), draft.end?.format(), draft.zoneId, FractionalIndex.between(last, null),
                    provenance.name.lowercase(), stamp.hlc, stamp.deviceId,
                )
                SaveActivityResult.Saved(id)
            }
        }
    }

    override suspend fun updateActivity(id: String, draft: ActivityDraft): SaveActivityResult {
        val errors = validate(draft).toMutableList()
        return withContext(io) {
            db.transactionWithResult {
                val row = db.activityQueries.selectActivityById(id).executeAsOneOrNull()
                if (row == null || row.deleted_at != null) return@transactionWithResult SaveActivityResult.NotFound
                if (draft.placeId != null && !placeBelongsTo(draft.placeId, row.trip_id)) errors += ActivityError.PLACE_NOT_FOUND
                if (errors.isNotEmpty()) return@transactionWithResult SaveActivityResult.Invalid(errors)
                val stamp = stamper.next()
                db.activityQueries.updateActivity(
                    draft.title.trim(), draft.notes?.trim()?.ifEmpty { null }, draft.kind.dbValue, draft.start?.format(), draft.end?.format(),
                    draft.zoneId, draft.placeId, stamp.hlc, stamp.deviceId, id,
                )
                SaveActivityResult.Saved(id)
            }
        }
    }

    override suspend fun moveActivity(id: String, toDayId: String?, toIndex: Int): SaveActivityResult = withContext(io) {
        db.transactionWithResult {
            val row = db.activityQueries.selectActivityById(id).executeAsOneOrNull()
            if (row == null || row.deleted_at != null) return@transactionWithResult SaveActivityResult.NotFound
            if (toDayId != null) {
                val day = db.dayQueries.selectDayById(toDayId).executeAsOneOrNull()
                if (day == null || day.deleted_at != null || day.trip_id != row.trip_id) {
                    return@transactionWithResult SaveActivityResult.Invalid(listOf(ActivityError.DAY_NOT_FOUND))
                }
            }
            val siblings = (
                if (toDayId != null) db.activityQueries.selectForDay(toDayId).executeAsList()
                else db.activityQueries.selectUnscheduled(row.trip_id).executeAsList()
                ).filter { it.id != id }
            val index = toIndex.coerceIn(0, siblings.size)
            val key = FractionalIndex.between(siblings.getOrNull(index - 1)?.sort_key, siblings.getOrNull(index)?.sort_key)
            val stamp = stamper.next()
            db.activityQueries.moveActivity(toDayId, key, stamp.hlc, stamp.deviceId, id)
            SaveActivityResult.Saved(id)
        }
    }

    override suspend fun setCompleted(id: String, completed: Boolean) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.activityQueries.setCompleted(if (completed) stamp.millis else null, stamp.hlc, stamp.deviceId, id)
        }
    }

    override suspend fun deleteActivity(id: String) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.activityQueries.softDeleteActivity(stamp.millis, stamp.hlc, stamp.deviceId, id)
        }
    }

    override suspend fun restoreActivity(id: String) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.activityQueries.restoreActivity(stamp.hlc, stamp.deviceId, id)
        }
    }

    override suspend fun addPlace(tripId: String, draft: PlaceDraft, provenance: Provenance): SavePlaceResult {
        val errors = validate(draft)
        if (errors.isNotEmpty()) return SavePlaceResult.Invalid(errors)
        return withContext(io) {
            db.transactionWithResult {
                val trip = db.tripQueries.selectById(tripId).executeAsOneOrNull()
                if (trip == null || trip.deleted_at != null) return@transactionWithResult SavePlaceResult.NotFound
                val stamp = stamper.next()
                val id = ids.newId()
                db.placeQueries.insertPlace(
                    id, tripId, draft.name.trim(), draft.address?.trim()?.ifEmpty { null }, draft.latitude, draft.longitude,
                    draft.notes?.trim()?.ifEmpty { null }, provenance.name.lowercase(), stamp.hlc, stamp.deviceId,
                )
                SavePlaceResult.Saved(id)
            }
        }
    }

    override suspend fun updatePlace(id: String, draft: PlaceDraft): SavePlaceResult {
        val errors = validate(draft)
        if (errors.isNotEmpty()) return SavePlaceResult.Invalid(errors)
        return withContext(io) {
            db.transactionWithResult {
                val row = db.placeQueries.selectPlaceById(id).executeAsOneOrNull()
                if (row == null || row.deleted_at != null) return@transactionWithResult SavePlaceResult.NotFound
                val stamp = stamper.next()
                db.placeQueries.updatePlace(
                    draft.name.trim(), draft.address?.trim()?.ifEmpty { null }, draft.latitude, draft.longitude,
                    draft.notes?.trim()?.ifEmpty { null }, stamp.hlc, stamp.deviceId, id,
                )
                SavePlaceResult.Saved(id)
            }
        }
    }

    override suspend fun deletePlace(id: String) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.placeQueries.softDeletePlace(stamp.millis, stamp.hlc, stamp.deviceId, id)
        }
    }

    private fun placeBelongsTo(placeId: String, tripId: String): Boolean {
        val place = db.placeQueries.selectPlaceById(placeId).executeAsOneOrNull()
        return place != null && place.deleted_at == null && place.trip_id == tripId
    }

    private fun validate(draft: ActivityDraft): List<ActivityError> {
        val errors = mutableListOf<ActivityError>()
        val title = draft.title.trim()
        if (title.isEmpty()) errors += ActivityError.EMPTY_TITLE
        if (title.length > MAX_TITLE) errors += ActivityError.TITLE_TOO_LONG
        val start = draft.start
        val end = draft.end
        if (start != null && end != null && end < start) errors += ActivityError.END_BEFORE_START
        if (start != null || end != null) {
            val zone = draft.zoneId
            if (zone.isNullOrBlank()) {
                errors += ActivityError.ZONE_REQUIRED
            } else {
                try {
                    ZonedTimes.zone(zone)
                } catch (e: IllegalArgumentException) {
                    errors += ActivityError.UNKNOWN_ZONE
                }
            }
        }
        return errors
    }

    private fun validate(draft: PlaceDraft): List<PlaceError> {
        val errors = mutableListOf<PlaceError>()
        val name = draft.name.trim()
        if (name.isEmpty()) errors += PlaceError.EMPTY_NAME
        if (name.length > MAX_TITLE) errors += PlaceError.NAME_TOO_LONG
        val lat = draft.latitude
        val lon = draft.longitude
        if ((lat == null) != (lon == null)) {
            errors += PlaceError.ONLY_ONE_COORDINATE
        } else if (lat != null && lon != null && (lat.isNaN() || lon.isNaN() || lat !in -90.0..90.0 || lon !in -180.0..180.0)) {
            errors += PlaceError.COORDINATE_OUT_OF_RANGE
        }
        return errors
    }

    /** `YYYY-MM-DDTHH:MM`: the schema stores times to the minute (ADR-0002). */
    private fun LocalDateTime.format(): String =
        "$date" + "T" + hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')

    private fun app.viajor.data.db.Activity.toRecord() = ActivityRecord(
        id = id,
        tripId = trip_id,
        dayId = day_id,
        placeId = place_id,
        title = title,
        notes = notes,
        kind = ActivityKind.fromDb(kind),
        start = start_local?.let(LocalDateTime::parse),
        end = end_local?.let(LocalDateTime::parse),
        zoneId = zone_id,
        sortKey = sort_key,
        completed = completed_at != null,
        provenance = Provenance.valueOf(source.uppercase()),
    )

    private fun app.viajor.data.db.Place.toRecord() = PlaceRecord(
        id = id,
        tripId = trip_id,
        name = name,
        address = address,
        latitude = lat,
        longitude = lon,
        notes = notes,
        provenance = Provenance.valueOf(source.uppercase()),
    )

    private companion object {
        const val MAX_TITLE = 200
    }
}
