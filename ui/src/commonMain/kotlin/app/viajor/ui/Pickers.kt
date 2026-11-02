package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_cancel
import viajorapp.ui.generated.resources.action_clear
import viajorapp.ui.generated.resources.action_ok
import viajorapp.ui.generated.resources.value_not_set
import kotlin.time.Instant

/** A button that shows the chosen date and opens the Material date picker; "Clear" removes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }) { Text(label + ": " + (value?.toString() ?: stringResource(Res.string.value_not_set))) }
        if (value != null) TextButton(onClick = { onChange(null) }) { Text(stringResource(Res.string.action_clear)) }
    }
    if (open) {
        // The picker works in UTC midnights, so convert both ways in UTC to avoid off-by-one days.
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) }
                    open = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(Res.string.action_cancel)) } },
        ) { DatePicker(state) }
    }
}

/** A button that shows the chosen time and opens the Material time picker (24-hour); "Clear" removes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(label: String, value: LocalTime?, onChange: (LocalTime?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }) { Text(label + ": " + (value?.let(::hhmm) ?: stringResource(Res.string.value_not_set))) }
        if (value != null) TextButton(onClick = { onChange(null) }) { Text(stringResource(Res.string.action_clear)) }
    }
    if (open) {
        val state = rememberTimePickerState(initialHour = value?.hour ?: 9, initialMinute = value?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { onChange(LocalTime(state.hour, state.minute)); open = false }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
}

internal fun hhmm(t: LocalTime): String = t.hour.toString().padStart(2, '0') + ":" + t.minute.toString().padStart(2, '0')
