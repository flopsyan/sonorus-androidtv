package org.sonorus.tv.data

import androidx.media3.common.PlaybackException
import kotlinx.serialization.SerializationException
import org.sonorus.tv.SonorusTvApp
import java.io.IOException

const val NO_SERVER = "Der Server ist nicht erreichbar."
const val NO_NETWORK = "Keine Internetverbindung."

/** A status the server answered with, when the body did not say more. */
class HttpStatusException(val status: Int) : IOException(statusMessage(status))

/** The same sentences as `statusMessage` in the web client's `public/js/api.js`. */
fun statusMessage(status: Int): String = when {
    status == 413 -> "Die Anfrage war für den Server zu groß."
    status == 502 || status == 503 ->
        "Sonorus ist nicht erreichbar, der Dienst startet vielleicht gerade neu (HTTP $status)."
    status == 504 -> "Der Server hat zu lange nicht geantwortet (HTTP 504)."
    status == 429 -> "Zu viele Anfragen, bitte kurz warten (HTTP 429)."
    status == 403 -> "Der Zugriff wurde vor Sonorus abgewiesen (HTTP 403)."
    status >= 500 -> "Der Server hat mit einem Fehler geantwortet (HTTP $status)."
    else -> "Unerwartete Antwort vom Server (HTTP $status)."
}

/** The sentence for any failure; Sonorus's own errors already say it in German. */
fun errorMessage(
    error: Throwable,
    noNetwork: Boolean = !SonorusTvApp.instance.connectivity.online.value,
): String = when (error) {
    is ApiException, is HttpStatusException -> error.message ?: NO_SERVER
    is SerializationException -> "Die Antwort des Servers passt nicht zu dieser App-Version."
    is java.net.UnknownHostException ->
        if (noNetwork) NO_NETWORK else "Diese Adresse gibt es nicht. Stimmt die Server-Adresse?"
    is java.net.ConnectException ->
        if (noNetwork) NO_NETWORK else "Der Server nimmt keine Verbindung an. Läuft Sonorus?"
    is java.net.SocketTimeoutException -> "Der Server antwortet nicht rechtzeitig."
    is javax.net.ssl.SSLException -> "Die verschlüsselte Verbindung kam nicht zustande."
    is IOException -> if (noNetwork) NO_NETWORK else NO_SERVER
    else -> "Fehler in der App: ${error.message ?: error.javaClass.simpleName}"
}

/** Why a video would not play, for the error codes that are not the network. */
fun playbackMessage(errorCode: Int, httpStatus: Int?): String = when {
    httpStatus == 404 -> "Die Datei fehlt auf dem Server."
    httpStatus != null -> statusMessage(httpStatus)
    else -> when (errorCode) {
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        -> "Dieses Format kann der Fernseher nicht abspielen."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        -> "Die Datei ist beschädigt oder unvollständig."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "Die Verbindung zum Server ist abgebrochen."
        else -> "Wiedergabefehler ${PlaybackException.getErrorCodeName(errorCode)}."
    }
}

// The names the player's Ton panel uses, keyed by the MIME type ExoPlayer reports.
private val CODEC_NAMES = mapOf(
    "audio/eac3" to "Dolby Digital+", "audio/eac3-joc" to "Dolby Digital+", "audio/ac3" to "Dolby Digital",
    "audio/vnd.dts" to "DTS", "audio/vnd.dts.hd" to "DTS", "audio/true-hd" to "TrueHD", "audio/mp4a-latm" to "AAC",
    "video/avc" to "H.264", "video/hevc" to "HEVC", "video/av01" to "AV1", "video/x-vnd.on2.vp9" to "VP9",
)

/** Names the track whose decoder gave up, since that is the real reason. */
fun videoFailureMessage(errorCode: Int, httpStatus: Int?, failedMime: String?): String {
    val track = when {
        httpStatus != null -> null
        failedMime?.startsWith("audio/") == true -> "Der Ton"
        failedMime?.startsWith("video/") == true -> "Das Bild"
        else -> null
    }
    if (track == null || failedMime == null) return playbackMessage(errorCode, httpStatus)
    val codec = CODEC_NAMES[failedMime] ?: failedMime.substringAfter('/').uppercase()
    return "$track ($codec) lässt sich auf diesem Fernseher nicht abspielen."
}
