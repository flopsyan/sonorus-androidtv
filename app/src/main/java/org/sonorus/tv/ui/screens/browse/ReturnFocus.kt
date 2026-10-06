package org.sonorus.tv.ui.screens.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.flow.first

/**
 * The element that led away from the screen. Navigation composes the screen afresh on
 * the way back, and Compose does not put the focus back by itself.
 */
@Stable
class ReturnFocus internal constructor(private var target: String?) {
    private var opened: String? = target
    private var settled by mutableStateOf(false)

    /** Call right before navigating away from the element marked [key]. */
    fun open(key: String) {
        opened = key
    }

    /** The screen's own first focus has landed; the element that led away may take over now. */
    fun settle() {
        settled = true
    }

    internal suspend fun awaitSettled() {
        snapshotFlow { settled }.first { it }
    }

    internal fun wants(key: String) = target == key

    internal fun restored() {
        target = null
    }

    companion object {
        val Saver = Saver<ReturnFocus, String>(save = { it.opened }, restore = { ReturnFocus(it) })
    }
}

@Composable
fun rememberReturnFocus(): ReturnFocus = rememberSaveable(saver = ReturnFocus.Saver) { ReturnFocus(null) }

/** Takes the focus once if this is the element the user left the screen from. */
@Composable
fun Modifier.returnFocus(memory: ReturnFocus, key: String): Modifier {
    if (!memory.wants(key)) return this
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        memory.awaitSettled()
        requester.requestFocusSoon()
        memory.restored()
    }
    return focusRequester(requester)
}

// Lazy lists and the TabRow compose their children while measuring, so right after the
// first composition there may be nothing to focus yet. Older Compose threw instead of saying no.
suspend fun FocusRequester.requestFocusSoon(): Boolean {
    repeat(5) {
        if (runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)) return true
        withFrameNanos { }
    }
    return false
}
