package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.data.TripRecord
import app.viajor.ui.presenter.ListStatus
import app.viajor.ui.presenter.TripListPresenter
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_archive
import viajorapp.ui.generated.resources.action_create_trip
import viajorapp.ui.generated.resources.action_delete
import viajorapp.ui.generated.resources.action_edit
import viajorapp.ui.generated.resources.action_open
import viajorapp.ui.generated.resources.action_settings
import viajorapp.ui.generated.resources.action_undo
import viajorapp.ui.generated.resources.empty_trips_body
import viajorapp.ui.generated.resources.empty_trips_title
import viajorapp.ui.generated.resources.load_error
import viajorapp.ui.generated.resources.tagline
import viajorapp.ui.generated.resources.trip_dates
import viajorapp.ui.generated.resources.trip_deleted
import viajorapp.ui.generated.resources.trip_undated
import viajorapp.ui.generated.resources.trips_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(deps: AppDependencies, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val presenter = remember { TripListPresenter(deps.trips, scope) }
    val state by presenter.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val deletedMessage = stringResource(Res.string.trip_deleted)
    val undoLabel = stringResource(Res.string.action_undo)

    LaunchedEffect(state.undoableDelete) {
        if (state.undoableDelete == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) presenter.undoDelete() else presenter.dismissUndo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.trips_title)) },
                actions = { TextButton(onClick = { navigator.open(Screen.Settings) }) { Text(stringResource(Res.string.action_settings)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // The empty state already has its own create button.
            if (state.status == ListStatus.CONTENT) {
                ExtendedFloatingActionButton(onClick = { navigator.open(Screen.TripEditor(null)) }) {
                    Text(stringResource(Res.string.action_create_trip))
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.status) {
                ListStatus.LOADING -> CircularProgressIndicator(Modifier.padding(24.dp))
                ListStatus.ERROR -> Text(stringResource(Res.string.load_error), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                ListStatus.EMPTY -> EmptyTrips { navigator.open(Screen.TripEditor(null)) }
                ListStatus.CONTENT -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.trips, key = { it.id }) { trip ->
                        TripCard(
                            trip,
                            onOpen = { navigator.open(Screen.Itinerary(trip.id)) },
                            onEdit = { navigator.open(Screen.TripEditor(trip.id)) },
                            onArchive = { presenter.archive(trip) },
                            onDelete = { presenter.delete(trip) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTrips(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.tagline), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(Res.string.empty_trips_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.empty_trips_body), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCreate) { Text(stringResource(Res.string.action_create_trip)) }
    }
}

@Composable
private fun TripCard(trip: TripRecord, onOpen: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(trip.title, style = MaterialTheme.typography.titleMedium)
            val start = trip.startDate
            val end = trip.endDate
            Text(
                if (start != null && end != null) stringResource(Res.string.trip_dates, start.toString(), end.toString()) else stringResource(Res.string.trip_undated),
                style = MaterialTheme.typography.bodyMedium,
            )
            trip.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen) { Text(stringResource(Res.string.action_open)) }
                TextButton(onClick = onEdit) { Text(stringResource(Res.string.action_edit)) }
                TextButton(onClick = onArchive) { Text(stringResource(Res.string.action_archive)) }
                TextButton(onClick = onDelete) { Text(stringResource(Res.string.action_delete)) }
            }
        }
    }
}
