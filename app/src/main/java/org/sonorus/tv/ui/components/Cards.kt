package org.sonorus.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.sonorus.tv.ui.theme.SonorusTheme

const val POSTER_RATIO = 2f / 3f
const val WIDE_RATIO = 16f / 9f

val PosterWidth = 132.dp
val WideWidth = 256.dp

private val CardShape = RoundedCornerShape(8.dp)

/** The focus look every card shares: a little larger, with an amber frame. */
@Composable
private fun focusBorder() = CardDefaults.border(
    focusedBorder = Border(BorderStroke(3.dp, SonorusTheme.colors.accent), shape = CardShape),
)

/** A poster with its title and one line under it. [progress] draws the watched bar, [done] the check. */
@Composable
fun PosterCard(
    poster: String?,
    title: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = PosterWidth,
    done: Boolean = false,
    progress: Double? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = SonorusTheme.colors
    Column(modifier.width(width)) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_RATIO),
            shape = CardDefaults.shape(CardShape),
            scale = CardDefaults.scale(focusedScale = 1.06f),
            border = focusBorder(),
            colors = CardDefaults.colors(containerColor = colors.surface2),
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = art(poster),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (poster == null) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textDim,
                        modifier = Modifier.align(Alignment.Center).padding(10.dp),
                    )
                }
                if (done) DoneBadge(Modifier.align(Alignment.TopEnd).padding(8.dp))
                if (progress != null) ProgressLine(progress, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        CardCaption(title, sub)
    }
}

/** A 16:9 picture that plays, with a title and one line under it. */
@Composable
fun WideCard(
    picture: String?,
    title: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = WideWidth,
    progress: Double? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = SonorusTheme.colors
    Column(modifier.width(width)) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(WIDE_RATIO),
            shape = CardDefaults.shape(CardShape),
            scale = CardDefaults.scale(focusedScale = 1.05f),
            border = focusBorder(),
            colors = CardDefaults.colors(containerColor = colors.surface2),
        ) {
            Box(Modifier.fillMaxSize().background(colors.surface2)) {
                AsyncImage(
                    model = art(picture),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                PlayBadge(Modifier.align(Alignment.Center))
                if (progress != null) ProgressLine(progress, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        CardCaption(title, sub)
    }
}

@Composable
private fun CardCaption(title: String, sub: String) {
    val colors = SonorusTheme.colors
    Spacer(Modifier.height(10.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (sub.isNotEmpty()) {
        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
