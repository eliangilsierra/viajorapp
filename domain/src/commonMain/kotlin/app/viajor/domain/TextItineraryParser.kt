package app.viajor.domain

/** How sure the parser is. GUESSED items must be reviewed; nothing is imported silently (requirement IMP-04). */
enum class Certainty { EXACT, GUESSED }

data class ClockTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23 && minute in 0..59) { "invalid time $hour:$minute" }
    }

    override fun toString(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/** A day heading found in the text: `Day 2`, an ISO date, or a weekday name. */
data class DayRef(val label: String, val number: Int?, val isoDate: String?)

data class ParsedItem(
    val line: Int,
    val day: DayRef?,
    val start: ClockTime?,
    val end: ClockTime?,
    val title: String,
    val certainty: Certainty,
)

data class ParsedHeading(val line: Int, val day: DayRef, val note: String)

data class UnparsedLine(val line: Int, val text: String)

/**
 * Every non-blank input line ends up in exactly one of [headings], [items] or [unparsed], so the
 * review screen can show what was understood and what was left alone.
 */
data class TextImportResult(val headings: List<ParsedHeading>, val items: List<ParsedItem>, val unparsed: List<UnparsedLine>)

object TextItineraryParser {
    private const val T = "(?:[01]?\\d|2[0-3])[:.h][0-5]\\d|(?:[01]?\\d|2[0-3])h|(?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s*(?:am|pm)"
    private val leadingRange = Regex("^($T)\\s*(?:-|–|—|to|a|hasta)\\s*($T)(?![\\d:.])\\s*[-–—:,.]?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val leadingTime = Regex("^($T)(?![\\d:.])\\s*[-–—:,.]?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val trailingTime = Regex("^(.*?\\S)\\s+(?:at|@|a las|a la)?\\s*($T)$", RegexOption.IGNORE_CASE)
    private val bullet = Regex("^(?:[-*•·]|\\d{1,2}[.)])\\s+")
    private val bareHour = Regex("^\\d{1,2}\\s+\\S")
    private val dayNumber = Regex("^(?:day|d[ií]a)\\s*(\\d{1,3})\\b\\s*[:\\-–—.]?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val isoDate = Regex("^(\\d{4}-\\d{2}-\\d{2})\\b\\s*[:\\-–—,.]?\\s*(.*)$")
    private val weekday = Regex(
        "^(monday|tuesday|wednesday|thursday|friday|saturday|sunday|lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)\\b\\s*[:\\-–—,.]?\\s*(.*)$",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String): TextImportResult {
        val headings = mutableListOf<ParsedHeading>()
        val items = mutableListOf<ParsedItem>()
        val unparsed = mutableListOf<UnparsedLine>()
        var day: DayRef? = null

        text.lines().forEachIndexed { index, raw ->
            val lineNumber = index + 1
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed

            val found = heading(line)
            if (found != null) {
                day = found.first
                headings += ParsedHeading(lineNumber, found.first, found.second)
                return@forEachIndexed
            }

            val hadBullet = bullet.containsMatchIn(line)
            val body = line.replace(bullet, "").trim()
            val item = itemFrom(lineNumber, body, hadBullet, day)
            if (item != null) items += item else unparsed += UnparsedLine(lineNumber, line)
        }
        return TextImportResult(headings, items, unparsed)
    }

    private fun heading(line: String): Pair<DayRef, String>? {
        dayNumber.matchEntire(line)?.let { m ->
            val n = m.groupValues[1].toInt()
            return DayRef("Day $n", n, null) to m.groupValues[2].trim()
        }
        isoDate.matchEntire(line)?.let { m ->
            return DayRef(m.groupValues[1], null, m.groupValues[1]) to m.groupValues[2].trim()
        }
        weekday.matchEntire(line)?.let { m ->
            // A weekday followed by a time reads as an item, so only bare headings count.
            val rest = m.groupValues[2].trim()
            if (leadingTime.matches(rest)) return null
            return DayRef(m.groupValues[1].replaceFirstChar { it.uppercase() }, null, null) to rest
        }
        return null
    }

    private fun itemFrom(line: Int, body: String, hadBullet: Boolean, day: DayRef?): ParsedItem? {
        leadingRange.matchEntire(body)?.let { m ->
            val title = clean(m.groupValues[3])
            if (title.isNotEmpty()) {
                return ParsedItem(line, day, parseTime(m.groupValues[1]), parseTime(m.groupValues[2]), title, Certainty.EXACT)
            }
        }
        leadingTime.matchEntire(body)?.let { m ->
            val title = clean(m.groupValues[2])
            if (title.isNotEmpty()) return ParsedItem(line, day, parseTime(m.groupValues[1]), null, title, Certainty.EXACT)
        }
        trailingTime.matchEntire(body)?.let { m ->
            val title = clean(m.groupValues[1])
            if (title.isNotEmpty() && title.any { it.isLetter() }) {
                return ParsedItem(line, day, parseTime(m.groupValues[2]), null, title, Certainty.GUESSED)
            }
        }
        if (!hadBullet) return null
        val title = clean(body)
        if (title.isEmpty()) return null
        // "9 Museum" could be a time or a list number; it stays untimed and must be reviewed.
        val certainty = if (bareHour.containsMatchIn(title)) Certainty.GUESSED else Certainty.EXACT
        return ParsedItem(line, day, null, null, title, certainty)
    }

    private fun clean(s: String): String = s.trim().trim('-', '–', '—', ':', ',', '.').trim()

    /** Accepts `9:30`, `09.30`, `14h30`, `14h`, `9am`, `9:30 PM`. Callers pass text matching the time pattern. */
    internal fun parseTime(token: String): ClockTime {
        val t = token.trim().lowercase()
        val meridiem = Regex("^(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)$").matchEntire(t)
        if (meridiem != null) {
            val h = meridiem.groupValues[1].toInt() % 12
            val minute = meridiem.groupValues[2].ifEmpty { "0" }.toInt()
            return ClockTime(if (meridiem.groupValues[3] == "pm") h + 12 else h, minute)
        }
        val hOnly = Regex("^(\\d{1,2})h$").matchEntire(t)
        if (hOnly != null) return ClockTime(hOnly.groupValues[1].toInt(), 0)
        val parts = t.split(':', '.', 'h')
        return ClockTime(parts[0].toInt(), parts[1].toInt())
    }
}
