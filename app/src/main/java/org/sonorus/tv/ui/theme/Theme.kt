package org.sonorus.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.darkColorScheme

val LocalSonorusColors = staticCompositionLocalOf { SonorusDarkColors }

object SonorusTheme {
    val colors: SonorusColors
        @Composable get() = LocalSonorusColors.current
}

val SonorusShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun SonorusTheme(content: @Composable () -> Unit) {
    val c = SonorusDarkColors
    // The stock components land in the same world as the hand-built ones.
    val scheme = darkColorScheme(
        primary = c.accent,
        onPrimary = c.accentInk,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.accent,
        secondary = c.accent,
        onSecondary = c.accentInk,
        secondaryContainer = c.surface3,
        onSecondaryContainer = c.text,
        background = c.bg,
        onBackground = c.text,
        surface = c.surface,
        onSurface = c.text,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.textDim,
        inverseSurface = c.text,
        inverseOnSurface = c.bg,
        error = c.danger,
        onError = c.text,
        errorContainer = c.dangerSoft,
        onErrorContainer = c.danger,
        border = c.accent,
        borderVariant = c.line,
    )
    CompositionLocalProvider(LocalSonorusColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = SonorusTypography, shapes = SonorusShapes, content = content)
    }
}
