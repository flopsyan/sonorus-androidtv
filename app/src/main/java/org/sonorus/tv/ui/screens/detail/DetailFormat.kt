package org.sonorus.tv.ui.screens.detail

import org.sonorus.tv.data.model.MovieDetail
import org.sonorus.tv.data.model.MovieVideo
import org.sonorus.tv.data.model.ShowDetail
import org.sonorus.tv.data.model.VideoEpisode
import org.sonorus.tv.data.model.VideoGenre
import org.sonorus.tv.data.model.VideoPerson
import org.sonorus.tv.data.model.VideoSeason
import org.sonorus.tv.data.model.VideoTech
import org.sonorus.tv.data.model.VideoTitle
import org.sonorus.tv.ui.VideoFmt
import java.util.Locale

internal fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

internal fun voteText(vote: Double?): String? =
    vote?.takeIf { it > 0 }?.let { String.format(Locale.GERMAN, "%.1f", it) }

internal fun movieFacts(m: MovieDetail): List<String> = listOfNotNull(
    m.year?.toString(),
    VideoFmt.certLabel(m.certification).ifEmpty { null },
    m.video?.duration?.takeIf { it > 0 }?.let(VideoFmt::durationLong),
)

internal fun techFacts(tech: VideoTech?): List<String> = if (tech == null) emptyList() else listOfNotNull(
    VideoFmt.resolutionLabel(tech.height, tech.width).ifEmpty { null },
    "HDR".takeIf { tech.hdr },
)

/** "2008-2013" while a series ran over several years, else its first year. */
internal fun showYears(year: Int?, endDate: String): String? {
    val end = endDate.take(4)
    return if (year != null && end.length == 4 && end != year.toString()) "$year-$end" else year?.toString()
}

internal fun showFacts(s: ShowDetail): List<String> {
    val seasons = s.seasons.count { it.season != 0 }
    return listOfNotNull(
        showYears(s.year, s.endDate),
        VideoFmt.certLabel(s.certification).ifEmpty { null },
        plural(seasons, "Staffel", "Staffeln").takeIf { seasons > 0 },
    )
}

internal fun showSeen(s: ShowDetail): List<String> =
    listOfNotNull("${s.watched}/${s.episodes} gesehen".takeIf { s.watched > 0 })

private val CREW_WORDS = mapOf("director" to "Regie", "writer" to "Drehbuch", "creator" to "Idee", "composer" to "Musik")

/** Label and value of each line under the buttons: genres, crew by role, studios, the first six of the cast. */
internal fun factLines(
    genres: List<VideoGenre>,
    crew: List<VideoPerson>,
    studios: List<String>,
    cast: List<VideoPerson>,
): List<Pair<String, String>> = buildList {
    if (genres.isNotEmpty()) add((if (genres.size > 1) "Genres" else "Genre") to genres.joinToString(", ") { it.name })
    for ((role, people) in crew.groupBy { it.role }) {
        add((CREW_WORDS[role] ?: role) to people.take(4).joinToString(", ") { it.name })
    }
    if (studios.isNotEmpty()) add((if (studios.size > 1) "Studios" else "Studio") to studios.joinToString(", "))
    if (cast.isNotEmpty()) add("Mit" to cast.take(6).joinToString(", ") { it.name })
}

internal fun moviePlayLabel(v: MovieVideo): String =
    if (v.progress.started && !v.progress.completed) {
        "Fortsetzen · noch ${VideoFmt.durationLong(v.duration - v.progress.position)}"
    } else {
        "Abspielen"
    }

/** "Als gesehen markieren", or with a [subject] "Staffel als ungesehen markieren". */
internal fun markLabel(subject: String?, done: Boolean): String {
    val rest = if (done) "als ungesehen markieren" else "als gesehen markieren"
    return if (subject == null) rest.replaceFirstChar { it.uppercase() } else "$subject $rest"
}

internal data class PlayAction(val label: String, val videoId: Int, val fromStart: Boolean)

/** The series' main button; the server's `next` is the first episode again once all are seen. */
internal fun showPlay(s: ShowDetail): PlayAction? {
    val next = s.next ?: s.seasons.firstOrNull()?.episodes?.firstOrNull() ?: return null
    val code = VideoFmt.episodeCode(next.season, next.episode, next.episodeEnd)
    val allDone = s.episodes > 0 && s.watched >= s.episodes
    return when {
        next.progress.started -> PlayAction("$code fortsetzen", next.id, false)
        allDone || s.next == null -> PlayAction("Von vorn", next.id, true)
        s.watched > 0 -> PlayAction("$code abspielen", next.id, false)
        else -> PlayAction("Abspielen", next.id, false)
    }
}

/** The season asked for, else the one with the next episode, else the first. */
internal fun initialSeason(seasons: List<VideoSeason>, start: Int?, next: Int?): Int? =
    (seasons.firstOrNull { it.season == start } ?: seasons.firstOrNull { it.season == next } ?: seasons.firstOrNull())?.season

internal fun seasonChip(name: String, watched: Int, total: Int): String =
    if (watched > 0) "$name · $watched/$total" else name

internal fun seasonFacts(x: VideoSeason, watched: Int): String = listOfNotNull(
    plural(x.episodes.size, "Folge", "Folgen"),
    x.airDate.take(4).ifEmpty { null },
    "$watched gesehen".takeIf { watched > 0 },
).joinToString(" · ")

/** The code inside a season's list, where the season is already on screen. */
internal fun episodeLabel(e: VideoEpisode): String =
    if (e.season == 0) {
        e.episode?.let { "Special $it" } ?: "Special"
    } else {
        "Folge ${e.episode ?: "?"}${e.episodeEnd?.let { "-$it" } ?: ""}"
    }

internal fun episodeTitle(e: VideoEpisode): String =
    listOf(VideoFmt.episodeCode(e.season, e.episode, e.episodeEnd), e.name).filter { it.isNotEmpty() }.joinToString(" · ")

/** The line under a poster: what is left of a started film, else year and length or seasons. */
internal fun titleSub(t: VideoTitle): String = when {
    t.isMovie && t.progress.started -> "noch ${VideoFmt.durationLong(t.duration - t.progress.position)}"
    t.isMovie -> listOfNotNull(t.year?.toString(), t.duration.takeIf { it > 0 }?.let(VideoFmt::durationLong)).joinToString(" · ")
    t.watched in 1 until t.episodes -> "${t.watched}/${t.episodes} gesehen"
    else -> listOfNotNull(t.year?.toString(), t.seasons.takeIf { it > 0 }?.let { plural(it, "Staffel", "Staffeln") })
        .joinToString(" · ")
}

internal fun seenOf(movies: List<VideoTitle>): String =
    "${movies.count { it.progress.completed }} von ${movies.size} gesehen"
