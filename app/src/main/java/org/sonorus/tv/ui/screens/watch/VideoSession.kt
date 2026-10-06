package org.sonorus.tv.ui.screens.watch

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.errorMessage
import org.sonorus.tv.data.model.AudioInfo
import org.sonorus.tv.data.model.Cue
import org.sonorus.tv.data.model.PlayerInfo
import org.sonorus.tv.data.videoFailureMessage
import org.sonorus.tv.player.Letterbox
import org.sonorus.tv.player.PictureShare
import org.sonorus.tv.player.VideoCaps
import org.sonorus.tv.player.VideoRules
import kotlin.math.abs

// How far a stream loads ahead; ExoPlayer's byte cap (about 140 MB) still stops a high bitrate sooner.
private const val AHEAD_MS = 5 * 60_000

/** Saves and play reports outlive the screen: the last save runs while it is already gone. */
private val keepScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/**
 * Everything that moves while one video plays. The contract is the web player's: a piped
 * stream cannot seek, so the clock is offset + position and a jump asks for a new stream.
 */
@OptIn(UnstableApi::class)
internal class VideoSession(context: Context, val info: PlayerInfo, private val scope: CoroutineScope) {

    private val app = SonorusTvApp.instance
    private val api = app.api

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(DefaultDataSource.Factory(context, OkHttpDataSource.Factory(api.client)))
        )
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMsForStreaming(
                    AHEAD_MS,
                    AHEAD_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                )
                .build()
        )
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        }

    var mode by mutableStateOf("")
        private set
    private var offset = 0.0
    // The force of the running attempt, so a re-login asks for the same stream again.
    private var force: String? = null
    var pending by mutableStateOf<Double?>(null)
        private set
    /** Where a held seek key has got to; the jump happens once it is let go. */
    var held by mutableStateOf<Double?>(null)
        private set
    var audio by mutableStateOf<Int?>(null)
        private set
    var sub by mutableStateOf<String?>(null)
        private set
    var cues by mutableStateOf<List<Cue>>(emptyList())
        private set
    var aspect by mutableFloatStateOf(16f / 9f)
        private set
    var playing by mutableStateOf(false)
        private set
    var wantsPlay by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(true)
        private set
    var ended by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
    var clock by mutableDoubleStateOf(0.0)
        private set
    var buffered by mutableDoubleStateOf(0.0)
        private set
    var nextDismissed by mutableStateOf(false)
    var picture by mutableStateOf<PictureShare?>(null)
        private set
    private val letterbox = Letterbox()
    private var pixels = IntArray(0)
    var forceComplete = false

    private var failed = 0
    private var relogged = false
    private var loadSeq = 0
    private var seekJob: Job? = null
    private var heldJob: Job? = null
    private var subSeq = 0

    // Time really watched, for the statistics; the web counts the same way.
    private var watched = 0.0
    private var reported = 0.0
    private var playId: Int? = null
    private var playPending = false
    private var lastSaved = -100.0
    private var lastTick = 0L

    val duration: Double get() = info.duration

    fun now(): Double = pending ?: VideoRules.clock(offset, player.currentPosition)

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantsPlay = playWhenReady
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (!isPlaying) save()
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (state == Player.STATE_ENDED) {
                    ended = true
                    forceComplete = true
                    save(force = true)
                }
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) aspect = size.width * size.pixelWidthHeightRatio / size.height
            }

            override fun onTracksChanged(tracks: Tracks) = applyAudio(tracks)

            override fun onPlayerError(e: PlaybackException) {
                val status = (e.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                // An expired session: log in again once and ask for the same stream, as the phone's downloads do.
                if (status == 401 && !relogged) {
                    relogged = true
                    val at = now()
                    scope.launch {
                        runCatching { api.relogin() }.onFailure {
                            error = errorMessage(it)
                            return@launch
                        }
                        load(at, force = force)
                    }
                    return
                }
                failed++
                val next = VideoRules.fallback(mode, failed)
                if (next != null) {
                    load(now(), force = next)
                } else {
                    val failedMime = (e as? ExoPlaybackException)
                        ?.takeIf { it.type == ExoPlaybackException.TYPE_RENDERER }
                        ?.rendererFormat?.sampleMimeType
                    error = videoFailureMessage(e.errorCode, status, failedMime)
                }
            }
        })
    }

    fun load(at: Double, audioIndex: Int? = audio, force: String? = null, paused: Boolean = false) {
        val seq = ++loadSeq
        pending = at
        error = null
        scope.launch {
            val plan = runCatching { api.videoPlan(info.id, at, audioIndex, VideoCaps.json, force).plan }
                .getOrElse {
                    if (seq == loadSeq) {
                        pending = null
                        error = errorMessage(it)
                    }
                    return@launch
                }
            if (seq != loadSeq) return@launch
            this@VideoSession.force = force
            val url = api.absolute(plan.url)
            val same = player.currentMediaItem?.localConfiguration?.uri?.toString() == url
            mode = plan.mode
            audio = plan.audio
            if (plan.mode == "direct") {
                offset = 0.0
                if (!same) player.setMediaItem(MediaItem.fromUri(url))
                // After a failure the same file needs preparing again.
                if (!same || player.playbackState == Player.STATE_IDLE) player.prepare()
                player.seekTo((at * 1000).toLong())
            } else {
                offset = plan.offset
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
            }
            pending = null
            player.playWhenReady = !paused
            applyAudio(player.currentTracks)
        }
    }

    fun retry() {
        failed = 0
        load(now())
    }

    /** A file plays its audio tracks itself; a stream carries only the one it was asked for. */
    private fun applyAudio(tracks: Tracks) {
        if (mode != "direct") return
        val wanted = audio ?: return
        val at = VideoRules.audioGroup(info.audio, wanted)
        val group = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.getOrNull(at) ?: return
        if (group.isSelected) return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }

    fun toggle() {
        if (ended) {
            ended = false
            seek(0.0)
            player.play()
        } else if (player.playWhenReady) player.pause() else player.play()
    }

    fun play() {
        if (ended || !player.playWhenReady) toggle()
    }

    fun pause() = player.pause()

    // A file seeks itself; a stream is asked for again a moment after the last jump,
    // so a row of presses on +10 does not start a row of ffmpegs.
    fun seek(t: Double) {
        val target = VideoRules.seekTarget(t, duration)
        if (target < duration - VideoRules.NEXT_LEAD) nextDismissed = false
        ended = false
        if (mode == "direct") {
            player.seekTo((target * 1000).toLong())
        } else {
            pending = target
            seekJob?.cancel()
            seekJob = scope.launch {
                delay(VideoRules.SEEK_DEBOUNCE_MS)
                load(target, paused = !player.playWhenReady)
            }
        }
        clock = target
        save()
    }

    /** One press or repeat of a seek key; a lost key-up still lands after the debounce. */
    fun nudge(by: Double) {
        held = VideoRules.nudge(held, now(), by, duration)
        heldJob?.cancel()
        heldJob = scope.launch {
            delay(VideoRules.SEEK_DEBOUNCE_MS)
            heldJob = null
            letGo()
        }
    }

    fun letGo() {
        heldJob?.cancel()
        heldJob = null
        val target = held ?: return
        held = null
        seek(target)
    }

    fun pickAudio(track: AudioInfo) {
        app.account.savePref("videoAudioLang", JsonPrimitive(track.lang)) { it.copy(videoAudioLang = track.lang) }
        audio = track.index
        load(now(), audioIndex = track.index)
    }

    fun pickSubtitle(key: String?, remember: Boolean = true) {
        sub = key
        cues = emptyList()
        val seq = ++subSeq
        val track = info.subtitles.firstOrNull { it.key == key }
        if (remember) {
            val lang = track?.lang?.ifEmpty { "und" } ?: ""
            app.account.savePref("videoSubLang", JsonPrimitive(lang)) { it.copy(videoSubLang = lang) }
        }
        if (track == null) return
        scope.launch {
            var told = false
            while (true) {
                val answer = runCatching { api.subtitleCues(info.id, track.key) }.getOrElse {
                    if (seq == subSeq) notice = errorMessage(it)
                    return@launch
                }
                if (seq != subSeq) return@launch
                if (!answer.pending) {
                    cues = answer.cues
                    if (told) notice = "Untertitel sind da."
                    return@launch
                }
                if (!told) {
                    notice = "Untertitel werden aus der Datei gelesen, das dauert einen Moment …"
                    told = true
                }
                delay(3_000)
            }
        }
    }

    fun defaultSubtitle(): String? = VideoRules.defaultSubtitle(info.subtitles, app.account.prefs.value.videoSubLang)

    fun sample(frame: Bitmap) {
        if (pixels.size != frame.width * frame.height) pixels = IntArray(frame.width * frame.height)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        letterbox.add(pixels, frame.width, frame.height)
        picture = letterbox.share
    }

    fun tick() {
        clock = now()
        buffered = if (pending != null) 0.0 else VideoRules.clock(offset, player.bufferedPosition)
        val t = System.currentTimeMillis()
        if (player.isPlaying && lastTick > 0) {
            val d = (t - lastTick) / 1000.0
            if (d > 0 && d < 2) watched += d
        }
        lastTick = t
        if (playId == null && !playPending && watched >= VideoRules.PLAY_AFTER) {
            playPending = true
            keepScope.launch {
                runCatching { api.startVideoPlay(info.id) }.onSuccess { playId = it.playId }
                playPending = false
            }
        }
        if (playId != null && watched - reported >= VideoRules.PLAY_REPORT_EVERY) reportWatched()
        if (player.isPlaying && abs(clock - lastSaved) >= VideoRules.SAVE_EVERY) save()
    }

    private fun reportWatched() {
        val id = playId ?: return
        reported = watched
        val seconds = watched
        keepScope.launch { runCatching { api.updateVideoPlay(id, seconds) } }
    }

    fun save(force: Boolean = false) {
        if (duration <= 0 || mode.isEmpty()) return
        val at = now()
        val completed = VideoRules.completed(at, duration, forceComplete)
        if (!VideoRules.saveDue(at, lastSaved, completed, force)) return
        lastSaved = at
        keepScope.launch { runCatching { api.setVideoProgress(info.id, at, completed) } }
    }

    fun release() {
        save(force = true)
        reportWatched()
        player.release()
    }
}
