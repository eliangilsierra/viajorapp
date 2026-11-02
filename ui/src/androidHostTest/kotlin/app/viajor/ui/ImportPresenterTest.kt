package app.viajor.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.viajor.data.ChangeStamper
import app.viajor.data.CreateTripResult
import app.viajor.data.IdGenerator
import app.viajor.data.SqlItineraryRepository
import app.viajor.data.SqlTripRepository
import app.viajor.data.db.ViajorDatabase
import app.viajor.domain.Provenance
import app.viajor.domain.TripInput
import app.viajor.ui.presenter.ImportError
import app.viajor.ui.presenter.ImportPresenter
import app.viajor.ui.presenter.ImportStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.util.Properties
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImportPresenterTest {
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

    private fun TestScope.eager() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private suspend fun datedTrip(): String =
        (trips.createTrip(TripInput("Lisbon", LocalDate.parse("2027-03-01"), LocalDate.parse("2027-03-03"))) as CreateTripResult.Created).tripId

    private suspend fun undatedTrip(): String = (trips.createTrip(TripInput("Someday", null, null)) as CreateTripResult.Created).tripId

    private val plan = """
        Day 1
        09:30 Pastéis de Belém
        - Walk around Alfama
        2027-03-02
        14h Pena Palace
        - 9 Museum of Art
        Notes that are not an item
    """.trimIndent()

    @Test
    fun pastedTextBecomesAReviewListWithUncertainItemsUnchecked() = runTest {
        val id = datedTrip()
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewText(plan)
        val s = p.state.value
        assertEquals(ImportStage.REVIEW, s.stage)
        assertEquals(1, s.unparsedLines)
        val byTitle = s.candidates.associateBy { it.title }
        assertEquals(1, byTitle.getValue("Pastéis de Belém").dayIndex)
        assertEquals(2, byTitle.getValue("Pena Palace").dayIndex)
        assertTrue(byTitle.getValue("Pena Palace").checked)
        assertEquals(true, byTitle.getValue("9 Museum of Art").uncertain)
        assertEquals(false, byTitle.getValue("9 Museum of Art").checked)
    }

    @Test
    fun onlyCheckedItemsAreAddedWithTheirDayTimeZoneAndImportProvenance() = runTest {
        val id = datedTrip()
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewText(plan)
        p.confirm()
        assertEquals(ImportStage.DONE, p.state.value.stage)
        assertEquals(3, p.state.value.added)

        val days = itinerary.observeDays(id).first()
        val day1 = itinerary.observeActivities(days[0].id).first()
        assertEquals(listOf("Pastéis de Belém", "Walk around Alfama"), day1.map { it.title })
        assertEquals(LocalDateTime.parse("2027-03-01T09:30"), day1.first().start)
        assertEquals("Europe/Lisbon", day1.first().zoneId)
        assertEquals(Provenance.IMPORT, day1.first().provenance)
        assertNull(day1.last().start)
        assertEquals(listOf("Pena Palace"), itinerary.observeActivities(days[1].id).first().map { it.title })
        assertEquals(emptyList(), itinerary.observeUnscheduled(id).first()) // the unchecked guess was left out
    }

    @Test
    fun theUserCanCheckAnUncertainItemAndItemsOnUnknownDaysGoToIdeas() = runTest {
        val id = datedTrip()
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewText("Day 9\n09:00 Somewhere\n- 9 Museum of Art")
        val uncertain = p.state.value.candidates.single { it.uncertain }
        p.toggle(uncertain.id)
        p.confirm()
        assertEquals(2, p.state.value.added)
        assertEquals(setOf("Somewhere", "9 Museum of Art"), itinerary.observeUnscheduled(id).first().map { it.title }.toSet())
    }

    @Test
    fun timesAreDroppedWhenTheDayHasNoDate() = runTest {
        val id = undatedTrip()
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewText("Day 1\n10:00-11:00 Market")
        p.confirm()
        val day = itinerary.observeDays(id).first().single()
        val a = itinerary.observeActivities(day.id).first().single()
        assertEquals("Market", a.title)
        assertNull(a.start)
        assertNull(a.zoneId)
    }

    @Test
    fun weekdayHeadingsMapToTheMatchingDatedDay() = runTest {
        val id = datedTrip() // 2027-03-01 is a Monday
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewText("martes\n- Lunch")
        assertEquals(2, p.state.value.candidates.single().dayIndex)
    }

    @Test
    fun aCsvWithTheHeaderColumnsIsReviewedAndImported() = runTest {
        val id = datedTrip()
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewCsv("Title,Day,Start,End,Notes\r\nBreakfast,1,08:30,09:15,\"near, the river\"\r\nDinner,2027-03-02,25:99,,\r\nIdea,,,,\r\n")
        val c = p.state.value.candidates
        assertEquals(3, c.size)
        assertEquals("near, the river", c[0].notes)
        assertEquals(true, c[1].uncertain) // unreadable time
        assertEquals(false, c[1].checked)
        assertEquals(2, c[1].dayIndex)
        p.confirm()
        assertEquals(2, p.state.value.added)
        assertEquals(listOf("Idea"), itinerary.observeUnscheduled(id).first().map { it.title })
    }

    @Test
    fun badInputIsReportedAndNothingChanges() = runTest {
        val id = datedTrip()
        val p = ImportPresenter(itinerary, eager(), id, "Europe/Lisbon")
        p.reviewText("just some words without structure")
        assertEquals(ImportError.NOTHING_FOUND, p.state.value.error)
        assertEquals(ImportStage.INPUT, p.state.value.stage)

        p.reviewCsv("name,when\nfoo,bar")
        assertEquals(ImportError.NOT_A_TABLE, p.state.value.error)
        p.reviewCsv("title\n\"unterminated")
        assertEquals(ImportError.BAD_FILE, p.state.value.error)
        assertEquals(emptyList(), itinerary.observeUnscheduled(id).first())
    }
}
