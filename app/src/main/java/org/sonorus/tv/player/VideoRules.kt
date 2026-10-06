package org.sonorus.tv.player

import org.sonorus.tv.data.model.AudioInfo
import org.sonorus.tv.data.model.Cue
import org.sonorus.tv.data.model.SubtitleInfo
import kotlin.math.abs

/** The player's rules; the numbers are `public/js/video-player.js`'s, so every client counts the same. */
object VideoRules {
    const val NEXT_LEAD = 30.0
    const val SAVE_EVERY = 10.0
    const val PLAY_AFTER = 10.0
    const val PLAY_REPORT_EVERY = 30.0
    const val COMPLETE_AT = 0.9
    const val SKIP = 10.0
    const val SEEK_DEBOUNCE_MS = 350L

    private const val MAX_FAILURES = 3

    /** What to force after a failure in [mode]: what the TV refused as it is gets one more try the expensive way. */
    fun fallback(mode: String, failures: Int): String? = when {
        failures >= MAX_FAILURES -> null
        mode == "direct" -> "remux"
        mode == "remux" -> "encode"
        else -> null
    }

    /** A piped stream starts at [offset], so its own position counts from there. */
    fun clock(offset: Double, positionMs: Long): Double = offset + positionMs / 1000.0

    fun seekTarget(t: Double, duration: Double): Double = t.coerceIn(0.0, (duration - 0.5).coerceAtLeast(0.0))

    /** A held seek key moves on from its last target, not from the playhead, which has not moved yet. */
    fun nudge(held: Double?, now: Double, by: Double, duration: Double): Double = seekTarget((held ?: now) + by, duration)

    fun completed(at: Double, duration: Double, forced: Boolean): Boolean = forced || at >= duration * COMPLETE_AT

    fun saveDue(at: Double, lastSaved: Double, completed: Boolean, force: Boolean): Boolean =
        force || completed || abs(at - lastSaved) >= 2

    /** A file lists its audio as groups in ffprobe's order, so the server's index becomes a place in that order. */
    fun audioGroup(audio: List<AudioInfo>, index: Int): Int = audio.sortedBy { it.index }.indexOfFirst { it.index == index }

    /** The account's subtitle language, a plain track before a forced or SDH one; null means off. */
    fun defaultSubtitle(subs: List<SubtitleInfo>, lang: String): String? {
        if (lang.isEmpty()) return null
        val usable = subs.filter { it.supported && it.lang.ifEmpty { "und" } == lang }
        return (usable.firstOrNull { !it.forced && !it.sdh } ?: usable.firstOrNull())?.key
    }

    fun cueText(cues: List<Cue>, at: Double): List<String> = cues.filter { it.s <= at && at <= it.e }.map { it.t }

    fun showNext(duration: Double, at: Double, seeking: Boolean, dismissed: Boolean, ended: Boolean): Boolean {
        val left = duration - at
        return duration >= 120 && !dismissed && ((left <= NEXT_LEAD && left > 0.3 && !seeking) || ended)
    }

    fun nextFill(duration: Double, at: Double, autoplay: Boolean, ended: Boolean): Float =
        if (autoplay && !ended) (1 - (duration - at) / NEXT_LEAD).toFloat().coerceIn(0f, 1f) else 0f
}
