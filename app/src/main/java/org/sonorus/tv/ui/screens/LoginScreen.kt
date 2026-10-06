package org.sonorus.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.data.errorMessage
import org.sonorus.tv.ui.components.SonorusButton
import org.sonorus.tv.ui.components.TvTextField
import org.sonorus.tv.ui.theme.SonorusTheme

@Composable
fun LoginScreen() {
    val app = SonorusTvApp.instance
    val colors = SonorusTheme.colors
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val first = remember { FocusRequester() }

    var server by rememberSaveable { mutableStateOf(app.account.serverUrl.ifEmpty { "https://" }) }
    var user by rememberSaveable { mutableStateOf(app.account.username) }
    var pass by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                app.account.login(server, user.trim(), pass)
            } catch (failure: Throwable) {
                error = errorMessage(failure)
            }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Sonorus", style = MaterialTheme.typography.displaySmall, color = colors.text)
            Text("Mit deinem Sonorus-Server verbinden.", style = MaterialTheme.typography.bodyLarge, color = colors.textDim)
            TvTextField(
                label = "Server-Adresse",
                value = server,
                onValueChange = { server = it },
                modifier = Modifier.focusRequester(first),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            TvTextField(
                label = "Benutzername",
                value = user,
                onValueChange = { user = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            TvTextField(
                label = "Passwort",
                value = pass,
                onValueChange = { pass = it },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.danger) }
            SonorusButton(if (busy) "Verbinde …" else "Anmelden", null, ::submit, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}
