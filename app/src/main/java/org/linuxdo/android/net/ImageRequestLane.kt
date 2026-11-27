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

/** 小头像即使几毫秒就返回，下一张主站图片也要错开发送；限流恢复后逐张加载。 */
internal class ImageSendPacer {
    private var lastSend: Long? = null
    private var recoveryUntil = 0L

    @Synchronized
    fun recoverAfter(cooldownUntil: Long) {
        recoveryUntil = maxOf(recoveryUntil, cooldownUntil + 60_000L)
    }

    @Synchronized
    fun reserveOrWait(now: Long): Long {
        val spacing = if (now < recoveryUntil) 750L else 200L
        val wait = (lastSend?.let { it + spacing - now } ?: 0L).coerceAtLeast(0L)
        if (wait == 0L) lastSend = now
        return wait
    }
}
