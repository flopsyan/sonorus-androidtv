package org.sonorus.tv.ui.screens.browse

import org.sonorus.tv.ui.components.SonorusButton
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.errorMessage
import org.sonorus.tv.data.model.ContinueItem
import org.sonorus.tv.data.model.VideoHomeResponse
import org.sonorus.tv.data.model.VideoTitle
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.ReloadOnReturn
import org.sonorus.tv.ui.components.Backdrop
import org.sonorus.tv.ui.components.EmptyNote
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.components.PosterCard
import org.sonorus.tv.ui.components.SectionLabel
import org.sonorus.tv.ui.components.Shelf
import org.sonorus.tv.ui.components.WideCard
import org.sonorus.tv.ui.components.art
import org.sonorus.tv.ui.rememberLoad
import org.sonorus.tv.ui.theme.SonorusTheme

private const val EMPTY_LIBRARY = "Noch keine Filme und Serien. Sonorus liest sie aus VIDEO_DIR auf dem Server: " +
    "\"movies\" mit einem Ordner je Film, \"shows\" mit einem Ordner je Serie."

private val HeroHeight = 300.dp

private fun continueKey(item: ContinueItem) = "continue:${item.kind}-${item.title.id}"

@Composable
fun OverviewTab(nav: Nav, returnFocus: ReturnFocus, focusTabs: () -> Unit) {
    val api = SonorusTvApp.instance.api
    val load = rememberLoad("videoHome") { api.videoHome() }
    ReloadOnReturn(load)
    LoadBox(load, Modifier.fillMaxSize()) { data ->
        if (data.movies.isEmpty() && data.shows.isEmpty()) {
            EmptyNote(EMPTY_LIBRARY)
        } else {
            Overview(data, nav, returnFocus, load.reload, focusTabs)
        }
    }
}

@Composable
private fun Overview(
    data: VideoHomeResponse,
    nav: Nav,
    returnFocus: ReturnFocus,
    reload: () -> Unit,
    focusTabs: () -> Unit,
) {
    val api = SonorusTvApp.instance.api
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val featured = remember(data) { pickFeatured(data.movies + data.shows) }
    val newMovies = remember(data) { newestMovies(data.movies) }
    val newShows = remember(data) { newestShows(data.shows) }
    val continueRow = remember { FocusRequester() }
    var menu by remember { mutableStateOf<ContinueItem?>(null) }
    var marked by remember { mutableStateOf<String?>(null) }

    // A film marked as seen leaves the row, and the focus would leave with it.
    LaunchedEffect(data) {
        val key = marked ?: return@LaunchedEffect
        marked = null
        if (data.carryOn.any { continueKey(it) == key }) return@LaunchedEffect
        withFrameNanos { } // the row drops the card while measuring
        if (data.carryOn.isEmpty() || !continueRow.requestFocusSoon()) focusTabs()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (featured != null) item(key = "hero") { FeaturedHero(featured, nav, returnFocus) }
        if (data.carryOn.isNotEmpty()) {
            item(key = "continue") {
                Shelf("Weiterschauen", Modifier.focusRequester(continueRow)) {
                    items(data.carryOn, key = ::continueKey) { item ->
                        ContinueCard(item, nav, returnFocus) { menu = item }
                    }
                }
            }
        }
        if (newMovies.isNotEmpty()) item(key = "movies") { TitleShelf("Neue Filme", newMovies, "new-movie", nav, returnFocus) }
        if (newShows.isNotEmpty()) item(key = "shows") { TitleShelf("Neue Folgen", newShows, "new-show", nav, returnFocus) }
        if (data.collections.isNotEmpty()) {
            item(key = "collections") {
                Shelf("Filmreihen") {
                    items(data.collections, key = { it.id }) { c ->
                        val key = "collection:${c.id}"
                        PosterCard(
                            poster = c.poster,
                            title = c.name,
                            sub = collectionSub(c),
                            onClick = {
                                returnFocus.open(key)
                                nav.collection(c.id)
                            },
                            modifier = Modifier.returnFocus(returnFocus, key),
                            done = c.watched >= c.movies,
                        )
                    }
                }
            }
        }
    }

    menu?.let { item ->
        val show = item.kind == "show"
        ChoiceDialog(
            title = item.title.title,
            options = listOf(if (show) "Zur Serie" else "Zum Film", "Als gesehen markieren"),
            selected = -1,
            onPick = { choice ->
                menu = null
                if (choice == 0) {
                    returnFocus.open(continueKey(item))
                    if (show) nav.show(item.title.id, item.video.season) else nav.movie(item.title.id)
                } else {
                    scope.launch {
                        runCatching { api.setVideoWatched(item.video.id, true) }
                            .onSuccess { marked = continueKey(item) }
                            .onFailure {
                                if (it !is CancellationException) Toast.makeText(context, errorMessage(it), Toast.LENGTH_LONG).show()
                            }
                        reload()
                    }
                }
            },
            onDismiss = { menu = null },
        )
    }
}

@Composable
private fun FeaturedHero(t: VideoTitle, nav: Nav, returnFocus: ReturnFocus) {
    val colors = SonorusTheme.colors
    val meta = remember(t) { heroMeta(t) }
    val videoId = t.videoId
    // TV lists scroll a focused element to 30 % of their height, which would cut the
    // hero's top off for its buttons.
    Box(Modifier.fillMaxWidth().height(HeroHeight).bringWholeIntoView()) {
        Backdrop(t.backdrop, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = PageGutter).width(460.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(if (t.isMovie) "Film-Tipp" else "Serien-Tipp")
            TitleOrLogo(t)
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
            if (t.overview.isNotEmpty()) {
                Text(
                    t.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SonorusButton(
                    "Details",
                    null,
                    onClick = {
                        returnFocus.open("hero:details")
                        if (t.isMovie) nav.movie(t.id) else nav.show(t.id)
                    },
                    modifier = Modifier.returnFocus(returnFocus, "hero:details"),
                )
                if (t.isMovie && videoId != null) {
                    SonorusButton(
                        "Abspielen",
                        Icons.Filled.PlayArrow,
                        onClick = {
                            returnFocus.open("hero:play")
                            nav.watch(videoId)
                        },
                        modifier = Modifier.returnFocus(returnFocus, "hero:play"),
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleOrLogo(t: VideoTitle) {
    if (t.logo != null) {
        AsyncImage(
            model = art(t.logo),
            contentDescription = t.title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = Modifier.height(64.dp).fillMaxWidth(0.8f),
        )
    } else {
        Text(
            t.title,
            style = MaterialTheme.typography.headlineLarge,
            color = SonorusTheme.colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** OK plays, a long press offers the title's page and marking it seen. */
@Composable
private fun ContinueCard(item: ContinueItem, nav: Nav, returnFocus: ReturnFocus, onMenu: () -> Unit) {
    val v = item.video
    val key = continueKey(item)
    WideCard(
        picture = continuePicture(item),
        title = item.title.title,
        sub = continueSub(item),
        onClick = {
            returnFocus.open(key)
            nav.watch(v.id)
        },
        modifier = Modifier.returnFocus(returnFocus, key),
        progress = v.progress.fraction.takeIf { v.progress.started },
        onLongClick = onMenu,
    )
}

@Composable
private fun TitleShelf(label: String, titles: List<VideoTitle>, prefix: String, nav: Nav, returnFocus: ReturnFocus) {
    Shelf(label) {
        items(titles, key = { it.id }) { t -> TitleCard(t, "$prefix:${t.id}", nav, returnFocus) }
    }
}
