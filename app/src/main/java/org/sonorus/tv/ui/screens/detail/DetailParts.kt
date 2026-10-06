package org.sonorus.tv.ui.screens.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.sonorus.tv.data.errorMessage
import org.sonorus.tv.data.model.VideoTitle
import org.sonorus.tv.ui.Load
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.components.Backdrop
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.components.PosterCard
import org.sonorus.tv.ui.components.Shelf
import org.sonorus.tv.ui.components.art
import org.sonorus.tv.ui.theme.RackLabel
import org.sonorus.tv.ui.theme.SonorusTheme
import org.sonorus.tv.ui.theme.num

/** The text column over the backdrop's dark left side. */
internal val TextWidth = 520.dp

/** The backdrop, dimmed once the page has scrolled, so episode text over the bright side stays readable. */
@Composable
internal fun DetailBackdrop(path: String?, scrolled: () -> Boolean) {
    val bg = SonorusTheme.colors.bg
    val dim = animateFloatAsState(if (scrolled()) 0.8f else 0f, tween(300), label = "dim")
    Backdrop(path, Modifier.fillMaxSize().drawWithContent { drawContent(); drawRect(bg, alpha = dim.value) })
}

/** Android TV scrolls a focused element to a third of the screen; inside the head that would hide the title. */
private class WholeBlockNode : Modifier.Node(), BringIntoViewModifierNode {
    // The extension asks the scrolling parents for this whole node instead of the child.
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) =
        bringIntoView(bounds = null)
}

private object WholeBlock : ModifierNodeElement<WholeBlockNode>() {
    override fun create() = WholeBlockNode()
    override fun update(node: WholeBlockNode) = Unit
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 1
}

@Composable
internal fun DetailHead(
    title: String,
    logo: String?,
    facts: List<String>,
    vote: Double?,
    after: List<String>,
    tagline: String,
    overview: String,
    actions: @Composable () -> Unit,
) {
    val colors = SonorusTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .then(WholeBlock)
            .padding(start = PageGutter, end = PageGutter, top = 32.dp),
    ) {
        Column(Modifier.widthIn(max = TextWidth), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (logo != null) {
                AsyncImage(
                    model = art(logo),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.height(84.dp).fillMaxWidth(0.8f),
                )
            } else {
                Text(title, style = MaterialTheme.typography.displaySmall, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            MetaLine(facts, vote, after)
            if (tagline.isNotEmpty()) {
                Text(
                    tagline,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = colors.textDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (overview.isNotEmpty()) {
                Text(overview, style = MaterialTheme.typography.bodyMedium, color = colors.text, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(20.dp))
        actions()
    }
}

@Composable
private fun MetaLine(facts: List<String>, vote: Double?, after: List<String>) {
    val colors = SonorusTheme.colors
    val style = MaterialTheme.typography.bodyMedium
    val score = voteText(vote)
    if (facts.isEmpty() && score == null && after.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = style, color = colors.textDim, maxLines = 1)
        if (score != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Star, "TMDB-Wertung", tint = colors.accent, modifier = Modifier.size(14.dp))
                Text(score, style = num(14.sp), color = colors.accent)
            }
        }
        if (after.isNotEmpty()) Text(after.joinToString(" · "), style = style, color = colors.textDim, maxLines = 1)
    }
}

@Composable
internal fun FactLines(lines: List<Pair<String, String>>) {
    val colors = SonorusTheme.colors
    Column(
        Modifier.padding(start = PageGutter, end = PageGutter, top = 20.dp).widthIn(max = TextWidth),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((label, value) in lines) {
            Row {
                Text(label.uppercase(), style = RackLabel, color = colors.textDim, modifier = Modifier.width(96.dp).padding(top = 2.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun markIcon(done: Boolean) = if (done) Icons.Filled.RemoveDone else Icons.Filled.Check

/** Every button on these pages: quiet until focused, then amber like the cards' focus frame. */
@Composable
internal fun DetailButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SonorusTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        colors = ButtonDefaults.colors(
            containerColor = colors.surface3,
            contentColor = colors.text,
            focusedContainerColor = colors.accent,
            focusedContentColor = colors.accentInk,
        ),
    ) {
        Icon(icon, null, Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label)
    }
}

@Composable
internal fun ErrorLine(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = SonorusTheme.colors.danger,
        modifier = Modifier.padding(top = 10.dp).widthIn(max = TextWidth),
    )
}

/** [first] takes the start focus on a page that has no button. */
@Composable
internal fun TitleShelf(
    label: String,
    titles: List<VideoTitle>,
    nav: Nav,
    back: ReturnFocus,
    keyPrefix: String,
    first: FocusRequester? = null,
) {
    Shelf(label, Modifier.padding(top = 28.dp)) {
        itemsIndexed(titles, key = { _, t -> t.id }) { i, t ->
            val key = "$keyPrefix${t.id}"
            PosterCard(
                poster = t.poster,
                title = t.title,
                sub = titleSub(t),
                onClick = {
                    back.leave(key)
                    if (t.isMovie) nav.movie(t.id) else nav.show(t.id)
                },
                modifier = back.target(key).then(if (i == 0 && first != null) Modifier.focusRequester(first) else Modifier),
                done = t.done,
                progress = t.progress.fraction.takeIf { t.isMovie && t.progress.started },
            )
        }
    }
}

/**
 * NavHost drops a page while the player or another page is on top, so only saved state comes back.
 * The element that left the page saves its key here and gets the focus again on return.
 */
internal class ReturnFocus(private val saved: MutableState<String?>) {
    val requester = FocusRequester()

    fun leave(key: String?) {
        saved.value = key
    }

    fun target(key: String): Modifier = if (saved.value == key) Modifier.focusRequester(requester) else Modifier
}

@Composable
internal fun rememberReturnFocus(): ReturnFocus {
    val saved = rememberSaveable { mutableStateOf<String?>(null) }
    return remember { ReturnFocus(saved) }
}

/** Lazy items attach a frame or two after the page appears, so the first try can miss. */
internal suspend fun focusFirst(vararg targets: FocusRequester): Boolean {
    repeat(4) {
        if (targets.any { runCatching { it.requestFocus(FocusDirection.Enter) }.getOrDefault(false) }) return true
        withFrameNanos { }
    }
    return false
}

/** A page's first focus: back where the user left it, else [primary]; [toTop] when neither is on screen. */
@Composable
internal fun StartFocus(back: ReturnFocus, primary: FocusRequester, toTop: suspend () -> Unit) {
    LaunchedEffect(Unit) {
        if (!focusFirst(back.requester, primary)) {
            toTop()
            focusFirst(primary)
        }
    }
}

/** Sends one watched change, takes its local marks back if the server refuses, then reloads the page. */
internal fun CoroutineScope.change(
    load: Load<*>,
    failure: MutableState<String?>,
    undo: () -> Unit = {},
    call: suspend () -> Unit,
) {
    failure.value = null
    launch {
        try {
            call()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            undo()
            failure.value = errorMessage(error)
        }
        load.reload()
    }
}
