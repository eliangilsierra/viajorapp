package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.ui.presenter.TripEditorPresenter
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_back
import viajorapp.ui.generated.resources.action_cancel
import viajorapp.ui.generated.resources.action_continue
import viajorapp.ui.generated.resources.action_save
import viajorapp.ui.generated.resources.confirm_shorten_body
import viajorapp.ui.generated.resources.confirm_shorten_title
import viajorapp.ui.generated.resources.editor_title_edit
import viajorapp.ui.generated.resources.editor_title_new
import viajorapp.ui.generated.resources.error_date_format
import viajorapp.ui.generated.resources.field_description
import viajorapp.ui.generated.resources.field_end_date
import viajorapp.ui.generated.resources.field_start_date
import viajorapp.ui.generated.resources.field_title
import viajorapp.ui.generated.resources.load_error
import viajorapp.ui.generated.resources.trip_not_found

/** Parses an optional ISO date. Blank is valid and means "no date"; anything else must be YYYY-MM-DD. */
private fun parseDate(text: String): Pair<LocalDate?, Boolean> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null to true
    return try {
        LocalDate.parse(trimmed) to true
    } catch (e: IllegalArgumentException) {
        null to false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditorScreen(deps: AppDependencies, navigator: Navigator, tripId: String?) {
    val scope = rememberCoroutineScope()
    val presenter = remember { TripEditorPresenter(deps.trips, scope, tripId) }
    val state by presenter.state.collectAsState()

    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var startOk by remember { mutableStateOf(true) }
    var endOk by remember { mutableStateOf(true) }

    // When an existing trip finishes loading, fill the date fields once.
    LaunchedEffect(state.startDate, state.endDate) {
        if (startText.isEmpty() && state.startDate != null) startText = state.startDate.toString()
        if (endText.isEmpty() && state.endDate != null) endText = state.endDate.toString()
    }
    LaunchedEffect(state.savedTripId) {
        val saved = state.savedTripId ?: return@LaunchedEffect
        if (tripId == null) navigator.replace(Screen.Itinerary(saved)) else navigator.back()
    }

    fun updateDates(start: String, end: String) {
        val (s, sOk) = parseDate(start)
        val (e, eOk) = parseDate(end)
        startOk = sOk
        endOk = eOk
        presenter.setDates(s, e)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (tripId == null) Res.string.editor_title_new else Res.string.editor_title_edit)) },
                navigationIcon = { TextButton(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.failed) Text(stringResource(Res.string.load_error), color = MaterialTheme.colorScheme.error)
            if (state.notFound) Text(stringResource(Res.string.trip_not_found), color = MaterialTheme.colorScheme.error)

            OutlinedTextField(
                value = state.title,
                onValueChange = presenter::setTitle,
                label = { Text(stringResource(Res.string.field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = presenter::setDescription,
                label = { Text(stringResource(Res.string.field_description)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it; updateDates(it, endText) },
                label = { Text(stringResource(Res.string.field_start_date)) },
                isError = !startOk,
                supportingText = { if (!startOk) Text(stringResource(Res.string.error_date_format)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it; updateDates(startText, it) },
                label = { Text(stringResource(Res.string.field_end_date)) },
                isError = !endOk,
                supportingText = { if (!endOk) Text(stringResource(Res.string.error_date_format)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.errors.forEach { Text(it.message(), color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (startOk && endOk) presenter.save() }, enabled = !state.saving) {
                    Text(stringResource(Res.string.action_save))
                }
                OutlinedButton(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_cancel)) }
            }
        }
    }

    if (state.daysNeedingConfirmation.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { presenter.setDates(state.startDate, state.endDate) },
            title = { Text(stringResource(Res.string.confirm_shorten_title)) },
            text = { Text(stringResource(Res.string.confirm_shorten_body, state.daysNeedingConfirmation.joinToString(", "))) },
            confirmButton = { TextButton(onClick = { presenter.save(confirmDropDays = true) }) { Text(stringResource(Res.string.action_continue)) } },
            dismissButton = {
                TextButton(onClick = { presenter.setDates(state.startDate, state.endDate) }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}
