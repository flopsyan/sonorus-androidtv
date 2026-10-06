package org.sonorus.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.sonorus.tv.SonorusTvApp
import org.sonorus.tv.ui.LoadBox
import org.sonorus.tv.ui.Nav
import org.sonorus.tv.ui.rememberLoad
import org.sonorus.tv.ui.screens.watch.VideoPlayer

@Composable
fun WatchScreen(videoId: Int, fromStart: Boolean, nav: Nav) {
    val load = rememberLoad(videoId) { SonorusTvApp.instance.api.playerInfo(videoId).video }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LoadBox(load) { info -> VideoPlayer(info, fromStart, nav) }
    }
}
