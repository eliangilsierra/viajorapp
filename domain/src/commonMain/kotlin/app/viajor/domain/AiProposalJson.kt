package app.viajor.domain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** Why a model's answer could not be used. The message never repeats the answer's content. */
class AiParseException(message: String) : Exception(message)

@Serializable
private data class ProposalDto(val summary: String = "", val operations: List<OperationDto> = emptyList())

@Serializable
private data class OperationDto(
    val id: String,
    val type: String,
    val item: ItemDto? = null,
    val target: String? = null,
    val changes: Map<String, JsonElement>? = null,
)

@Serializable
private data class ItemDto(
    val title: String,
    val notes: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val start: String? = null,
    val end: String? = null,
    val zone: String? = null,
)

/**
 * Turns a model's text answer into an [AiProposal] (ADR-0005). Parsing is strict: unknown operation types and
 * unknown fields are rejected. The result must still go through [ProposalValidator] before anything is shown
 * or applied; this class only checks the shape.
 */
object AiProposalJson {
    const val MAX_CHARS = 200_000

    private val json = Json { ignoreUnknownKeys = false; isLenient = false }
    private val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)

    fun parse(answer: String): AiProposal {
        if (answer.length > MAX_CHARS) throw AiParseException("the answer is too long")
        val text = extractJson(answer) ?: throw AiParseException("the answer has no JSON object")
        val dto = try {
            json.decodeFromString(ProposalDto.serializer(), text)
        } catch (e: SerializationException) {
            throw AiParseException("the answer is not a valid proposal")
        } catch (e: IllegalArgumentException) {
            throw AiParseException("the answer is not a valid proposal")
        }
        return AiProposal(dto.summary, dto.operations.map(::toOperation))
    }

    private fun toOperation(op: OperationDto): ProposalOp = when (op.type) {
        "add" -> {
            val item = op.item ?: throw AiParseException("an add operation has no item")
            ProposalOp.Add(op.id, NewItem(item.title, item.notes, item.lat, item.lon, item.start, item.end, item.zone))
        }
        "update" -> {
            val target = op.target ?: throw AiParseException("an update operation has no target")
            val changes = op.changes ?: throw AiParseException("an update operation has no changes")
            ProposalOp.Update(op.id, target, changes.mapValues { (_, v) -> scalar(v) })
        }
        "remove" -> ProposalOp.Remove(op.id, op.target ?: throw AiParseException("a remove operation has no target"))
        else -> throw AiParseException("an operation has an unknown type")
    }

    /** Field changes are strings (null clears a field); numbers are accepted and kept as text. */
    private fun scalar(value: JsonElement): String? = when (value) {
        is JsonNull -> null
        is JsonPrimitive -> value.content
        else -> throw AiParseException("a change value is not a simple value")
    }

    /** The JSON object in [answer]: the first fenced block if there is one, else from the first `{` to the last `}`. */
    internal fun extractJson(answer: String): String? {
        fenced.find(answer)?.groupValues?.get(1)?.trim()?.takeIf { it.startsWith("{") }?.let { return it }
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        return if (start in 0 until end) answer.substring(start, end + 1) else null
    }
}

/** What would be sent to a provider, shown to the person before anything leaves the device (ADR-0005). */
data class PromptPreview(val system: String, val user: String, val dataSent: List<String>)

object AiPrompt {
    private const val MAX_REQUEST = 2_000

    const val SYSTEM = """You help plan a trip. Answer with a single JSON object and nothing else:
{"summary": "<one sentence>", "operations": [
  {"id": "<unique>", "type": "add", "item": {"title": "...", "notes": "...", "lat": 0.0, "lon": 0.0, "start": "YYYY-MM-DDTHH:MM", "end": "YYYY-MM-DDTHH:MM", "zone": "Area/City"}},
  {"id": "<unique>", "type": "update", "target": "<existing item id>", "changes": {"title": "..."}},
  {"id": "<unique>", "type": "remove", "target": "<existing item id>"}]}
Rules: only suggest changes; the person reviews each one. Use only item ids you were given. Do not invent coordinates you are not sure of: leave lat and lon out. Do not include any secret or key in your answer."""

    /** Builds the request from the items of one trip and the person's text. Key-shaped text in the request is masked. */
    fun build(itinerary: Itinerary, request: String): PromptPreview {
        val safeRequest = SecretRedactor().redact(request.trim().take(MAX_REQUEST))
        val sent = linkedSetOf("Your request text")
        val lines = itinerary.items.map { item ->
            sent += "Item titles"
            val parts = mutableListOf("id=${item.id}", "title=${item.title}")
            item.notes?.let { sent += "Item notes"; parts += "notes=$it" }
            if (item.lat != null && item.lon != null) {
                sent += "Coordinates"
                parts += "lat=${item.lat}"
                parts += "lon=${item.lon}"
            }
            if (item.startLocal != null || item.endLocal != null) {
                sent += "Times and time zones"
                item.startLocal?.let { parts += "start=$it" }
                item.endLocal?.let { parts += "end=$it" }
                item.zoneId?.let { parts += "zone=$it" }
            }
            parts.joinToString("; ")
        }
        val user = buildString {
            append("Request: ").append(safeRequest).append("\n\nCurrent items:\n")
            if (lines.isEmpty()) append("(none)") else append(lines.joinToString("\n"))
        }
        return PromptPreview(SYSTEM, user, sent.toList())
    }
}
