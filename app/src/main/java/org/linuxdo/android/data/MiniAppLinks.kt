package org.linuxdo.android.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.linuxdo.android.Config

/** 小程序的公开地址；外部打开时移除仅供内嵌容器使用的参数。 */
fun externalMiniAppUrl(value: String?): String? {
    val url = value?.trim()?.toHttpUrlOrNull() ?: return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    val host = url.host.lowercase().trimEnd('.')
    if (host != Config.HOST && !host.endsWith(".${Config.HOST}")) return null
    if (!url.pathSegments.firstOrNull().equals("apps", ignoreCase = true)) return null
    return url.newBuilder().removeAllQueryParameters("embed_mode").build().toString()
}

enum class MiniAppOpenMode(val key: String, val label: String) {
    Internal("internal", "内置浏览器"),
    External("external", "外部浏览器");

    companion object {
        // 升级沿用原来的外部打开方式。
        fun fromKey(key: String?): MiniAppOpenMode = entries.firstOrNull { it.key == key } ?: External
    }
}

enum class MiniAppDestination { Launcher, Internal, External }

/** 广场始终显示本地常用页，打开具体小程序时才应用浏览器偏好。 */
fun miniAppDestination(value: String?, mode: MiniAppOpenMode): MiniAppDestination? {
    val url = externalMiniAppUrl(value)?.toHttpUrlOrNull() ?: return null
    if (url.pathSegments.dropLastWhile(String::isEmpty).size == 1) return MiniAppDestination.Launcher
    return if (mode == MiniAppOpenMode.Internal) MiniAppDestination.Internal else MiniAppDestination.External
}
