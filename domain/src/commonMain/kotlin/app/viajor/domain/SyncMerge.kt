package app.viajor.domain

/**
 * Hybrid logical clock stamp (ADR-0006): wall time, a counter for events in the same millisecond, and the
 * device id as the final tie-break so any two stamps are totally ordered on every device.
 */
data class Hlc(val wallMillis: Long, val counter: Int, val deviceId: String) : Comparable<Hlc> {
    init {
        require(wallMillis >= 0) { "wall time must not be negative" }
        require(counter in 0..MAX_COUNTER) { "counter out of range" }
        require(deviceId.isNotEmpty() && deviceId.none { it.isWhitespace() }) { "device id must be non-empty without spaces" }
    }

    override fun compareTo(other: Hlc): Int =
        compareValuesBy(this, other, Hlc::wallMillis, Hlc::counter, Hlc::deviceId)

    /** Fixed-width form that sorts as text in the same order as [compareTo] (the device id is last). */
    override fun toString(): String = "${wallMillis.toString().padStart(15, '0')}-${counter.toString().padStart(5, '0')}-$deviceId"

    companion object {
        const val MAX_COUNTER = 99_999

        fun parse(text: String): Hlc {
            val parts = text.split("-", limit = 3)
            require(parts.size == 3) { "not an HLC: $text" }
            return Hlc(
                parts[0].toLongOrNull() ?: throw IllegalArgumentException("not an HLC: $text"),
                parts[1].toIntOrNull() ?: throw IllegalArgumentException("not an HLC: $text"),
                parts[2],
            )
        }
    }
}

/**
 * One device's clock. It never reads the system time itself: callers pass the current wall time, which
 * keeps it testable. Stamps from [now] and [receive] strictly increase.
 */
class HlcClock(private val deviceId: String, private var last: Hlc? = null, private val maxDriftMillis: Long = 60_000) {
    fun now(wallMillis: Long): Hlc {
        val previous = last
        val next = when {
            previous == null || wallMillis > previous.wallMillis -> Hlc(wallMillis, 0, deviceId)
            else -> bump(previous.wallMillis, previous.counter)
        }
        last = next
        return next
    }

    /** Merges a stamp seen on another device; rejects stamps too far ahead of local time. */
    fun receive(remote: Hlc, wallMillis: Long): Hlc {
        require(remote.wallMillis - wallMillis <= maxDriftMillis) { "remote clock is too far ahead (drift limit $maxDriftMillis ms)" }
        val previous = last
        val maxWall = maxOf(wallMillis, remote.wallMillis, previous?.wallMillis ?: 0)
        val counter = when {
            previous != null && previous.wallMillis == maxWall && remote.wallMillis == maxWall -> maxOf(previous.counter, remote.counter) + 1
            previous != null && previous.wallMillis == maxWall -> previous.counter + 1
            remote.wallMillis == maxWall -> remote.counter + 1
            else -> 0
        }
        val next = if (counter > Hlc.MAX_COUNTER) bump(maxWall, Hlc.MAX_COUNTER) else Hlc(maxWall, counter, deviceId)
        last = next
        return next
    }

    private fun bump(wall: Long, counter: Int): Hlc =
        if (counter >= Hlc.MAX_COUNTER) Hlc(wall + 1, 0, deviceId) else Hlc(wall, counter + 1, deviceId)
}

/** A field's value and the stamp of its last edit. A null value means the field is unset. */
data class FieldVersion(val value: String?, val hlc: Hlc)

/** A synced record as a map of field versions. Deletion is the reserved field [DELETED] with value `true`. */
data class SyncRecord(val fields: Map<String, FieldVersion>) {
    companion object {
        const val DELETED = "_deleted"
    }
}

sealed interface Conflict {
    /** Both sides changed [field] to different values. [newer] won; [older] is kept for the user to review. */
    data class SameField(val field: String, val older: FieldVersion, val newer: FieldVersion) : Conflict

    /** One side deleted the record while the other edited [editedFields]; the record is kept, not deleted. */
    data class EditedWhileDeleted(val editedFields: Set<String>, val deleteStamp: Hlc) : Conflict
}

data class MergeResult(val record: SyncRecord, val conflicts: List<Conflict>)

object SyncMerge {
    /**
     * Three-way, field-level merge of [local] and [remote] against their common [base] (null if the record
     * is new on both). The result does not depend on which side is called local, nothing is overwritten
     * silently, and conflicts are returned alongside a deterministic provisional winner.
     */
    fun merge(base: SyncRecord?, local: SyncRecord, remote: SyncRecord): MergeResult {
        val merged = mutableMapOf<String, FieldVersion>()
        val conflicts = mutableListOf<Conflict>()
        val keys = (local.fields.keys + remote.fields.keys).sorted()
        for (key in keys) {
            val b = base?.fields?.get(key)
            val l = local.fields[key]
            val r = remote.fields[key]
            merged[key] = when {
                l == null -> r!!
                r == null -> l
                l == r -> l
                else -> {
                    val changedL = l.value != b?.value
                    val changedR = r.value != b?.value
                    when {
                        !changedL && !changedR -> maxOf(l, r, compareBy<FieldVersion> { it.hlc })
                        changedL && !changedR -> l
                        changedR && !changedL -> r
                        l.value == r.value -> maxOf(l, r, compareBy<FieldVersion> { it.hlc })
                        else -> {
                            val (older, newer) = if (l.hlc < r.hlc) l to r else r to l
                            conflicts += Conflict.SameField(key, older, newer)
                            newer
                        }
                    }
                }
            }
        }
        resolveDeleteConflict(base, local, remote, merged)?.let { conflicts += it }
        return MergeResult(SyncRecord(merged), conflicts)
    }

    private fun resolveDeleteConflict(
        base: SyncRecord?,
        local: SyncRecord,
        remote: SyncRecord,
        merged: MutableMap<String, FieldVersion>,
    ): Conflict.EditedWhileDeleted? {
        val result = merged[SyncRecord.DELETED] ?: return null
        if (result.value != "true") return null
        fun deletes(side: SyncRecord) = side.fields[SyncRecord.DELETED]?.value == "true" &&
            base?.fields?.get(SyncRecord.DELETED)?.value != "true"
        fun edited(side: SyncRecord): Set<String> = side.fields
            .filter { (k, v) -> k != SyncRecord.DELETED && v.value != base?.fields?.get(k)?.value }
            .keys
        val editedByLocal = if (deletes(remote) && !deletes(local)) edited(local) else emptySet()
        val editedByRemote = if (deletes(local) && !deletes(remote)) edited(remote) else emptySet()
        val edits = editedByLocal + editedByRemote
        if (edits.isEmpty()) return null
        // Keep the record: undo the deletion with a stamp after both sides, so the choice is deterministic.
        val latest = listOfNotNull(local.fields[SyncRecord.DELETED]?.hlc, remote.fields[SyncRecord.DELETED]?.hlc).max()
        merged[SyncRecord.DELETED] = FieldVersion(null, latest)
        return Conflict.EditedWhileDeleted(edits, result.hlc)
    }
}
