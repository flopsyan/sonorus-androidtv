package org.sonorus.tv.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import org.sonorus.tv.data.errorMessage
import org.sonorus.tv.ui.components.ErrorNote
import org.sonorus.tv.ui.components.Loading

/** What a screen is doing while it fetches its data. */
class Load<T> internal constructor(
    val value: T?,
    val error: String?,
    val loading: Boolean,
    /** Fetches again and keeps showing the old value meanwhile, so focus does not jump. */
    val reload: () -> Unit,
)

/** Fetches once per [key] and hands the screen its data, an error or a spinner. */
@Composable
fun <T> rememberLoad(vararg key: Any?, fetch: suspend () -> T): Load<T> {
    var value by remember(*key) { mutableStateOf<T?>(null) }
    var error by remember(*key) { mutableStateOf<String?>(null) }
    var loading by remember(*key) { mutableStateOf(true) }
    var attempt by remember(*key) { mutableIntStateOf(0) }

    LaunchedEffect(*key, attempt) {
        loading = true
        error = null
        try {
            value = fetch()
        } catch (cancel: CancellationException) {
            // The screen was left; the effect that replaces this one owns the state now.
            throw cancel
        } catch (failure: Throwable) {
            error = errorMessage(failure)
        }
        loading = false
    }
    return Load(value, error, loading) { attempt++ }
}

/** Reloads when the screen comes back to the front, e.g. from the player with a new position. */
@Composable
fun ReloadOnReturn(load: Load<*>) {
    var first by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        if (first) first = false else load.reload()
        onPauseOrDispose { }
    }
}

private enum class Phase { WAITING, READY, FAILED }

/** Spinner, error or content. [content] must draw exactly one root composable. */
@Composable
fun <T> LoadBox(
    load: Load<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val phase = when {
        load.value != null -> Phase.READY
        load.loading -> Phase.WAITING
        else -> Phase.FAILED
    }
    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(120))) },
        modifier = modifier,
        label = "load",
    ) { shown ->
        when (shown) {
            Phase.READY -> load.value?.let { content(it) }
            Phase.WAITING -> Loading()
            Phase.FAILED -> ErrorNote(load.error.orEmpty(), onRetry = load.reload)
        }
    }
}
