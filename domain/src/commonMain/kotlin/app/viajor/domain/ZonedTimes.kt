package app.viajor.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** What a local date-time means in a zone (ADR-0002): daylight saving can make it missing or ambiguous. */
sealed interface ResolvedTime {
    /** The instant to use unless the user chooses otherwise. */
    val instant: Instant

    data class Exact(override val instant: Instant) : ResolvedTime

    /** The requested local time does not exist (clocks jumped forward); [instant] is the shifted time. */
    data class Gap(val requested: LocalDateTime, override val instant: Instant) : ResolvedTime

    /** The local time happens twice (clocks went back); [instant] is the earlier one. */
    data class Overlap(val earlier: Instant, val later: Instant) : ResolvedTime {
        override val instant: Instant get() = earlier
    }
}

object ZonedTimes {
    /** Parses an IANA zone id such as `Europe/Lisbon`; unknown ids are an error, never a silent UTC. */
    fun zone(id: String): TimeZone {
        require(id.isNotBlank()) { "time zone id is empty" }
        return try {
            TimeZone.of(id)
        } catch (e: Exception) {
            throw IllegalArgumentException("unknown time zone id: $id", e)
        }
    }

    fun resolve(local: LocalDateTime, zoneId: String): ResolvedTime {
        val tz = zone(zoneId)
        val approx = local.toInstant(UtcOffset.ZERO)
        // Offsets in force a day either side cover any single transition around the local time.
        val offsets = setOf(
            tz.offsetAt(approx - 24.hours),
            tz.offsetAt(approx),
            tz.offsetAt(approx + 24.hours),
        )
        val valid = offsets
            .map { offset -> local.toInstant(offset) }
            .filter { instant -> local.toInstant(tz.offsetAt(instant)) == instant }
            .distinct()
            .sorted()
        return when (valid.size) {
            0 -> ResolvedTime.Gap(local, local.toInstant(tz))
            1 -> ResolvedTime.Exact(valid.single())
            else -> ResolvedTime.Overlap(valid.first(), valid.last())
        }
    }

    /**
     * Elapsed time between two local date-times, each in its own zone (a flight crosses zones), so an
     * overnight item that spans a daylight-saving change has its real length.
     */
    fun duration(start: LocalDateTime, startZoneId: String, end: LocalDateTime, endZoneId: String): Duration {
        val elapsed = resolve(end, endZoneId).instant - resolve(start, startZoneId).instant
        require(!elapsed.isNegative()) { "end must not precede start" }
        return elapsed
    }

    /** Converts a timed item to the absolute span used by [Overlap]. */
    fun toSpan(id: String, start: LocalDateTime, startZoneId: String, end: LocalDateTime, endZoneId: String): TimedSpan =
        TimedSpan(
            id,
            resolve(start, startZoneId).instant.toEpochMilliseconds(),
            resolve(end, endZoneId).instant.toEpochMilliseconds(),
        )
}
