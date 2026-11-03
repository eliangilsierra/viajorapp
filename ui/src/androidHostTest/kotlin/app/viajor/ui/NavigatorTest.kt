package app.viajor.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {
    @Test
    fun theBackStackSurvivesSaveAndRestore() {
        val nav = Navigator()
        nav.open(Screen.Itinerary("trip-1"))
        nav.open(Screen.TripEditor("trip-1"))
        nav.open(Screen.Settings)
        val saved = Navigator.Saver.run { /* encode */ nav.encode() }
        val restored = Navigator(saved.map(Screen::decode))
        assertEquals(Screen.Settings, restored.current)
        restored.back()
        assertEquals(Screen.TripEditor("trip-1"), restored.current)
        restored.back()
        assertEquals(Screen.Itinerary("trip-1"), restored.current)
        restored.back()
        assertEquals(Screen.TripList, restored.current)
        assertFalse(restored.canGoBack)
    }

    @Test
    fun screensEncodeToShortTextAndUnknownTextFallsBackToTheList() {
        assertEquals(Screen.TripEditor(null), Screen.decode(Screen.TripEditor(null).encode()))
        assertEquals(Screen.TripList, Screen.decode("something-from-a-newer-version"))
        assertEquals(Screen.TripList, Navigator(emptyList()).current)
        assertTrue(Navigator().encode() == listOf("list"))
    }

    @Test
    fun tabsSwitchWithAFreshStackAndBackReturnsToTheTripList() {
        val nav = Navigator()
        nav.open(Screen.Itinerary("t1"))
        nav.switchTab(Screen.AllMap)
        assertEquals(Screen.AllMap, nav.current)
        assertEquals(listOf("allmap"), nav.encode())
        assertTrue(nav.canGoBack) // Back leaves a tab for the trip list
        nav.back()
        assertEquals(Screen.TripList, nav.current)
        assertFalse(nav.canGoBack)
        nav.back() // the host closes the app; the navigator stays put
        assertEquals(Screen.TripList, nav.current)
    }
}
