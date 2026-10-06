package org.sonorus.tv.data.model

import kotlinx.serialization.Serializable

// The parts of the server's envelopes this app reads; everything else is ignored.

@Serializable
data class ApiError(
    val ok: Boolean = false,
    val error: String = "",
    val message: String = "",
)

@Serializable
data class User(
    val id: Int = 0,
    val username: String = "",
    val displayName: String = "",
)

/** The video keys of `users.prefs`; the web and the phone write the same ones. */
@Serializable
data class Prefs(
    val videoSubLang: String = "",
    val videoAudioLang: String = "",
    val videoAutoplay: Boolean = true,
)

@Serializable
data class Bootstrap(
    val user: User = User(),
    val prefs: Prefs = Prefs(),
)

/** `/api/search` answers for the whole library; only films and series are read here. */
@Serializable
data class SearchResponse(
    val q: String = "",
    val movies: List<VideoTitle> = emptyList(),
    val shows: List<VideoTitle> = emptyList(),
)
