package org.sonorus.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.sonorus.tv.data.model.*
import java.io.IOException
import java.util.concurrent.TimeUnit

/** An error the server itself produced, carrying its German message. */
class ApiException(val code: String, override val message: String) : IOException(message)

internal val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * The part of the Sonorus REST API that films and series need.
 *
 * A native client sends no `Origin`, which `rejectCrossSite` on the server lets
 * through, so this is the same API the web client speaks.
 */
class SonorusApi(private val session: Session) {

    private val json = ApiJson

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(session.cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Ten parallel 401s cause one login, not ten. */
    private val loginLock = Mutex()

    private class Answer(val text: String, val status: Int, val ok: Boolean)

    val serverUrl: String get() = session.serverUrl

    /** A server path like `/api/videos/3/file` or `/video-art/x.jpg`, made absolute. */
    fun absolute(path: String): String = if (path.startsWith("http")) path else session.serverUrl + path

    /** Artwork path to URL; null stays null so an image slot can show its placeholder. */
    fun art(path: String?): String? = if (path.isNullOrEmpty()) null else absolute(path)

    // --- Login ------------------------------------------------------------------

    /**
     * The browser's form POST. Success is the 302, whose cookie is all that is
     * needed, so redirects are not followed.
     */
    suspend fun login(server: String, user: String, pass: String): Unit = withContext(Dispatchers.IO) {
        val base = server.trim().trimEnd('/')
        val url = (base + "/login").toHttpUrlOrNull()
            ?: throw ApiException("bad_url", "Die Server-Adresse ist keine gültige URL.")

        val body = FormBody.Builder()
            .add("username", user)
            .add("password", pass)
            .add("next", "/")
            .build()

        val noRedirects = client.newBuilder().followRedirects(false).build()
        val request = Request.Builder().url(url).post(body).build()

        noRedirects.newCall(request).execute().use { res ->
            when {
                res.isRedirect -> Unit
                res.code == 401 -> throw ApiException("bad_login", "Benutzername oder Passwort falsch.")
                res.code == 429 -> throw ApiException("blocked", "Zu viele Fehlversuche. Bitte kurz warten.")
                // The setup page answers 200 when no account exists yet.
                res.code == 200 -> throw ApiException("bad_login", "Benutzername oder Passwort falsch.")
                else -> throw ApiException("http_${res.code}", statusMessage(res.code))
            }
        }
        session.store(base, user, pass)
    }

    /** For callers that speak HTTP directly, like the player's data source. */
    suspend fun relogin() {
        loginLock.withLock { login(session.serverUrl, session.username, session.password) }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(session.serverUrl + "/logout")
                .post(FormBody.Builder().build())
                .build()
            client.newBuilder().followRedirects(false).build().newCall(request).execute().close()
        }
        session.clear()
    }

    // --- Plumbing ---------------------------------------------------------------

    private fun url(path: String, query: Map<String, String?> = emptyMap()): HttpUrl {
        val base = (session.serverUrl + path).toHttpUrlOrNull()
            ?: throw ApiException("bad_url", "Die Server-Adresse ist keine gültige URL.")
        if (query.isEmpty()) return base
        return base.newBuilder().apply {
            for ((k, v) in query) if (!v.isNullOrEmpty()) addQueryParameter(k, v)
        }.build()
    }

    private fun jsonBody(build: JsonObject): RequestBody =
        build.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    /** A 401 means the cookie expired; the stored credentials log in again and the call is retried once. */
    private suspend fun call(request: Request, retry: Boolean = true): JsonElement =
        withContext(Dispatchers.IO) {
            val answer = client.newCall(request).execute().use { res ->
                Answer(res.body.string(), res.code, res.isSuccessful)
            }

            if (answer.status == 401 && retry && session.password.isNotEmpty()) {
                loginLock.withLock { login(session.serverUrl, session.username, session.password) }
                return@withContext call(request.newBuilder().build(), retry = false)
            }

            // Not JSON means a proxy answered instead of Sonorus; the status is the only clue.
            val parsed = runCatching { json.parseToJsonElement(answer.text) }.getOrNull()
                ?: throw ApiException("not_json", statusMessage(answer.status))

            if (!answer.ok) {
                val err = runCatching { json.decodeFromJsonElement(ApiError.serializer(), parsed) }.getOrNull()
                throw ApiException(err?.error ?: "http_${answer.status}", err?.message ?: statusMessage(answer.status))
            }
            parsed
        }

    private suspend inline fun <reified T> get(path: String, query: Map<String, String?> = emptyMap()): T =
        json.decodeFromJsonElement<T>(call(Request.Builder().url(url(path, query)).get().build()))

    private suspend inline fun <reified T> post(path: String, body: JsonObject = JsonObject(emptyMap())): T =
        json.decodeFromJsonElement<T>(call(Request.Builder().url(url(path)).post(jsonBody(body)).build()))

    private suspend inline fun <reified T> put(path: String, body: JsonObject): T =
        json.decodeFromJsonElement<T>(call(Request.Builder().url(url(path)).put(jsonBody(body)).build()))

    // --- Account ----------------------------------------------------------------

    suspend fun bootstrap(): Bootstrap = get("/api/bootstrap")

    /** The account keeps this, so the web and the phone see the same choice. */
    suspend fun setPref(key: String, value: JsonElement) {
        put<JsonElement>("/api/prefs", buildJsonObject {
            put("key", key)
            put("value", value)
        })
    }

    suspend fun search(q: String): SearchResponse = get("/api/search", mapOf("q" to q))

    // --- Films and series ---------------------------------------------------------

    suspend fun videoHome(): VideoHomeResponse = get("/api/video-home")

    suspend fun movies(): MoviesResponse = get("/api/movies")

    suspend fun shows(): ShowsResponse = get("/api/shows")

    suspend fun movie(id: Int): MovieResponse = get("/api/movies/$id")

    suspend fun show(id: Int): ShowResponse = get("/api/shows/$id")

    suspend fun videoCollections(): CollectionsResponse = get("/api/collections")

    suspend fun videoCollection(id: Int): CollectionResponse = get("/api/collections/$id")

    suspend fun playerInfo(id: Int): PlayerInfoResponse = get("/api/videos/$id")

    suspend fun videoPlan(id: Int, start: Double, audio: Int?, caps: JsonObject, force: String?): PlanResponse =
        post("/api/videos/$id/plan", buildJsonObject {
            put("start", start)
            audio?.let { put("audio", it) }
            put("caps", caps)
            force?.let { put("force", it) }
        })

    suspend fun subtitleCues(id: Int, key: String): CuesResponse = get("/api/videos/$id/subtitles/$key")

    suspend fun setVideoProgress(id: Int, position: Double, completed: Boolean) {
        put<JsonElement>("/api/videos/$id/progress", buildJsonObject {
            put("position", position)
            put("completed", completed)
        })
    }

    suspend fun setVideoWatched(id: Int, watched: Boolean) {
        put<JsonElement>("/api/videos/$id/watched", buildJsonObject { put("watched", watched) })
    }

    /** A whole film or series, or one season of it. */
    suspend fun setTitleWatched(id: Int, watched: Boolean, season: Int? = null) {
        put<JsonElement>("/api/video-titles/$id/watched", buildJsonObject {
            put("watched", watched)
            season?.let { put("season", it) }
        })
    }

    suspend fun startVideoPlay(videoId: Int): PlayIdResponse =
        post("/api/video-plays", buildJsonObject { put("videoId", videoId) })

    suspend fun updateVideoPlay(playId: Int, seconds: Double) {
        put<JsonElement>("/api/video-plays/$playId", buildJsonObject { put("seconds", seconds) })
    }
}
