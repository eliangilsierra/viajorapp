package app.viajor.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextItineraryParserTest {
    private val sample = """
        Day 1 - Lisbon
        09:30 Pastéis de Belém
        - 11:00-13:00 Museu do Azulejo
        * Walk around Alfama
        Some loose thought about the trip

        2027-03-02: Sintra
        14h Pena Palace
        10am to 12pm Quinta da Regaleira
    """.trimIndent()

    @Test
    fun readsHeadingsTimesRangesAndBullets() {
        val r = TextItineraryParser.parse(sample)
        assertEquals(listOf("Day 1", "2027-03-02"), r.headings.map { it.day.label })
        assertEquals(listOf("Lisbon", "Sintra"), r.headings.map { it.note })

        val byTitle = r.items.associateBy { it.title }
        val pasteis = byTitle.getValue("Pastéis de Belém")
        assertEquals(ClockTime(9, 30), pasteis.start)
        assertNull(pasteis.end)
        assertEquals(1, pasteis.day?.number)

        val museu = byTitle.getValue("Museu do Azulejo")
        assertEquals(ClockTime(11, 0) to ClockTime(13, 0), museu.start to museu.end)

        assertEquals(ClockTime(14, 0), byTitle.getValue("Pena Palace").start)
        assertEquals("2027-03-02", byTitle.getValue("Pena Palace").day?.isoDate)
        val regaleira = byTitle.getValue("Quinta da Regaleira")
        assertEquals(ClockTime(10, 0) to ClockTime(12, 0), regaleira.start to regaleira.end)
    }

    @Test
    fun aLineWithoutATimeHasNoTimeAndPlainProseIsNotMadeIntoAnItem() {
        val r = TextItineraryParser.parse(sample)
        val walk = r.items.single { it.title == "Walk around Alfama" }
        assertNull(walk.start)
        assertEquals(Certainty.EXACT, walk.certainty)
        assertEquals(listOf("Some loose thought about the trip"), r.unparsed.map { it.text })
    }

    @Test
    fun ambiguousInputIsGuessedNeverExact() {
        val r = TextItineraryParser.parse("- 9 Museum of Art\nMuseum visit at 10:30\n")
        val bare = r.items.single { it.title == "9 Museum of Art" }
        assertNull(bare.start)
        assertEquals(Certainty.GUESSED, bare.certainty)
        val trailing = r.items.single { it.title == "Museum visit" }
        assertEquals(ClockTime(10, 30), trailing.start)
        assertEquals(Certainty.GUESSED, trailing.certainty)
    }

    @Test
    fun spanishHeadingsAndTimesWork() {
        val r = TextItineraryParser.parse("Día 2: Oporto\n9h30 Ribeira\nlunes\n- 15:00 a 17:00 Bodega")
        assertEquals(listOf("Day 2", "Lunes"), r.headings.map { it.day.label })
        assertEquals(ClockTime(9, 30), r.items.first { it.title == "Ribeira" }.start)
        val bodega = r.items.first { it.title == "Bodega" }
        assertEquals(ClockTime(15, 0) to ClockTime(17, 0), bodega.start to bodega.end)
        assertEquals("Lunes", bodega.day?.label)
    }

    @Test
    fun twelveHourClockConvertsCorrectly() {
        assertEquals(ClockTime(0, 0), TextItineraryParser.parseTime("12am"))
        assertEquals(ClockTime(12, 0), TextItineraryParser.parseTime("12pm"))
        assertEquals(ClockTime(21, 15), TextItineraryParser.parseTime("9:15 PM"))
        assertEquals(ClockTime(9, 5), TextItineraryParser.parseTime("09.05"))
    }

    @Test
    fun invalidTimesAreNotRecognised() {
        val r = TextItineraryParser.parse("25:00 Impossible\n- 10:75 Also impossible")
        assertTrue(r.items.none { it.start == ClockTime(0, 0) })
        assertTrue(r.items.all { it.start == null })
    }

    @Test
    fun everyNonBlankLineLandsInExactlyOnePlace() {
        val fragments = listOf("Day 3", "2027-03-04", "monday", "09:30 Coffee", "- Walk", "10-11 Tour", "random words", "  ", "-", "14h", "* 9 Park", "Café at 8:15")
        val random = Random(20271027)
        repeat(200) {
            val lines = List(random.nextInt(1, 12)) { fragments[random.nextInt(fragments.size)] }
            val r = TextItineraryParser.parse(lines.joinToString("\n"))
            val seen = (r.headings.map { it.line } + r.items.map { it.line } + r.unparsed.map { it.line })
            val expected = lines.mapIndexedNotNull { i, l -> if (l.isBlank()) null else i + 1 }
            assertEquals(expected, seen.sorted(), "lines lost or duplicated for $lines")
            assertEquals(seen.size, seen.toSet().size)
        }
    }
}
