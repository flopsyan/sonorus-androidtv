package org.sonorus.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.sonorus.tv.ui.AppNav
import org.sonorus.tv.ui.screens.LoginScreen
import org.sonorus.tv.ui.theme.SonorusTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonorusTheme {
                val loggedIn by SonorusTvApp.instance.account.loggedIn.collectAsState()
                Box(Modifier.fillMaxSize().background(SonorusTheme.colors.bg)) {
                    if (loggedIn) AppNav() else LoginScreen()
                }
            }
        }
    }
}
