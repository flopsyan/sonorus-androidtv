package org.sonorus.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.sonorus.tv.data.model.SubtitleInfo

class VideoFormatTest {

    @Test
    fun `a subtitle track says under its language what kind it is`() {
        assertEquals("Englisch" to "Untertitel", VideoFmt.subtitleLabel(SubtitleInfo(lang = "eng", supported = true)))
        assertEquals("Englisch" to "Closed Captions", VideoFmt.subtitleLabel(SubtitleInfo(lang = "eng", sdh = true, supported = true)))
    }

    @Test
    fun `a file next to the video says so, a bitmap track keeps its note`() {
        assertEquals(
            "Englisch" to "Closed Captions · Datei",
            VideoFmt.subtitleLabel(SubtitleInfo(lang = "en", sdh = true, supported = true, external = true)),
        )
        assertEquals(
            "Deutsch · erzwungen" to "Bild-Untertitel, nicht unterstützt",
            VideoFmt.subtitleLabel(SubtitleInfo(lang = "ger", forced = true, supported = false)),
        )
    }

    @Test
    fun `episode codes, certifications and the clock read like the web`() {
        assertEquals("S2 · E5-6", VideoFmt.episodeCode(2, 5, 6))
        assertEquals("Special 3", VideoFmt.episodeCode(0, 3, null))
        assertEquals("FSK 16", VideoFmt.certLabel("DE:16"))
        assertEquals("1 Std. 38 Min.", VideoFmt.durationLong(98 * 60.0))
        assertEquals("1:02:03", VideoFmt.clock(3723.0))
    }
}
