package app.viajor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.viajor.domain.TileSourcePresets
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_back
import viajorapp.ui.generated.resources.map_attribution
import viajorapp.ui.generated.resources.map_no_coordinates
import viajorapp.ui.generated.resources.map_note
import viajorapp.ui.generated.resources.map_title

/**
 * The places of a trip over the base map. Places without coordinates are only listed. The tile provider and its
 * attribution are always shown, and the text says the base map needs a connection (ADR-0003).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(deps: AppDependencies, navigator: Navigator, tripId: String) {
    val places by remember { deps.itinerary.observePlaces(tripId) }.collectAsState(emptyList())
    val source = TileSourcePresets.openFreeMap
    val points = places.mapNotNull { it.point() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.map_title)) },
                navigationIcon = { TextButton(onClick = { navigator.back() }) { Text(stringResource(Res.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                MapSurface(source = source, points = points, modifier = Modifier.fillMaxSize())
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.map_attribution, source.attribution), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(Res.string.map_note, source.displayName), style = MaterialTheme.typography.bodySmall)
                if (points.isEmpty()) Text(stringResource(Res.string.map_no_coordinates), style = MaterialTheme.typography.bodySmall)
                places.forEach { place ->
                    val point = place.point()
                    Text(
                        if (point != null) "${place.name} (${point.latitude}, ${point.longitude})" else place.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
