package org.sonorus.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.screens.browse.CatalogTab
import org.sonorus.tv.ui.screens.browse.OverviewTab
import org.sonorus.tv.ui.screens.browse.SearchTab
import org.sonorus.tv.ui.screens.browse.rememberReturnFocus
import org.sonorus.tv.ui.screens.browse.requestFocusSoon
import org.sonorus.tv.ui.screens.browse.returnFocus

private enum class BrowseTab(val label: String) { HOME("Übersicht"), MOVIES("Filme"), SHOWS("Serien"), SEARCH("Suche") }

@Composable
fun BrowseScreen(nav: Nav) {
    var tab by rememberSaveable { mutableStateOf(BrowseTab.HOME) }
    val tabFocus = remember { BrowseTab.entries.map { FocusRequester() } }
    val returnFocus = rememberReturnFocus()
    val pages = rememberSaveableStateHolder()
    var contentFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusTab = { runCatching { tabFocus[tab.ordinal].requestFocus() }; Unit }

    LaunchedEffect(Unit) {
        tabFocus[tab.ordinal].requestFocusSoon()
        returnFocus.settle()
    }
    // Back from the content goes to the tabs first; from the tabs it leaves the app.
    BackHandler(enabled = contentFocused, onBack = focusTab)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = PageGutter, end = PageGutter, top = 27.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabRow(
                selectedTabIndex = tab.ordinal,
                // Coming up from the content lands on the open tab, not the nearest one.
                modifier = Modifier.weight(1f).focusRestorer(tabFocus[tab.ordinal]),
            ) {
                BrowseTab.entries.forEach { t ->
                    Tab(
                        selected = t == tab,
                        onFocus = { tab = t },
                        onClick = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.focusRequester(tabFocus[t.ordinal]),
                    ) {
                        Text(
                            t.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    returnFocus.open("account")
                    nav.account()
                },
                modifier = Modifier.returnFocus(returnFocus, "account"),
            ) { Icon(Icons.Filled.Person, "Konto") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().onFocusChanged { contentFocused = it.hasFocus }) {
            pages.SaveableStateProvider(tab.name) {
                when (tab) {
                    BrowseTab.HOME -> OverviewTab(nav, returnFocus, focusTab)
                    BrowseTab.MOVIES -> CatalogTab(nav, movies = true, returnFocus)
                    BrowseTab.SHOWS -> CatalogTab(nav, movies = false, returnFocus)
                    BrowseTab.SEARCH -> SearchTab(nav, returnFocus)
                }
            }
        }
    }
}
