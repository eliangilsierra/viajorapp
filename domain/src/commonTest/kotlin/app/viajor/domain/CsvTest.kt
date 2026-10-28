package app.viajor.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CsvTest {
    private fun parse(s: String, limits: CsvLimits = CsvLimits()) = CsvReader.parse(s, limits)

    @Test
    fun readsQuotedFieldsWithCommasQuotesAndNewlines() {
        val t = parse("a,\"b,c\",\"say \"\"hi\"\"\"\r\n\"line1\nline2\",x,\"\"\r\n")
        assertEquals(listOf(listOf("a", "b,c", "say \"hi\""), listOf("line1\nline2", "x", "")), t.rows)
        assertEquals(emptyList(), t.raggedRowNumbers)
    }

    @Test
    fun acceptsLfCrlfAndCrRowEndingsAndNoTrailingNewline() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parse("a,b\nc,d").rows)
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parse("a,b\r\nc,d\r\n").rows)
        assertEquals(listOf(listOf("a"), listOf("b")), parse("a\rb\r").rows)
    }

    @Test
    fun stripsBomAndSkipsBlankLines() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parse("﻿a,b\n\nc,d\n").rows)
    }

    @Test
    fun emptyTrailingCellsAndQuotedEmptyRowsSurvive() {
        assertEquals(listOf(listOf("a", "", "")), parse("a,,\n").rows)
        assertEquals(listOf(listOf("")), parse("\"\"\n").rows)
    }

    @Test
    fun reportsRowsWithADifferentColumnCount() {
        val t = parse("a,b,c\n1,2\n4,5,6\n7\n")
        assertEquals(listOf(2, 4), t.raggedRowNumbers)
    }

    @Test
    fun unterminatedQuoteIsAnError() {
        assertFailsWith<CsvException> { parse("a,\"b\n") }
    }

    @Test
    fun limitsAreEnforcedNotSilentlyTruncated() {
        assertFailsWith<CsvException> { parse("a,b\n", CsvLimits(maxChars = 3)) }
        assertFailsWith<CsvException> { parse("a\nb\nc\n", CsvLimits(maxRows = 2)) }
        assertFailsWith<CsvException> { parse("a,b,c\n", CsvLimits(maxColumns = 2)) }
        assertEquals(2, parse("a\nb\n", CsvLimits(maxRows = 2)).rows.size)
    }

    @Test
    fun writerQuotesOnlyWhenNeededAndUsesCrlf() {
        val out = CsvWriter.write(listOf(listOf("a", "b,c", "d\"e"), listOf("x", "y", "z")))
        assertEquals("a,\"b,c\",\"d\"\"e\"\r\nx,y,z\r\n", out)
    }

    @Test
    fun formulaLikeCellsAreNeutralisedOnExportAndRestoredOnImport() {
        val rows = listOf(listOf("=HYPERLINK(\"http://x\")", "+1+1", "-cmd", "@SUM(A1)", "\t=x", "'=already", "normal"))
        val out = CsvWriter.write(rows)
        // Every dangerous cell starts with an apostrophe in the file, so a spreadsheet shows text.
        assertTrue(out.startsWith("\"'=HYPERLINK("))
        assertTrue(out.contains(",'+1+1,'-cmd,'@SUM(A1),"))
        assertEquals(rows, CsvReader.parse(out).rows)
    }

    @Test
    fun plainNegativeNumbersAreNotPrefixed() {
        val out = CsvWriter.write(listOf(listOf("-5", "-12.50", "-5+2")))
        assertEquals("-5,-12.50,'-5+2\r\n", out)
        assertEquals(listOf(listOf("-5", "-12.50", "-5+2")), CsvReader.parse(out).rows)
    }

    @Test
    fun restoreCanBeSwitchedOff() {
        assertEquals(listOf(listOf("'=1")), CsvReader.parse("'=1\n", restoreFormulaCells = false).rows)
        assertEquals(listOf(listOf("=1")), CsvReader.parse("'=1\n").rows)
    }

    @Test
    fun randomTablesRoundTrip() {
        val alphabet = listOf("a", "b", " ", ",", "\"", "\n", "\r", "=", "+", "-", "@", "'", "\t", "é", "1")
        val random = Random(20271027)
        repeat(300) {
            val cols = random.nextInt(1, 5)
            val rows = List(random.nextInt(1, 6)) {
                List(cols) { List(random.nextInt(0, 6)) { alphabet[random.nextInt(alphabet.size)] }.joinToString("") }
            }
            val back = CsvReader.parse(CsvWriter.write(rows)).rows
            assertEquals(rows, back, "round trip failed for $rows")
        }
    }
}
