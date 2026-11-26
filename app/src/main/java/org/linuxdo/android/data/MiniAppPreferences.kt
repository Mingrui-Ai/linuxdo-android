package org.linuxdo.android.data

import android.content.SharedPreferences

class MiniAppPreferences(private val preferences: SharedPreferences) {
    fun openMode(): MiniAppOpenMode = MiniAppOpenMode.fromKey(preferences.getString(KEY, null))

    fun setOpenMode(mode: MiniAppOpenMode) {
        preferences.edit().putString(KEY, mode.key).apply()
    }

    fun homePullEnabled(): Boolean = preferences.getBoolean(HOME_PULL_KEY, false)

    fun setHomePullEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(HOME_PULL_KEY, enabled).apply()
    }

    companion object {
        const val KEY = "mini_app_open_mode"
        const val HOME_PULL_KEY = "mini_app_home_pull_enabled"
    }
}
