package org.sonorus.tv.ui.screens.browse

import org.sonorus.tv.data.model.ContinueItem
import org.sonorus.tv.data.model.VideoCollectionSummary
import org.sonorus.tv.data.model.VideoTitle
import org.sonorus.tv.ui.VideoFmt
import java.text.NumberFormat
import java.util.Locale

enum class VideoSort(val label: String) {
    TITLE("Titel"), YEAR("Jahr"), ADDED("Neu hinzugefügt"), VOTE("TMDB-Wertung"), WATCHED("Zuletzt gesehen"),
}

private val numbers = NumberFormat.getIntegerInstance(Locale.GERMAN)

/** "1 Staffel", "1.234 Titel". */
fun plural(count: Int, one: String, many: String): String =
    "${numbers.format(count)} ${if (count == 1) one else many}"

// The tip changes once a day, among the eight newest titles with a backdrop -
// unfinished ones first. The same rule as `pickFeatured` in video-views.js.
fun pickFeatured(list: List<VideoTitle>, now: Long = System.currentTimeMillis()): VideoTitle? {
    val withArt = list.filter { it.backdrop != null }
    if (withArt.isEmpty()) return null
    val fresh = withArt.filterNot { it.done }
    val pool = fresh.ifEmpty { withArt }
        .sortedByDescending { it.newest ?: it.addedAt }
        .take(8)
    val day = (now / 86_400_000L).toInt()
    return pool[day % pool.size]
}

fun newestMovies(movies: List<VideoTitle>): List<VideoTitle> = movies.sortedByDescending { it.addedAt }.take(16)

fun newestShows(shows: List<VideoTitle>): List<VideoTitle> = shows.sortedByDescending { it.newest ?: it.addedAt }.take(16)

fun sortTitles(list: List<VideoTitle>, sort: VideoSort): List<VideoTitle> {
    val byTitle = compareBy<VideoTitle> { it.title.lowercase() }
    return when (sort) {
        VideoSort.TITLE -> list.sortedWith(byTitle)
        VideoSort.YEAR -> list.sortedWith(compareByDescending<VideoTitle> { it.year ?: 0 }.then(byTitle))
        VideoSort.ADDED -> list.sortedWith(compareByDescending<VideoTitle> { it.newest ?: it.addedAt }.then(byTitle))
        VideoSort.VOTE -> list.sortedWith(compareByDescending<VideoTitle> { it.vote ?: 0.0 }.then(byTitle))
        VideoSort.WATCHED -> list.sortedWith(compareByDescending<VideoTitle> { it.watchedAt.orEmpty() }.then(byTitle))
    }
}

/** [genre] 0 means every genre. */
fun filterTitles(list: List<VideoTitle>, genre: Int, unwatched: Boolean): List<VideoTitle> =
    list.filter { (genre == 0 || genre in it.genreIds) && (!unwatched || !it.done) }

/** The line under a poster: what is left of a started film, how far a series is. */
fun titleSub(t: VideoTitle): String = when {
    t.isMovie && t.progress.started -> "noch ${VideoFmt.durationLong(t.duration - t.progress.position)}"
    t.isMovie -> listOfNotNull(t.year?.toString(), t.duration.takeIf { it > 0 }?.let(VideoFmt::durationLong)).joinToString(" · ")
    t.watched in 1 until t.episodes -> "${t.watched}/${t.episodes} gesehen"
    else -> listOfNotNull(t.year?.toString(), t.seasons.takeIf { it > 0 }?.let { plural(it, "Staffel", "Staffeln") })
        .joinToString(" · ")
}

/** Year · FSK · running time for a film, number of seasons for a series. */
fun heroMeta(t: VideoTitle): String = listOfNotNull(
    t.year?.toString(),
    VideoFmt.certLabel(t.certification).ifEmpty { null },
    if (t.isMovie) t.duration.takeIf { it > 0 }?.let(VideoFmt::durationLong)
    else t.seasons.takeIf { it > 0 }?.let { plural(it, "Staffel", "Staffeln") },
).joinToString(" · ")

fun continuePicture(item: ContinueItem): String? {
    val t = item.title
    return if (item.kind == "show") item.video.still ?: t.thumb ?: t.backdrop else t.thumb ?: t.backdrop
}

fun continueSub(item: ContinueItem): String {
    val v = item.video
    return if (item.kind == "show") {
        VideoFmt.episodeCode(v.season, v.episode, v.episodeEnd) + if (v.name.isNotEmpty()) " · ${v.name}" else ""
    } else {
        "noch ${VideoFmt.durationLong(v.duration - v.progress.position)}"
    }
}

fun collectionSub(c: VideoCollectionSummary): String =
    plural(c.movies, "Film", "Filme") + if (c.watched > 0) " · ${c.watched} gesehen" else ""
