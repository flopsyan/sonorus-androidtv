package org.sonorus.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.sonorus.tv.ui.screens.AccountScreen
import org.sonorus.tv.ui.screens.BrowseScreen
import org.sonorus.tv.ui.screens.CollectionScreen
import org.sonorus.tv.ui.screens.MovieScreen
import org.sonorus.tv.ui.screens.ShowScreen
import org.sonorus.tv.ui.screens.WatchScreen

@Composable
fun AppNav() {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    val id = navArgument("id") { type = NavType.IntType }

    NavHost(controller, startDestination = Routes.BROWSE) {
        composable(Routes.BROWSE) { BrowseScreen(nav) }
        composable(Routes.ACCOUNT) { AccountScreen() }
        composable(Routes.MOVIE, listOf(id)) { MovieScreen(it.arguments!!.getInt("id"), nav) }
        composable(
            Routes.SHOW,
            listOf(id, navArgument("season") { type = NavType.StringType; nullable = true }),
        ) {
            ShowScreen(it.arguments!!.getInt("id"), it.arguments?.getString("season")?.toIntOrNull(), nav)
        }
        composable(Routes.COLLECTION, listOf(id)) { CollectionScreen(it.arguments!!.getInt("id"), nav) }
        composable(
            Routes.WATCH,
            listOf(id, navArgument("t") { type = NavType.StringType; nullable = true }),
        ) {
            WatchScreen(it.arguments!!.getInt("id"), fromStart = it.arguments?.getString("t") == "0", nav)
        }
    }
}
