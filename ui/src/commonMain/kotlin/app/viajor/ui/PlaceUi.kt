package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.data.ActivityRecord
import app.viajor.data.PlaceDraft
import app.viajor.data.PlaceError
import app.viajor.data.PlaceRecord
import app.viajor.domain.GeoPoint
import app.viajor.domain.LegSource
import app.viajor.domain.TravelEstimate
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_add_place
import viajorapp.ui.generated.resources.action_cancel
import viajorapp.ui.generated.resources.action_delete
import viajorapp.ui.generated.resources.action_save
import viajorapp.ui.generated.resources.error_number_format
import viajorapp.ui.generated.resources.error_place_coordinates_range
import viajorapp.ui.generated.resources.error_place_empty_name
import viajorapp.ui.generated.resources.error_place_name_too_long
import viajorapp.ui.generated.resources.error_place_only_one_coordinate
import viajorapp.ui.generated.resources.estimate_line
import viajorapp.ui.generated.resources.field_latitude
import viajorapp.ui.generated.resources.field_longitude
import viajorapp.ui.generated.resources.field_notes
import viajorapp.ui.generated.resources.field_place
import viajorapp.ui.generated.resources.field_place_address
import viajorapp.ui.generated.resources.field_place_name
import viajorapp.ui.generated.resources.place_no_coordinates
import viajorapp.ui.generated.resources.place_none
import viajorapp.ui.generated.resources.section_places
import viajorapp.ui.generated.resources.unit_km
import viajorapp.ui.generated.resources.unit_meters
import viajorapp.ui.generated.resources.unit_minutes

internal fun PlaceRecord.point(): GeoPoint? {
    val lat = latitude
    val lon = longitude
    return if (lat != null && lon != null) GeoPoint(lat, lon) else null
}

/**
 * A straight-line walking estimate between two consecutive activities that both have places with coordinates.
 * It is always labelled as an estimate and never as a route (ADR-0004).
 */
@Composable
internal fun EstimateLine(from: ActivityRecord, to: ActivityRecord, places: Map<String, PlaceRecord>) {
    val a = from.placeId?.let(places::get)?.point() ?: return
    val b = to.placeId?.let(places::get)?.point() ?: return
    val leg = TravelEstimate.walking(a, b)
    if (leg.source != LegSource.ESTIMATE) return
    val meters = leg.distanceMeters ?: return
    val seconds = leg.durationSeconds ?: return
    val distance = if (meters < 1000) stringResource(Res.string.unit_meters, meters.toInt()) else stringResource(Res.string.unit_km, kmText(meters))
    val minutes = stringResource(Res.string.unit_minutes, ((seconds + 30) / 60).coerceAtLeast(1).toInt())
    Text(
        stringResource(Res.string.estimate_line, distance, minutes),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 56.dp, bottom = 4.dp),
    )
}

/** Kilometres with one decimal, without locale formatting (the unit string carries the language). */
internal fun kmText(meters: Long): String {
    val tenths = (meters + 50) / 100
    return "${tenths / 10}.${tenths % 10}"
}

@Composable
internal fun PlacesSection(places: List<PlaceRecord>, onAdd: () -> Unit, onDelete: (PlaceRecord) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.section_places), style = MaterialTheme.typography.titleMedium)
        places.forEach { place ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.bodyLarge)
                    val lat = place.latitude
                    val lon = place.longitude
                    Text(
                        if (lat != null && lon != null) "$lat, $lon" else stringResource(Res.string.place_no_coordinates),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { onDelete(place) }) { Text(stringResource(Res.string.action_delete)) }
            }
        }
        TextButton(onClick = onAdd) { Text(stringResource(Res.string.action_add_place)) }
    }
}

@Composable
internal fun PlaceDialog(errors: List<PlaceError>, onDismiss: () -> Unit, onSave: (PlaceDraft) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var numberError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.action_add_place)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.field_place_name)) }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text(stringResource(Res.string.field_place_address)) }, singleLine = true)
                OutlinedTextField(latText, { latText = it }, label = { Text(stringResource(Res.string.field_latitude)) }, singleLine = true)
                OutlinedTextField(lonText, { lonText = it }, label = { Text(stringResource(Res.string.field_longitude)) }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(Res.string.field_notes)) })
                if (numberError) Text(stringResource(Res.string.error_number_format), color = MaterialTheme.colorScheme.error)
                errors.forEach { Text(it.message(), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val lat = latText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                val lon = lonText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                numberError = (latText.isNotBlank() && lat == null) || (lonText.isNotBlank() && lon == null)
                if (!numberError) onSave(PlaceDraft(name, address.ifBlank { null }, lat, lon, notes.ifBlank { null }))
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun PlaceError.message(): String = stringResource(
    when (this) {
        PlaceError.EMPTY_NAME -> Res.string.error_place_empty_name
        PlaceError.NAME_TOO_LONG -> Res.string.error_place_name_too_long
        PlaceError.ONLY_ONE_COORDINATE -> Res.string.error_place_only_one_coordinate
        PlaceError.COORDINATE_OUT_OF_RANGE -> Res.string.error_place_coordinates_range
    },
)

/** Chooses the place of an activity from the trip's places, or none. */
@Composable
internal fun PlacePicker(places: List<PlaceRecord>, selectedId: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = places.firstOrNull { it.id == selectedId }
    Box {
        TextButton(onClick = { open = true }) {
            Text(stringResource(Res.string.field_place) + ": " + (selected?.name ?: stringResource(Res.string.place_none)))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(Res.string.place_none)) }, onClick = { open = false; onSelect(null) })
            places.forEach { place ->
                DropdownMenuItem(text = { Text(place.name) }, onClick = { open = false; onSelect(place.id) })
            }
        }
    }
}
