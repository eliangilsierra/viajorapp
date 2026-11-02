package app.viajor.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.ActivityDraft
import app.viajor.data.ActivityError
import app.viajor.data.ChangeStamper
import app.viajor.data.IdGenerator
import app.viajor.data.ItineraryRepository
import app.viajor.data.SqlItineraryRepository
import app.viajor.data.SqlTripRepository
import app.viajor.data.TripRepository
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.TripError
import app.viajor.ui.presenter.ItineraryPresenter
import app.viajor.ui.presenter.ListStatus
import app.viajor.ui.presenter.TripEditorPresenter
import app.viajor.ui.presenter.TripListPresenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.IOException
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PresentersTest {
    private var counter = 0
    private var clock = 1L
    private val trips: TripRepository
    private val itinerary: ItineraryRepository

    init {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        ViajorDatabase.Schema.create(driver)
        val db = ViajorDatabase(driver)
        val stamper = ChangeStamper("dev") { clock++ }
        val ids = IdGenerator { "id-${++counter}" }
        trips = SqlTripRepository(db, stamper, ids, EmptyCoroutineContext)
        itinerary = SqlItineraryRepository(db, stamper, ids, EmptyCoroutineContext)
    }

    private fun d(s: String) = LocalDate.parse(s)

    /** A scope whose coroutines run eagerly, so state is up to date after each call. */
    private fun TestScope.eager() = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    @Test
    fun theListStartsEmptyThenShowsACreatedTrip() = runTest {
        val list = TripListPresenter(trips, eager())
        assertEquals(ListStatus.EMPTY, list.state.value.status)

        val editor = TripEditorPresenter(trips, eager())
        editor.setTitle("Lisbon")
        editor.setDates(d("2027-03-01"), d("2027-03-03"))
        editor.save()
        assertNotNull(editor.state.value.savedTripId)

        assertEquals(ListStatus.CONTENT, list.state.value.status)
        assertEquals(listOf("Lisbon"), list.state.value.trips.map { it.title })
    }

    @Test
    fun validationErrorsAreShownAndNothingIsSaved() = runTest {
        val editor = TripEditorPresenter(trips, eager())
        editor.setTitle("  ")
        editor.setDates(d("2027-03-05"), d("2027-03-01"))
        editor.save()
        assertEquals(listOf(TripError.EMPTY_TITLE, TripError.END_BEFORE_START), editor.state.value.errors)
        assertNull(editor.state.value.savedTripId)
        assertEquals(false, editor.state.value.saving)
        assertEquals(emptyList(), trips.observeTrips().first())
    }

    @Test
    fun deletingOffersUndoAndUndoBringsTheTripBack() = runTest {
        val list = TripListPresenter(trips, eager())
        TripEditorPresenter(trips, eager()).apply { setTitle("Porto"); save() }
        val porto = list.state.value.trips.single()

        list.delete(porto)
        assertEquals(ListStatus.EMPTY, list.state.value.status)
        assertEquals(porto, list.state.value.undoableDelete)

        list.undoDelete()
        assertEquals(listOf("Porto"), list.state.value.trips.map { it.title })
        assertNull(list.state.value.undoableDelete)
    }

    @Test
    fun editingLoadsTheTripAndShorteningAsksBeforeMovingContent() = runTest {
        val create = TripEditorPresenter(trips, eager())
        create.setTitle("Lisbon")
        create.setDates(d("2027-03-01"), d("2027-03-03"))
        create.save()
        val id = create.state.value.savedTripId!!

        val itin = ItineraryPresenter(trips, itinerary, eager(), id)
        val lastDay = itin.state.value.days.last().day.id
        itin.addActivity(lastDay, ActivityDraft("Keep me"))

        val edit = TripEditorPresenter(trips, eager(), id)
        assertEquals("Lisbon", edit.state.value.title)
        assertEquals(d("2027-03-03"), edit.state.value.endDate)

        edit.setDates(d("2027-03-01"), d("2027-03-02"))
        edit.save()
        assertEquals(listOf(3), edit.state.value.daysNeedingConfirmation)
        assertNull(edit.state.value.savedTripId)

        edit.save(confirmDropDays = true)
        assertEquals(id, edit.state.value.savedTripId)
        assertEquals(2, itin.state.value.days.size)
        assertEquals(listOf("Keep me"), itin.state.value.unscheduled.map { it.title })
    }

    @Test
    fun theItineraryShowsDaysAndMovingChangesTheOrderOfTheExposedState() = runTest {
        val create = TripEditorPresenter(trips, eager())
        create.setTitle("Lisbon")
        create.setDates(d("2027-03-01"), d("2027-03-02"))
        create.save()
        val id = create.state.value.savedTripId!!

        val itin = ItineraryPresenter(trips, itinerary, eager(), id)
        assertEquals(ListStatus.CONTENT, itin.state.value.status)
        assertEquals(2, itin.state.value.days.size)
        val day1 = itin.state.value.days[0].day.id
        val day2 = itin.state.value.days[1].day.id

        itin.addActivity(day1, ActivityDraft("A"))
        itin.addActivity(day1, ActivityDraft("B"))
        itin.addActivity(day1, ActivityDraft("C"))
        assertEquals(listOf("A", "B", "C"), itin.state.value.days[0].activities.map { it.title })

        val c = itin.state.value.days[0].activities.last()
        itin.move(c.id, day1, 0)
        assertEquals(listOf("C", "A", "B"), itin.state.value.days[0].activities.map { it.title })

        itin.move(c.id, day2, 0)
        assertEquals(listOf("C"), itin.state.value.days[1].activities.map { it.title })

        itin.setCompleted(c.id, true)
        assertTrue(itin.state.value.days[1].activities.single().completed)

        itin.delete(c)
        assertEquals(emptyList(), itin.state.value.days[1].activities)
        itin.undoDelete()
        assertEquals(1, itin.state.value.days[1].activities.size)
    }

    @Test
    fun activityErrorsAreExposedAndCleared() = runTest {
        val create = TripEditorPresenter(trips, eager())
        create.setTitle("Lisbon")
        create.save()
        val id = create.state.value.savedTripId!!
        val itin = ItineraryPresenter(trips, itinerary, eager(), id)

        itin.addActivity(null, ActivityDraft(" "))
        assertEquals(listOf(ActivityError.EMPTY_TITLE), itin.state.value.errors)
        itin.clearErrors()
        assertEquals(emptyList(), itin.state.value.errors)
        itin.addActivity(null, ActivityDraft("Idea"))
        assertEquals(listOf("Idea"), itin.state.value.unscheduled.map { it.title })
    }

    @Test
    fun undatedTripsCanGrowByOneDay() = runTest {
        val create = TripEditorPresenter(trips, eager())
        create.setTitle("Someday")
        create.save()
        val id = create.state.value.savedTripId!!
        val itin = ItineraryPresenter(trips, itinerary, eager(), id)
        assertEquals(1, itin.state.value.days.size)
        itin.addDay()
        assertEquals(2, itin.state.value.days.size)
    }

    @Test
    fun aRepositoryFailureBecomesAnErrorStateNotACrash() = runTest {
        val failing = object : TripRepository by trips {
            override fun observeTrips() = flow<List<app.viajor.data.TripRecord>> { throw IOException("disk") }
        }
        val list = TripListPresenter(failing, eager())
        assertEquals(ListStatus.ERROR, list.state.value.status)

        val failingWrites = object : TripRepository by trips {
            override suspend fun deleteTrip(id: String) { throw IOException("disk") }
        }
        TripEditorPresenter(trips, eager()).apply { setTitle("X"); save() }
        val ok = TripListPresenter(failingWrites, eager())
        ok.delete(ok.state.value.trips.single())
        assertEquals(ListStatus.ERROR, ok.state.value.status)
        assertNull(ok.state.value.undoableDelete)
    }
}
