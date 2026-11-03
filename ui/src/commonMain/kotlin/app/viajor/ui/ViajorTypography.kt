package app.viajor.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.inter_medium
import viajorapp.ui.generated.resources.inter_regular
import viajorapp.ui.generated.resources.inter_semibold
import viajorapp.ui.generated.resources.manrope_extrabold
import viajorapp.ui.generated.resources.manrope_semibold

/**
 * The Waypoint type scale (design/brand/concepts/01-waypoint/typography.md): Manrope for headings, Inter for body and
 * UI, both under the SIL OFL. Body and label styles use tabular figures so dates and times line up.
 */
@Composable
fun viajorTypography(): Typography {
    val display = FontFamily(
        Font(Res.font.manrope_semibold, FontWeight.SemiBold),
        Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
    )
    val body = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )
    return remember(display, body) {
        fun heading(size: Int, line: Int, weight: FontWeight) =
            TextStyle(fontFamily = display, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

        fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: TextUnit = TextUnit.Unspecified) =
            TextStyle(fontFamily = body, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing, fontFeatureSettings = "tnum")

        Typography(
            displayLarge = heading(40, 48, FontWeight.ExtraBold),
            displayMedium = heading(36, 44, FontWeight.ExtraBold),
            displaySmall = heading(32, 40, FontWeight.ExtraBold),
            headlineLarge = heading(28, 36, FontWeight.ExtraBold),
            headlineMedium = heading(24, 32, FontWeight.ExtraBold),
            headlineSmall = heading(22, 30, FontWeight.SemiBold),
            titleLarge = heading(22, 28, FontWeight.SemiBold),
            titleMedium = heading(18, 26, FontWeight.SemiBold),
            titleSmall = heading(16, 24, FontWeight.SemiBold),
            bodyLarge = text(16, 24),
            bodyMedium = text(15, 22),
            bodySmall = text(13, 18),
            labelLarge = text(14, 20, FontWeight.SemiBold),
            labelMedium = text(12, 16, FontWeight.Medium),
            labelSmall = text(11, 16, FontWeight.Medium),
        )
    }
}
