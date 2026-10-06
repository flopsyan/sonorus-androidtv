package org.sonorus.tv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Server, credentials and the session cookie, encrypted.
 *
 * The credentials are kept because the cookie expires after 30 days and a password
 * change invalidates it; [SonorusApi] logs in again by itself on a 401.
 */
class Session(context: Context) {

    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sonorus_session",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Base URL without a trailing slash. */
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, "") ?: ""
        private set(value) = prefs.edit().putString(KEY_SERVER, value.trimEnd('/')).apply()

    var username: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        private set(value) = prefs.edit().putString(KEY_USER, value).apply()

    var password: String
        get() = prefs.getString(KEY_PASS, "") ?: ""
        private set(value) = prefs.edit().putString(KEY_PASS, value).apply()

    val isConfigured: Boolean get() = serverUrl.isNotEmpty() && username.isNotEmpty()

    fun store(server: String, user: String, pass: String) {
        serverUrl = server
        username = user
        password = pass
    }

    fun clear() {
        cookieJar.forget()
        prefs.edit().clear().apply()
    }

    val cookieJar = SessionCookieJar()

    /** Sonorus sets exactly one cookie, so that is all this keeps. */
    inner class SessionCookieJar : CookieJar {
        private var cookie: Cookie? = null

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            for (c in cookies) {
                if (c.name != COOKIE_NAME) continue
                // Logging out clears the cookie by sending an empty value.
                if (c.value.isEmpty()) forget() else remember(c)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            listOfNotNull(cookie ?: restore(url))

        private fun remember(c: Cookie) {
            cookie = c
            prefs.edit().putString(KEY_COOKIE, c.value).apply()
        }

        private fun restore(url: HttpUrl): Cookie? {
            val value = prefs.getString(KEY_COOKIE, "").orEmpty()
            if (value.isEmpty()) return null
            return Cookie.Builder()
                .name(COOKIE_NAME)
                .value(value)
                .domain(url.host)
                .path("/")
                .build()
                .also { cookie = it }
        }

        fun forget() {
            cookie = null
            prefs.edit().remove(KEY_COOKIE).apply()
        }
    }

    private companion object {
        const val COOKIE_NAME = "sonorus-session"
        const val KEY_SERVER = "server"
        const val KEY_USER = "user"
        const val KEY_PASS = "pass"
        const val KEY_COOKIE = "cookie"
    }
}
