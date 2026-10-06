package org.sonorus.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.model.VideoCollection
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.ReloadOnReturn
import org.sonorus.tv.ui.components.Backdrop
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.components.PosterCard
import org.sonorus.tv.ui.components.PosterWidth
import org.sonorus.tv.ui.components.SectionLabel
import org.sonorus.tv.ui.rememberLoad
import org.sonorus.tv.ui.screens.detail.StartFocus
import org.sonorus.tv.ui.screens.detail.TextWidth
import org.sonorus.tv.ui.screens.detail.rememberReturnFocus
import org.sonorus.tv.ui.screens.detail.seenOf
import org.sonorus.tv.ui.screens.detail.titleSub
import org.sonorus.tv.ui.theme.SonorusTheme

@Composable
fun CollectionScreen(id: Int, nav: Nav) {
    val load = rememberLoad("collection", id) { SonorusTvApp.instance.api.videoCollection(id).collection }
    ReloadOnReturn(load)
    LoadBox(load, Modifier.fillMaxSize()) { c -> CollectionPage(c, nav) }
}

/** The head stays put and only the posters scroll, so the name never leaves the screen. */
@Composable
private fun CollectionPage(c: VideoCollection, nav: Nav) {
    val colors = SonorusTheme.colors
    val grid = rememberLazyGridState()
    val back = rememberReturnFocus()
    val primary = remember { FocusRequester() }

    Box(Modifier.fillMaxSize()) {
        Backdrop(c.backdrop, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
            Column(
                Modifier.padding(horizontal = PageGutter).widthIn(max = TextWidth),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Filmreihe")
                Text(c.name, style = MaterialTheme.typography.displaySmall, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(seenOf(c.movies), style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
                if (c.overview.isNotEmpty()) {
                    Text(c.overview, style = MaterialTheme.typography.bodyMedium, color = colors.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.FixedSize(PosterWidth),
                state = grid,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = PageGutter, end = PageGutter, top = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                itemsIndexed(c.movies, key = { _, t -> t.id }) { i, t ->
                    val key = "m${t.id}"
                    PosterCard(
                        poster = t.poster,
                        title = t.title,
                        sub = titleSub(t),
                        onClick = { back.leave(key); nav.movie(t.id) },
                        modifier = back.target(key).then(if (i == 0) Modifier.focusRequester(primary) else Modifier),
                        done = t.done,
                        progress = t.progress.fraction.takeIf { t.progress.started },
                    )
                }
            }
        }
    }
    StartFocus(back, primary) { grid.scrollToItem(0) }
}
