package app.viajor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import app.viajor.designsystem.ViajorTheme

/**
 * Application root shared by Android and iOS. The platform host builds [AppDependencies] and a [Navigator]
 * and wires its back button to [Navigator.back]. Screens keep no state across a host restart yet.
 */
@Composable
fun ViajorApp(deps: AppDependencies, navigator: Navigator) {
    ViajorTheme {
        val screen = navigator.current
        // The key gives every screen its own presenter and drops it when the user leaves.
        key(screen) {
            when (screen) {
                Screen.TripList -> TripListScreen(deps, navigator)
                is Screen.TripEditor -> TripEditorScreen(deps, navigator, screen.tripId)
                is Screen.Itinerary -> ItineraryScreen(deps, navigator, screen.tripId)
            }
        }
    }
}
