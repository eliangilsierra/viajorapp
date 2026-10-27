package app.viajor.domain

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ZonedTimesTest {
    private fun at(s: String) = LocalDateTime.parse(s)

    @Test
    fun ordinaryTimeIsExact() {
        val r = ZonedTimes.resolve(at("2027-06-15T12:00"), "Europe/Lisbon")
        assertEquals(ResolvedTime.Exact(Instant.parse("2027-06-15T11:00:00Z")), r)
    }

    @Test
    fun springForwardGapIsReportedWithTheShiftedInstant() {
        // Lisbon clocks jump from 01:00 to 02:00 on 2027-03-28, so 01:30 does not exist.
        val r = ZonedTimes.resolve(at("2027-03-28T01:30"), "Europe/Lisbon")
        val gap = assertIs<ResolvedTime.Gap>(r)
        assertEquals(at("2027-03-28T01:30"), gap.requested)
        assertEquals(Instant.parse("2027-03-28T01:30:00Z"), gap.instant) // 02:30 local, UTC+1
    }

    @Test
    fun fallBackOverlapReportsBothInstantsAndDefaultsToTheEarlier() {
        // Lisbon clocks go from 02:00 back to 01:00 on 2027-10-31, so 01:30 happens twice.
        val r = ZonedTimes.resolve(at("2027-10-31T01:30"), "Europe/Lisbon")
        val overlap = assertIs<ResolvedTime.Overlap>(r)
        assertEquals(Instant.parse("2027-10-31T00:30:00Z"), overlap.earlier)
        assertEquals(Instant.parse("2027-10-31T01:30:00Z"), overlap.later)
        assertEquals(overlap.earlier, r.instant)
    }

    @Test
    fun overnightDurationAcrossSpringForwardLosesAnHour() {
        // New York DST starts 2027-03-14 at 02:00: 22:00 to 06:00 is 7 real hours, not 8.
        val d = ZonedTimes.duration(at("2027-03-13T22:00"), "America/New_York", at("2027-03-14T06:00"), "America/New_York")
        assertEquals(7.hours, d)
    }

    @Test
    fun overnightDurationAcrossFallBackGainsAnHour() {
        val d = ZonedTimes.duration(at("2027-11-06T22:00"), "America/New_York", at("2027-11-07T06:00"), "America/New_York")
        assertEquals(9.hours, d)
    }

    @Test
    fun flightAcrossZonesUsesEachZone() {
        // Depart Lisbon 10:00 (UTC+1), arrive New York 13:00 (UTC-4): 8 hours in the air.
        val d = ZonedTimes.duration(at("2027-06-15T10:00"), "Europe/Lisbon", at("2027-06-15T13:00"), "America/New_York")
        assertEquals(8.hours, d)
    }

    @Test
    fun endBeforeStartIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ZonedTimes.duration(at("2027-06-15T10:00"), "Europe/Lisbon", at("2027-06-15T09:00"), "Europe/Lisbon")
        }
    }

    @Test
    fun unknownOrBlankZoneIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> { ZonedTimes.zone("Mars/Olympus") }
        assertFailsWith<IllegalArgumentException> { ZonedTimes.zone("") }
    }

    @Test
    fun spansFeedOverlapDetectionAcrossZones() {
        // 09:00-11:00 Lisbon (08:00-10:00 UTC) overlaps 05:00-06:00 New York (09:00-10:00 UTC).
        val a = ZonedTimes.toSpan("a", at("2027-06-15T09:00"), "Europe/Lisbon", at("2027-06-15T11:00"), "Europe/Lisbon")
        val b = ZonedTimes.toSpan("b", at("2027-06-15T05:00"), "America/New_York", at("2027-06-15T06:00"), "America/New_York")
        assertEquals(listOf(OverlapPair("a", "b")), Overlap.find(listOf(a, b)))
    }

    @Test
    fun aucklandOrdinaryTimeIsExact() {
        val r = ZonedTimes.resolve(at("2027-01-10T08:00"), "Pacific/Auckland")
        assertEquals(Instant.parse("2027-01-09T19:00:00Z"), r.instant) // NZDT is UTC+13
    }
}
