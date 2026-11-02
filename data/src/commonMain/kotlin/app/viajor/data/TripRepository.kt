package app.viajor.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.DayPlanner
import app.viajor.domain.ExistingDay
import app.viajor.domain.FractionalIndex
import app.viajor.domain.TripInput
import app.viajor.domain.TripValidation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.coroutines.CoroutineContext

interface TripRepository {
    fun observeTrips(): Flow<List<TripRecord>>

    /** Emits null when the trip does not exist or is deleted. */
    fun observeTrip(id: String): Flow<TripRecord?>

    suspend fun createTrip(input: TripInput, description: String? = null): CreateTripResult

    suspend fun updateTrip(id: String, input: TripInput, description: String? = null, confirmDropDays: Boolean = false): UpdateTripResult

    /** Appends a day to an undated trip (dated trips grow by changing their end date). */
    suspend fun addDay(tripId: String): Boolean

    suspend fun deleteTrip(id: String)

    suspend fun restoreTrip(id: String)

    suspend fun setArchived(id: String, archived: Boolean)
}

/** Stores trips in SQLDelight. Every write is one transaction and carries a stamp (ADR-0002). */
class SqlTripRepository(
    private val db: ViajorDatabase,
    private val stamper: ChangeStamper,
    private val ids: IdGenerator = IdGenerator.Random,
    private val io: CoroutineContext = Dispatchers.Default,
) : TripRepository {
    override fun observeTrips(): Flow<List<TripRecord>> =
        db.tripQueries.selectActive().asFlow().mapToList(io).map { rows -> rows.map { it.toRecord() } }

    override fun observeTrip(id: String): Flow<TripRecord?> =
        db.tripQueries.selectById(id).asFlow().mapToOneOrNull(io).map { row -> row?.takeIf { it.deleted_at == null }?.toRecord() }

    override suspend fun createTrip(input: TripInput, description: String?): CreateTripResult {
        val errors = TripValidation.validate(input)
        if (errors.isNotEmpty()) return CreateTripResult.Invalid(errors)
        return withContext(io) {
            db.transactionWithResult {
                val stamp = stamper.next()
                val tripId = ids.newId()
                db.tripQueries.insertTrip(
                    tripId, input.title.trim(), description?.trim()?.ifEmpty { null },
                    input.startDate?.toString(), input.endDate?.toString(), stamp.hlc, stamp.deviceId,
                )
                val start = input.startDate
                val end = input.endDate
                val days = if (start != null && end != null) DayPlanner.daysFor(start, end) else DayPlanner.undated(1)
                for (day in days) {
                    db.dayQueries.insertDay(ids.newId(), tripId, day.index.toLong(), day.date?.toString(), null, stamp.hlc, stamp.deviceId)
                }
                CreateTripResult.Created(tripId)
            }
        }
    }

    override suspend fun updateTrip(id: String, input: TripInput, description: String?, confirmDropDays: Boolean): UpdateTripResult {
        val errors = TripValidation.validate(input)
        if (errors.isNotEmpty()) return UpdateTripResult.Invalid(errors)
        return withContext(io) {
            db.transactionWithResult {
                val trip = db.tripQueries.selectById(id).executeAsOneOrNull()
                if (trip == null || trip.deleted_at != null) return@transactionWithResult UpdateTripResult.NotFound

                val allDays = db.dayQueries.selectAllDaysForTrip(id).executeAsList()
                val activeDays = allDays.filter { it.deleted_at == null }
                val existing = activeDays.map { day ->
                    ExistingDay(day.day_index.toInt(), db.dayQueries.countActivitiesForDay(day.id).executeAsOne() > 0)
                }
                val plan = DayPlanner.resize(existing, input.startDate, input.endDate, newUndatedCount = activeDays.size.coerceAtLeast(1))
                if (plan.daysNeedingConfirmation.isNotEmpty() && !confirmDropDays) {
                    return@transactionWithResult UpdateTripResult.NeedsConfirmation(plan.daysNeedingConfirmation.map { it.index })
                }

                val stamp = stamper.next()
                db.tripQueries.updateTripDetails(
                    input.title.trim(), description?.trim()?.ifEmpty { null },
                    input.startDate?.toString(), input.endDate?.toString(), stamp.hlc, stamp.deviceId, id,
                )
                val byIndex = allDays.associateBy { it.day_index.toInt() }
                for (planned in plan.days) {
                    val row = byIndex[planned.index]
                    val date = planned.date?.toString()
                    when {
                        row == null -> db.dayQueries.insertDay(ids.newId(), id, planned.index.toLong(), date, null, stamp.hlc, stamp.deviceId)
                        row.deleted_at != null || row.date != date -> db.dayQueries.setDay(date, null, stamp.hlc, stamp.deviceId, row.id)
                    }
                }
                val dropped = (plan.emptyDaysToDrop + plan.daysNeedingConfirmation).map { it.index }.toSet()
                for (row in activeDays.filter { it.day_index.toInt() in dropped }) {
                    moveDayActivitiesToUnscheduled(id, row.id, stamp)
                    db.dayQueries.setDay(row.date, stamp.millis, stamp.hlc, stamp.deviceId, row.id)
                }
                UpdateTripResult.Updated
            }
        }
    }

    /** Dropped days never delete content: their activities go to the end of the trip's unscheduled list. */
    private fun moveDayActivitiesToUnscheduled(tripId: String, dayId: String, stamp: Stamp) {
        val activities = db.activityQueries.selectForDay(dayId).executeAsList()
        var last = db.activityQueries.lastSortKeyUnscheduled(tripId).executeAsOneOrNull()
        for (activity in activities) {
            val key = FractionalIndex.between(last, null)
            db.activityQueries.moveActivity(null, key, stamp.hlc, stamp.deviceId, activity.id)
            last = key
        }
    }

    override suspend fun addDay(tripId: String): Boolean = withContext(io) {
        db.transactionWithResult {
            val trip = db.tripQueries.selectById(tripId).executeAsOneOrNull()
            if (trip == null || trip.deleted_at != null || trip.start_date != null) return@transactionWithResult false
            val all = db.dayQueries.selectAllDaysForTrip(tripId).executeAsList()
            val activeCount = all.count { it.deleted_at == null }
            if (activeCount >= TripValidation.MAX_DAYS) return@transactionWithResult false
            val stamp = stamper.next()
            val nextIndex = activeCount + 1
            val revive = all.firstOrNull { it.day_index.toInt() == nextIndex }
            if (revive != null) {
                db.dayQueries.setDay(null, null, stamp.hlc, stamp.deviceId, revive.id)
            } else {
                db.dayQueries.insertDay(ids.newId(), tripId, nextIndex.toLong(), null, null, stamp.hlc, stamp.deviceId)
            }
            true
        }
    }

    override suspend fun deleteTrip(id: String) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.tripQueries.softDelete(stamp.millis, stamp.hlc, stamp.deviceId, id)
        }
    }

    override suspend fun restoreTrip(id: String) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.tripQueries.restore(stamp.hlc, stamp.deviceId, id)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean) = withContext(io) {
        db.transaction {
            val stamp = stamper.next()
            db.tripQueries.setArchived(if (archived) stamp.millis else null, stamp.hlc, stamp.deviceId, id)
        }
    }

    private fun app.viajor.data.db.Trip.toRecord() = TripRecord(
        id = id,
        title = title,
        description = description,
        startDate = start_date?.let(LocalDate::parse),
        endDate = end_date?.let(LocalDate::parse),
        archived = archived_at != null,
    )
}
