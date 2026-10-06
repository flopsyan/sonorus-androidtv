package org.sonorus.tv.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The Sonorus palette from the web's `public/css/styles.css`, dark only: a TV is watched in the dark. */
@Immutable
data class SonorusColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,
    val lineSoft: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val accent: Color,
    val accentHi: Color,
    val accentSoft: Color,
    val accentLine: Color,
    val accentInk: Color,
    val danger: Color,
    val dangerSoft: Color,
    val ok: Color,
)

val SonorusDarkColors = SonorusColors(
    bg = Color(0xFF100E14),
    surface = Color(0xFF1A171F),
    surface2 = Color(0xFF221D29),
    surface3 = Color(0xFF2B2434),
    line = Color(0xFF2A2431),
    lineSoft = Color(0xFF201B27),
    text = Color(0xFFF2EEF4),
    textDim = Color(0xFF9B90A6),
    textFaint = Color(0xFF6D6479),
    accent = Color(0xFFF5A524),
    accentHi = Color(0xFFFFBE57),
    accentSoft = Color(0x24F5A524),
    accentLine = Color(0x59F5A524),
    accentInk = Color(0xFF24170A),
    danger = Color(0xFFF0705F),
    dangerSoft = Color(0x26F0705F),
    ok = Color(0xFF63C98A),
)
