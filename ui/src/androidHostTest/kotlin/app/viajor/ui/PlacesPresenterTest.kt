package app.viajor.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.ChangeStamper
import app.viajor.data.IdGenerator
import app.viajor.data.PlaceDraft
import app.viajor.data.PlaceError
import app.viajor.data.SqlItineraryRepository
import app.viajor.data.SqlTripRepository
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.TripInput
import app.viajor.ui.presenter.ItineraryPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesPresenterTest {
    private var counter = 0
    private val trips: SqlTripRepository
    private val itinerary: SqlItineraryRepository

    init {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        ViajorDatabase.Schema.create(driver)
        val db = ViajorDatabase(driver)
        val stamper = ChangeStamper("dev") { 1L }
        val ids = IdGenerator { "id-${++counter}" }
        trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
        itinerary = SqlItineraryRepository(db, stamper, ids, EmptyCoroutineContext)
    }

    private fun TestScope.presenter(): ItineraryPresenter = kotlinx.coroutines.runBlocking {
        val id = (trips.createTrip(TripInput("Lisbon", null, null)) as app.viajor.data.CreateTripResult.Created).tripId
        ItineraryPresenter(trips, itinerary, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), id)
    }

    @Test
    fun placesCanBeAddedWithValidationErrorsAndDeleted() = runTest {
        val p = presenter()
        p.addPlace(PlaceDraft(" ", latitude = 10.0))
        assertEquals(listOf(PlaceError.EMPTY_NAME, PlaceError.ONLY_ONE_COORDINATE), p.state.value.placeErrors)
        assertEquals(emptyList(), p.state.value.places)

        p.addPlace(PlaceDraft("Alfama", latitude = 38.71, longitude = -9.13))
        assertEquals(emptyList(), p.state.value.placeErrors)
        assertEquals(listOf("Alfama"), p.state.value.places.map { it.name })

        p.deletePlace(p.state.value.places.single().id)
        assertEquals(emptyList(), p.state.value.places)
    }

    @Test
    fun kilometreTextRoundsToOneDecimalWithoutLocaleFormatting() {
        assertEquals("1.0", kmText(1_000))
        assertEquals("1.2", kmText(1_234))
        assertEquals("23.4", kmText(23_449))
        assertEquals("0.1", kmText(100))
        assertEquals("10.0", kmText(9_960))
    }
}
