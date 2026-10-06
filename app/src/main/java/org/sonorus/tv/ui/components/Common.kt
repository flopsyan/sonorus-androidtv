package org.sonorus.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.ui.theme.RackLabel
import org.sonorus.tv.ui.theme.SonorusTheme

/** Left and right inset of every page; TV overscan eats the outer few percent. */
val PageGutter = 48.dp

/** Artwork path from the server, made absolute. */
fun art(path: String?): String? = SonorusTvApp.instance.api.art(path)

@Composable
fun Loading(modifier: Modifier = Modifier) {
    val accent = SonorusTheme.colors.accent
    val turn by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            drawArc(accent, turn, 270f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/** A failure with a retry button that takes the focus, so OK on the remote retries. */
@Composable
fun ErrorNote(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    Column(
        modifier.fillMaxSize().padding(PageGutter),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = SonorusTheme.colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp),
        )
        if (onRetry != null) {
            SonorusButton("Erneut versuchen", null, onRetry, Modifier.focusRequester(focus))
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        }
    }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(PageGutter), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = SonorusTheme.colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp),
        )
    }
}

/** The front-panel label over a section. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = RackLabel, color = SonorusTheme.colors.textDim, modifier = modifier)
}

/**
 * A labelled row of cards. Focus comes back to the card that had it, the way every
 * TV app's rows behave.
 */
@Composable
fun Shelf(label: String, modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        SectionLabel(label, Modifier.padding(start = PageGutter, bottom = 12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusRestorer(),
            contentPadding = PaddingValues(horizontal = PageGutter, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** A thin accent bar for how far a video has been watched. */
@Composable
fun ProgressLine(fraction: Double, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x8C000000)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.toFloat().coerceIn(0.02f, 1f))
                .height(4.dp)
                .background(SonorusTheme.colors.accent),
        )
    }
}

@Composable
fun DoneBadge(modifier: Modifier = Modifier) {
    val colors = SonorusTheme.colors
    Box(modifier.size(26.dp).clip(CircleShape).background(colors.accent), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Check, "Gesehen", tint = colors.accentInk, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PlayBadge(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(modifier.size(size).clip(CircleShape).background(Color(0x9E100E14)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** A full-bleed picture faded into the page on the left and at the bottom, for heroes and detail pages. */
@Composable
fun Backdrop(path: String?, modifier: Modifier = Modifier) {
    val bg = SonorusTheme.colors.bg
    Box(modifier) {
        AsyncImage(
            model = art(path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to bg, 0.55f to bg.copy(alpha = 0.55f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to bg)))
    }
}
