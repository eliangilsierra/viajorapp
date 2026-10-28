package app.viajor.domain

/** Where an item came from (ADR-0005). AI and geocoder results are always visible as such. */
enum class Provenance { USER, IMPORT, AI, GEOCODER }

/** The slice of an itinerary item an AI proposal can touch. Times are local `YYYY-MM-DDTHH:MM` plus a zone id. */
data class ItineraryItem(
    val id: String,
    val title: String,
    val notes: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val startLocal: String? = null,
    val endLocal: String? = null,
    val zoneId: String? = null,
    val provenance: Provenance = Provenance.USER,
)

data class Itinerary(val items: List<ItineraryItem>)

/** Fields of a new item proposed by the AI. */
data class NewItem(
    val title: String,
    val notes: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val startLocal: String? = null,
    val endLocal: String? = null,
    val zoneId: String? = null,
)

/** One independently acceptable change. [opId] is unique within a proposal. */
sealed interface ProposalOp {
    val opId: String

    data class Add(override val opId: String, val item: NewItem) : ProposalOp

    /** [changes] maps an allowed field name to its new string value (null clears an optional field). */
    data class Update(override val opId: String, val targetId: String, val changes: Map<String, String?>) : ProposalOp

    data class Remove(override val opId: String, val targetId: String) : ProposalOp
}

data class AiProposal(val summary: String, val ops: List<ProposalOp>)

data class ProposalLimits(
    val maxOps: Int = 50,
    val maxSummary: Int = 500,
    val maxTitle: Int = 200,
    val maxNotes: Int = 2_000,
)

data class ProposalIssue(val opId: String?, val message: String)

object ProposalValidator {
    val UPDATABLE_FIELDS = setOf("title", "notes", "lat", "lon", "startLocal", "endLocal", "zoneId")
    private val localDateTime = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")

    /** Every problem at once. A proposal with any issue must not be applied at all. */
    fun validate(proposal: AiProposal, itinerary: Itinerary, limits: ProposalLimits = ProposalLimits()): List<ProposalIssue> {
        val issues = mutableListOf<ProposalIssue>()
        if (proposal.summary.length > limits.maxSummary) issues += ProposalIssue(null, "summary is longer than ${limits.maxSummary}")
        if (proposal.ops.isEmpty()) issues += ProposalIssue(null, "proposal has no operations")
        if (proposal.ops.size > limits.maxOps) issues += ProposalIssue(null, "more than ${limits.maxOps} operations")
        val ids = itinerary.items.associateBy { it.id }
        val seenOps = mutableSetOf<String>()
        val seenTargets = mutableSetOf<String>()
        for (op in proposal.ops) {
            if (op.opId.isBlank() || !seenOps.add(op.opId)) issues += ProposalIssue(op.opId, "operation id is blank or repeated")
            when (op) {
                is ProposalOp.Add -> issues += checkFields(op.opId, fieldsOf(op.item), limits, requireTitle = true)
                is ProposalOp.Update -> {
                    val target = ids[op.targetId]
                    if (target == null) issues += ProposalIssue(op.opId, "target ${op.targetId} does not exist")
                    if (!seenTargets.add(op.targetId)) issues += ProposalIssue(op.opId, "more than one operation targets ${op.targetId}")
                    if (op.changes.isEmpty()) issues += ProposalIssue(op.opId, "update changes nothing")
                    val unknown = op.changes.keys - UPDATABLE_FIELDS
                    if (unknown.isNotEmpty()) issues += ProposalIssue(op.opId, "fields not allowed: ${unknown.sorted()}")
                    if (target != null && unknown.isEmpty()) {
                        issues += checkFields(op.opId, fieldsOf(applyChanges(target, op.changes)), limits, requireTitle = true)
                    }
                }
                is ProposalOp.Remove -> {
                    if (!ids.containsKey(op.targetId)) issues += ProposalIssue(op.opId, "target ${op.targetId} does not exist")
                    if (!seenTargets.add(op.targetId)) issues += ProposalIssue(op.opId, "more than one operation targets ${op.targetId}")
                }
            }
        }
        return issues
    }

    private data class Fields(
        val title: String,
        val notes: String?,
        val lat: Double?,
        val lon: Double?,
        val start: String?,
        val end: String?,
        val zone: String?,
    )

    private fun fieldsOf(item: NewItem) = Fields(item.title, item.notes, item.lat, item.lon, item.startLocal, item.endLocal, item.zoneId)

    private fun fieldsOf(item: ItineraryItem) = Fields(item.title, item.notes, item.lat, item.lon, item.startLocal, item.endLocal, item.zoneId)

    private fun checkFields(opId: String, f: Fields, limits: ProposalLimits, requireTitle: Boolean): List<ProposalIssue> {
        val issues = mutableListOf<ProposalIssue>()
        fun bad(message: String) = issues.add(ProposalIssue(opId, message))
        if (requireTitle && f.title.isBlank()) bad("title is empty")
        if (f.title.length > limits.maxTitle) bad("title is longer than ${limits.maxTitle}")
        if ((f.notes?.length ?: 0) > limits.maxNotes) bad("notes are longer than ${limits.maxNotes}")
        if ((f.lat == null) != (f.lon == null)) bad("latitude and longitude must be set together")
        if (f.lat != null && (f.lat.isNaN() || f.lat !in -90.0..90.0)) bad("latitude is out of range")
        if (f.lon != null && (f.lon.isNaN() || f.lon !in -180.0..180.0)) bad("longitude is out of range")
        for ((name, v) in listOf("start" to f.start, "end" to f.end)) {
            if (v != null && !localDateTime.matches(v)) bad("$name time must look like 2027-03-01T09:30")
        }
        if (f.start != null && f.end != null && f.end < f.start) bad("end is before start")
        if ((f.start != null || f.end != null) && f.zone.isNullOrBlank()) bad("a time needs a time zone id")
        return issues
    }

    internal fun applyChanges(item: ItineraryItem, changes: Map<String, String?>): ItineraryItem {
        var out = item
        for ((key, value) in changes) {
            out = when (key) {
                "title" -> out.copy(title = value ?: "")
                "notes" -> out.copy(notes = value)
                "lat" -> out.copy(lat = value?.toDoubleOrNull() ?: if (value == null) null else Double.NaN)
                "lon" -> out.copy(lon = value?.toDoubleOrNull() ?: if (value == null) null else Double.NaN)
                "startLocal" -> out.copy(startLocal = value)
                "endLocal" -> out.copy(endLocal = value)
                "zoneId" -> out.copy(zoneId = value)
                else -> out
            }
        }
        return out
    }
}

/** What accepting or rejecting one operation would do, for a review screen. */
data class DiffEntry(
    val opId: String,
    val kind: Kind,
    val targetId: String?,
    val before: Map<String, String?>,
    val after: Map<String, String?>,
) {
    enum class Kind { ADD, UPDATE, REMOVE }
}

object ProposalDiff {
    fun of(proposal: AiProposal, itinerary: Itinerary): List<DiffEntry> {
        val byId = itinerary.items.associateBy { it.id }
        return proposal.ops.map { op ->
            when (op) {
                is ProposalOp.Add -> DiffEntry(
                    op.opId, DiffEntry.Kind.ADD, null, emptyMap(),
                    mapOf("title" to op.item.title, "notes" to op.item.notes, "startLocal" to op.item.startLocal, "endLocal" to op.item.endLocal),
                )
                is ProposalOp.Update -> {
                    val target = byId[op.targetId]
                    DiffEntry(
                        op.opId, DiffEntry.Kind.UPDATE, op.targetId,
                        op.changes.keys.associateWith { key -> target?.let { valueOf(it, key) } },
                        op.changes,
                    )
                }
                is ProposalOp.Remove -> DiffEntry(
                    op.opId, DiffEntry.Kind.REMOVE, op.targetId,
                    byId[op.targetId]?.let { mapOf("title" to it.title) } ?: emptyMap(), emptyMap(),
                )
            }
        }
    }

    private fun valueOf(item: ItineraryItem, key: String): String? = when (key) {
        "title" -> item.title
        "notes" -> item.notes
        "lat" -> item.lat?.toString()
        "lon" -> item.lon?.toString()
        "startLocal" -> item.startLocal
        "endLocal" -> item.endLocal
        "zoneId" -> item.zoneId
        else -> null
    }
}

object ProposalApply {
    /**
     * Applies only the [accepted] operations. The whole proposal must validate first; an invalid proposal is
     * rejected as a whole and nothing changes. Added and changed items get provenance [Provenance.AI].
     * Rejected operations leave the itinerary exactly as it was. [newId] makes an id for each added item.
     */
    fun apply(
        itinerary: Itinerary,
        proposal: AiProposal,
        accepted: Set<String>,
        limits: ProposalLimits = ProposalLimits(),
        newId: (opId: String) -> String,
    ): Itinerary {
        val issues = ProposalValidator.validate(proposal, itinerary, limits)
        require(issues.isEmpty()) { "invalid proposal: ${issues.joinToString("; ") { "${it.opId ?: "-"}: ${it.message}" }}" }
        val unknown = accepted - proposal.ops.map { it.opId }.toSet()
        require(unknown.isEmpty()) { "accepted operations not in the proposal: ${unknown.sorted()}" }

        val chosen = proposal.ops.filter { it.opId in accepted }
        val removed = chosen.filterIsInstance<ProposalOp.Remove>().map { it.targetId }.toSet()
        val updates = chosen.filterIsInstance<ProposalOp.Update>().associateBy { it.targetId }
        val kept = itinerary.items.filter { it.id !in removed }.map { item ->
            updates[item.id]?.let { ProposalValidator.applyChanges(item, it.changes).copy(provenance = Provenance.AI) } ?: item
        }
        val added = chosen.filterIsInstance<ProposalOp.Add>().map { op ->
            ItineraryItem(
                id = newId(op.opId), title = op.item.title, notes = op.item.notes, lat = op.item.lat, lon = op.item.lon,
                startLocal = op.item.startLocal, endLocal = op.item.endLocal, zoneId = op.item.zoneId, provenance = Provenance.AI,
            )
        }
        return Itinerary(kept + added)
    }
}
