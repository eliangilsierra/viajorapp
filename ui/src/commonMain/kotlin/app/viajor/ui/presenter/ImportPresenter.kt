package app.viajor.ui.presenter

import app.viajor.data.ActivityDraft
import app.viajor.data.DayRecord
import app.viajor.data.ItineraryRepository
import app.viajor.data.SaveActivityResult
import app.viajor.domain.Certainty
import app.viajor.domain.CsvException
import app.viajor.domain.CsvReader
import app.viajor.domain.DayRef
import app.viajor.domain.Provenance
import app.viajor.domain.TextItineraryParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.coroutines.cancellation.CancellationException

enum class ImportStage { INPUT, REVIEW, DONE }

enum class ImportError { NOTHING_FOUND, NOT_A_TABLE, BAD_FILE, FAILED }

/** One item the person can accept. Uncertain ones start unchecked (requirement IMP-04: nothing is imported silently). */
data class ImportCandidate(
    val id: Int,
    val title: String,
    val notes: String?,
    val dayIndex: Int?,
    val start: LocalTime?,
    val end: LocalTime?,
    val uncertain: Boolean,
    val checked: Boolean,
)

data class ImportState(
    val stage: ImportStage = ImportStage.INPUT,
    val candidates: List<ImportCandidate> = emptyList(),
    val unparsedLines: Int = 0,
    val error: ImportError? = null,
    val added: Int = 0,
    val failed: Int = 0,
)

/** Review-then-import for pasted text and CSV files. Only checked items are added; the rest are dropped. */
class ImportPresenter(
    private val itinerary: ItineraryRepository,
    private val scope: CoroutineScope,
    private val tripId: String,
    private val zoneId: String,
) {
    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()
    private var days: List<DayRecord> = emptyList()

    fun reviewText(text: String) = review {
        val result = TextItineraryParser.parse(text)
        var next = 0
        val items = result.items.map { item ->
            ImportCandidate(
                id = next++, title = item.title, notes = null, dayIndex = resolveDay(item.day),
                start = item.start?.let { LocalTime(it.hour, it.minute) }, end = item.end?.let { LocalTime(it.hour, it.minute) },
                uncertain = item.certainty == Certainty.GUESSED, checked = item.certainty == Certainty.EXACT,
            )
        }
        items to result.unparsed.size
    }

    fun reviewCsv(text: String) = review {
        val table = try {
            CsvReader.parse(text)
        } catch (e: CsvException) {
            _state.update { it.copy(error = ImportError.BAD_FILE) }
            return@review null
        }
        val header = table.rows.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
        val title = header.indexOf("title")
        if (title < 0) {
            _state.update { it.copy(error = ImportError.NOT_A_TABLE) }
            return@review null
        }
        val dayCol = header.indexOf("day")
        val startCol = header.indexOf("start")
        val endCol = header.indexOf("end")
        val notesCol = header.indexOf("notes")
        var bad = 0
        var next = 0
        val items = table.rows.drop(1).mapNotNull { row ->
            val name = row.getOrNull(title)?.trim().orEmpty()
            if (name.isEmpty()) {
                bad++
                return@mapNotNull null
            }
            val startText = row.getOrNull(startCol)?.trim().orEmpty().takeIf { startCol >= 0 }
            val endText = row.getOrNull(endCol)?.trim().orEmpty().takeIf { endCol >= 0 }
            val start = startText?.takeIf { it.isNotEmpty() }?.let(::parseHhmm)
            val end = endText?.takeIf { it.isNotEmpty() }?.let(::parseHhmm)
            val unreadable = (!startText.isNullOrEmpty() && start == null) || (!endText.isNullOrEmpty() && end == null)
            ImportCandidate(
                id = next++, title = name, notes = row.getOrNull(notesCol)?.trim()?.ifEmpty { null }.takeIf { notesCol >= 0 },
                dayIndex = if (dayCol >= 0) resolveDayText(row.getOrNull(dayCol).orEmpty().trim()) else null,
                start = start, end = end, uncertain = unreadable, checked = !unreadable,
            )
        }
        items to bad
    }

    fun toggle(id: Int) = _state.update { s -> s.copy(candidates = s.candidates.map { if (it.id == id) it.copy(checked = !it.checked) else it }) }

    fun back() = _state.update { ImportState() }

    fun confirm() {
        val chosen = _state.value.candidates.filter { it.checked }
        scope.launch {
            var added = 0
            var failed = 0
            try {
                for (c in chosen) {
                    val day = c.dayIndex?.let { index -> days.firstOrNull { it.index == index } }
                    val date = day?.date
                    val draft = ActivityDraft(
                        title = c.title,
                        notes = c.notes,
                        start = if (date != null && c.start != null) LocalDateTime(date, c.start) else null,
                        end = if (date != null && c.end != null) LocalDateTime(date, c.end) else null,
                        zoneId = if (date != null && (c.start != null || c.end != null)) zoneId else null,
                    )
                    when (itinerary.addActivity(tripId, day?.id, draft, Provenance.IMPORT)) {
                        is SaveActivityResult.Saved -> added++
                        else -> failed++
                    }
                }
                _state.update { it.copy(stage = ImportStage.DONE, added = added, failed = failed) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = ImportError.FAILED) }
            }
        }
    }

    private fun review(block: suspend () -> Pair<List<ImportCandidate>, Int>?) = scope.launch {
        _state.update { it.copy(error = null) }
        try {
            days = itinerary.observeDays(tripId).first()
            val found = block() ?: return@launch
            if (found.first.isEmpty()) {
                _state.update { it.copy(error = ImportError.NOTHING_FOUND) }
            } else {
                _state.update { it.copy(stage = ImportStage.REVIEW, candidates = found.first, unparsedLines = found.second) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(error = ImportError.FAILED) }
        }
    }

    /** The day a parsed heading points at: by number, by date, or by weekday (first matching day). Unknown means Ideas. */
    private fun resolveDay(ref: DayRef?): Int? {
        ref ?: return null
        ref.number?.let { n -> return days.firstOrNull { it.index == n }?.index }
        ref.isoDate?.let { iso -> return days.firstOrNull { it.date?.toString() == iso }?.index }
        val weekday = weekdayOf(ref.label) ?: return null
        return days.firstOrNull { it.date?.dayOfWeek == weekday }?.index
    }

    private fun resolveDayText(text: String): Int? {
        if (text.isEmpty()) return null
        text.toIntOrNull()?.let { n -> return days.firstOrNull { it.index == n }?.index }
        val date = try {
            LocalDate.parse(text)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return days.firstOrNull { it.date == date }?.index
    }

    private fun weekdayOf(label: String): DayOfWeek? = when (label.lowercase()) {
        "monday", "lunes" -> DayOfWeek.MONDAY
        "tuesday", "martes" -> DayOfWeek.TUESDAY
        "wednesday", "miércoles", "miercoles" -> DayOfWeek.WEDNESDAY
        "thursday", "jueves" -> DayOfWeek.THURSDAY
        "friday", "viernes" -> DayOfWeek.FRIDAY
        "saturday", "sábado", "sabado" -> DayOfWeek.SATURDAY
        "sunday", "domingo" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseHhmm(text: String): LocalTime? {
        val m = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$").matchEntire(text) ?: return null
        return LocalTime(m.groupValues[1].toInt(), m.groupValues[2].toInt())
    }
}
