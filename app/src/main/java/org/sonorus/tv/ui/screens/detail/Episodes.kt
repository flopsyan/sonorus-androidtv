package org.sonorus.tv.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.sonorus.tv.data.model.VideoEpisode
import org.sonorus.tv.data.model.VideoSeason
import org.sonorus.tv.ui.VideoFmt
import org.sonorus.tv.ui.components.DoneBadge
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.components.ProgressLine
import org.sonorus.tv.ui.components.WIDE_RATIO
import org.sonorus.tv.ui.components.art
import org.sonorus.tv.ui.theme.RackLabel
import org.sonorus.tv.ui.theme.SonorusTheme
import org.sonorus.tv.ui.theme.num

/** One chip per season; landing on a chip picks it, and entering the row lands on the picked one. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SeasonChips(
    seasons: List<VideoSeason>,
    selected: Int?,
    watched: (VideoSeason) -> Int,
    onPick: (Int) -> Unit,
) {
    val colors = SonorusTheme.colors
    val chips = remember(seasons.size) { List(seasons.size) { FocusRequester() } }
    val picked = seasons.indexOfFirst { it.season == selected }.coerceAtLeast(0)
    Row(
        Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .focusRestorer(chips[picked])
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PageGutter, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        seasons.forEachIndexed { i, x ->
            FilterChip(
                selected = x.season == selected,
                onClick = { onPick(x.season) },
                modifier = Modifier
                    .focusRequester(chips[i])
                    .onFocusChanged { if (it.isFocused) onPick(x.season) },
                colors = FilterChipDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = colors.textDim,
                    focusedContainerColor = colors.accent,
                    focusedContentColor = colors.accentInk,
                    selectedContainerColor = colors.surface3,
                    selectedContentColor = colors.text,
                    focusedSelectedContainerColor = colors.accent,
                    focusedSelectedContentColor = colors.accentInk,
                ),
            ) {
                Text(seasonChip(x.name, watched(x), x.episodes.size))
            }
        }
    }
}

@Composable
internal fun SeasonHead(x: VideoSeason, watched: Int, onMark: (Boolean) -> Unit) {
    val colors = SonorusTheme.colors
    val done = watched >= x.episodes.size
    Column(
        Modifier.padding(start = PageGutter, end = PageGutter, top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(x.name, style = MaterialTheme.typography.titleLarge, color = colors.text)
        Text(seasonFacts(x, watched), style = MaterialTheme.typography.bodySmall, color = colors.textDim)
        if (x.overview.isNotEmpty()) {
            Text(
                x.overview,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 640.dp),
            )
        }
        DetailButton(markLabel("Staffel", done), markIcon(done), { onMark(!done) })
    }
}

private val RowShape = RoundedCornerShape(10.dp)
private val RowInset = 10.dp

/** OK plays, a long press on OK opens the episode's menu. */
@Composable
internal fun EpisodeRow(
    e: VideoEpisode,
    done: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SonorusTheme.colors
    val label = episodeLabel(e)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.padding(horizontal = PageGutter - RowInset, vertical = 3.dp).fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colors.text,
            focusedContainerColor = colors.surface3,
            focusedContentColor = colors.text,
        ),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, colors.accent), shape = RowShape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            Modifier.padding(RowInset),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(160.dp)
                    .aspectRatio(WIDE_RATIO)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface2),
            ) {
                AsyncImage(
                    model = art(e.still),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (e.progress.started && !done) {
                    ProgressLine(e.progress.fraction, Modifier.align(Alignment.BottomStart).padding(6.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label.uppercase(), style = RackLabel, color = colors.textDim, modifier = Modifier.weight(1f))
                    if (e.duration > 0) Text(VideoFmt.durationLong(e.duration), style = num(12.sp), color = colors.textFaint)
                }
                Text(
                    e.name.ifEmpty { label },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) colors.textDim else colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (e.overview.isNotEmpty()) {
                    Text(
                        e.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (done) DoneBadge()
        }
    }
}

@Composable
internal fun EpisodeMenu(
    e: VideoEpisode,
    done: Boolean,
    onMark: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SonorusTheme.colors
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(380.dp)
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                episodeTitle(e),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailButton(markLabel(null, done), markIcon(done), onMark, Modifier.fillMaxWidth().focusRequester(first))
            DetailButton("Von vorn abspielen", Icons.Filled.Replay, onRestart, Modifier.fillMaxWidth())
        }
        LaunchedEffect(Unit) { focusFirst(first) }
    }
}
