package app.viajor.ui

import androidx.compose.runtime.Composable
import app.viajor.data.ActivityError
import app.viajor.domain.TripError
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.error_activity_day_missing
import viajorapp.ui.generated.resources.error_activity_empty_title
import viajorapp.ui.generated.resources.error_activity_end_before_start
import viajorapp.ui.generated.resources.error_activity_place_missing
import viajorapp.ui.generated.resources.error_activity_title_too_long
import viajorapp.ui.generated.resources.error_activity_zone
import viajorapp.ui.generated.resources.error_trip_empty_title
import viajorapp.ui.generated.resources.error_trip_end_before_start
import viajorapp.ui.generated.resources.error_trip_only_one_date
import viajorapp.ui.generated.resources.error_trip_title_too_long
import viajorapp.ui.generated.resources.error_trip_too_many_days

@Composable
internal fun TripError.message(): String = stringResource(
    when (this) {
        TripError.EMPTY_TITLE -> Res.string.error_trip_empty_title
        TripError.TITLE_TOO_LONG -> Res.string.error_trip_title_too_long
        TripError.ONLY_ONE_DATE -> Res.string.error_trip_only_one_date
        TripError.END_BEFORE_START -> Res.string.error_trip_end_before_start
        TripError.TOO_MANY_DAYS -> Res.string.error_trip_too_many_days
    },
)

@Composable
internal fun ActivityError.message(): String = stringResource(
    when (this) {
        ActivityError.EMPTY_TITLE -> Res.string.error_activity_empty_title
        ActivityError.TITLE_TOO_LONG -> Res.string.error_activity_title_too_long
        ActivityError.END_BEFORE_START -> Res.string.error_activity_end_before_start
        ActivityError.ZONE_REQUIRED, ActivityError.UNKNOWN_ZONE -> Res.string.error_activity_zone
        ActivityError.DAY_NOT_FOUND -> Res.string.error_activity_day_missing
        ActivityError.PLACE_NOT_FOUND -> Res.string.error_activity_place_missing
    },
)
