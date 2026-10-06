package org.sonorus.tv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.sonorus.tv.data.model.Prefs
import org.sonorus.tv.data.model.User

/** Whether this TV is signed in, and the account's video prefs. */
class Account(
    private val api: SonorusApi,
    private val session: Session,
    private val scope: CoroutineScope,
) {
    private val _loggedIn = MutableStateFlow(session.isConfigured)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _prefs = MutableStateFlow(Prefs())
    val prefs: StateFlow<Prefs> = _prefs.asStateFlow()

    init {
        if (session.isConfigured) refresh()
    }

    /** A failed bootstrap keeps the defaults; the screens show their own errors. */
    fun refresh() {
        scope.launch {
            runCatching { api.bootstrap() }.onSuccess {
                _user.value = it.user
                _prefs.value = it.prefs
            }
        }
    }

    suspend fun login(server: String, user: String, pass: String) {
        api.login(server, user, pass)
        _loggedIn.value = true
        refresh()
    }

    fun logout() {
        scope.launch {
            api.logout()
            _user.value = null
            _prefs.value = Prefs()
            _loggedIn.value = false
        }
    }

    /** Shown at once; the server write is best effort, like the phone's. */
    fun savePref(key: String, value: JsonElement, apply: (Prefs) -> Prefs) {
        _prefs.value = apply(_prefs.value)
        scope.launch { runCatching { api.setPref(key, value) } }
    }

    val serverUrl: String get() = session.serverUrl
    val username: String get() = session.username
}
