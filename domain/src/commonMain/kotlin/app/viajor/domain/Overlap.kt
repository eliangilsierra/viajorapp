package app.viajor.domain

/** A timed itinerary item reduced to absolute epoch milliseconds so zones and DST are already resolved. */
data class TimedSpan(val id: String, val startMillis: Long, val endMillis: Long) {
    init {
        require(endMillis >= startMillis) { "end must not precede start" }
    }
}

data class OverlapPair(val firstId: String, val secondId: String)

object Overlap {
    /**
     * Pairs of spans whose open intervals intersect. Touching spans (one ends when the next starts) do
     * not overlap. Zero-length spans never overlap anything. Items without times are not passed in.
     */
    fun find(spans: List<TimedSpan>): List<OverlapPair> {
        val sorted = spans.filter { it.endMillis > it.startMillis }.sortedWith(compareBy({ it.startMillis }, { it.id }))
        val result = mutableListOf<OverlapPair>()
        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                if (sorted[j].startMillis >= sorted[i].endMillis) break
                result += OverlapPair(sorted[i].id, sorted[j].id)
            }
        }
        return result
    }
}
