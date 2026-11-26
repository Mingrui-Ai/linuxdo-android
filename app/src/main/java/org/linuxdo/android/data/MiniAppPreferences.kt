package org.linuxdo.android.data

import android.content.SharedPreferences

class MiniAppPreferences(private val preferences: SharedPreferences) {
    fun openMode(): MiniAppOpenMode = MiniAppOpenMode.fromKey(preferences.getString(KEY, null))

    fun setOpenMode(mode: MiniAppOpenMode) {
        preferences.edit().putString(KEY, mode.key).apply()
    }

    companion object {
        const val KEY = "mini_app_open_mode"
    }
}
