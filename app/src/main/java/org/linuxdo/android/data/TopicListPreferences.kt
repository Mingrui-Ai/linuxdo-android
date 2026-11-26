package org.linuxdo.android.data

import java.io.Serializable

/** 每个列表独立保存排序；旧全局设置无法判断来源，不迁移到所有列表。 */
data class TopicListSort(
    val order: TopicSortOrder = TopicSortOrder.Default,
    val ascending: Boolean = false,
) {
    fun select(selected: TopicSortOrder) = TopicListSort(
        selected, selected != TopicSortOrder.Default && selected == order && !ascending,
    )
    val apiAscending: Boolean? get() = ascending.takeIf { order.apiValue != null }
}

object TopicListKey {
    fun feed(feed: HomeFeedType) = "feed:${feed.name}"
    fun category(id: Int) = "category:$id"
    fun tag(tag: String) = "tag:$tag"
}

/** 由入口携带范围，输入框、历史关键词和翻页不能丢掉该约束。 */
data class TopicSearchScope(val categoryId: Int? = null, val categoryName: String? = null, val tag: String? = null) : Serializable {
    val label: String get() = categoryName ?: tag?.let { "# $it" }.orEmpty()

    fun applyTo(query: String): String {
        // 先清掉用户输入中同类范围，最后追加不可编辑的入口范围。
        val clean = query.replace(Regex("(?i)(?<!\\S)(?:categor(?:y|ies):|tags?:|#)\\S+"), " ").trim()
        return when {
            categoryId != null -> "$clean category:$categoryId"
            !tag.isNullOrBlank() -> "$clean tags:$tag"
            else -> query
        }
    }
}
