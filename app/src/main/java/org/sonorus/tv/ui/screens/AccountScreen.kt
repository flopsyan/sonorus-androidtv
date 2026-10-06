package org.sonorus.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import org.sonorus.tv.BuildConfig
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.ui.components.PageGutter
import org.sonorus.tv.ui.components.SectionLabel
import org.sonorus.tv.ui.theme.SonorusTheme

@Composable
fun AccountScreen() {
    val account = SonorusTvApp.instance.account
    val user by account.user.collectAsState()
    val colors = SonorusTheme.colors
    val focus = remember { FocusRequester() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = PageGutter, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionLabel("Konto")
        Text(
            user?.displayName?.ifEmpty { null } ?: account.username,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.text,
        )
        Text(account.serverUrl, style = MaterialTheme.typography.bodyLarge, color = colors.textDim)
        OutlinedButton(onClick = { account.logout() }, modifier = Modifier.focusRequester(focus)) {
            Text("Abmelden")
        }
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
