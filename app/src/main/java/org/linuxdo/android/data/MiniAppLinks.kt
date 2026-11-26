package org.linuxdo.android.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.linuxdo.android.Config

/** 应用广场及其中的小程序由系统浏览器/对应应用打开，移除仅供内嵌容器使用的参数。 */
fun externalMiniAppUrl(value: String?): String? {
    val url = value?.trim()?.toHttpUrlOrNull() ?: return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    val host = url.host.lowercase().trimEnd('.')
    if (host != Config.HOST && !host.endsWith(".${Config.HOST}")) return null
    if (!url.pathSegments.firstOrNull().equals("apps", ignoreCase = true)) return null
    return url.newBuilder().removeAllQueryParameters("embed_mode").build().toString()
}
