package app.viajor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import app.viajor.data.BackupService
import app.viajor.data.ItineraryRepository
import app.viajor.data.SearchRepository
import app.viajor.data.TripRepository

/** What the shared UI needs from the platform; the Android app (and later iOS) builds this once. */
class AppDependencies(
    val trips: TripRepository,
    val itinerary: ItineraryRepository,
    val backup: BackupService,
    val search: SearchRepository,
    /** IANA id of the device's time zone, used for activity times (ADR-0002). */
    val timeZoneId: String,
)

sealed interface Screen {
    data object TripList : Screen

    data class TripEditor(val tripId: String?) : Screen

    data class Itinerary(val tripId: String) : Screen

    data object Settings : Screen

    data object Search : Screen

    data class Import(val tripId: String) : Screen

    data class Map(val tripId: String) : Screen

    /** The map of every place of every trip; a top-level tab. */
    data object AllMap : Screen

    /** A short text form that survives rotation and process death; see [Navigator.Saver]. */
    fun encode(): String = when (this) {
        TripList -> "list"
        is TripEditor -> "edit:" + (tripId ?: "")
        is Itinerary -> "trip:$tripId"
        Settings -> "settings"
        Search -> "search"
        is Import -> "import:$tripId"
        is Map -> "map:$tripId"
        AllMap -> "allmap"
    }

    companion object {
        fun decode(text: String): Screen = when {
            text == "settings" -> Settings
            text == "search" -> Search
            text == "allmap" -> AllMap
            text.startsWith("edit:") -> TripEditor(text.removePrefix("edit:").ifEmpty { null })
            text.startsWith("import:") -> Import(text.removePrefix("import:"))
            text.startsWith("map:") -> Map(text.removePrefix("map:"))
            text.startsWith("trip:") -> Itinerary(text.removePrefix("trip:"))
            else -> TripList
        }
    }
}

/** A minimal back stack. The platform host calls [back] from its back button or gesture. */
class Navigator(start: List<Screen> = listOf(Screen.TripList)) {
    private var stack by mutableStateOf(start.ifEmpty { listOf(Screen.TripList) })

    val current: Screen get() = stack.last()
    /** True when Back does something: pops a screen, or returns from another tab to the trip list. */
    val canGoBack: Boolean get() = stack.size > 1 || current != Screen.TripList

    fun open(screen: Screen) {
        stack = stack + screen
    }

    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1) else if (current != Screen.TripList) stack = listOf(Screen.TripList)
    }

    /** Switches to a top-level tab; the tab starts with a fresh stack. */
    fun switchTab(screen: Screen) {
        stack = listOf(screen)
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
