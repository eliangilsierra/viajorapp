package app.viajor.domain

/** Size limits applied before and during parsing; callers choose them (an import screen may ask first). */
data class CsvLimits(
    val maxChars: Int = 5_000_000,
    val maxRows: Int = 50_000,
    val maxColumns: Int = 200,
)

class CsvException(message: String) : Exception(message)

/** Parsed rows plus the 1-based numbers of rows whose column count differs from the first row. */
data class CsvTable(val rows: List<List<String>>, val raggedRowNumbers: List<Int>)

/** RFC 4180 reader: quoted fields, doubled quotes, embedded newlines, CRLF/LF/CR rows, optional BOM. */
object CsvReader {
    fun parse(text: String, limits: CsvLimits = CsvLimits(), restoreFormulaCells: Boolean = true): CsvTable {
        if (text.length > limits.maxChars) throw CsvException("file is larger than ${limits.maxChars} characters")
        val src = text.removePrefix("﻿")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var cellWasQuoted = false
        var rowHasContent = false
        var i = 0

        fun endCell() {
            if (row.size >= limits.maxColumns) throw CsvException("more than ${limits.maxColumns} columns in a row")
            val value = cell.toString()
            row += if (restoreFormulaCells) CsvFormulaGuard.restore(value) else value
            cell.clear()
            cellWasQuoted = false
        }

        fun endRow() {
            // A completely blank line is skipped; a quoted empty cell (`""`) is a real row.
            if (rowHasContent) {
                endCell()
                if (rows.size >= limits.maxRows) throw CsvException("more than ${limits.maxRows} rows")
                rows += row
            }
            row = mutableListOf()
            cell.clear()
            cellWasQuoted = false
            rowHasContent = false
        }

        while (i < src.length) {
            val c = src[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < src.length && src[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when {
                    c == '"' && cell.isEmpty() && !cellWasQuoted -> {
                        inQuotes = true
                        cellWasQuoted = true
                        rowHasContent = true
                    }
                    c == ',' -> {
                        rowHasContent = true
                        endCell()
                    }
                    c == '\r' || c == '\n' -> {
                        if (c == '\r' && i + 1 < src.length && src[i + 1] == '\n') i++
                        endRow()
                    }
                    else -> {
                        cell.append(c)
                        rowHasContent = true
                    }
                }
            }
            i++
        }
        if (inQuotes) throw CsvException("a quoted field is not closed")
        endRow()

        val width = rows.firstOrNull()?.size
        val ragged = if (width == null) emptyList() else rows.mapIndexedNotNull { index, r -> if (r.size != width) index + 1 else null }
        return CsvTable(rows, ragged)
    }
}

/** Writer producing CRLF rows. Cells that a spreadsheet could execute as formulas are neutralised. */
object CsvWriter {
    fun write(rows: List<List<String>>, neutralizeFormulas: Boolean = true, bom: Boolean = false): String {
        val out = StringBuilder()
        if (bom) out.append('﻿')
        for (row in rows) {
            val single = row.size == 1
            out.append(
                row.joinToString(",") { raw ->
                    val value = if (neutralizeFormulas) CsvFormulaGuard.protect(raw) else raw
                    encode(value, forceQuote = single && value.isEmpty())
                },
            )
            out.append("\r\n")
        }
        return out.toString()
    }

    private fun encode(value: String, forceQuote: Boolean): String {
        val needsQuotes = forceQuote || value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }
}

/**
 * Spreadsheet formula injection guard. A cell that starts with `=`, `+`, `-`, `@`, tab or CR (after any
 * apostrophes) gets one leading apostrophe on export so spreadsheets show it as text; [restore] removes
 * exactly that apostrophe on import, so every cell round-trips. Plain negative numbers are left alone.
 */
object CsvFormulaGuard {
    private val needsGuard = Regex("^'*[=+\\-@\\t\\r]")
    private val plainNumber = Regex("^-?[0-9]+([.,][0-9]+)?$")
    private val guarded = Regex("^'+[=+\\-@\\t\\r]")

    fun protect(value: String): String =
        if (needsGuard.containsMatchIn(value) && !plainNumber.matches(value)) "'$value" else value

    fun restore(value: String): String = if (guarded.containsMatchIn(value)) value.substring(1) else value

}
