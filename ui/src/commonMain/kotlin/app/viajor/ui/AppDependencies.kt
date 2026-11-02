package app.viajor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import app.viajor.data.BackupService
import app.viajor.data.ItineraryRepository
import app.viajor.data.TripRepository

/** What the shared UI needs from the platform; the Android app (and later iOS) builds this once. */
class AppDependencies(
    val trips: TripRepository,
    val itinerary: ItineraryRepository,
    val backup: BackupService,
    /** IANA id of the device's time zone, used for activity times (ADR-0002). */
    val timeZoneId: String,
)

sealed interface Screen {
    data object TripList : Screen

    data class TripEditor(val tripId: String?) : Screen

    data class Itinerary(val tripId: String) : Screen

    data object Settings : Screen

    /** A short text form that survives rotation and process death; see [Navigator.Saver]. */
    fun encode(): String = when (this) {
        TripList -> "list"
        is TripEditor -> "edit:" + (tripId ?: "")
        is Itinerary -> "trip:$tripId"
        Settings -> "settings"
    }

    companion object {
        fun decode(text: String): Screen = when {
            text == "settings" -> Settings
            text.startsWith("edit:") -> TripEditor(text.removePrefix("edit:").ifEmpty { null })
            text.startsWith("trip:") -> Itinerary(text.removePrefix("trip:"))
            else -> TripList
        }
    }
}

/** A minimal back stack. The platform host calls [back] from its back button or gesture. */
class Navigator(start: List<Screen> = listOf(Screen.TripList)) {
    private var stack by mutableStateOf(start.ifEmpty { listOf(Screen.TripList) })

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

    internal fun encode(): List<String> = stack.map { it.encode() }

    companion object {
        /** Keeps the back stack across rotation and process death (ids only, no data). */
        val Saver: Saver<Navigator, List<String>> = Saver(
            save = { it.encode() },
            restore = { saved -> Navigator(saved.map(Screen::decode)) },
        )
    }
}
