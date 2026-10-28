package app.viajor.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TripDaysTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun oneDayPerDateInclusiveNumberedFromOne() {
        val days = DayPlanner.daysFor(d("2027-03-01"), d("2027-03-03"))
        assertEquals(
            listOf(PlannedDay(1, d("2027-03-01")), PlannedDay(2, d("2027-03-02")), PlannedDay(3, d("2027-03-03"))),
            days,
        )
    }

    @Test
    fun singleDayAndLeapDayRangesWork() {
        assertEquals(1, DayPlanner.daysFor(d("2027-05-05"), d("2027-05-05")).size)
        val leap = DayPlanner.daysFor(d("2028-02-28"), d("2028-03-01"))
        assertEquals(listOf("2028-02-28", "2028-02-29", "2028-03-01"), leap.map { it.date.toString() })
        assertEquals(2, DayPlanner.daysFor(d("2027-02-28"), d("2027-03-01")).size) // 2027 is not a leap year
    }

    @Test
    fun undatedTripsGetNumberedDaysWithoutDates() {
        assertEquals(listOf(PlannedDay(1, null), PlannedDay(2, null)), DayPlanner.undated(2))
        assertFailsWith<IllegalArgumentException> { DayPlanner.undated(0) }
    }

    @Test
    fun validationReportsEveryProblem() {
        assertEquals(emptyList(), TripValidation.validate(TripInput("Lisbon", d("2027-03-01"), d("2027-03-05"))))
        assertEquals(emptyList(), TripValidation.validate(TripInput("Someday", null, null)))
        assertEquals(listOf(TripError.EMPTY_TITLE), TripValidation.validate(TripInput("   ", null, null)))
        assertEquals(
            listOf(TripError.TITLE_TOO_LONG),
            TripValidation.validate(TripInput("x".repeat(TripValidation.MAX_TITLE_LENGTH + 1), null, null)),
        )
        assertEquals(listOf(TripError.ONLY_ONE_DATE), TripValidation.validate(TripInput("A", d("2027-03-01"), null)))
        assertEquals(
            listOf(TripError.EMPTY_TITLE, TripError.END_BEFORE_START),
            TripValidation.validate(TripInput("", d("2027-03-05"), d("2027-03-01"))),
        )
    }

    @Test
    fun overlongTripsAreRejected() {
        val tooLong = TripInput("Round the world", d("2027-01-01"), d("2028-01-02")) // 367 days
        assertEquals(listOf(TripError.TOO_MANY_DAYS), TripValidation.validate(tooLong))
        assertFailsWith<IllegalArgumentException> { DayPlanner.daysFor(d("2027-01-01"), d("2028-01-02")) }
    }

    @Test
    fun shrinkingNeverDropsDaysWithContentSilently() {
        val existing = listOf(ExistingDay(1, true), ExistingDay(2, false), ExistingDay(3, true), ExistingDay(4, false))
        val plan = DayPlanner.resize(existing, d("2027-03-01"), d("2027-03-02"))
        assertEquals(2, plan.days.size)
        assertEquals(listOf(4), plan.emptyDaysToDrop.map { it.index })
        assertEquals(listOf(3), plan.daysNeedingConfirmation.map { it.index })
    }

    @Test
    fun growingKeepsExistingDaysAndAddsNewOnes() {
        val existing = listOf(ExistingDay(1, true), ExistingDay(2, true))
        val plan = DayPlanner.resize(existing, d("2027-03-01"), d("2027-03-04"))
        assertEquals(4, plan.days.size)
        assertEquals(emptyList(), plan.emptyDaysToDrop)
        assertEquals(emptyList(), plan.daysNeedingConfirmation)
    }

    @Test
    fun shiftingTheStartMovesDatesButKeepsDayIndexes() {
        val existing = listOf(ExistingDay(1, true), ExistingDay(2, true))
        val plan = DayPlanner.resize(existing, d("2027-04-10"), d("2027-04-11"))
        assertEquals(listOf(1 to "2027-04-10", 2 to "2027-04-11"), plan.days.map { it.index to it.date.toString() })
        assertEquals(emptyList(), plan.daysNeedingConfirmation)
    }

    @Test
    fun makingATripUndatedKeepsItsLength() {
        val existing = listOf(ExistingDay(1, true), ExistingDay(2, true), ExistingDay(3, false))
        val plan = DayPlanner.resize(existing, null, null)
        assertEquals(listOf(PlannedDay(1, null), PlannedDay(2, null), PlannedDay(3, null)), plan.days)
    }
}
