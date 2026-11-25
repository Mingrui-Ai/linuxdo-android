package org.linuxdo.android.net

/** 用户主动选择的联网方式；系统 VPN 仍由 Android 决定是否接管底层流量。 */
enum class NetworkMode(val preferenceValue: String, val label: String) {
    BUILT_IN("built_in", "内置网络"),
    SYSTEM("system", "跟随系统");

    companion object {
        fun fromPreference(value: String?): NetworkMode? = entries.firstOrNull { it.preferenceValue == value }

        /** 仅在首次升级时沿用旧版当时实际生效的路径，此后不随 VPN 开关改变用户选择。 */
        fun fromLegacy(isVpn: Boolean, isProxy: Boolean, forceOverVpn: Boolean): NetworkMode =
            if (isProxy || (isVpn && !forceOverVpn)) SYSTEM else BUILT_IN
    }
}
