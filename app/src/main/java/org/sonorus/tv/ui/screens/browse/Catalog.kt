package org.sonorus.tv.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.model.VideoGenre
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.ReloadOnReturn
import org.sonorus.tv.ui.components.EmptyNote
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.rememberLoad
import org.sonorus.tv.ui.theme.SonorusTheme
import org.sonorus.tv.ui.theme.num

/** Every film or every series, as a poster grid under the web's filters. */
@Composable
fun CatalogTab(nav: Nav, movies: Boolean, returnFocus: ReturnFocus) {
    val api = SonorusTvApp.instance.api
    val load = rememberLoad(movies) {
        if (movies) api.movies().let { it.movies to it.genres } else api.shows().let { it.shows to it.genres }
    }
    ReloadOnReturn(load)
    var genre by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableStateOf(VideoSort.TITLE) }
    var unwatched by rememberSaveable { mutableStateOf(false) }

    LoadBox(load, Modifier.fillMaxSize()) { (titles, genres) ->
        if (titles.isEmpty()) {
            EmptyNote(if (movies) "Keine Filme." else "Keine Serien.")
        } else {
            val shown = remember(titles, genre, sort, unwatched) {
                sortTitles(filterTitles(titles, genre, unwatched), sort)
            }
            val prefix = if (movies) "movie" else "show"
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = PageGutter, end = PageGutter, top = 4.dp, bottom = 27.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "bar", span = { GridItemSpan(maxLineSpan) }) {
                    BrowseBar(genres, genre, sort, unwatched, shown.size, { genre = it }, { sort = it }, { unwatched = it })
                }
                items(shown, key = { it.id }) { t ->
                    TitleCard(t, "$prefix:${t.id}", nav, returnFocus, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// FilterChip is still marked experimental in tv-material 1.1.
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrowseBar(
    genres: List<VideoGenre>,
    genre: Int,
    sort: VideoSort,
    unwatched: Boolean,
    count: Int,
    onGenre: (Int) -> Unit,
    onSort: (VideoSort) -> Unit,
    onUnwatched: (Boolean) -> Unit,
) {
    var genreOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (genres.isNotEmpty()) {
            FilterChip(
                selected = genre != 0,
                onClick = { genreOpen = true },
                leadingIcon = { Icon(Icons.Filled.FilterList, null, Modifier.size(FilterChipDefaults.IconSize)) },
            ) { Text(genres.firstOrNull { it.id == genre }?.name ?: "Alle Genres") }
        }
        FilterChip(
            selected = sort != VideoSort.TITLE,
            onClick = { sortOpen = true },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(FilterChipDefaults.IconSize)) },
        ) { Text(sort.label) }
        FilterChip(selected = unwatched, onClick = { onUnwatched(!unwatched) }) { Text("Nur ungesehene") }
        Spacer(Modifier.weight(1f))
        Text(plural(count, "Titel", "Titel"), style = num(13.sp), color = SonorusTheme.colors.textFaint)
    }

    if (genreOpen) {
        ChoiceDialog(
            title = "Genre",
            options = listOf("Alle Genres") + genres.map { "${it.name} (${it.count})" },
            selected = genres.indexOfFirst { it.id == genre } + 1,
            onPick = {
                onGenre(if (it == 0) 0 else genres[it - 1].id)
                genreOpen = false
            },
            onDismiss = { genreOpen = false },
        )
    }
    if (sortOpen) {
        ChoiceDialog(
            title = "Sortierung",
            options = VideoSort.entries.map { it.label },
            selected = sort.ordinal,
            onPick = {
                onSort(VideoSort.entries[it])
                sortOpen = false
            },
            onDismiss = { sortOpen = false },
        )
    }
}
