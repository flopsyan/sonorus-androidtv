package org.sonorus.tv.ui

import androidx.navigation.NavController

object Routes {
    const val BROWSE = "browse"
    const val ACCOUNT = "account"
    const val MOVIE = "movies/{id}"
    const val SHOW = "shows/{id}?season={season}"
    const val COLLECTION = "collections/{id}"
    const val WATCH = "watch/{id}?t={t}"
}

/** Where a screen can go. Screens get this instead of the controller. */
class Nav(private val controller: NavController) {
    fun movie(id: Int) = controller.navigate("movies/$id")

    /** [season] opens the series at that season instead of the next unseen episode's. */
    fun show(id: Int, season: Int? = null) =
        controller.navigate(if (season == null) "shows/$id" else "shows/$id?season=$season")

    fun collection(id: Int) = controller.navigate("collections/$id")

    /** Resumes at the saved position unless [fromStart]. */
    fun watch(videoId: Int, fromStart: Boolean = false) =
        controller.navigate(if (fromStart) "watch/$videoId?t=0" else "watch/$videoId")

    /** The next episode replaces the current player, so Back leads out of the player, not into the last episode. */
    fun watchNext(videoId: Int) = controller.navigate("watch/$videoId") {
        popUpTo(Routes.WATCH) { inclusive = true }
    }

    fun account() = controller.navigate(Routes.ACCOUNT)

    fun back() {
        controller.popBackStack()
    }
}
