package org.sonorus.tv.ui.screens.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.model.SearchResponse
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.components.EmptyNote
import org.sonorus.tv.ui.components.Loading
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.components.Shelf
import org.sonorus.tv.ui.components.TvTextField
import org.sonorus.tv.ui.rememberLoad

@Composable
fun SearchTab(nav: Nav, returnFocus: ReturnFocus) {
    val api = SonorusTvApp.instance.api
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var term by remember { mutableStateOf(query.trim()) }
    // One load for every term, so the last results stay up while the next ones come in.
    val load = rememberLoad("search") { term.let { if (it.length < 2) SearchResponse() else api.search(it) } }
    LaunchedEffect(query) {
        delay(400)
        val next = query.trim()
        if (next != term) {
            term = next
            load.reload()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TvTextField(
            label = "Suchen",
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = PageGutter).width(480.dp),
            placeholder = "Film oder Serie",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        )
        if (term.length < 2) {
            EmptyNote("Wonach suchst du?")
        } else {
            LoadBox(load, Modifier.weight(1f)) { data ->
                when {
                    load.error != null && !load.loading -> EmptyNote(load.error.orEmpty())
                    data.movies.isEmpty() && data.shows.isEmpty() -> if (load.loading) Loading() else EmptyNote("Nichts gefunden.")
                    else -> SearchResults(data, nav, returnFocus)
                }
            }
        }
    }
}

@Composable
private fun SearchResults(data: SearchResponse, nav: Nav, returnFocus: ReturnFocus) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 27.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        for ((label, titles) in listOf("Filme" to data.movies, "Serien" to data.shows)) {
            if (titles.isEmpty()) continue
            item(key = label) {
                Shelf(label) {
                    items(titles, key = { it.id }) { t ->
                        TitleCard(t, "search-${t.kind}:${t.id}", nav, returnFocus, sub = t.year?.toString().orEmpty())
                    }
                }
            }
        }
    }
}
