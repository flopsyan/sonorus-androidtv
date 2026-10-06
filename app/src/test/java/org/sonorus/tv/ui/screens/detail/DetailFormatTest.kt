package org.sonorus.tv.ui.screens.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.sonorus.tv.data.model.MovieDetail
import org.sonorus.tv.data.model.MovieVideo
import org.sonorus.tv.data.model.ShowDetail
import org.sonorus.tv.data.model.VideoEpisode
import org.sonorus.tv.data.model.VideoGenre
import org.sonorus.tv.data.model.VideoPerson
import org.sonorus.tv.data.model.VideoProgress
import org.sonorus.tv.data.model.VideoSeason
import org.sonorus.tv.data.model.VideoTech
import org.sonorus.tv.data.model.VideoTitle

class DetailFormatTest {

    private val started = VideoProgress(position = 600.0, started = true, fraction = 0.1)
    private val completed = VideoProgress(completed = true)

    private fun episode(id: Int, season: Int, episode: Int, progress: VideoProgress = VideoProgress()) =
        VideoEpisode(id = id, season = season, episode = episode, progress = progress)

    @Test
    fun `a film's meta line has year, rating and length, the tech part resolution and HDR`() {
        val m = MovieDetail(year = 2010, certification = "DE:12", video = MovieVideo(duration = 148 * 60.0))
        assertEquals(listOf("2010", "FSK 12", "2 Std. 28 Min."), movieFacts(m))
        assertEquals(listOf("4K", "HDR"), techFacts(VideoTech(width = 3840, height = 2160, hdr = true)))
        assertEquals(listOf("1080p"), techFacts(VideoTech(width = 1920, height = 1080)))
        assertEquals(emptyList<String>(), techFacts(null))
    }

    @Test
    fun `the vote has one German decimal and an unrated title shows none`() {
        assertEquals("7,4", voteText(7.36))
        assertNull(voteText(0.0))
        assertNull(voteText(null))
    }

    @Test
    fun `a series that ran over years shows the span, specials are no season`() {
        assertEquals("2008-2013", showYears(2008, "2013-09-29"))
        assertEquals("2008", showYears(2008, "2008-12-01"))
        assertEquals("2008", showYears(2008, ""))
        val s = ShowDetail(
            year = 2008,
            endDate = "2013-09-29",
            certification = "DE:16",
            seasons = listOf(VideoSeason(season = 1), VideoSeason(season = 2), VideoSeason(season = 0)),
        )
        assertEquals(listOf("2008-2013", "FSK 16", "2 Staffeln"), showFacts(s))
        assertEquals(listOf("1 Staffel"), showFacts(ShowDetail(seasons = listOf(VideoSeason(season = 1)))))
    }

    @Test
    fun `fact lines name genres, crew by role, studios and six of the cast`() {
        val cast = (1..8).map { VideoPerson(name = "P$it", role = "cast") }
        val lines = factLines(
            genres = listOf(VideoGenre(name = "Drama"), VideoGenre(name = "Krimi")),
            crew = listOf(VideoPerson(name = "Vince", role = "creator"), VideoPerson(name = "Dave", role = "composer")),
            studios = listOf("AMC"),
            cast = cast,
        )
        assertEquals(
            listOf(
                "Genres" to "Drama, Krimi",
                "Idee" to "Vince",
                "Musik" to "Dave",
                "Studio" to "AMC",
                "Mit" to "P1, P2, P3, P4, P5, P6",
            ),
            lines,
        )
        assertEquals(emptyList<Pair<String, String>>(), factLines(emptyList(), emptyList(), emptyList(), emptyList()))
    }

    @Test
    fun `a started film resumes with the time left, a finished one plays again`() {
        assertEquals("Abspielen", moviePlayLabel(MovieVideo(duration = 6000.0)))
        assertEquals("Fortsetzen · noch 1 Std. 30 Min.", moviePlayLabel(MovieVideo(duration = 6000.0, progress = started)))
        assertEquals("Abspielen", moviePlayLabel(MovieVideo(duration = 6000.0, progress = completed)))
    }

    @Test
    fun `mark labels flip with the state and carry their subject`() {
        assertEquals("Als gesehen markieren", markLabel(null, done = false))
        assertEquals("Als ungesehen markieren", markLabel(null, done = true))
        assertEquals("Serie als gesehen markieren", markLabel("Serie", done = false))
        assertEquals("Staffel als ungesehen markieren", markLabel("Staffel", done = true))
    }

    @Test
    fun `the series button resumes, continues or starts over`() {
        val e1 = episode(1, 1, 1)
        val e3 = episode(3, 1, 3)
        val seasons = listOf(VideoSeason(season = 1, episodes = listOf(e1, e3)))

        val fresh = ShowDetail(seasons = seasons, episodes = 2, next = e1)
        assertEquals(PlayAction("Abspielen", 1, false), showPlay(fresh))

        val going = ShowDetail(seasons = seasons, episodes = 2, watched = 1, next = e3)
        assertEquals(PlayAction("S1 · E3 abspielen", 3, false), showPlay(going))

        val resumed = going.copy(next = e3.copy(progress = started))
        assertEquals(PlayAction("S1 · E3 fortsetzen", 3, false), showPlay(resumed))

        val allSeen = ShowDetail(seasons = seasons, episodes = 2, watched = 2, next = e1.copy(progress = completed))
        assertEquals(PlayAction("Von vorn", 1, true), showPlay(allSeen))
        assertEquals(PlayAction("Von vorn", 1, true), showPlay(allSeen.copy(next = null)))

        assertNull(showPlay(ShowDetail()))
    }

    @Test
    fun `the first season shown is the asked one, else the next episode's, else the first`() {
        val seasons = listOf(VideoSeason(season = 1), VideoSeason(season = 2), VideoSeason(season = 0))
        assertEquals(0, initialSeason(seasons, start = 0, next = 2))
        assertEquals(2, initialSeason(seasons, start = null, next = 2))
        assertEquals(2, initialSeason(seasons, start = 7, next = 2))
        assertEquals(1, initialSeason(seasons, start = null, next = null))
        assertNull(initialSeason(emptyList(), start = 1, next = 1))
    }

    @Test
    fun `season chips and heads count what was seen`() {
        assertEquals("Staffel 1", seasonChip("Staffel 1", watched = 0, total = 10))
        assertEquals("Staffel 1 · 3/10", seasonChip("Staffel 1", watched = 3, total = 10))
        val x = VideoSeason(season = 1, airDate = "2008-01-20", episodes = List(7) { episode(it, 1, it + 1) })
        assertEquals("7 Folgen · 2008", seasonFacts(x, watched = 0))
        assertEquals("7 Folgen · 2008 · 2 gesehen", seasonFacts(x, watched = 2))
        assertEquals("1 Folge", seasonFacts(VideoSeason(episodes = listOf(episode(1, 1, 1))), watched = 0))
    }

    @Test
    fun `episodes read as Folge or Special inside a season, with the full code in the menu`() {
        assertEquals("Folge 3", episodeLabel(episode(1, 1, 3)))
        assertEquals("Folge 3-4", episodeLabel(VideoEpisode(season = 1, episode = 3, episodeEnd = 4)))
        assertEquals("Special 2", episodeLabel(episode(1, 0, 2)))
        assertEquals("Special", episodeLabel(VideoEpisode(season = 0)))
        assertEquals("S2 · E5 · Ozymandias", episodeTitle(VideoEpisode(season = 2, episode = 5, name = "Ozymandias")))
        assertEquals("S2 · E5", episodeTitle(VideoEpisode(season = 2, episode = 5)))
    }

    @Test
    fun `poster lines and the collection count`() {
        assertEquals("2010 · 2 Std. 28 Min.", titleSub(VideoTitle(year = 2010, duration = 148 * 60.0)))
        assertEquals("noch 1 Std. 30 Min.", titleSub(VideoTitle(duration = 6000.0, progress = started)))
        assertEquals("3/10 gesehen", titleSub(VideoTitle(kind = "show", episodes = 10, watched = 3)))
        assertEquals("2008 · 5 Staffeln", titleSub(VideoTitle(kind = "show", year = 2008, seasons = 5, episodes = 62, watched = 62)))
        val movies = listOf(VideoTitle(progress = completed), VideoTitle(), VideoTitle(progress = completed))
        assertEquals("2 von 3 gesehen", seenOf(movies))
    }
}
