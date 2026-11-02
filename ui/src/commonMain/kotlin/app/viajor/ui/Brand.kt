package app.viajor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import viajorapp.ui.generated.resources.Res
import viajorapp.ui.generated.resources.app_name
import viajorapp.ui.generated.resources.brand_lockup_dark
import viajorapp.ui.generated.resources.brand_lockup_light
import viajorapp.ui.generated.resources.brand_symbol_dark
import viajorapp.ui.generated.resources.brand_symbol_light
import viajorapp.ui.generated.resources.status_stored_on_device

/** True when the current theme is dark; the brand has a separate lockup and symbol for dark backgrounds. */
@Composable
private fun darkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** The Viajorapp symbol (the V drawn as a route). Decorative next to text, so it has no description by default. */
@Composable
fun BrandSymbol(size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(
        painter = painterResource(if (darkSurface()) Res.drawable.brand_symbol_dark else Res.drawable.brand_symbol_light),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

/** The horizontal lockup: symbol plus the hand-drawn wordmark. */
@Composable
fun BrandLockup(height: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (darkSurface()) Res.drawable.brand_lockup_dark else Res.drawable.brand_lockup_light),
        contentDescription = stringResource(Res.string.app_name),
        modifier = modifier.height(height),
    )
}

/** The top of every top-level screen: the lockup on the left and the screen's actions on the right. */
@Composable
fun BrandHeader(modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandLockup(height = 34.dp)
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * A small pill with an accent dot. It states something that is true of the app today ("Stored on this device"),
 * never a capability that does not exist (such as offline maps).
 */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StoredOnDeviceChip(modifier: Modifier = Modifier) = StatusChip(stringResource(Res.string.status_stored_on_device), modifier)
