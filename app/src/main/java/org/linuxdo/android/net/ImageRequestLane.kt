package org.linuxdo.android.net

import okhttp3.HttpUrl
import org.linuxdo.android.Config

/** 沿用站内图片限额；调度队列与下载中的并发使用同一组上限。 */
internal enum class ImageRequestLane(val concurrency: Int) {
    EMOJI(12), AVATAR(3), MEDIA(4),
}

internal fun siteImageLane(url: HttpUrl, isSignature: Boolean = false): ImageRequestLane? {
    val host = url.host.lowercase().trimEnd('.')
    val isLinuxDo = host == Config.HOST || host.endsWith(".${Config.HOST}")
    val isLdStatic = host == "ldstatic.com" || host.endsWith(".ldstatic.com")
    if (!isLinuxDo && !isLdStatic) return null
    val path = url.encodedPath
    return when {
        path.startsWith("/images/emoji/") || path.startsWith("/svg-sprite/") -> ImageRequestLane.EMOJI
        path.startsWith("/user_avatar/") || path.startsWith("/letter_avatar") -> ImageRequestLane.AVATAR
        isSignature || path.startsWith("/uploads/") || path.startsWith("/images/") -> ImageRequestLane.MEDIA
        imageExtensions.any { path.endsWith(it, ignoreCase = true) } || isLdStatic -> ImageRequestLane.MEDIA
        else -> null
    }
}

private val imageExtensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".avif", ".svg", ".ico")
