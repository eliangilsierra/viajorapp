package app.viajor.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SyncMergeTest {
    private fun h(wall: Long, device: String, counter: Int = 0) = Hlc(wall, counter, device)
    private fun fv(value: String?, hlc: Hlc) = FieldVersion(value, hlc)
    private fun record(vararg f: Pair<String, FieldVersion>) = SyncRecord(mapOf(*f))

    private val base = record("title" to fv("Lisbon", h(1, "a")), "notes" to fv("n", h(1, "a")))

    @Test
    fun hlcOrdersByWallThenCounterThenDeviceAndRoundTripsAsText() {
        assertTrue(h(1, "a") < h(2, "a"))
        assertTrue(h(1, "a", 1) > h(1, "b", 0))
        assertTrue(h(1, "a") < h(1, "b"))
        val x = Hlc(1_700_000_000_123, 7, "device-with-dashes")
        assertEquals(x, Hlc.parse(x.toString()))
        assertTrue(Hlc(9, 0, "a").toString() < Hlc(10, 0, "a").toString())
        assertFailsWith<IllegalArgumentException> { Hlc.parse("nonsense") }
        assertFailsWith<IllegalArgumentException> { Hlc(1, 0, "has space") }
    }

    @Test
    fun clockIsMonotonicEvenWhenWallTimeStandsStillOrGoesBack() {
        val clock = HlcClock("a")
        val first = clock.now(1000)
        val second = clock.now(1000)
        val third = clock.now(900)
        assertTrue(first < second && second < third)
        assertEquals(1000, third.wallMillis)
    }

    @Test
    fun receivingAdvancesPastTheRemoteStampAndRejectsExcessiveDrift() {
        val clock = HlcClock("a", maxDriftMillis = 1000)
        val local = clock.now(1000)
        val remote = h(1500, "b", 3)
        val after = clock.receive(remote, 1000)
        assertTrue(after > remote && after > local)
        assertFailsWith<IllegalArgumentException> { clock.receive(h(5000, "b"), 1000) }
    }

    @Test
    fun editsToDifferentFieldsAreBothKept() {
        val local = record("title" to fv("Lisbon 2027", h(5, "a")), "notes" to base.fields.getValue("notes"))
        val remote = record("title" to base.fields.getValue("title"), "notes" to fv("bring cash", h(6, "b")))
        val result = SyncMerge.merge(base, local, remote)
        assertEquals(emptyList(), result.conflicts)
        assertEquals("Lisbon 2027", result.record.fields.getValue("title").value)
        assertEquals("bring cash", result.record.fields.getValue("notes").value)
    }

    @Test
    fun sameFieldEditedOnBothSidesIsAConflictWithADeterministicWinner() {
        val local = record("title" to fv("Porto", h(5, "a")), "notes" to base.fields.getValue("notes"))
        val remote = record("title" to fv("Faro", h(6, "b")), "notes" to base.fields.getValue("notes"))
        val result = SyncMerge.merge(base, local, remote)
        val conflict = assertIs<Conflict.SameField>(result.conflicts.single())
        assertEquals("title", conflict.field)
        assertEquals("Porto", conflict.older.value)
        assertEquals("Faro", conflict.newer.value)
        assertEquals("Faro", result.record.fields.getValue("title").value)
        // Swapping the sides gives the same answer.
        assertEquals(result, SyncMerge.merge(base, remote, local))
    }

    @Test
    fun bothSidesMakingTheSameEditIsNotAConflict() {
        val local = record("title" to fv("Porto", h(5, "a")), "notes" to base.fields.getValue("notes"))
        val remote = record("title" to fv("Porto", h(7, "b")), "notes" to base.fields.getValue("notes"))
        val result = SyncMerge.merge(base, local, remote)
        assertEquals(emptyList(), result.conflicts)
        assertEquals(h(7, "b"), result.record.fields.getValue("title").hlc)
    }

    @Test
    fun editAgainstDeleteKeepsTheRecordAndReportsAConflict() {
        val deleted = SyncRecord(base.fields + (SyncRecord.DELETED to fv("true", h(8, "a"))))
        val edited = record("title" to fv("Lisbon", h(1, "a")), "notes" to fv("new notes", h(9, "b")))
        val result = SyncMerge.merge(base, deleted, edited)
        val conflict = assertIs<Conflict.EditedWhileDeleted>(result.conflicts.single())
        assertEquals(setOf("notes"), conflict.editedFields)
        assertEquals(null, result.record.fields.getValue(SyncRecord.DELETED).value)
        assertEquals("new notes", result.record.fields.getValue("notes").value)
        assertEquals(result, SyncMerge.merge(base, edited, deleted))
    }

    @Test
    fun deleteWithoutAConcurrentEditDeletes() {
        val deleted = SyncRecord(base.fields + (SyncRecord.DELETED to fv("true", h(8, "a"))))
        val result = SyncMerge.merge(base, deleted, base)
        assertEquals(emptyList(), result.conflicts)
        assertEquals("true", result.record.fields.getValue(SyncRecord.DELETED).value)
    }

    @Test
    fun randomMergesAreCommutativeIdempotentAndNeverLoseAConflict() {
        val random = Random(20271027)
        val fields = listOf("title", "notes", "date", "place")
        repeat(500) { run ->
            val b = SyncRecord(fields.associateWith { fv("base-$it", h(1, "z")) })
            fun side(device: String, wallStart: Long): SyncRecord {
                val f = b.fields.toMutableMap()
                var wall = wallStart
                for (name in fields) {
                    when (random.nextInt(4)) {
                        0 -> f[name] = fv(if (random.nextBoolean()) "same" else "$device-$name-$run", h(wall++, device))
                    }
                }
                if (random.nextInt(8) == 0) f[SyncRecord.DELETED] = fv("true", h(wall, device))
                return SyncRecord(f)
            }
            val l = side("a", 10)
            val r = side("b", 10)
            val lr = SyncMerge.merge(b, l, r)
            val rl = SyncMerge.merge(b, r, l)
            assertEquals(lr.record, rl.record, "commutative record, run $run")
            assertEquals(lr.conflicts.toSet(), rl.conflicts.toSet(), "commutative conflicts, run $run")
            assertEquals(lr.record, SyncMerge.merge(b, lr.record, r).record, "idempotent with remote, run $run")
            assertEquals(lr.record, SyncMerge.merge(b, lr.record, l).record, "idempotent with local, run $run")
            for (name in fields) {
                val changedL = l.fields.getValue(name).value != b.fields.getValue(name).value
                val changedR = r.fields.getValue(name).value != b.fields.getValue(name).value
                if (changedL && changedR && l.fields.getValue(name).value != r.fields.getValue(name).value) {
                    assertTrue(lr.conflicts.any { it is Conflict.SameField && it.field == name }, "missing conflict for $name, run $run")
                }
            }
        }
    }
}
