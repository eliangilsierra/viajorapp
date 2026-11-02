package app.viajor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.viajor.data.ItineraryRepository
import app.viajor.data.TripRepository

/** What the shared UI needs from the platform; the Android app (and later iOS) builds this once. */
class AppDependencies(
    val trips: TripRepository,
    val itinerary: ItineraryRepository,
    /** IANA id of the device's time zone, used for activity times (ADR-0002). */
    val timeZoneId: String,
)

sealed interface Screen {
    data object TripList : Screen

    data class TripEditor(val tripId: String?) : Screen

    data class Itinerary(val tripId: String) : Screen
}

/** A minimal back stack. The platform host calls [back] from its back button or gesture. */
class Navigator(start: Screen = Screen.TripList) {
    private var stack by mutableStateOf(listOf(start))

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun open(screen: Screen) {
        stack = stack + screen
    }

    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    /** Replaces the current screen, for example the editor after a trip was created. */
    fun replace(screen: Screen) {
        stack = stack.dropLast(1) + screen
    }
}
