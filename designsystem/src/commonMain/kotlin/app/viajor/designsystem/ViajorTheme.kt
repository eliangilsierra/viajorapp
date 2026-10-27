package app.viajor.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens. Values are provisional until the brand concept is approved
 * (docs/design/BRAND_STRATEGY.md); screens must use these tokens, never literals.
 */
object ViajorTokens {
    val primaryLight = Color(0xFF1B5E5A)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val surfaceLight = Color(0xFFFAFAF7)
    val onSurfaceLight = Color(0xFF1C1B1A)
    val primaryDark = Color(0xFF7FD0C9)
    val onPrimaryDark = Color(0xFF00201E)
    val surfaceDark = Color(0xFF121413)
    val onSurfaceDark = Color(0xFFE6E4E0)
}

@Composable
fun ViajorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = ViajorTokens.primaryDark,
            onPrimary = ViajorTokens.onPrimaryDark,
            surface = ViajorTokens.surfaceDark,
            onSurface = ViajorTokens.onSurfaceDark,
            background = ViajorTokens.surfaceDark,
            onBackground = ViajorTokens.onSurfaceDark,
        )
    } else {
        lightColorScheme(
            primary = ViajorTokens.primaryLight,
            onPrimary = ViajorTokens.onPrimaryLight,
            surface = ViajorTokens.surfaceLight,
            onSurface = ViajorTokens.onSurfaceLight,
            background = ViajorTokens.surfaceLight,
            onBackground = ViajorTokens.onSurfaceLight,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
