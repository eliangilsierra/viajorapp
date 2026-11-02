package app.viajor.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** One complete set of semantic colors for a light or dark appearance. */
data class ViajorColors(
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val textMuted: Color,
    val outline: Color,
)

/**
 * Waypoint brand tokens (concept 01, chosen by the owner on 2026-10-27). Values mirror
 * design/brand/concepts/01-waypoint/palette.json; the contrast pairs listed there are checked by
 * ViajorTokensTest. Screens must use these tokens through the theme, never color literals.
 */
object ViajorTokens {
    val light = ViajorColors(
        primary = Color(0xFF0B5563),
        onPrimary = Color(0xFFFFFFFF),
        accent = Color(0xFFB85C00),
        onAccent = Color(0xFFFFFFFF),
        background = Color(0xFFF7F5EF),
        surface = Color(0xFFFFFFFF),
        text = Color(0xFF10272D),
        textMuted = Color(0xFF44585F),
        outline = Color(0xFF728A91),
    )

    val dark = ViajorColors(
        primary = Color(0xFF6CCBDB),
        onPrimary = Color(0xFF06242B),
        accent = Color(0xFFFFC845),
        onAccent = Color(0xFF2A1B00),
        background = Color(0xFF0B1A1E),
        surface = Color(0xFF12282E),
        text = Color(0xFFE8F1F2),
        textMuted = Color(0xFFA9BDC2),
        outline = Color(0xFF5F8791),
    )
}

/**
 * Material3 roles not mapped here (tertiary, inverse and tint roles) keep Material
 * defaults until a component needs them; map them from the Waypoint tokens at that point.
 */
@Composable
fun ViajorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        val t = ViajorTokens.dark
        darkColorScheme(
            primary = t.primary,
            onPrimary = t.onPrimary,
            secondary = t.accent,
            onSecondary = t.onAccent,
            primaryContainer = t.primary,
            onPrimaryContainer = t.onPrimary,
            secondaryContainer = t.surface,
            onSecondaryContainer = t.text,
            surfaceContainerLowest = t.surface,
            surfaceContainerLow = t.surface,
            surfaceContainer = t.surface,
            surfaceContainerHigh = t.surface,
            surfaceContainerHighest = t.surface,
            background = t.background,
            onBackground = t.text,
            surface = t.surface,
            onSurface = t.text,
            onSurfaceVariant = t.textMuted,
            outline = t.outline,
        )
    } else {
        val t = ViajorTokens.light
        lightColorScheme(
            primary = t.primary,
            onPrimary = t.onPrimary,
            secondary = t.accent,
            onSecondary = t.onAccent,
            primaryContainer = t.primary,
            onPrimaryContainer = t.onPrimary,
            secondaryContainer = t.surface,
            onSecondaryContainer = t.text,
            surfaceContainerLowest = t.surface,
            surfaceContainerLow = t.surface,
            surfaceContainer = t.surface,
            surfaceContainerHigh = t.surface,
            surfaceContainerHighest = t.surface,
            background = t.background,
            onBackground = t.text,
            surface = t.surface,
            onSurface = t.text,
            onSurfaceVariant = t.textMuted,
            outline = t.outline,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
