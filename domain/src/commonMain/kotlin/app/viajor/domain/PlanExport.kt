package app.viajor.domain

/** One activity as it appears in an exported plan. Times are already formatted `HH:MM`. */
data class ExportActivity(
    val title: String,
    val notes: String? = null,
    val start: String? = null,
    val end: String? = null,
    val place: String? = null,
    val completed: Boolean = false,
)

data class ExportDay(val index: Int, val date: String?, val activities: List<ExportActivity>)

data class ExportPlan(val title: String, val description: String?, val days: List<ExportDay>, val ideas: List<ExportActivity>)

/** Readable and re-importable forms of a trip (requirement EXP-02). Nothing here contains keys or device ids. */
object PlanExport {
    val CSV_HEADER = listOf("title", "day", "start", "end", "notes", "place")

    fun markdown(plan: ExportPlan): String = buildString {
        append("# ").append(oneLine(plan.title)).append("\n\n")
        plan.description?.takeIf { it.isNotBlank() }?.let { append(oneLine(it)).append("\n\n") }
        for (day in plan.days) {
            append("## Day ").append(day.index)
            day.date?.let { append(" · ").append(it) }
            append("\n\n")
            if (day.activities.isEmpty()) append("_Nothing planned._\n") else day.activities.forEach { append(line(it)) }
            append("\n")
        }
        if (plan.ideas.isNotEmpty()) {
            append("## Ideas\n\n")
            plan.ideas.forEach { append(line(it)) }
        }
    }.trimEnd() + "\n"

    /** CSV with the importer's header columns plus `place`. Formula-like cells are neutralised. */
    fun csv(plan: ExportPlan): String {
        val rows = mutableListOf(CSV_HEADER)
        for (day in plan.days) day.activities.forEach { rows += row(it, day.index.toString()) }
        plan.ideas.forEach { rows += row(it, "") }
        return CsvWriter.write(rows)
    }

    private fun row(a: ExportActivity, day: String) =
        listOf(a.title, day, a.start.orEmpty(), a.end.orEmpty(), a.notes.orEmpty(), a.place.orEmpty())

    private fun line(a: ExportActivity): String = buildString {
        append("- [").append(if (a.completed) "x" else " ").append("] ")
        if (a.start != null) {
            append(a.start)
            a.end?.let { append("–").append(it) }
            append(" ")
        }
        append(oneLine(a.title))
        a.place?.takeIf { it.isNotBlank() }?.let { append(" (").append(oneLine(it)).append(")") }
        a.notes?.takeIf { it.isNotBlank() }?.let { append(" — ").append(oneLine(it)) }
        append("\n")
    }

    private fun oneLine(text: String) = text.replace("\r", " ").replace("\n", " ").trim()
}
