package org.sonorus.tv.ui.screens

import org.sonorus.tv.ui.components.SonorusButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.model.ShowDetail
import org.sonorus.tv.data.model.VideoEpisode
import org.sonorus.tv.ui.Load
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.ReloadOnReturn
import org.sonorus.tv.ui.rememberLoad
import org.sonorus.tv.ui.screens.detail.DetailBackdrop
import org.sonorus.tv.ui.screens.detail.DetailHead
import org.sonorus.tv.ui.screens.detail.EpisodeMenu
import org.sonorus.tv.ui.screens.detail.EpisodeRow
import org.sonorus.tv.ui.screens.detail.ErrorLine
import org.sonorus.tv.ui.screens.detail.FactLines
import org.sonorus.tv.ui.screens.detail.SeasonChips
import org.sonorus.tv.ui.screens.detail.SeasonHead
import org.sonorus.tv.ui.screens.detail.StartFocus
import org.sonorus.tv.ui.screens.detail.TitleShelf
import org.sonorus.tv.ui.screens.detail.change
import org.sonorus.tv.ui.screens.detail.factLines
import org.sonorus.tv.ui.screens.detail.initialSeason
import org.sonorus.tv.ui.screens.detail.markIcon
import org.sonorus.tv.ui.screens.detail.markLabel
import org.sonorus.tv.ui.screens.detail.rememberReturnFocus
import org.sonorus.tv.ui.screens.detail.showFacts
import org.sonorus.tv.ui.screens.detail.showPlay
import org.sonorus.tv.ui.screens.detail.showSeen

@Composable
fun ShowScreen(id: Int, startSeason: Int?, nav: Nav) {
    val load = rememberLoad("show", id) { SonorusTvApp.instance.api.show(id).show }
    ReloadOnReturn(load)
    LoadBox(load, Modifier.fillMaxSize()) { s -> ShowPage(s, load, startSeason, nav) }
}

@Composable
private fun ShowPage(s: ShowDetail, load: Load<ShowDetail>, startSeason: Int?, nav: Nav) {
    val api = SonorusTvApp.instance.api
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val back = rememberReturnFocus()
    val primary = remember { FocusRequester() }
    val failure = remember { mutableStateOf<String?>(null) }
    // A mark shows at once and holds until the reloaded page carries it.
    val marks = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(s) { marks.clear() }
    var picked by rememberSaveable { mutableStateOf(initialSeason(s.seasons, startSeason, s.next?.season)) }
    var menu by remember { mutableStateOf<VideoEpisode?>(null) }

    val season = s.seasons.firstOrNull { it.season == picked } ?: s.seasons.firstOrNull()
    val play = showPlay(s)
    val allDone = s.episodes > 0 && s.watched >= s.episodes
    val facts = factLines(s.genres, s.crew, s.studios, s.cast)
    fun done(e: VideoEpisode) = marks[e.id] ?: e.progress.completed
    fun mark(episodes: List<VideoEpisode>, watched: Boolean, call: suspend () -> Unit) {
        episodes.forEach { marks[it.id] = watched }
        scope.change(load, failure, undo = { episodes.forEach { marks.remove(it.id) } }, call = call)
    }

    Box(Modifier.fillMaxSize()) {
        DetailBackdrop(s.backdrop) { list.canScrollBackward }
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "head") {
                DetailHead(s.title, s.logo, showFacts(s), s.vote, showSeen(s), s.tagline, s.overview) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (play != null) {
                            SonorusButton(
                                play.label,
                                if (play.fromStart) Icons.Filled.Replay else Icons.Filled.PlayArrow,
                                { back.leave(null); nav.watch(play.videoId, play.fromStart) },
                                Modifier.focusRequester(primary),
                            )
                        }
                        SonorusButton(
                            markLabel("Serie", allDone),
                            markIcon(allDone),
                            { scope.change(load, failure) { api.setTitleWatched(s.id, !allDone) } },
                            if (play == null) Modifier.focusRequester(primary) else Modifier,
                        )
                    }
                    failure.value?.let { ErrorLine(it) }
                }
            }
            if (facts.isNotEmpty()) item(key = "facts") { FactLines(facts) }
            if (s.seasons.size > 1) {
                item(key = "seasons") {
                    SeasonChips(s.seasons, season?.season, { x -> x.episodes.count(::done) }, { picked = it })
                }
            }
            if (season != null) {
                item(key = "season") {
                    SeasonHead(season, season.episodes.count(::done)) { watched ->
                        mark(season.episodes, watched) { api.setTitleWatched(s.id, watched, season.season) }
                    }
                }
                items(season.episodes, key = { it.id }) { e ->
                    val key = "e${e.id}"
                    EpisodeRow(
                        e,
                        done(e),
                        onClick = { back.leave(key); nav.watch(e.id) },
                        onLongClick = { menu = e },
                        modifier = back.target(key),
                    )
                }
            }
            if (s.similar.isNotEmpty()) item(key = "similar") { TitleShelf("Ähnliche Titel", s.similar, nav, back, "s") }
        }
    }

    menu?.let { e ->
        val done = done(e)
        EpisodeMenu(
            e,
            done,
            onMark = {
                menu = null
                mark(listOf(e), !done) { api.setVideoWatched(e.id, !done) }
            },
            onRestart = {
                menu = null
                back.leave("e${e.id}")
                nav.watch(e.id, fromStart = true)
            },
            onDismiss = { menu = null },
        )
    }
    StartFocus(back, primary) { list.scrollToItem(0) }
}
