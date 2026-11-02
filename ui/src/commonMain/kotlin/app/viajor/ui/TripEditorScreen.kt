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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.ui.presenter.TripEditorPresenter
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
import viajorapp.ui.generated.resources.field_description
import viajorapp.ui.generated.resources.field_title
import viajorapp.ui.generated.resources.label_end_date
import viajorapp.ui.generated.resources.label_start_date
import viajorapp.ui.generated.resources.load_error
import viajorapp.ui.generated.resources.trip_not_found

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditorScreen(deps: AppDependencies, navigator: Navigator, tripId: String?) {
    val scope = rememberCoroutineScope()
    val presenter = remember { TripEditorPresenter(deps.trips, scope, tripId) }
    val state by presenter.state.collectAsState()

    LaunchedEffect(state.savedTripId) {
        val saved = state.savedTripId ?: return@LaunchedEffect
        if (tripId == null) navigator.replace(Screen.Itinerary(saved)) else navigator.back()
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
            DateField(stringResource(Res.string.label_start_date), state.startDate, { presenter.setDates(it, state.endDate) })
            DateField(stringResource(Res.string.label_end_date), state.endDate, { presenter.setDates(state.startDate, it) })
            state.errors.forEach { Text(it.message(), color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { presenter.save() }, enabled = !state.saving) { Text(stringResource(Res.string.action_save)) }
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
