package app.viajor.designsystem

import kotlin.test.Test
import kotlin.test.assertTrue

class ViajorTokensTest {
    private val appearances = mapOf("light" to ViajorTokens.light, "dark" to ViajorTokens.dark)

    private fun assertAtLeast(name: String, ratio: Double, required: Double) =
        assertTrue(ratio >= required, "$name: contrast $ratio is below $required")

    @Test
    fun textPairsMeetWcagAa() {
        for ((mode, c) in appearances) {
            assertAtLeast("$mode text on background", Contrast.ratio(c.text, c.background), 4.5)
            assertAtLeast("$mode muted text on background", Contrast.ratio(c.textMuted, c.background), 4.5)
            assertAtLeast("$mode text on surface", Contrast.ratio(c.text, c.surface), 4.5)
            assertAtLeast("$mode on-primary on primary", Contrast.ratio(c.onPrimary, c.primary), 4.5)
            assertAtLeast("$mode on-accent on accent", Contrast.ratio(c.onAccent, c.accent), 4.5)
        }
    }

    @Test
    fun graphicsPairsMeetThreeToOne() {
        for ((mode, c) in appearances) {
            assertAtLeast("$mode primary on background", Contrast.ratio(c.primary, c.background), 3.0)
            assertAtLeast("$mode primary on surface", Contrast.ratio(c.primary, c.surface), 3.0)
            assertAtLeast("$mode accent on background", Contrast.ratio(c.accent, c.background), 3.0)
            assertAtLeast("$mode outline on background", Contrast.ratio(c.outline, c.background), 3.0)
        }
    }

    @Test
    fun ratioMatchesKnownValues() {
        val black = androidx.compose.ui.graphics.Color.Black
        val white = androidx.compose.ui.graphics.Color.White
        assertTrue(kotlin.math.abs(Contrast.ratio(black, white) - 21.0) < 0.01)
        assertTrue(kotlin.math.abs(Contrast.ratio(white, white) - 1.0) < 0.001)
    }
}
