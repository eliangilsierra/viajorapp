package app.viajor.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.ActivityDraft
import app.viajor.data.ChangeStamper
import app.viajor.data.CreateTripResult
import app.viajor.data.IdGenerator
import app.viajor.data.SearchHit
import app.viajor.data.SearchKind
import app.viajor.data.SearchRepository
import app.viajor.data.SqlItineraryRepository
import app.viajor.data.SqlSearchRepository
import app.viajor.data.SqlTripRepository
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.TripInput
import app.viajor.ui.presenter.SearchPresenter
import app.viajor.ui.presenter.SearchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPresenterTest {
    private var counter = 0
    private val trips: SqlTripRepository
    private val itinerary: SqlItineraryRepository
    private val search: SqlSearchRepository

    init {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        ViajorDatabase.Schema.create(driver)
        val db = ViajorDatabase(driver)
        val stamper = ChangeStamper("dev") { 1L }
        val ids = IdGenerator { "id-${++counter}" }
        trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
        itinerary = SqlItineraryRepository(db, stamper, ids, EmptyCoroutineContext)
        search = SqlSearchRepository(db, EmptyCoroutineContext)
    }

    private fun TestScope.eager() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private suspend fun seed(): String {
        val id = (trips.createTrip(TripInput("Lisbon", null, null)) as CreateTripResult.Created).tripId
        itinerary.addActivity(id, null, ActivityDraft("Café da manhã", "pastéis de nata"))
        return id
    }

    @Test
    fun resultsEmptyAndIdleStatesFollowTheText() = runTest {
        val tripId = seed()
        val p = SearchPresenter(search, eager())
        assertEquals(SearchStatus.IDLE, p.state.value.status)

        p.setQuery("cafe")
        assertEquals(SearchStatus.RESULTS, p.state.value.status)
        val hit = p.state.value.hits.single()
        assertEquals(SearchKind.ACTIVITY, hit.kind)
        assertEquals(tripId, hit.tripId)

        p.setQuery("zzz")
        assertEquals(SearchStatus.EMPTY, p.state.value.status)

        p.setQuery("   ")
        assertEquals(SearchStatus.IDLE, p.state.value.status)
        assertEquals(emptyList(), p.state.value.hits)
    }

    @Test
    fun aFailingSearchBecomesAnErrorState() = runTest {
        val failing = object : SearchRepository {
            override suspend fun search(text: String, tripId: String?, limit: Int): List<SearchHit> = throw IOException("disk")
        }
        val p = SearchPresenter(failing, eager())
        p.setQuery("anything")
        assertEquals(SearchStatus.ERROR, p.state.value.status)
    }
}
