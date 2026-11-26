package org.linuxdo.android.data

import java.net.URI
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileStatus(
    val emoji: String = "",
    val description: String = "",
    @SerialName("ends_at") val endsAt: String? = null,
) {
    fun isVisible(now: Instant): Boolean = emoji.isNotBlank() && description.isNotBlank() &&
        (endsAt == null || runCatching { Instant.parse(endsAt).isAfter(now) }.getOrDefault(false))
}

@Serializable
data class ProfileFeaturedTopic(val id: Long = 0, val title: String = "")

@Serializable
data class ProfileInviter(
    val username: String = "",
    val name: String? = null,
    @SerialName("avatar_template") val avatarTemplate: String? = null,
)

/** Account-only contact data must never be shown while viewing another user's cached card. */
internal fun visibleProfileEmail(card: UserCardData?, viewerId: Long): String? =
    card?.email?.trim()?.takeIf { it.isNotEmpty() && viewerId > 0 && card.id == viewerId }

/** 兼容旧版“找工作”快捷项保存的非 Discourse 名称。 */
internal fun normalizeProfileStatusEmoji(raw: String): String = raw.trim().trim(':').let {
    if (it == "magnifying_glass_tilted_left") "mag" else it
}

/** 只省略与当前 Lv 对应的默认头衔，保留自选徽章或自定义头衔。 */
internal fun profileDisplayTitle(title: String?, level: Int?): String? {
    val value = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val defaults = when (level) {
        0 -> setOf("新用户", "new user")
        1 -> setOf("基本用户", "basic user")
        2 -> setOf("成员", "member")
        3 -> setOf("活跃用户", "regular")
        4 -> setOf("领导者", "leader")
        else -> emptySet()
    }
    return value.takeUnless { it.lowercase() in defaults }
}

/** 资料网站仅作为普通 HTTP(S) 网页打开。 */
internal fun profileWebsiteUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        val uri = URI(value)
        value.takeIf { uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null }
    }.getOrNull()
}

internal fun visibleFollowCount(count: Int?, canSee: Boolean?): Int? =
    count?.takeIf { it >= 0 && canSee != false }
