package org.sonorus.tv.ui.screens

import org.sonorus.tv.ui.components.SonorusButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.model.MovieDetail
import org.sonorus.tv.ui.Load
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.ReloadOnReturn
import org.sonorus.tv.ui.components.ProgressLine
import org.sonorus.tv.ui.rememberLoad
import org.sonorus.tv.ui.screens.detail.DetailBackdrop
import org.sonorus.tv.ui.screens.detail.DetailHead
import org.sonorus.tv.ui.screens.detail.ErrorLine
import org.sonorus.tv.ui.screens.detail.FactLines
import org.sonorus.tv.ui.screens.detail.StartFocus
import org.sonorus.tv.ui.screens.detail.TextWidth
import org.sonorus.tv.ui.screens.detail.TitleShelf
import org.sonorus.tv.ui.screens.detail.change
import org.sonorus.tv.ui.screens.detail.factLines
import org.sonorus.tv.ui.screens.detail.markIcon
import org.sonorus.tv.ui.screens.detail.markLabel
import org.sonorus.tv.ui.screens.detail.moviePlayLabel
import org.sonorus.tv.ui.screens.detail.movieFacts
import org.sonorus.tv.ui.screens.detail.rememberReturnFocus
import org.sonorus.tv.ui.screens.detail.techFacts
import org.sonorus.tv.ui.theme.SonorusTheme

@Composable
fun MovieScreen(id: Int, nav: Nav) {
    val load = rememberLoad("movie", id) { SonorusTvApp.instance.api.movie(id).movie }
    ReloadOnReturn(load)
    LoadBox(load, Modifier.fillMaxSize()) { m -> MoviePage(m, load, nav) }
}

@Composable
private fun MoviePage(m: MovieDetail, load: Load<MovieDetail>, nav: Nav) {
    val api = SonorusTvApp.instance.api
    val v = m.video
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val back = rememberReturnFocus()
    val primary = remember { FocusRequester() }
    val failure = remember { mutableStateOf<String?>(null) }
    val series = m.collection?.movies.orEmpty().filter { it.id != m.id }
    val facts = factLines(m.genres, m.crew, m.studios, m.cast)

    Box(Modifier.fillMaxSize()) {
        DetailBackdrop(m.backdrop) { list.canScrollBackward }
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "head") {
                DetailHead(m.title, m.logo, movieFacts(m), m.vote, techFacts(v?.tech), m.tagline, m.overview) {
                    if (v == null) {
                        Text("Keine Datei auf dem Server.", style = MaterialTheme.typography.bodyMedium, color = SonorusTheme.colors.textDim)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SonorusButton(
                                moviePlayLabel(v),
                                Icons.Filled.PlayArrow,
                                { back.leave(null); nav.watch(v.id) },
                                Modifier.focusRequester(primary),
                            )
                            if (v.progress.started) {
                                SonorusButton("Von vorn", Icons.Filled.Replay, { back.leave(null); nav.watch(v.id, fromStart = true) })
                            }
                            val done = v.progress.completed
                            SonorusButton(markLabel(null, done), markIcon(done), {
                                scope.change(load, failure) { api.setTitleWatched(m.id, !done) }
                            })
                        }
                        if (v.progress.started) ProgressLine(v.progress.fraction, Modifier.padding(top = 16.dp).width(TextWidth))
                    }
                    failure.value?.let { ErrorLine(it) }
                }
            }
            if (facts.isNotEmpty()) item(key = "facts") { FactLines(facts) }
            if (series.isNotEmpty()) {
                item(key = "collection") {
                    TitleShelf("Filmreihe: ${m.collection?.name.orEmpty()}", series, nav, back, "c", primary.takeIf { v == null })
                }
            }
            if (m.similar.isNotEmpty()) {
                item(key = "similar") {
                    TitleShelf("Ähnliche Titel", m.similar, nav, back, "s", primary.takeIf { v == null && series.isEmpty() })
                }
            }
        }
    }
    StartFocus(back, primary) { list.scrollToItem(0) }
}
