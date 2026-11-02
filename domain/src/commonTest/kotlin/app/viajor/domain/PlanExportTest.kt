package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanExportTest {
    private val plan = ExportPlan(
        title = "Lisbon",
        description = "Spring trip",
        days = listOf(
            ExportDay(
                1, "2027-03-01",
                listOf(
                    ExportActivity("Walk", "bring water", "10:00", "12:00", "Alfama", completed = true),
                    ExportActivity("Lunch"),
                ),
            ),
            ExportDay(2, null, emptyList()),
        ),
        ideas = listOf(ExportActivity("Fado night", "ask the hotel")),
    )

    @Test
    fun markdownListsDaysTimesPlacesAndIdeasInOrder() {
        val md = PlanExport.markdown(plan)
        assertEquals(
            """
            # Lisbon

            Spring trip

            ## Day 1 · 2027-03-01

            - [x] 10:00–12:00 Walk (Alfama) — bring water
            - [ ] Lunch

            ## Day 2

            _Nothing planned._

            ## Ideas

            - [ ] Fado night — ask the hotel
            """.trimIndent() + "\n",
            md,
        )
    }

    @Test
    fun markdownKeepsEachActivityOnOneLine() {
        val md = PlanExport.markdown(ExportPlan("A\nB", null, listOf(ExportDay(1, null, listOf(ExportActivity("x\ny", "n\r\nm")))), emptyList()))
        assertTrue("# A B" in md)
        assertTrue("- [ ] x y — n  m" in md)
    }

    @Test
    fun csvHasTheImporterColumnsAndReadsBackExactly() {
        val csv = PlanExport.csv(plan)
        val table = CsvReader.parse(csv).rows
        assertEquals(listOf("title", "day", "start", "end", "notes", "place"), table.first())
        assertEquals(listOf("Walk", "1", "10:00", "12:00", "bring water", "Alfama"), table[1])
        assertEquals(listOf("Lunch", "1", "", "", "", ""), table[2])
        assertEquals(listOf("Fado night", "", "", "", "ask the hotel", ""), table[3])
        assertEquals(4, table.size)
    }

    @Test
    fun csvNeutralisesFormulaLikeCellsAndStillRoundTrips() {
        val risky = ExportPlan("T", null, listOf(ExportDay(1, null, listOf(ExportActivity("=HYPERLINK(\"http://x\")", "+1", place = "@home")))), emptyList())
        val csv = PlanExport.csv(risky)
        assertTrue("'=HYPERLINK" in csv)
        val back = CsvReader.parse(csv).rows[1]
        assertEquals("=HYPERLINK(\"http://x\")", back[0])
        assertEquals("+1", back[4])
        assertEquals("@home", back[5])
    }
}
