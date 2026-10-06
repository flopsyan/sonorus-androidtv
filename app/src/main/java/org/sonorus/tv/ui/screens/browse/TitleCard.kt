package org.sonorus.tv.ui.screens.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.sonorus.tv.data.model.VideoTitle
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.components.PosterCard

/** A film or series poster that opens its page; [key] has to be unique on the screen. */
@Composable
fun TitleCard(
    t: VideoTitle,
    key: String,
    nav: Nav,
    returnFocus: ReturnFocus,
    modifier: Modifier = Modifier,
    sub: String = titleSub(t),
) {
    PosterCard(
        poster = t.poster,
        title = t.title,
        sub = sub,
        onClick = {
            returnFocus.open(key)
            if (t.isMovie) nav.movie(t.id) else nav.show(t.id)
        },
        modifier = modifier.returnFocus(returnFocus, key),
        done = t.done,
        progress = t.progress.fraction.takeIf { t.isMovie && t.progress.started },
    )
}
