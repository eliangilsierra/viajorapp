package app.viajor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.viajor.data.TripRecord
import app.viajor.data.TripSummary
import app.viajor.ui.presenter.HeroKind
import app.viajor.ui.presenter.HeroTrip
import app.viajor.ui.presenter.ListStatus
import app.viajor.ui.presenter.TripListPresenter
import app.viajor.ui.presenter.pickHeroTrip
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_archive
import viajorapp.ui.generated.resources.action_create_trip
import viajorapp.ui.generated.resources.action_delete
import viajorapp.ui.generated.resources.action_edit
import viajorapp.ui.generated.resources.action_more
import viajorapp.ui.generated.resources.action_undo
import viajorapp.ui.generated.resources.brand_symbol_dark
import viajorapp.ui.generated.resources.empty_trips_body
import viajorapp.ui.generated.resources.empty_trips_title
import viajorapp.ui.generated.resources.hero_next
import viajorapp.ui.generated.resources.hero_now
import viajorapp.ui.generated.resources.ic_chevron
import viajorapp.ui.generated.resources.ic_more
import viajorapp.ui.generated.resources.load_error
import viajorapp.ui.generated.resources.tagline
import viajorapp.ui.generated.resources.trip_dates
import viajorapp.ui.generated.resources.trip_deleted
import viajorapp.ui.generated.resources.trip_stats
import viajorapp.ui.generated.resources.trip_undated
import viajorapp.ui.generated.resources.trips_title
import kotlin.time.Clock

@Composable
fun TripListScreen(deps: AppDependencies, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val presenter = remember { TripListPresenter(deps.trips, scope) }
    val state by presenter.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val deletedMessage = stringResource(Res.string.trip_deleted)
    val undoLabel = stringResource(Res.string.action_undo)
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    LaunchedEffect(state.undoableDelete) {
        if (state.undoableDelete == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) presenter.undoDelete() else presenter.dismissUndo()
    }

    Scaffold(
        topBar = { BrandHeader { StoredOnDeviceChip() } },
        bottomBar = { MainNavigationBar(Screen.TripList, navigator) },
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
                ListStatus.CONTENT -> {
                    val hero = remember(state.trips, today) { pickHeroTrip(state.trips, today) }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (hero != null) item(key = "hero") { HeroCard(hero) { navigator.open(Screen.Itinerary(hero.summary.trip.id)) } }
                        item(key = "heading") {
                            Text(stringResource(Res.string.trips_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                        }
                        itemsIndexed(state.trips, key = { _, it -> it.trip.id }) { index, summary ->
                            TripCard(
                                number = index + 1,
                                summary = summary,
                                onOpen = { navigator.open(Screen.Itinerary(summary.trip.id)) },
                                onEdit = { navigator.open(Screen.TripEditor(summary.trip.id)) },
                                onArchive = { presenter.archive(summary.trip) },
                                onDelete = { presenter.delete(summary.trip) },
                            )
                        }
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
        BrandSymbol(96.dp)
        Text(stringResource(Res.string.tagline), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(Res.string.empty_trips_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.empty_trips_body), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCreate) { Text(stringResource(Res.string.action_create_trip)) }
    }
}

/** The highlighted trip: a teal card with the symbol as a watermark. The label is true ("next" or "now"). */
@Composable
private fun HeroCard(hero: HeroTrip, onOpen: () -> Unit) {
    val trip = hero.summary.trip
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onOpen),
    ) {
        Image(
            painter = painterResource(Res.drawable.brand_symbol_dark),
            contentDescription = null,
            alpha = 0.16f,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 48.dp, y = (-24).dp).size(200.dp),
        )
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(if (hero.kind == HeroKind.NEXT) Res.string.hero_next else Res.string.hero_now).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
            Text(trip.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
            Text(dateRange(trip), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary)
            Text(stats(hero.summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
            StoredOnDeviceChip(Modifier.padding(top = 8.dp))
        }
    }
}

/** A trip as an outlined card: numbered badge, title, dates and counts, a menu and a chevron. */
@Composable
private fun TripCard(number: Int, summary: TripSummary, onOpen: () -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    val trip = summary.trip
    var menu by remember { mutableStateOf(false) }
    val moreLabel = stringResource(Res.string.action_more)
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape).clickable(onClick = onOpen)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(trip.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(dateRange(trip), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stats(summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(painterResource(Res.drawable.ic_more), contentDescription = moreLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_edit)) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_archive)) }, onClick = { menu = false; onArchive() })
                DropdownMenuItem(text = { Text(stringResource(Res.string.action_delete)) }, onClick = { menu = false; onDelete() })
            }
        }
        Icon(painterResource(Res.drawable.ic_chevron), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun dateRange(trip: TripRecord): String {
    val start = trip.startDate
    val end = trip.endDate
    return if (start != null && end != null) stringResource(Res.string.trip_dates, start.toString(), end.toString()) else stringResource(Res.string.trip_undated)
}

@Composable
private fun stats(summary: TripSummary): String =
    stringResource(Res.string.trip_stats, summary.dayCount, summary.placeCount, summary.activityCount)
