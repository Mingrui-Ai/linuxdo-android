package org.linuxdo.android.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** 与网页共用 draft_key；只比较可编辑内容，服务端额外字段原样保留。 */
@Serializable
data class SyncedDraftContent(
    val raw: String = "",
    val title: String = "",
    val categoryId: Int? = null,
    val tags: List<String> = emptyList(),
    val action: String = "createTopic",
    val topicId: Long? = null,
    val replyToPostNumber: Int? = null,
    val postId: Long? = null,
    val archetype: String = "regular",
) {
    val empty: Boolean get() = raw.isBlank() && (action !in setOf("createTopic", "createPrivateMessage") || title.isBlank())
}

data class RemoteDraft(val sequence: Long, val content: SyncedDraftContent?, val data: String? = null)

internal fun decodeSyncedDraft(key: String, data: String): SyncedDraftContent {
    val obj = Json.parseToJsonElement(data).jsonObject
    fun text(name: String) = (obj[name] as? JsonPrimitive)?.contentOrNull.orEmpty()
    return SyncedDraftContent(
        raw = text("reply"), title = text("title"),
        categoryId = text("categoryId").toIntOrNull() ?: text("category_id").toIntOrNull(),
        tags = (obj["tags"] as? JsonArray).orEmpty().mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull ?: ((it as? JsonObject)?.get("name") as? JsonPrimitive)?.contentOrNull
        },
        action = text("action").ifBlank { if (key.startsWith("topic_")) "reply" else "createTopic" },
        topicId = text("topic_id").toLongOrNull() ?: key.removePrefix("topic_").toLongOrNull(),
        replyToPostNumber = text("reply_to_post_number").toIntOrNull(),
        postId = text("postId").toLongOrNull(),
        archetype = text("archetypeId").ifBlank { "regular" },
    )
}

internal fun encodeSyncedDraft(content: SyncedDraftContent, remoteData: String?): String {
    val data = remoteData?.let { Json.parseToJsonElement(it).jsonObject.toMutableMap() } ?: mutableMapOf()
    data["reply"] = JsonPrimitive(content.raw)
    data["title"] = JsonPrimitive(content.title)
    data["action"] = JsonPrimitive(content.action)
    data["categoryId"] = content.categoryId?.let { JsonPrimitive(it) } ?: JsonNull
    data["tags"] = JsonArray(content.tags.map { JsonPrimitive(it) })
    data["archetypeId"] = JsonPrimitive(content.archetype)
    data["topic_id"] = content.topicId?.let { JsonPrimitive(it) } ?: JsonNull
    data["reply_to_post_number"] = content.replyToPostNumber?.let { JsonPrimitive(it) } ?: JsonNull
    data["postId"] = content.postId?.let { JsonPrimitive(it) } ?: JsonNull
    return JsonObject(data).toString()
}

@Serializable
data class LocalSyncedDraft(
    val key: String,
    val content: SyncedDraftContent,
    val base: SyncedDraftContent? = null,
    val sequence: Long? = null,
    val remoteData: String? = null,
    val dirty: Boolean = false,
    val conflict: Boolean = false,
    val updatedAt: Long = 0,
)

enum class DraftMergeAction { UseRemote, SaveLocal, Conflict, Unchanged }

/** 三方合并：同一基线上的单端修改可同步，两端都改动时绝不覆盖。 */
fun draftMergeAction(local: LocalSyncedDraft?, remote: RemoteDraft): DraftMergeAction = when {
    local == null || !local.dirty -> DraftMergeAction.UseRemote
    local.content.takeUnless { it.empty } == remote.content?.takeUnless { it.empty } -> DraftMergeAction.Unchanged
    local.sequence == null && remote.content == null -> DraftMergeAction.SaveLocal
    local.sequence != null && local.base == remote.content -> DraftMergeAction.SaveLocal
    else -> DraftMergeAction.Conflict
}

/** 未发送内容加密并按账号存储；同步失败和进程重启均不丢稿。 */
class AccountDraftStore(context: Context) {
    private val prefs = SafeEncryptedPrefs.open(context, "account_draft_sync")
    private val json = Json { ignoreUnknownKeys = true }

    fun legacyImported(userId: Long, key: String): Boolean = prefs.getString("legacy_${userId}_$key", null) != null
    fun markLegacyImported(userId: Long, key: String) {
        check(prefs.edit().putString("legacy_${userId}_$key", "done").commit())
    }

    @Synchronized
    fun entries(userId: Long): List<LocalSyncedDraft> {
        if (userId <= 0) return emptyList()
        return prefs.getString("drafts_$userId", null)?.let {
            json.decodeFromString<List<LocalSyncedDraft>>(it)
        }.orEmpty()
    }

    @Synchronized
    fun get(userId: Long, key: String): LocalSyncedDraft? = entries(userId).firstOrNull { it.key == key }

    @Synchronized
    fun put(userId: Long, value: LocalSyncedDraft) {
        require(userId > 0)
        val updated = entries(userId).filterNot { it.key == value.key } + value
        check(prefs.edit().putString("drafts_$userId", json.encodeToString(updated)).commit()) { "草稿尚未保存到手机" }
    }

    @Synchronized
    fun remove(userId: Long, key: String) {
        if (userId <= 0) return
        check(prefs.edit().putString("drafts_$userId", json.encodeToString(entries(userId).filterNot { it.key == key })).commit())
    }
}

@Serializable
data class PendingRead(val id: String, val topicId: Long, val topicTimeMs: Long, val postTimesMs: Map<Int, Long>)

/** 每批有独立 ID；成功只移除本批，不误删请求期间新增的阅读。 */
class ReadSyncOutbox(context: Context) {
    private val prefs = context.getSharedPreferences("read_sync_outbox", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun entries(userId: Long): List<PendingRead> = if (userId <= 0) emptyList() else
        prefs.getString("pending_$userId", null)?.let { json.decodeFromString<List<PendingRead>>(it) }.orEmpty()

    @Synchronized
    internal fun enqueue(userId: Long, topicId: Long, batch: ReadTimingBatch) {
        if (userId <= 0 || batch.postTimesMs.isEmpty()) return
        write(userId, entries(userId) + PendingRead(java.util.UUID.randomUUID().toString(), topicId, batch.topicTimeMs, batch.postTimesMs))
    }

    @Synchronized
    fun acknowledge(userId: Long, id: String) = write(userId, entries(userId).filterNot { it.id == id })

    @Synchronized
    fun clear(userId: Long) = write(userId, emptyList())

    private fun write(userId: Long, entries: List<PendingRead>) {
        if (userId <= 0) return
        check(prefs.edit().putString("pending_$userId", json.encodeToString(entries)).commit()) { "阅读记录保存失败" }
    }
}
