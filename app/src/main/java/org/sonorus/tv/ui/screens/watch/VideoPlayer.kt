package org.sonorus.tv.ui.screens.watch

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonPrimitive
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.model.PlayerInfo
import org.sonorus.tv.player.Letterbox
import org.sonorus.tv.player.PictureShare
import org.sonorus.tv.player.VideoRules
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.VideoFmt
import org.sonorus.tv.ui.components.ErrorNote
import org.sonorus.tv.ui.components.Loading
import org.sonorus.tv.ui.components.PageGutter
import kotlin.coroutines.resume
import kotlin.math.min
import kotlin.math.roundToInt

private const val HIDE_AFTER_MS = 4_000L
private const val SAMPLE_W = 192
private const val SAMPLE_H = 108

/**
 * The player itself keeps the focus until Up or Down enters the controls, so OK pauses and
 * Left and Right jump even while the controls are up. Media keys work wherever the focus is.
 */
@Composable
internal fun VideoPlayer(info: PlayerInfo, fromStart: Boolean, nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember(info.id) { VideoSession(context, info, scope) }
    val app = SonorusTvApp.instance
    val prefs by app.account.prefs.collectAsState()
    val autoplay = prefs.videoAutoplay

    var controls by remember { mutableStateOf(true) }
    var poked by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var menu by remember { mutableStateOf(false) }
    var fill by remember { mutableStateOf(app.settings.videoFill) }
    var surface by remember { mutableStateOf<SurfaceView?>(null) }
    val rootFocus = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }
    var rootHasFocus by remember { mutableStateOf(false) }
    var controlsFocused by remember { mutableStateOf(false) }
    var focusSpot by remember { mutableStateOf<FocusSpot?>(null) }

    fun pinned() = !session.wantsPlay || session.buffering || session.pending != null || session.ended
    fun shown() = (controls || pinned()) && !menu && session.error == null

    fun poke() {
        controls = true
        poked = SystemClock.uptimeMillis()
    }

    fun hide() {
        controls = false
        if (controlsFocused) runCatching { rootFocus.requestFocus() }
    }

    fun goNext() {
        val next = info.next ?: return
        session.forceComplete = true
        nav.watchNext(next.id)
    }

    fun seekKey(down: Boolean, by: Double) {
        if (down) {
            session.nudge(by)
            poke()
        } else {
            session.letGo()
        }
    }

    // Wherever the focus is: media keys, and any key keeps the controls up.
    fun mediaKey(e: KeyEvent): Boolean {
        val down = e.type == KeyEventType.KeyDown
        if (down) poked = SystemClock.uptimeMillis()
        val first = down && e.nativeKeyEvent.repeatCount == 0
        when (e.key) {
            Key.MediaPlayPause -> if (first) { session.toggle(); poke() }
            Key.MediaPlay -> if (first) { session.play(); poke() }
            Key.MediaPause -> if (first) { session.pause(); poke() }
            Key.MediaFastForward -> seekKey(down, VideoRules.SKIP)
            Key.MediaRewind -> seekKey(down, -VideoRules.SKIP)
            Key.MediaNext -> if (first) goNext()
            Key.Menu -> if (first && !menu && session.error == null) { poke(); focusSpot = FocusSpot.PLAY }
            // Both halves are taken, or the system hands an unclaimed one to another app's media session.
            else -> return false
        }
        return true
    }

    // What no control took. Arrows the focus can still move with are left to it.
    fun playerKey(e: KeyEvent): Boolean {
        if (menu || session.error != null) return false
        val down = e.type == KeyEventType.KeyDown
        val first = down && e.nativeKeyEvent.repeatCount == 0
        when (e.key) {
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (first) { session.toggle(); poke() }
            Key.DirectionLeft, Key.DirectionRight -> {
                if (!rootFocused) return false
                seekKey(down, if (e.key == Key.DirectionLeft) -VideoRules.SKIP else VideoRules.SKIP)
            }
            Key.DirectionUp, Key.DirectionDown -> {
                if (shown() && !rootFocused) return false
                if (first) { poke(); focusSpot = FocusSpot.PLAY }
            }
            else -> return false
        }
        return true
    }

    DisposableEffect(session) {
        session.load(if (fromStart) 0.0 else info.progress.position)
        session.pickSubtitle(session.defaultSubtitle(), remember = false)
        onDispose { session.release() }
    }

    // A stop pauses and keeps the place: Home, another app, or the TV going to standby.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        session.pause()
        session.save(force = true)
    }

    val view = LocalView.current
    val keepOn = session.wantsPlay && !session.ended
    DisposableEffect(view, keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(session) {
        while (true) {
            session.tick()
            val now = SystemClock.uptimeMillis()
            // The countdown only starts once nothing holds the controls up.
            if (pinned() || menu) poked = now
            else if (controls && session.held == null && now - poked > HIDE_AFTER_MS) hide()
            delay(250)
        }
    }

    // Whatever took the focus with it when it went (the card, the menu, an error) hands it back to the player.
    LaunchedEffect(rootHasFocus) {
        if (rootHasFocus) return@LaunchedEffect
        withFrameNanos { }
        if (!rootHasFocus) runCatching { rootFocus.requestFocus() }
    }

    // A small copy of the picture once a second, to learn its black bars; only while the zoom is wanted.
    LaunchedEffect(session, fill) {
        if (!fill) return@LaunchedEffect
        val frame = createBitmap(SAMPLE_W, SAMPLE_H)
        while (true) {
            delay(1_000)
            val view = surface ?: continue
            // A paused or buffering frame would count the same dark still several times.
            if (!session.playing || session.buffering || session.pending != null) continue
            if (view.copyInto(frame)) session.sample(frame)
        }
    }

    LaunchedEffect(session.notice) {
        if (session.notice != null) {
            delay(4_000)
            session.notice = null
        }
    }

    LaunchedEffect(session.ended) {
        if (!session.ended) return@LaunchedEffect
        when {
            info.next == null -> nav.back()
            autoplay && !session.nextDismissed -> goNext()
            else -> session.nextDismissed = false
        }
    }

    BackHandler {
        when {
            menu -> {
                menu = false
                poke()
                focusSpot = FocusSpot.MENU
            }
            shown() && !pinned() -> hide()
            else -> nav.back()
        }
    }

    val share = session.picture?.takeIf { fill } ?: PictureShare(1f, 1f)
    val shareW by animateFloatAsState(share.width, tween(300), label = "fillWidth")
    val shareH by animateFloatAsState(share.height, tween(300), label = "fillHeight")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { mediaKey(it) }
            .onKeyEvent { playerKey(it) }
            .focusRequester(rootFocus)
            .onFocusChanged {
                rootFocused = it.isFocused
                rootHasFocus = it.hasFocus
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { SurfaceView(it).also { view -> session.player.setVideoSurfaceView(view); surface = view } },
            onRelease = { session.player.clearVideoSurfaceView(it); surface = null },
            // Sized past the screen for the zoom; a SurfaceView does not reliably follow a graphicsLayer scale.
            modifier = Modifier.layout { measurable, constraints ->
                val boxW = constraints.maxWidth
                val boxH = constraints.maxHeight
                val aspect = session.aspect
                val zoom = Letterbox.zoom(boxW.toFloat(), boxH.toFloat(), aspect, PictureShare(shareW, shareH))
                val w = (min(boxW.toFloat(), boxH * aspect) * zoom).roundToInt()
                val h = (w / aspect).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(boxW, boxH) { placeable.place((boxW - w) / 2, (boxH - h) / 2) }
            },
        )

        val visible = shown()
        Subtitles(session, lifted = visible)

        if ((session.buffering || session.pending != null) && session.error == null) Loading()

        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Controls(
                session = session,
                focusSpot = focusSpot,
                onFocusTaken = { focusSpot = null },
                onFocusChanged = { controlsFocused = it },
                onPoke = ::poke,
                onMenu = {
                    menu = true
                    poke()
                },
                onNext = if (info.next != null) ::goNext else null,
            )
        }

        UpNext(session, autoplay, lifted = visible, grabFocus = rootFocused, onPlay = ::goNext)

        session.notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                // Clear of the menu, which takes the right edge.
                modifier = Modifier
                    .align(if (menu) Alignment.TopStart else Alignment.TopEnd)
                    .padding(top = 32.dp, start = PageGutter, end = PageGutter)
                    .widthIn(max = 480.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (menu) {
            TracksPanel(
                session = session,
                fill = fill,
                onFill = { on ->
                    fill = on
                    app.settings.videoFill = on
                },
                autoplay = autoplay,
                onAutoplay = { on ->
                    app.account.savePref("videoAutoplay", JsonPrimitive(on)) {
                        it.copy(videoAutoplay = on)
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        session.error?.let {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000))) {
                ErrorNote(it, onRetry = session::retry)
            }
        }
    }
}

/** The server's cues, sized for a screen three metres away and lifted over the controls. */
@Composable
private fun BoxScope.Subtitles(session: VideoSession, lifted: Boolean) {
    val lines = VideoRules.cueText(session.cues, session.clock)
    if (lines.isEmpty()) return
    val text = remember(lines) { CueMarkup.annotated(lines) }
    Text(
        text,
        style = TextStyle(
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            shadow = Shadow(Color.Black, blurRadius = 8f),
        ),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = if (lifted) 190.dp else 44.dp, start = 96.dp, end = 96.dp)
            .background(Color(0x73000000), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun BoxScope.UpNext(
    session: VideoSession,
    autoplay: Boolean,
    lifted: Boolean,
    grabFocus: Boolean,
    onPlay: () -> Unit,
) {
    val next = session.info.next ?: return
    val at = session.clock
    val seeking = session.pending != null || session.held != null
    if (!VideoRules.showNext(session.duration, at, seeking, session.nextDismissed, session.ended)) return
    NextCard(
        label = VideoFmt.episodeCode(next.season, next.episode, next.episodeEnd) +
            if (next.name.isNotEmpty()) " · ${next.name}" else "",
        fill = VideoRules.nextFill(session.duration, at, autoplay, session.ended),
        grabFocus = grabFocus,
        onPlay = onPlay,
        onDismiss = { session.nextDismissed = true },
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = PageGutter, bottom = if (lifted) 190.dp else 40.dp),
    )
}

private suspend fun SurfaceView.copyInto(frame: Bitmap): Boolean = suspendCancellableCoroutine { done ->
    try {
        PixelCopy.request(this, frame, { done.resume(it == PixelCopy.SUCCESS) }, Handler(Looper.getMainLooper()))
    } catch (e: IllegalArgumentException) {
        done.resume(false)
    }
}
