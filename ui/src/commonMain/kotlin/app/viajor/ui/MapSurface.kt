package app.viajor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.viajor.domain.GeoPoint
import app.viajor.domain.TileSource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

/**
 * The only place that touches the map library (ADR-0003), so it can be replaced.
 * Attribution for [source] must be shown next to this surface by the caller.
 * [points] are drawn as circle markers; the camera starts centred on them.
 */
@Composable
fun MapSurface(source: TileSource, points: List<GeoPoint> = emptyList(), modifier: Modifier = Modifier) {
    // The initial camera is fixed when the map state is created, and places arrive after the first frame, so
    // the state is recreated when the points change.
    key(points) { MapContent(source, points, modifier) }
}

@Composable
private fun MapContent(source: TileSource, points: List<GeoPoint>, modifier: Modifier) {
    val center = if (points.isEmpty()) {
        Position(longitude = 0.0, latitude = 20.0)
    } else {
        Position(longitude = points.map { it.longitude }.average(), latitude = points.map { it.latitude }.average())
    }
    val zoom = if (points.isEmpty()) 1.0 else if (points.size == 1) 12.0 else 9.0
    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(source.styleUrl),
        initialCameraPosition = CameraPosition(center = center, zoom = zoom),
    ) {
        val markers = rememberGeoJsonSource(
            GeoJsonData.Features(FeatureCollection(points.map { Feature<Point, Nothing?>(geometry = Point(Position(longitude = it.longitude, latitude = it.latitude)), properties = null) })),
        )
        CircleLayer(
            id = "viajor-places",
            source = markers,
            radius = const(7.dp),
            color = const(Color(0xFFB85C00)),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp),
        )
    }
    MaplibreMap(modifier = modifier, state = state)
}
