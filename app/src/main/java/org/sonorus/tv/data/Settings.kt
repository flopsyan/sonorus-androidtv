package org.sonorus.tv.data

import android.content.Context

/** Switches that belong to this TV rather than to the account. */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("sonorus_local", Context.MODE_PRIVATE)

    /** Films zoomed past their burnt-in black bars. */
    var videoFill: Boolean
        get() = prefs.getBoolean(KEY_VIDEO_FILL, false)
        set(value) = prefs.edit().putBoolean(KEY_VIDEO_FILL, value).apply()

    private companion object {
        const val KEY_VIDEO_FILL = "videoFill"
    }
}
