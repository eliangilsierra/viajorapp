package app.viajor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TileSourceTest {
    @Test
    fun cleartextStyleUrlIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            TileSource("x", "X", "http://example.com/style.json", "attr", OfflinePolicy.FORBIDDEN)
        }
    }

    @Test
    fun forbiddenSourceCannotCarryOfflineLimits() {
        assertFailsWith<IllegalArgumentException> {
            TileSource("x", "X", "https://example.com/s.json", "attr", OfflinePolicy.FORBIDDEN, OfflineLimits(1, 1, 1))
        }
    }

    @Test
    fun presetDeclaresItsLimits() {
        assertEquals(3_000, TileSourcePresets.openFreeMap.offlineLimits?.maxTiles)
    }
}
