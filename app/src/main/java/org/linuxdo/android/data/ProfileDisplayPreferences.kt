package org.linuxdo.android.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stable keys keep display choices independent of labels, accounts and server profile data. */
enum class ProfileDisplayField(val key: String) {
    Background("background"), Status("status"), Title("title"), Level("level"),
    Bio("bio"), CommunityScore("community_score"), Badges("badges"),
    JoinedAt("joined_at"), LastActive("last_active"), FollowCounts("follow_counts"),
    Website("website"), FeaturedTopic("featured_topic"),
    Inviter("inviter"), Email("email"), Points("points"),
    DaysVisited("days_visited"), TimeRead("time_read"), PostsRead("posts_read"),
    TopicsEntered("topics_entered"), TopicCount("topic_count"), PostCount("post_count"),
    LikesReceived("likes_received"), LikesGiven("likes_given"),
    RecentTimeRead("recent_time_read"), Solutions("solutions"), Bookmarks("bookmarks"),
}

data class ProfileDisplayPreferences(val hiddenFields: Set<ProfileDisplayField> = emptySet()) {
    operator fun get(field: ProfileDisplayField): Boolean = field !in hiddenFields
}

/** Local presentation only: never writes account/profile APIs or clears their cached values. */
class ProfileDisplayStore(private val preferences: SharedPreferences) {
    private val state = MutableStateFlow(ProfileDisplayPreferences(
        ProfileDisplayField.entries.filterTo(mutableSetOf()) {
            !preferences.getBoolean("profile_display_${it.key}", true)
        },
    ))
    val settings = state.asStateFlow()

    @Synchronized
    fun setVisible(field: ProfileDisplayField, visible: Boolean) {
        preferences.edit().putBoolean("profile_display_${field.key}", visible).apply()
        state.value = ProfileDisplayPreferences(
            if (visible) state.value.hiddenFields - field else state.value.hiddenFields + field,
        )
    }

    @Synchronized
    fun restoreDefaults() {
        preferences.edit().apply {
            ProfileDisplayField.entries.forEach { remove("profile_display_${it.key}") }
        }.apply()
        state.value = ProfileDisplayPreferences()
    }
}
