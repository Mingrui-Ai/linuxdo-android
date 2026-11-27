package org.linuxdo.android.data

import java.time.ZonedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

enum class UserNotificationLevel(val apiValue: String, val label: String, val description: String) {
    Normal("normal", "常规", "接收此用户的私信、通知和提及"),
    Mute("mute", "免打扰", "不接收此用户的私信、通知和提及"),
    Ignore("ignore", "忽略", "隐藏此用户的回复，并停止接收其私信、通知和提及"),
}

@Serializable
data class UserNotificationSettings(
    val id: Long,
    val username: String,
    val muted: Boolean,
    val ignored: Boolean,
    @SerialName("can_mute_user") val canMute: Boolean,
    @SerialName("can_ignore_user") val canIgnore: Boolean,
) {
    val level: UserNotificationLevel get() = when {
        ignored -> UserNotificationLevel.Ignore
        muted -> UserNotificationLevel.Mute
        else -> UserNotificationLevel.Normal
    }

    fun allows(level: UserNotificationLevel): Boolean = when (level) {
        UserNotificationLevel.Normal -> true
        UserNotificationLevel.Mute -> canMute
        UserNotificationLevel.Ignore -> canIgnore
    }

    fun withLevel(level: UserNotificationLevel) = copy(
        muted = level == UserNotificationLevel.Mute,
        ignored = level == UserNotificationLevel.Ignore,
    )
}

/** Calendar shortcuts use the same start-of-day/month and 1000-year “forever” as Discourse. */
enum class UserIgnoreDuration(val label: String) {
    Tomorrow("明天"), TwoWeeks("两周后"), NextMonth("下个月"),
    TwoMonths("两个月后"), ThreeMonths("三个月后"), FourMonths("四个月后"),
    SixMonths("六个月后"), OneYear("一年后"), Forever("永久");

    fun expiresAt(now: ZonedDateTime = ZonedDateTime.now()): String {
        val date = when (this) {
            Tomorrow -> now.plusDays(1)
            TwoWeeks -> now.plusWeeks(2).let {
                if (it.dayOfWeek == java.time.DayOfWeek.SUNDAY) it.plusDays(1) else it.with(java.time.DayOfWeek.MONDAY)
            }
            NextMonth -> now.plusMonths(1).withDayOfMonth(1)
            TwoMonths -> now.plusMonths(2).withDayOfMonth(1)
            ThreeMonths -> now.plusMonths(3).withDayOfMonth(1)
            FourMonths -> now.plusMonths(4).withDayOfMonth(1)
            SixMonths -> now.plusMonths(6).withDayOfMonth(1)
            OneYear -> now.plusYears(1).withDayOfMonth(1)
            Forever -> now.plusYears(1000).withDayOfMonth(1)
        }
        return date.withHour(8).withMinute(0).withSecond(0).withNano(0).toInstant().toString()
    }
}

internal fun userNotificationPayload(
    settings: UserNotificationSettings,
    level: UserNotificationLevel,
    expiringAt: String?,
    actingUserId: Long,
    now: java.time.Instant = java.time.Instant.now(),
): JsonObject {
    require(actingUserId > 0 && settings.id > 0 && settings.id != actingUserId) { "不能修改自己的通知设置" }
    require(settings.username.isNotBlank() && settings.allows(level)) { "当前账号无权使用此通知设置" }
    if (level == UserNotificationLevel.Ignore) {
        require(!expiringAt.isNullOrBlank() && runCatching {
            java.time.Instant.parse(expiringAt).isAfter(now)
        }.getOrDefault(false)) { "请选择有效的忽略截止时间" }
    }
    return buildJsonObject {
        put("notification_level", level.apiValue)
        put("acting_user_id", actingUserId)
        if (level == UserNotificationLevel.Ignore) put("expiring_at", expiringAt)
    }
}
