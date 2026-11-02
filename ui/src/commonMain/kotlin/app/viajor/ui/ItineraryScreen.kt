package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.viajor.data.ActivityDraft
import app.viajor.data.ActivityRecord
import app.viajor.domain.PlanExport
import app.viajor.ui.presenter.DayWithActivities
import app.viajor.ui.presenter.ItineraryPresenter
import app.viajor.ui.presenter.ListStatus
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_add_activity
import viajorapp.ui.generated.resources.action_add_day
import viajorapp.ui.generated.resources.action_add_idea
import viajorapp.ui.generated.resources.action_back
import viajorapp.ui.generated.resources.action_cancel
import viajorapp.ui.generated.resources.action_delete
import viajorapp.ui.generated.resources.action_edit
import viajorapp.ui.generated.resources.action_export
import viajorapp.ui.generated.resources.action_import
import viajorapp.ui.generated.resources.export_csv
import viajorapp.ui.generated.resources.export_markdown
import viajorapp.ui.generated.resources.msg_failed
import viajorapp.ui.generated.resources.msg_plan_saved
import viajorapp.ui.generated.resources.action_more
import viajorapp.ui.generated.resources.action_move_down
import viajorapp.ui.generated.resources.action_move_up
import viajorapp.ui.generated.resources.action_save
import viajorapp.ui.generated.resources.action_undo
import viajorapp.ui.generated.resources.activity_deleted
import viajorapp.ui.generated.resources.cd_mark_done
import viajorapp.ui.generated.resources.day_heading
import viajorapp.ui.generated.resources.day_heading_dated
import viajorapp.ui.generated.resources.field_activity_title
import viajorapp.ui.generated.resources.field_notes
import viajorapp.ui.generated.resources.action_move_to
import viajorapp.ui.generated.resources.label_end_time
import viajorapp.ui.generated.resources.label_start_time
import viajorapp.ui.generated.resources.move_to_ideas
import viajorapp.ui.generated.resources.move_to_title
import viajorapp.ui.generated.resources.ideas_heading
import viajorapp.ui.generated.resources.load_error
import viajorapp.ui.generated.resources.no_activities
import viajorapp.ui.generated.resources.time_zone_note
import viajorapp.ui.generated.resources.times_need_date
import viajorapp.ui.generated.resources.trip_not_found

/** What the activity dialog edits: a new activity (existing == null) on [dayId], or an existing one. */
private data class EditTarget(val dayId: String?, val dayDate: LocalDate?, val existing: ActivityRecord?)

private fun LocalDateTime.hhmm() = hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(deps: AppDependencies, files: FileAccess, navigator: Navigator, tripId: String) {
    val scope = rememberCoroutineScope()
    val presenter = remember { ItineraryPresenter(deps.trips, deps.itinerary, scope, tripId) }
    val state by presenter.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var addingPlace by remember { mutableStateOf(false) }
    var editingPlace by remember { mutableStateOf<app.viajor.data.PlaceRecord?>(null) }
    var movingActivity by remember { mutableStateOf<ActivityRecord?>(null) }
    val deletedMessage = stringResource(Res.string.activity_deleted)
    val undoLabel = stringResource(Res.string.action_undo)
    val savedMessage = stringResource(Res.string.msg_plan_saved)
    val failedMessage = stringResource(Res.string.msg_failed)
    var exportMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.undoableDelete) {
        if (state.undoableDelete == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) presenter.undoDelete() else presenter.dismissUndo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.trip?.title.orEmpty()) },
                navigationIcon = { TextButton(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_back)) } },
                actions = {
                    TextButton(onClick = { navigator.open(Screen.Import(tripId)) }) { Text(stringResource(Res.string.action_import)) }
                    Box {
                        TextButton(onClick = { exportMenu = true }) { Text(stringResource(Res.string.action_export)) }
                        DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                            for (csv in listOf(false, true)) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (csv) Res.string.export_csv else Res.string.export_markdown)) },
                                    onClick = {
                                        exportMenu = false
                                        val plan = state.toExportPlan()
                                        if (plan != null) {
                                            scope.launch {
                                                val saved = try {
                                                    if (csv) files.saveText(plan.title.fileName() + ".csv", "text/csv", PlanExport.csv(plan))
                                                    else files.saveText(plan.title.fileName() + ".md", "text/markdown", PlanExport.markdown(plan))
                                                } catch (e: Exception) {
                                                    snackbar.showSnackbar(failedMessage)
                                                    false
                                                }
                                                if (saved) snackbar.showSnackbar(savedMessage)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.status) {
                ListStatus.LOADING -> CircularProgressIndicator(Modifier.padding(24.dp))
                ListStatus.ERROR -> Text(stringResource(Res.string.load_error), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                ListStatus.EMPTY -> Text(stringResource(Res.string.trip_not_found), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                ListStatus.CONTENT -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.days.forEach { dayWithActivities ->
                        dayItems(
                            dayWithActivities,
                            placesById = state.places.associateBy { it.id },
                            onToggle = { a, done -> presenter.setCompleted(a.id, done) },
                            onMove = { a, index -> presenter.move(a.id, dayWithActivities.day.id, index) },
                            onEdit = { a -> editing = EditTarget(dayWithActivities.day.id, dayWithActivities.day.date, a) },
                            onMoveTo = { a -> movingActivity = a },
                            onDelete = { a -> presenter.delete(a) },
                            onAdd = { editing = EditTarget(dayWithActivities.day.id, dayWithActivities.day.date, null) },
                        )
                    }
                    if (state.trip?.startDate == null) {
                        item(key = "add-day") { TextButton(onClick = { presenter.addDay() }) { Text(stringResource(Res.string.action_add_day)) } }
                    }
                    item(key = "ideas-heading") { Text(stringResource(Res.string.ideas_heading), style = MaterialTheme.typography.titleMedium) }
                    items(state.unscheduled, key = { it.id }) { a ->
                        val index = state.unscheduled.indexOf(a)
                        ActivityRow(
                            a, canMoveUp = index > 0, canMoveDown = index < state.unscheduled.lastIndex,
                            onToggle = { presenter.setCompleted(a.id, it) },
                            onMoveUp = { presenter.move(a.id, null, index - 1) },
                            onMoveDown = { presenter.move(a.id, null, index + 1) },
                            onEdit = { editing = EditTarget(null, null, a) },
                            onMoveTo = { movingActivity = a },
                            onDelete = { presenter.delete(a) },
                        )
                    }
                    item(key = "add-idea") { TextButton(onClick = { editing = EditTarget(null, null, null) }) { Text(stringResource(Res.string.action_add_idea)) } }
                    item(key = "places") {
                        PlacesSection(state.places, onAdd = { addingPlace = true }, onEdit = { editingPlace = it }, onDelete = { presenter.deletePlace(it.id) })
                    }
                }
            }
        }
    }

    if (addingPlace || editingPlace != null) {
        val existingPlace = editingPlace
        PlaceDialog(
            initial = existingPlace,
            errors = state.placeErrors,
            onDismiss = { presenter.clearErrors(); addingPlace = false; editingPlace = null },
            onSave = { draft ->
                presenter.clearErrors()
                val job = if (existingPlace == null) presenter.addPlace(draft) else presenter.editPlace(existingPlace.id, draft)
                scope.launch {
                    job.join()
                    if (presenter.state.value.placeErrors.isEmpty()) {
                        addingPlace = false
                        editingPlace = null
                    }
                }
            },
        )
    }

    movingActivity?.let { activity ->
        MoveDialog(
            days = state.days.map { it.day },
            onDismiss = { movingActivity = null },
            onPick = { dayId ->
                presenter.move(activity.id, dayId, Int.MAX_VALUE)
                movingActivity = null
            },
        )
    }

    editing?.let { target ->
        ActivityDialog(
            target = target,
            places = state.places,
            timeZoneId = deps.timeZoneId,
            errors = state.errors,
            onDismiss = { presenter.clearErrors(); editing = null },
            onSave = { draft ->
                presenter.clearErrors()
                val job = if (target.existing == null) presenter.addActivity(target.dayId, draft) else presenter.editActivity(target.existing.id, draft)
                scope.launch {
                    job.join()
                    if (presenter.state.value.errors.isEmpty()) editing = null
                }
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dayItems(
    day: DayWithActivities,
    placesById: Map<String, app.viajor.data.PlaceRecord>,
    onToggle: (ActivityRecord, Boolean) -> Unit,
    onMove: (ActivityRecord, Int) -> Unit,
    onEdit: (ActivityRecord) -> Unit,
    onMoveTo: (ActivityRecord) -> Unit,
    onDelete: (ActivityRecord) -> Unit,
    onAdd: () -> Unit,
) {
    item(key = "day-${day.day.id}") { DayHeading(day) }
    if (day.activities.isEmpty()) {
        item(key = "empty-${day.day.id}") { Text(stringResource(Res.string.no_activities), style = MaterialTheme.typography.bodyMedium) }
    }
    items(day.activities, key = { it.id }) { a ->
        val index = day.activities.indexOf(a)
        Column {
            ActivityRow(
                a, canMoveUp = index > 0, canMoveDown = index < day.activities.lastIndex,
                onToggle = { onToggle(a, it) },
                onMoveUp = { onMove(a, index - 1) },
                onMoveDown = { onMove(a, index + 1) },
                onEdit = { onEdit(a) },
                onMoveTo = { onMoveTo(a) },
                onDelete = { onDelete(a) },
            )
            day.activities.getOrNull(index + 1)?.let { next -> EstimateLine(a, next, placesById) }
        }
    }
    item(key = "add-${day.day.id}") { TextButton(onClick = onAdd) { Text(stringResource(Res.string.action_add_activity)) } }
}

@Composable
private fun DayHeading(day: DayWithActivities) {
    val date = day.day.date
    Text(
        if (date != null) stringResource(Res.string.day_heading_dated, day.day.index, date.toString()) else stringResource(Res.string.day_heading, day.day.index),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ActivityRow(
    activity: ActivityRecord,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onMoveTo: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val doneLabel = stringResource(Res.string.cd_mark_done, activity.title)
    val moreLabel = stringResource(Res.string.action_more)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Checkbox(checked = activity.completed, onCheckedChange = onToggle, modifier = Modifier.semantics { contentDescription = doneLabel })
        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(activity.title, style = MaterialTheme.typography.bodyLarge)
            val start = activity.start
            val end = activity.end
            if (start != null) {
                Text(
                    if (end != null) "${start.hhmm()}–${end.hhmm()}" else start.hhmm(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            activity.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Box {
            TextButton(onClick = { menu = true }, modifier = Modifier.semantics { contentDescription = moreLabel }) { Text("⋮") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_edit)) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_move_up)) }, enabled = canMoveUp, onClick = { menu = false; onMoveUp() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_move_down)) }, enabled = canMoveDown, onClick = { menu = false; onMoveDown() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_move_to)) }, onClick = { menu = false; onMoveTo() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_delete)) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

@Composable
private fun ActivityDialog(
    target: EditTarget,
    places: List<app.viajor.data.PlaceRecord>,
    timeZoneId: String,
    errors: List<app.viajor.data.ActivityError>,
    onDismiss: () -> Unit,
    onSave: (ActivityDraft) -> Unit,
) {
    val existing = target.existing
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var startTime by remember { mutableStateOf(existing?.start?.time) }
    var endTime by remember { mutableStateOf(existing?.end?.time) }
    var placeId by remember { mutableStateOf(existing?.placeId) }
    val canHaveTimes = target.dayDate != null || existing?.start != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) Res.string.action_add_activity else Res.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(Res.string.field_activity_title)) }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(Res.string.field_notes)) })
                if (target.dayDate != null) {
                    TimeField(stringResource(Res.string.label_start_time), startTime, { startTime = it })
                    TimeField(stringResource(Res.string.label_end_time), endTime, { endTime = it })
                    Text(stringResource(Res.string.time_zone_note, timeZoneId), style = MaterialTheme.typography.bodySmall)
                } else if (target.dayId != null) {
                    Text(stringResource(Res.string.times_need_date), style = MaterialTheme.typography.bodySmall)
                }
                if (places.isNotEmpty()) PlacePicker(places, placeId) { placeId = it }
                errors.forEach { Text(it.message(), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = target.dayDate
                val start = startTime
                val end = endTime
                val hasTimes = canHaveTimes && date != null && (start != null || end != null)
                onSave(
                    ActivityDraft(
                        title = title,
                        notes = notes.ifBlank { null },
                        start = if (hasTimes && start != null) LocalDateTime(date!!, start) else null,
                        end = if (hasTimes && end != null) LocalDateTime(date!!, end) else null,
                        zoneId = if (hasTimes) existing?.zoneId ?: timeZoneId else existing?.zoneId,
                        placeId = placeId,
                        kind = existing?.kind ?: app.viajor.data.ActivityKind.ACTIVITY,
                    ),
                )
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun MoveDialog(days: List<app.viajor.data.DayRecord>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.move_to_title)) },
        text = {
            Column {
                days.forEach { day ->
                    val date = day.date
                    val label = if (date != null) stringResource(Res.string.day_heading_dated, day.index, date.toString()) else stringResource(Res.string.day_heading, day.index)
                    TextButton(onClick = { onPick(day.id) }) { Text(label) }
                }
                TextButton(onClick = { onPick(null) }) { Text(stringResource(Res.string.move_to_ideas)) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/** Turns what is on screen into the plain structure the exporters take; null while the trip is not loaded. */
private fun app.viajor.ui.presenter.ItineraryState.toExportPlan(): app.viajor.domain.ExportPlan? {
    val trip = trip ?: return null
    val names = places.associate { it.id to it.name }
    fun map(a: ActivityRecord) = app.viajor.domain.ExportActivity(
        title = a.title,
        notes = a.notes,
        start = a.start?.hhmm(),
        end = a.end?.hhmm(),
        place = a.placeId?.let(names::get),
        completed = a.completed,
    )
    return app.viajor.domain.ExportPlan(
        title = trip.title,
        description = trip.description,
        days = days.map { d -> app.viajor.domain.ExportDay(d.day.index, d.day.date?.toString(), d.activities.map(::map)) },
        ideas = unscheduled.map(::map),
    )
}

/** A safe file name stem: letters and digits kept, everything else becomes a dash. */
private fun String.fileName(): String =
    map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-').ifEmpty { "trip" }.take(60)
