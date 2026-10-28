package app.viajor.data

import app.viajor.data.db.ViajorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

enum class SearchKind { PLACE, ACTIVITY }

data class SearchHit(val kind: SearchKind, val id: String, val tripId: String, val text: String)

interface SearchRepository {
    /** Searches places and activities, optionally inside one trip. Blank or symbol-only text finds nothing. */
    suspend fun search(text: String, tripId: String? = null, limit: Int = 50): List<SearchHit>
}

class SqlSearchRepository(
    private val db: ViajorDatabase,
    private val io: CoroutineContext = Dispatchers.Default,
) : SearchRepository {
    override suspend fun search(text: String, tripId: String?, limit: Int): List<SearchHit> {
        val query = SearchQuery.toMatch(text) ?: return emptyList()
        return withContext(io) {
            db.searchQueries.search(query, tripId, limit.coerceIn(1, 200).toLong()).executeAsList().map {
                SearchHit(
                    kind = if (it.kind == "place") SearchKind.PLACE else SearchKind.ACTIVITY,
                    id = it.ref_id.orEmpty(),
                    tripId = it.trip_id.orEmpty(),
                    text = it.body.orEmpty().trim(),
                )
            }
        }
    }
}

/** Turns what a person typed into a safe FTS query, so quotes and operators in the text cannot break the syntax. */
object SearchQuery {
    const val MAX_TOKENS = 8

    /**
     * Each run of letters or digits becomes a lower-case prefix term; terms are ANDed. Lower case matters:
     * FTS operators such as OR, NOT and NEAR are only recognised in capitals. Returns null when nothing searchable is left.
     */
    fun toMatch(text: String): String? {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (c in text) {
            if (c.isLetterOrDigit()) {
                current.append(c.lowercaseChar())
            } else if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) tokens += current.toString()
        if (tokens.isEmpty()) return null
        return tokens.take(MAX_TOKENS).joinToString(" ") { "$it*" }
    }
}
