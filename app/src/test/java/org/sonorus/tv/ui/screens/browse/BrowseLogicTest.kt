package org.sonorus.tv.ui.screens.browse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.sonorus.tv.data.model.ContinueItem
import org.sonorus.tv.data.model.VideoCollectionSummary
import org.sonorus.tv.data.model.VideoEpisode
import org.sonorus.tv.data.model.VideoProgress
import org.sonorus.tv.data.model.VideoTitle

class BrowseLogicTest {

    private val day = 86_400_000L

    private fun movie(id: Int, added: String = "2026-01-01", backdrop: String? = "/b/$id", done: Boolean = false) =
        VideoTitle(id = id, title = "M$id", addedAt = added, backdrop = backdrop, progress = VideoProgress(completed = done))

    @Test
    fun `the tip skips titles without a backdrop and is null when none has one`() {
        assertNull(pickFeatured(listOf(movie(1, backdrop = null))))
        assertEquals(2, pickFeatured(listOf(movie(1, backdrop = null), movie(2)), now = 0)?.id)
    }

    @Test
    fun `the tip prefers unfinished titles and falls back to finished ones`() {
        val list = listOf(movie(1, done = true), movie(2))
        repeat(3) { assertEquals(2, pickFeatured(list, now = it * day)?.id) }
        assertEquals(1, pickFeatured(listOf(movie(1, done = true)), now = 5 * day)?.id)
    }

    @Test
    fun `the tip turns once a day through the eight newest`() {
        val list = (1..10).map { movie(it, added = "2026-01-%02d".format(it)) }
        // Newest first: 10, 9, ... 3; titles 1 and 2 never come up.
        assertEquals(10, pickFeatured(list, now = 0)?.id)
        assertEquals(9, pickFeatured(list, now = day + 5)?.id)
        assertEquals(3, pickFeatured(list, now = 7 * day)?.id)
        assertEquals(10, pickFeatured(list, now = 8 * day)?.id)
    }

    @Test
    fun `a series counts by its newest episode`() {
        val show = VideoTitle(id = 7, kind = "show", backdrop = "/b", addedAt = "2020-01-01", newest = "2026-05-01")
        assertEquals(7, pickFeatured(listOf(movie(1, added = "2026-02-01"), show), now = 0)?.id)
        assertEquals(listOf(7, 8), newestShows(listOf(show.copy(id = 8, newest = null), show)).map { it.id })
    }

    @Test
    fun `sorting falls back to the title, ignoring case`() {
        val list = listOf(
            VideoTitle(id = 1, title = "beta", year = 2001, vote = 7.0, addedAt = "2026-01-02"),
            VideoTitle(id = 2, title = "Alpha", year = 2001, vote = 8.0, addedAt = "2026-01-01", watchedAt = "2026-03-01"),
            VideoTitle(id = 3, title = "Gamma", year = 2010, addedAt = "2026-01-03"),
        )
        assertEquals(listOf(2, 1, 3), sortTitles(list, VideoSort.TITLE).map { it.id })
        assertEquals(listOf(3, 2, 1), sortTitles(list, VideoSort.YEAR).map { it.id })
        assertEquals(listOf(3, 1, 2), sortTitles(list, VideoSort.ADDED).map { it.id })
        assertEquals(listOf(2, 1, 3), sortTitles(list, VideoSort.VOTE).map { it.id })
        assertEquals(listOf(2, 1, 3), sortTitles(list, VideoSort.WATCHED).map { it.id })
    }

    @Test
    fun `the filter keeps a genre and drops what is seen`() {
        val list = listOf(
            VideoTitle(id = 1, genreIds = listOf(28)),
            VideoTitle(id = 2, genreIds = listOf(28, 35), progress = VideoProgress(completed = true)),
            VideoTitle(id = 3, kind = "show", genreIds = listOf(35), episodes = 4, watched = 4),
            VideoTitle(id = 4, kind = "show", genreIds = listOf(35), episodes = 4, watched = 2),
        )
        assertEquals(listOf(1, 2, 3, 4), filterTitles(list, 0, false).map { it.id })
        assertEquals(listOf(1, 2), filterTitles(list, 28, false).map { it.id })
        assertEquals(listOf(1, 4), filterTitles(list, 0, true).map { it.id })
        assertEquals(listOf(4), filterTitles(list, 35, true).map { it.id })
    }

    @Test
    fun `a poster says what is left, how far a series is, or year and length`() {
        val started = VideoTitle(duration = 5400.0, progress = VideoProgress(position = 1800.0, started = true))
        assertEquals("noch 1 Std.", titleSub(started))
        assertEquals("2019 · 1 Std. 38 Min.", titleSub(VideoTitle(year = 2019, duration = 5880.0)))
        assertEquals("2019", titleSub(VideoTitle(year = 2019)))
        assertEquals("3/10 gesehen", titleSub(VideoTitle(kind = "show", episodes = 10, watched = 3, seasons = 2)))
        assertEquals("2008 · 5 Staffeln", titleSub(VideoTitle(kind = "show", year = 2008, seasons = 5, episodes = 62, watched = 62)))
        assertEquals("1 Staffel", titleSub(VideoTitle(kind = "show", seasons = 1)))
    }

    @Test
    fun `the hero line names year, rating and length or seasons`() {
        assertEquals("2019 · FSK 12 · 2 Std. 1 Min.", heroMeta(VideoTitle(year = 2019, certification = "DE:12", duration = 7260.0)))
        assertEquals("2008 · 5 Staffeln", heroMeta(VideoTitle(kind = "show", year = 2008, seasons = 5)))
        assertEquals("", heroMeta(VideoTitle()))
    }

    @Test
    fun `carry on shows the episode for a series and the rest for a film`() {
        val title = VideoTitle(id = 1, thumb = "/t", backdrop = "/b")
        val episode = ContinueItem(
            kind = "show",
            title = title,
            video = VideoEpisode(season = 2, episode = 5, name = "Ozymandias", still = "/s"),
        )
        assertEquals("S2 · E5 · Ozymandias", continueSub(episode))
        assertEquals("/s", continuePicture(episode))
        assertEquals("/t", continuePicture(episode.copy(video = VideoEpisode(season = 1, episode = 1))))

        val film = ContinueItem(
            kind = "movie",
            title = title.copy(thumb = null),
            video = VideoEpisode(duration = 7200.0, progress = VideoProgress(position = 3480.0)),
        )
        assertEquals("noch 1 Std. 2 Min.", continueSub(film))
        assertEquals("/b", continuePicture(film))
    }

    @Test
    fun `counts read the German way`() {
        assertEquals("1.234 Titel", plural(1234, "Titel", "Titel"))
        assertEquals("1 Film", collectionSub(VideoCollectionSummary(movies = 1)))
        assertEquals("3 Filme · 2 gesehen", collectionSub(VideoCollectionSummary(movies = 3, watched = 2)))
    }
}
