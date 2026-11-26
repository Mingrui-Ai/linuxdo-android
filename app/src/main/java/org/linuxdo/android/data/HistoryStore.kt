package org.linuxdo.android.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ReadingHistoryEntry(
    val topicId: Long,
    val title: String,
    val lastSeenAt: Long,
    val lastSeenPostNumber: Int = 0,
    val authorUsername: String? = null,
    val avatarTemplate: String? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val totalReplies: Int = 0,
)

class HistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile
    private var currentUserId: Long = 0L

    @Synchronized
    fun switchUser(userId: Long) {
        currentUserId = maxOf(0L, userId)
    }

    private fun storageKey(userId: Long): String =
        if (userId > 0L) "${KEY_ENTRIES}_$userId" else KEY_ENTRIES

    @Synchronized
    fun entries(userId: Long = currentUserId): List<ReadingHistoryEntry> {
        val key = storageKey(userId)
        val raw = preferences.getString(key, null)
        if (raw == null && userId > 0L) {
            val legacy = preferences.getString(KEY_ENTRIES, null)
            if (!legacy.isNullOrBlank()) {
                preferences.edit {
                    putString(key, legacy)
                    remove(KEY_ENTRIES)
                }
                return runCatching {
                    json.decodeFromString<List<ReadingHistoryEntry>>(legacy)
                }.getOrDefault(emptyList()).sortedByDescending { it.lastSeenAt }
            }
        }
        return runCatching {
            json.decodeFromString<List<ReadingHistoryEntry>>(raw.orEmpty())
        }.getOrDefault(emptyList()).sortedByDescending { it.lastSeenAt }
    }

    @Synchronized
    fun record(
        topicId: Long,
        title: String,
        postNumber: Int = 0,
        authorUsername: String? = null,
        avatarTemplate: String? = null,
        categoryName: String? = null,
        categoryColor: String? = null,
        totalReplies: Int = 0,
        userId: Long = currentUserId,
    ) {
        if (topicId <= 0L || title.isBlank()) return
        val current = entries(userId).toMutableList()
        val previous = current.firstOrNull { it.topicId == topicId }
        current.removeAll { it.topicId == topicId }
        current.add(
            0,
            ReadingHistoryEntry(
                topicId = topicId,
                title = title,
                lastSeenAt = System.currentTimeMillis(),
                lastSeenPostNumber = maxOf(previous?.lastSeenPostNumber ?: 0, postNumber),
                authorUsername = authorUsername ?: previous?.authorUsername,
                avatarTemplate = avatarTemplate ?: previous?.avatarTemplate,
                categoryName = categoryName ?: previous?.categoryName,
                categoryColor = categoryColor ?: previous?.categoryColor,
                totalReplies = if (totalReplies > 0) totalReplies else (previous?.totalReplies ?: 0),
            ),
        )
        val raw = json.encodeToString(current.take(MAX_ENTRIES))
        preferences.edit { putString(storageKey(userId), raw) }
    }

    @Synchronized
    fun mergeRemote(userId: Long, incoming: List<ReadingHistoryEntry>, reset: Boolean, preserveOlder: Boolean = false): List<ReadingHistoryEntry> {
        val key = "remote_$userId"
        val previous = if (reset && !preserveOlder) emptyList() else preferences.getString(key, null)?.let {
            json.decodeFromString<List<ReadingHistoryEntry>>(it)
        }.orEmpty()
        val merged = (if (reset) incoming + previous else previous + incoming).distinctBy { it.topicId }
        preferences.edit {
            putString(key, json.encodeToString(merged))
            if (reset) putLong("remote_fetched_$userId", System.currentTimeMillis())
        }
        return syncedEntries(userId)
    }

    @Synchronized
    fun syncedEntries(userId: Long = currentUserId): List<ReadingHistoryEntry> {
        val local = entries(userId)
        val remote = preferences.getString("remote_$userId", null)?.let {
            json.decodeFromString<List<ReadingHistoryEntry>>(it)
        }.orEmpty()
        val localById = local.associateBy { it.topicId }
        val ids = remote.map { it.topicId }.toSet()
        val fetchedAt = preferences.getLong("remote_fetched_$userId", 0L)
        val recent = local.filter { it.lastSeenAt > fetchedAt }
        val recentIds = recent.map { it.topicId }.toSet()
        return recent + remote.filterNot { it.topicId in recentIds }.map { entry ->
            localById[entry.topicId]?.let { cached -> entry.copy(
                lastSeenAt = cached.lastSeenAt,
                lastSeenPostNumber = maxOf(entry.lastSeenPostNumber, cached.lastSeenPostNumber),
            ) } ?: entry
        } + local.filter { it.topicId !in ids && it.topicId !in recentIds }
    }

    @Synchronized
    fun clear(userId: Long = currentUserId) {
        preferences.edit { remove(storageKey(userId)); remove("remote_$userId") }
    }

    @Synchronized
    fun updateMetadata(userId: Long, topicId: Long, title: String, categoryName: String?, categoryColor: String?) {
        val current = entries(userId)
        val updated = current.map { if (it.topicId == topicId) it.copy(title = title, categoryName = categoryName, categoryColor = categoryColor) else it }
        if (updated != current) check(preferences.edit().putString(storageKey(userId), json.encodeToString(updated)).commit()) { "历史记录更新失败" }
    }

    private companion object {
        const val PREFERENCES = "reading_history"
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 500
    }
}

@Serializable
data class TopicReadingPosition(
    val postNumber: Int = 1,
    val partIndex: Int = 0,
    val offset: Int = 0,
    val atTop: Boolean = true,
    val filterUserId: Long? = null,
    val descending: Boolean = false,
    val sortByLikes: Boolean = false,
    val collapsed: List<Long> = emptyList(),
    val expanded: List<Long> = emptyList(),
) : java.io.Serializable
