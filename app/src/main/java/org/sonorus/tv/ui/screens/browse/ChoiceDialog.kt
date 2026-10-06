package org.sonorus.tv.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import org.sonorus.tv.ui.components.SectionLabel
import org.sonorus.tv.ui.theme.SonorusTheme

/** A short list to pick one entry from; focus starts on [selected], or on the first entry when it is -1. */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SonorusTheme.colors
    val start = selected.coerceIn(0, options.lastIndex)
    val list = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(400.dp)
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(vertical = 20.dp),
        ) {
            SectionLabel(title, Modifier.padding(start = 28.dp, end = 28.dp, bottom = 12.dp))
            LazyColumn(
                state = list,
                modifier = Modifier.heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(options) { i, label ->
                    ListItem(
                        selected = i == selected,
                        onClick = { onPick(i) },
                        headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = if (i == selected) {
                            { Icon(Icons.Filled.Check, null) }
                        } else {
                            null
                        },
                        modifier = if (i == start) Modifier.focusRequester(focus) else Modifier,
                    )
                }
            }
        }
        LaunchedEffect(Unit) { focus.requestFocusSoon() }
    }
}
