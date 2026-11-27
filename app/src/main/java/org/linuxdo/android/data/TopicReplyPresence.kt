package org.linuxdo.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.longOrNull

internal fun topicReplyPresenceName(topicId: Long) = "/discourse-presence/reply/$topicId"
internal fun topicReplyPresenceBusChannel(topicId: Long) = "/presence${topicReplyPresenceName(topicId)}"

@Serializable
internal data class TopicReplyPresenceSnapshot(
    @SerialName("last_message_id") val lastMessageId: Long,
    val users: List<DiscourseUser> = emptyList(),
)

@Serializable
internal data class TopicReplyPresenceDelta(
    @SerialName("entering_users") val entering: List<DiscourseUser>? = null,
    @SerialName("leaving_user_ids") val leaving: List<Long>? = null,
)

/** 官方 Presence 快照 + 连续消息游标；漏消息时清空并重新取快照，避免显示已经离开的用户。 */
internal class TopicReplyPresence {
    var snapshot: TopicReplyPresenceSnapshot? = null
        private set

    fun reset() { snapshot = null }

    fun replace(value: TopicReplyPresenceSnapshot) {
        snapshot = value.copy(users = value.users.filter { it.id > 0 && it.username.isNotBlank() }.distinctBy { it.id })
    }

    fun others(currentUserId: Long): List<DiscourseUser> =
        snapshot?.users.orEmpty().filter { it.id != currentUserId }

    fun apply(topicId: Long, message: TopicBusMessage, json: Json): Boolean {
        val current = snapshot ?: return false
        if (message.channel == "/__status") {
            val cursor = ((message.data as? JsonObject)?.get(topicReplyPresenceBusChannel(topicId)) as? JsonPrimitive)?.longOrNull
            if (cursor != null && cursor != current.lastMessageId) reset()
        } else if (message.channel == topicReplyPresenceBusChannel(topicId)) {
            if (message.messageId <= current.lastMessageId) return true
            if (message.messageId != current.lastMessageId + 1) {
                reset()
            } else {
                val delta = runCatching { json.decodeFromJsonElement<TopicReplyPresenceDelta>(message.data) }.getOrNull()
                if (delta == null || (delta.entering == null && delta.leaving == null)) reset()
                else replace(TopicReplyPresenceSnapshot(message.messageId,
                    (current.users + delta.entering.orEmpty()).filterNot { it.id in delta.leaving.orEmpty() }))
            }
        }
        return snapshot != null
    }
}
