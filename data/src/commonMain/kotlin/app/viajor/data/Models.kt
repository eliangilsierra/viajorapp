package app.viajor.data

import app.viajor.domain.Provenance
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** Plain records the rest of the app works with; they hide the generated SQLDelight row classes. */
data class TripRecord(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val archived: Boolean,
)

data class DayRecord(val id: String, val tripId: String, val index: Int, val date: LocalDate?, val title: String?)

data class PlaceRecord(
    val id: String,
    val tripId: String,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
    val provenance: Provenance,
)

enum class ActivityKind(val dbValue: String) {
    ACTIVITY("activity"),
    TRANSPORT("transport"),
    LODGING("lodging"),
    MEAL("meal"),
    NOTE("note"),
    ;

    companion object {
        fun fromDb(value: String): ActivityKind = entries.first { it.dbValue == value }
    }
}

data class ActivityRecord(
    val id: String,
    val tripId: String,
    val dayId: String?,
    val placeId: String?,
    val title: String,
    val notes: String?,
    val kind: ActivityKind,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val zoneId: String?,
    val sortKey: String,
    val completed: Boolean,
    val provenance: Provenance,
)

/** What a user enters for an activity. Times are local to [zoneId] and kept to the minute. */
data class ActivityDraft(
    val title: String,
    val notes: String? = null,
    val kind: ActivityKind = ActivityKind.ACTIVITY,
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val zoneId: String? = null,
    val placeId: String? = null,
)

data class PlaceDraft(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
)

enum class ActivityError { EMPTY_TITLE, TITLE_TOO_LONG, END_BEFORE_START, ZONE_REQUIRED, UNKNOWN_ZONE, DAY_NOT_FOUND, PLACE_NOT_FOUND }

enum class PlaceError { EMPTY_NAME, NAME_TOO_LONG, ONLY_ONE_COORDINATE, COORDINATE_OUT_OF_RANGE }

sealed interface CreateTripResult {
    data class Created(val tripId: String) : CreateTripResult

    data class Invalid(val errors: List<app.viajor.domain.TripError>) : CreateTripResult
}

sealed interface UpdateTripResult {
    data object Updated : UpdateTripResult

    data object NotFound : UpdateTripResult

    data class Invalid(val errors: List<app.viajor.domain.TripError>) : UpdateTripResult

    /**
     * Shortening the trip would remove days that hold activities. Nothing was changed. Call again with
     * `confirmDropDays = true` to continue; those activities become unscheduled ideas, never deleted.
     */
    data class NeedsConfirmation(val dayIndexes: List<Int>) : UpdateTripResult
}

sealed interface SaveActivityResult {
    data class Saved(val activityId: String) : SaveActivityResult

    data object NotFound : SaveActivityResult

    data class Invalid(val errors: List<ActivityError>) : SaveActivityResult
}

sealed interface SavePlaceResult {
    data class Saved(val placeId: String) : SavePlaceResult

    data object NotFound : SavePlaceResult

    data class Invalid(val errors: List<PlaceError>) : SavePlaceResult
}
