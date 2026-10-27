package app.viajor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.viajor.domain.TileSource
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle

/**
 * The only place that touches the map library (ADR-0003), so it can be replaced.
 * Attribution for [source] must be shown next to this surface by the caller.
 */
@Composable
fun MapSurface(source: TileSource, modifier: Modifier = Modifier) {
    val state = rememberMapState(baseStyle = BaseStyle.Uri(source.styleUrl))
    MaplibreMap(modifier = modifier, state = state)
}
