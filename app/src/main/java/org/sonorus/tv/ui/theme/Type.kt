package org.sonorus.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

// Sized for a screen three metres away; a 1080p TV is 960x540 dp.
val SonorusTypography = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

/** The front-panel label over a section: small, wide-tracked monospace. */
val RackLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.14.em,
)

/** Every number, monospace so a running clock does not jitter. */
fun num(size: TextUnit = 14.sp, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = size,
    fontWeight = weight,
)
