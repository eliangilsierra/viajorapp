package app.viajor.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG 2.x contrast ratio, used to check the brand tokens (text needs 4.5:1, graphics 3:1). */
object Contrast {
    fun ratio(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(c: Color): Double =
        0.2126 * linear(c.red) + 0.7152 * linear(c.green) + 0.0722 * linear(c.blue)

    private fun linear(channel: Float): Double {
        val v = channel.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
}
