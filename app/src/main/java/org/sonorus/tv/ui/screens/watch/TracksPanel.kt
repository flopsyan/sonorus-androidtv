package org.sonorus.tv.ui.screens.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import org.sonorus.tv.ui.VideoFmt
import org.sonorus.tv.ui.components.SectionLabel

@Composable
internal fun TracksPanel(
    session: VideoSession,
    fill: Boolean,
    onFill: (Boolean) -> Unit,
    autoplay: Boolean,
    onAutoplay: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = session.info
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(
        modifier
            .fillMaxHeight()
            .width(400.dp)
            .background(Color(0xF2141119))
            // Focus stays in the panel until Back closes it.
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (info.audio.isNotEmpty()) {
            SectionLabel("Ton", Modifier.padding(start = 12.dp, bottom = 4.dp))
            info.audio.forEachIndexed { i, a ->
                val (main, tech) = VideoFmt.audioLabel(a)
                PanelRow(main, tech, a.index == session.audio, if (i == 0) Modifier.focusRequester(first) else Modifier) {
                    session.pickAudio(a)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        SectionLabel("Untertitel", Modifier.padding(start = 12.dp, bottom = 4.dp))
        PanelRow("Aus", "", session.sub == null, if (info.audio.isEmpty()) Modifier.focusRequester(first) else Modifier) {
            session.pickSubtitle(null)
        }
        for (s in info.subtitles) {
            val (main, kind) = VideoFmt.subtitleLabel(s)
            PanelRow(main, kind, s.key == session.sub, enabled = s.supported) { session.pickSubtitle(s.key) }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Bild", Modifier.padding(start = 12.dp, bottom = 4.dp))
        ToggleRow("Bild füllen", "Zoomt über schwarze Balken, die im Film eingebrannt sind.", fill, onFill)
        if (info.kind == "show") {
            Spacer(Modifier.height(16.dp))
            SectionLabel("Folgen", Modifier.padding(start = 12.dp, bottom = 4.dp))
            ToggleRow("Nächste Folge automatisch", "", autoplay, onAutoplay)
        }
    }
}

@Composable
private fun PanelRow(
    main: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        headlineContent = { Text(main, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (sub.isEmpty()) null else {
            { Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = if (selected) {
            { Icon(Icons.Filled.Check, "Gewählt") }
        } else null,
    )
}

@Composable
private fun ToggleRow(label: String, hint: String, on: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        selected = false,
        onClick = { onChange(!on) },
        headlineContent = { Text(label) },
        supportingContent = if (hint.isEmpty()) null else {
            { Text(hint) }
        },
        trailingContent = { Switch(checked = on, onCheckedChange = null) },
    )
}
