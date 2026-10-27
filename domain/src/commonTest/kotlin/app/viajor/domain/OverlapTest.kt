package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OverlapTest {
    @Test
    fun touchingSpansDoNotOverlap() {
        val result = Overlap.find(listOf(TimedSpan("a", 0, 100), TimedSpan("b", 100, 200)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun intersectingSpansAreReportedOnce() {
        val result = Overlap.find(listOf(TimedSpan("b", 50, 150), TimedSpan("a", 0, 100)))
        assertEquals(listOf(OverlapPair("a", "b")), result)
    }

    @Test
    fun containedSpanOverlapsItsContainer() {
        val result = Overlap.find(listOf(TimedSpan("big", 0, 1000), TimedSpan("small", 100, 200), TimedSpan("late", 2000, 2100)))
        assertEquals(listOf(OverlapPair("big", "small")), result)
    }

    @Test
    fun zeroLengthSpansNeverOverlap() {
        val result = Overlap.find(listOf(TimedSpan("a", 0, 100), TimedSpan("pin", 50, 50)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun endBeforeStartIsRejected() {
        assertFailsWith<IllegalArgumentException> { TimedSpan("x", 10, 5) }
    }
}
