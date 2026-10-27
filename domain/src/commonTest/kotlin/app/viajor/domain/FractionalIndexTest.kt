package app.viajor.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FractionalIndexTest {
    @Test
    fun firstKeyIsValid() {
        val key = FractionalIndex.between(null, null)
        assertTrue(FractionalIndex.isValid(key))
    }

    @Test
    fun keyBetweenIsStrictlyBetween() {
        val a = FractionalIndex.between(null, null)
        val b = FractionalIndex.between(a, null)
        val mid = FractionalIndex.between(a, b)
        assertTrue(a < mid && mid < b, "$a < $mid < $b")
    }

    @Test
    fun appendAndPrependKeepOrder() {
        var keys = listOf(FractionalIndex.between(null, null))
        repeat(200) { keys = keys + FractionalIndex.between(keys.last(), null) }
        repeat(200) { keys = listOf(FractionalIndex.between(null, keys.first())) + keys }
        assertEquals(keys.sorted(), keys)
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all(FractionalIndex::isValid))
    }

    @Test
    fun randomInsertionsStayOrderedAndUnique() {
        val random = Random(42)
        val keys = mutableListOf(FractionalIndex.between(null, null))
        repeat(2_000) {
            val index = random.nextInt(keys.size + 1)
            val before = keys.getOrNull(index - 1)
            val after = keys.getOrNull(index)
            val key = FractionalIndex.between(before, after)
            keys.add(index, key)
        }
        assertEquals(keys.sorted(), keys)
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all(FractionalIndex::isValid))
    }

    @Test
    fun rejectsInvalidAndMisorderedBounds() {
        assertFailsWith<IllegalArgumentException> { FractionalIndex.between("b", "a") }
        assertFailsWith<IllegalArgumentException> { FractionalIndex.between("a", "a") }
        assertFailsWith<IllegalArgumentException> { FractionalIndex.between("a0", null) }
        assertFailsWith<IllegalArgumentException> { FractionalIndex.between("a!", null) }
    }
}
