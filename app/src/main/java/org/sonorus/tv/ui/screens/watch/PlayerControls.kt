package org.sonorus.tv.ui.screens.watch

import org.sonorus.tv.ui.components.SonorusButton
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.sonorus.tv.player.VideoRules
import org.sonorus.tv.ui.VideoFmt
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.theme.RackLabel
import org.sonorus.tv.ui.theme.SonorusTheme
import org.sonorus.tv.ui.theme.num

/** Where the focus goes once the controls are drawn. */
internal enum class FocusSpot { PLAY, MENU }

private val Scrim = Color(0xD9000000)

@Composable
internal fun Controls(
    session: VideoSession,
    focusSpot: FocusSpot?,
    onFocusTaken: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onPoke: () -> Unit,
    onMenu: () -> Unit,
    onNext: (() -> Unit)?,
) {
    val info = session.info
    val playFocus = remember { FocusRequester() }
    val menuFocus = remember { FocusRequester() }

    LaunchedEffect(focusSpot) {
        val target = when (focusSpot) {
            FocusSpot.PLAY -> playFocus
            FocusSpot.MENU -> menuFocus
            null -> return@LaunchedEffect
        }
        runCatching { target.requestFocus() }
        onFocusTaken()
    }

    Box(Modifier.fillMaxSize().onFocusChanged { onFocusChanged(it.hasFocus) }.focusGroup()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Scrim, Color.Transparent)))
                .padding(start = PageGutter, end = PageGutter, top = 32.dp, bottom = 56.dp),
        ) {
            Text(
                info.title.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (info.kind == "show") {
                Text(
                    VideoFmt.episodeCode(info.season, info.episode, info.episodeEnd) +
                        if (info.name.isNotEmpty()) " · ${info.name}" else "",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Scrim)))
                .padding(start = PageGutter, end = PageGutter, top = 64.dp, bottom = 28.dp),
        ) {
            ClockText(session)
            Spacer(Modifier.height(6.dp))
            SeekBar(session, onStep = { dir -> session.nudge(dir * VideoRules.SKIP); onPoke() }, onRelease = session::letGo)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PlayerIconButton(Icons.Filled.Replay10, "10 Sekunden zurück") {
                    session.seek(session.now() - VideoRules.SKIP)
                    onPoke()
                }
                val pauses = session.wantsPlay && !session.ended
                PlayerIconButton(
                    if (pauses) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (pauses) "Pause" else "Abspielen",
                    Modifier.focusRequester(playFocus),
                ) {
                    session.toggle()
                    onPoke()
                }
                PlayerIconButton(Icons.Filled.Forward10, "10 Sekunden vor") {
                    session.seek(session.now() + VideoRules.SKIP)
                    onPoke()
                }
                Spacer(Modifier.weight(1f))
                PlayerButton(Icons.Filled.Subtitles, "Ton & Untertitel", Modifier.focusRequester(menuFocus), onMenu)
                if (onNext != null) PlayerButton(Icons.Filled.SkipNext, "Nächste Folge", onClick = onNext)
            }
        }
    }
}

@Composable
private fun ClockText(session: VideoSession) {
    val at = session.held ?: session.clock
    Text(
        VideoFmt.clock(at) + " / " + VideoFmt.clock(session.duration),
        style = num(16.sp),
        color = Color.White,
    )
}

/** Left and Right move it while it has the focus; a held key jumps once, when it is let go. */
@Composable
private fun SeekBar(session: VideoSession, onStep: (Int) -> Unit, onRelease: () -> Unit) {
    val duration = session.duration
    val at = session.held ?: session.clock
    val fraction = if (duration > 0) (at / duration).toFloat().coerceIn(0f, 1f) else 0f
    val buffered = if (duration > 0) (session.buffered / duration).toFloat().coerceIn(0f, 1f) else 0f
    val accent = SonorusTheme.colors.accent
    var focused by remember { mutableStateOf(false) }
    val line = if (focused) 8.dp else 4.dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                val dir = when (e.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> return@onKeyEvent false
                }
                when (e.type) {
                    KeyEventType.KeyDown -> onStep(dir)
                    KeyEventType.KeyUp -> onRelease()
                }
                true
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(line)
                .clip(RoundedCornerShape(line / 2))
                .background(Color.White.copy(alpha = 0.24f)),
        ) {
            if (buffered > fraction) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(buffered).background(Color.White.copy(alpha = 0.5f)))
            }
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(accent))
        }
        if (focused) {
            // A box as wide as the played part puts the knob at its end without measuring anything.
            Box(Modifier.fillMaxWidth(fraction)) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .requiredSize(20.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun PlayerIconButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = SonorusTheme.colors
    IconButton(
        onClick = onClick,
        modifier = modifier.size(52.dp),
        colors = IconButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White,
            focusedContainerColor = colors.accent,
            focusedContentColor = colors.accentInk,
        ),
    ) {
        Icon(icon, label, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun PlayerButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = SonorusTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White,
            focusedContainerColor = colors.accent,
            focusedContentColor = colors.accentInk,
        ),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/** Offers the next episode near the end; it takes the focus only from the bare player, never from the controls. */
@Composable
internal fun NextCard(
    label: String,
    fill: Float,
    grabFocus: Boolean,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonorusTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (grabFocus) runCatching { focus.requestFocus() }
    }
    Column(
        modifier
            .width(340.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE6181520))
            .focusGroup()
            .padding(16.dp),
    ) {
        Text("NÄCHSTE FOLGE", style = RackLabel, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(fill).height(3.dp).background(colors.accent))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SonorusButton("Jetzt ansehen", null, onPlay, Modifier.focusRequester(focus))
            SonorusButton("Abspann ansehen", null, onDismiss)
        }
    }
}
