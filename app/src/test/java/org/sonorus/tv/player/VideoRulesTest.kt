package org.sonorus.tv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sonorus.tv.data.model.AudioInfo
import org.sonorus.tv.data.model.Cue
import org.sonorus.tv.data.model.SubtitleInfo

class VideoRulesTest {

    @Test
    fun `a refused file is remuxed, then encoded, then given up`() {
        assertEquals("remux", VideoRules.fallback("direct", 1))
        assertEquals("encode", VideoRules.fallback("remux", 2))
        assertNull(VideoRules.fallback("encode", 1))
    }

    @Test
    fun `three failures end the chain wherever it stands`() {
        assertNull(VideoRules.fallback("direct", 3))
        assertNull(VideoRules.fallback("remux", 3))
    }

    @Test
    fun `nothing is forced before a plan has said what plays`() {
        assertNull(VideoRules.fallback("", 1))
    }

    @Test
    fun `a piped stream's clock starts at its offset`() {
        assertEquals(612.5, VideoRules.clock(600.0, 12_500), 1e-9)
        assertEquals(12.5, VideoRules.clock(0.0, 12_500), 1e-9)
    }

    @Test
    fun `a jump stays inside the film`() {
        assertEquals(0.0, VideoRules.seekTarget(-8.0, 3600.0), 0.0)
        assertEquals(3599.5, VideoRules.seekTarget(4000.0, 3600.0), 0.0)
        assertEquals(1200.0, VideoRules.seekTarget(1200.0, 3600.0), 0.0)
    }

    @Test
    fun `a video too short to clamp lands at zero`() {
        assertEquals(0.0, VideoRules.seekTarget(10.0, 0.0), 0.0)
    }

    @Test
    fun `a held key adds up from its last target, not from the playhead`() {
        var held: Double? = null
        repeat(3) { held = VideoRules.nudge(held, now = 100.0, by = VideoRules.SKIP, duration = 3600.0) }
        assertEquals(130.0, held!!, 0.0)
    }

    @Test
    fun `a held key stops at the ends`() {
        assertEquals(0.0, VideoRules.nudge(5.0, now = 100.0, by = -VideoRules.SKIP, duration = 3600.0), 0.0)
        assertEquals(3599.5, VideoRules.nudge(3595.0, now = 100.0, by = VideoRules.SKIP, duration = 3600.0), 0.0)
    }

    @Test
    fun `ninety percent counts as seen`() {
        assertFalse(VideoRules.completed(3239.0, 3600.0, forced = false))
        assertTrue(VideoRules.completed(3240.0, 3600.0, forced = false))
    }

    @Test
    fun `going on to the next episode counts as seen wherever it stood`() {
        assertTrue(VideoRules.completed(60.0, 3600.0, forced = true))
    }

    @Test
    fun `a save is skipped when the place barely moved`() {
        assertFalse(VideoRules.saveDue(101.0, 100.0, completed = false, force = false))
        assertTrue(VideoRules.saveDue(102.0, 100.0, completed = false, force = false))
        assertTrue(VideoRules.saveDue(98.0, 100.0, completed = false, force = false))
    }

    @Test
    fun `the last save and a finished video are always written`() {
        assertTrue(VideoRules.saveDue(100.0, 100.0, completed = false, force = true))
        assertTrue(VideoRules.saveDue(100.0, 100.0, completed = true, force = false))
    }

    @Test
    fun `the audio group is the track's place in ffprobe's order`() {
        val audio = listOf(AudioInfo(index = 3, lang = "eng"), AudioInfo(index = 1, lang = "ger"), AudioInfo(index = 2, lang = "hin"))
        assertEquals(0, VideoRules.audioGroup(audio, 1))
        assertEquals(1, VideoRules.audioGroup(audio, 2))
        assertEquals(2, VideoRules.audioGroup(audio, 3))
        assertEquals(-1, VideoRules.audioGroup(audio, 7))
    }

    private val subs = listOf(
        SubtitleInfo(key = "s3", lang = "ger", forced = true, supported = true),
        SubtitleInfo(key = "s4", lang = "ger", sdh = true, supported = true),
        SubtitleInfo(key = "s5", lang = "ger", supported = true),
        SubtitleInfo(key = "s6", lang = "eng", supported = false),
        SubtitleInfo(key = "x0", lang = "", supported = true),
    )

    @Test
    fun `no subtitle language means subtitles off`() {
        assertNull(VideoRules.defaultSubtitle(subs, ""))
    }

    @Test
    fun `a plain track wins over forced and SDH ones`() {
        assertEquals("s5", VideoRules.defaultSubtitle(subs, "ger"))
    }

    @Test
    fun `a forced or SDH track is taken when it is all there is`() {
        assertEquals("s3", VideoRules.defaultSubtitle(subs.filter { it.key != "s5" }, "ger"))
    }

    @Test
    fun `an image subtitle is never the default`() {
        assertNull(VideoRules.defaultSubtitle(subs, "eng"))
    }

    @Test
    fun `a track without a language answers to und`() {
        assertEquals("x0", VideoRules.defaultSubtitle(subs, "und"))
    }

    @Test
    fun `cues on screen are those around the moment, both ends included`() {
        val cues = listOf(Cue(1.0, 3.0, "a"), Cue(2.5, 4.0, "b"), Cue(5.0, 6.0, "c"))
        assertEquals(listOf("a", "b"), VideoRules.cueText(cues, 2.5))
        assertEquals(listOf("b"), VideoRules.cueText(cues, 4.0))
        assertEquals(emptyList<String>(), VideoRules.cueText(cues, 4.5))
    }

    @Test
    fun `the next card shows in the last thirty seconds`() {
        assertFalse(VideoRules.showNext(1800.0, 1769.0, seeking = false, dismissed = false, ended = false))
        assertTrue(VideoRules.showNext(1800.0, 1770.0, seeking = false, dismissed = false, ended = false))
        assertFalse(VideoRules.showNext(1800.0, 1799.8, seeking = false, dismissed = false, ended = false))
    }

    @Test
    fun `the next card waits for a jump and stays away once dismissed`() {
        assertFalse(VideoRules.showNext(1800.0, 1780.0, seeking = true, dismissed = false, ended = false))
        assertFalse(VideoRules.showNext(1800.0, 1780.0, seeking = false, dismissed = true, ended = false))
    }

    @Test
    fun `the next card shows at the end, but never after a clip under two minutes`() {
        assertTrue(VideoRules.showNext(1800.0, 1800.0, seeking = false, dismissed = false, ended = true))
        assertFalse(VideoRules.showNext(90.0, 80.0, seeking = false, dismissed = false, ended = true))
    }

    @Test
    fun `the countdown fills over the last thirty seconds, only with autoplay`() {
        assertEquals(0.5f, VideoRules.nextFill(1800.0, 1785.0, autoplay = true, ended = false), 1e-6f)
        assertEquals(0f, VideoRules.nextFill(1800.0, 1785.0, autoplay = false, ended = false), 0f)
        assertEquals(0f, VideoRules.nextFill(1800.0, 1800.0, autoplay = true, ended = true), 0f)
    }
}
