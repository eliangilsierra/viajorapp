package app.viajor.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.action_search
import viajorapp.ui.generated.resources.action_settings
import viajorapp.ui.generated.resources.ic_map
import viajorapp.ui.generated.resources.ic_route
import viajorapp.ui.generated.resources.ic_search
import viajorapp.ui.generated.resources.ic_sliders
import viajorapp.ui.generated.resources.map_title
import viajorapp.ui.generated.resources.trips_title

/** The four top-level destinations of the concept: Trips, Map, Search and Settings. */
@Composable
fun MainNavigationBar(current: Screen, navigator: Navigator) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = Color.Unspecified) {
        Item(Screen.TripList, current, navigator, Res.drawable.ic_route, stringResource(Res.string.trips_title), colors)
        Item(Screen.AllMap, current, navigator, Res.drawable.ic_map, stringResource(Res.string.map_title), colors)
        Item(Screen.Search, current, navigator, Res.drawable.ic_search, stringResource(Res.string.action_search), colors)
        Item(Screen.Settings, current, navigator, Res.drawable.ic_sliders, stringResource(Res.string.action_settings), colors)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Item(
    target: Screen,
    current: Screen,
    navigator: Navigator,
    icon: DrawableResource,
    label: String,
    colors: androidx.compose.material3.NavigationBarItemColors,
) {
    NavigationBarItem(
        selected = current == target,
        onClick = { if (current != target) navigator.switchTab(target) },
        icon = { Icon(painterResource(icon), contentDescription = null) },
        label = {
            // Four labels share the bar's width, so their text stops growing at 130% of the system size.
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, minOf(density.fontScale, 1.3f))) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        colors = colors,
    )
}
