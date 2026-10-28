package app.viajor.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

enum class TripError { EMPTY_TITLE, TITLE_TOO_LONG, ONLY_ONE_DATE, END_BEFORE_START, TOO_MANY_DAYS }

/** Raw user input for creating or editing a trip. Dates are both set or both null (an undated trip). */
data class TripInput(val title: String, val startDate: LocalDate?, val endDate: LocalDate?)

object TripValidation {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_DAYS = 366

    /** All problems at once, so a form can show every error; an empty list means the input is valid. */
    fun validate(input: TripInput): List<TripError> {
        val errors = mutableListOf<TripError>()
        val title = input.title.trim()
        if (title.isEmpty()) errors += TripError.EMPTY_TITLE
        if (title.length > MAX_TITLE_LENGTH) errors += TripError.TITLE_TOO_LONG
        val start = input.startDate
        val end = input.endDate
        if ((start == null) != (end == null)) {
            errors += TripError.ONLY_ONE_DATE
        } else if (start != null && end != null) {
            if (end < start) errors += TripError.END_BEFORE_START
            else if (start.daysUntil(end) + 1 > MAX_DAYS) errors += TripError.TOO_MANY_DAYS
        }
        return errors
    }
}

/** A day of a trip: 1-based [index], with a [date] only when the trip is dated. */
data class PlannedDay(val index: Int, val date: LocalDate?)

/** A day already stored, and whether the user has put anything on it. */
data class ExistingDay(val index: Int, val hasContent: Boolean)

/**
 * The result of changing a trip's length. Nothing is removed by the planner: [emptyDaysToDrop] can go
 * without asking; [daysNeedingConfirmation] hold content and must be confirmed (or kept) by the caller.
 */
data class ResizePlan(
    val days: List<PlannedDay>,
    val emptyDaysToDrop: List<ExistingDay>,
    val daysNeedingConfirmation: List<ExistingDay>,
)

object DayPlanner {
    /** One day per date, inclusive, numbered from 1. */
    fun daysFor(start: LocalDate, end: LocalDate): List<PlannedDay> {
        require(end >= start) { "end must not precede start" }
        val count = start.daysUntil(end) + 1
        require(count <= TripValidation.MAX_DAYS) { "a trip has at most ${TripValidation.MAX_DAYS} days" }
        return List(count) { PlannedDay(it + 1, start.plus(it, DateTimeUnit.DAY)) }
    }

    /** Days of an undated trip: numbered from 1 with no dates. */
    fun undated(count: Int): List<PlannedDay> {
        require(count in 1..TripValidation.MAX_DAYS) { "day count must be between 1 and ${TripValidation.MAX_DAYS}" }
        return List(count) { PlannedDay(it + 1, null) }
    }

    /**
     * Plans a change to [newStart]..[newEnd] (or [newUndatedCount] days when undated). Existing days keep
     * their index, so content moves with its day when the start date shifts; only days beyond the new length
     * are candidates for removal.
     */
    fun resize(
        existing: List<ExistingDay>,
        newStart: LocalDate?,
        newEnd: LocalDate?,
        newUndatedCount: Int = existing.size.coerceAtLeast(1),
    ): ResizePlan {
        val days = if (newStart != null && newEnd != null) daysFor(newStart, newEnd) else undated(newUndatedCount)
        val beyond = existing.filter { it.index > days.size }.sortedBy { it.index }
        return ResizePlan(
            days = days,
            emptyDaysToDrop = beyond.filter { !it.hasContent },
            daysNeedingConfirmation = beyond.filter { it.hasContent },
        )
    }
}
