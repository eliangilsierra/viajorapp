package app.viajor.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.date_no_year
import viajorapp.ui.generated.resources.date_with_year
import viajorapp.ui.generated.resources.month_short_1
import viajorapp.ui.generated.resources.month_short_10
import viajorapp.ui.generated.resources.month_short_11
import viajorapp.ui.generated.resources.month_short_12
import viajorapp.ui.generated.resources.month_short_2
import viajorapp.ui.generated.resources.month_short_3
import viajorapp.ui.generated.resources.month_short_4
import viajorapp.ui.generated.resources.month_short_5
import viajorapp.ui.generated.resources.month_short_6
import viajorapp.ui.generated.resources.month_short_7
import viajorapp.ui.generated.resources.month_short_8
import viajorapp.ui.generated.resources.month_short_9
import viajorapp.ui.generated.resources.trip_dates

private val monthNames = listOf(
    Res.string.month_short_1, Res.string.month_short_2, Res.string.month_short_3, Res.string.month_short_4,
    Res.string.month_short_5, Res.string.month_short_6, Res.string.month_short_7, Res.string.month_short_8,
    Res.string.month_short_9, Res.string.month_short_10, Res.string.month_short_11, Res.string.month_short_12,
)

/** A date for people to read ("Mar 1, 2027" or "1 mar 2027"). Files, exports and parsing keep ISO dates. */
@Composable
internal fun friendlyDate(date: LocalDate, withYear: Boolean = true): String {
    val month = stringResource(monthNames[date.month.ordinal])
    return stringResource(if (withYear) Res.string.date_with_year else Res.string.date_no_year, month, date.day, date.year)
}

/** "Mar 1 to Mar 3, 2027"; the start repeats the year only when it differs from the end's. */
@Composable
internal fun friendlyDateRange(start: LocalDate, end: LocalDate): String =
    stringResource(Res.string.trip_dates, friendlyDate(start, withYear = start.year != end.year), friendlyDate(end))
