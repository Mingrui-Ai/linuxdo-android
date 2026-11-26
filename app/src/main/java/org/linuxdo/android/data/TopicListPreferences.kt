package org.linuxdo.android.data

import java.io.Serializable

/** 每个列表独立保存排序；旧全局设置无法判断来源，不迁移到所有列表。 */
data class TopicListSort(
    val order: TopicSortOrder = TopicSortOrder.Default,
    val ascending: Boolean = false,
) {
    fun select(selected: TopicSortOrder) = TopicListSort(
        selected, selected.supportsAscending && selected == order && !ascending,
    )
    val apiAscending: Boolean? get() = ascending.takeIf { order.supportsAscending && order.apiValue != null }
}

/** 首页筛选条件：支持类别、LV 等级、标签三维度自由组合，均可独立选择或清除。 */
data class HomeFilterState(
    val categoryId: Int? = null,
    val categorySlug: String? = null,
    val categoryName: String? = null,
    val level: Int? = null,
    val tag: String? = null,
) : Serializable {
    val isActive: Boolean get() = categoryId != null || level != null || !tag.isNullOrBlank()
    val activeCount: Int get() = (if (categoryId != null) 1 else 0) + (if (level != null) 1 else 0) + (if (!tag.isNullOrBlank()) 1 else 0)
    val hasCategoryOrLevel: Boolean get() = categoryId != null || level != null

    /** 生成当前首页筛选对应的排序持久化键，不同筛选组合可独立记忆排序。 */
    fun sortKey(): String = when {
        !isActive -> TopicListKey.feed(HomeFeedType.Latest)
        else -> buildString {
            append("home")
            if (categoryId != null) append(":c$categoryId")
            if (level != null) append(":lv$level")
            if (!tag.isNullOrBlank()) append(":t${tag.trim()}")
        }
    }
}

/** 根据已加载的分类树解析某个等级子版块列表及父分类对应的等级映射。 */
object HomeFilterResolver {
    private val LEVEL_SUFFIX_REGEX = Regex("[,，\\s]*Lv\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)

    fun extractLevel(category: Category): Int? =
        LEVEL_SUFFIX_REGEX.find(category.name)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..3 }

    fun baseCategoryName(name: String): String =
        name.replace(LEVEL_SUFFIX_REGEX, "").trim().ifBlank { name }

    /** 解析父类别在指定等级下对应的真实子类别；无精确匹配时返回 null。 */
    fun resolveCategoryWithLevel(categories: List<Category>, parentCategoryId: Int, level: Int?): Category? {
        val groups = compactCategoryGroups(categories)
        val group = groups.firstOrNull { (parent, levels) ->
            parent.id == parentCategoryId || levels.any { it.id == parentCategoryId }
        }
        if (group == null) return flattenCategories(categories).firstOrNull { it.id == parentCategoryId }
        val (parent, levels) = group
        if (level == null) return parent
        // 某些版块父分类本身即 Lv.0（或无 Lv.0 子类而直接放在父分类下）
        val matched = levels.firstOrNull { extractLevel(it) == level }
        if (matched != null) return matched
        if (level == 0 && levels.isNotEmpty() && extractLevel(parent) == null) return parent
        return null
    }

    /** 获取指定类别支持的等级集合（升序）。 */
    fun supportedLevelsForCategory(categories: List<Category>, categoryId: Int): List<Int> {
        val group = compactCategoryGroups(categories).firstOrNull { (parent, levels) ->
            parent.id == categoryId || levels.any { it.id == categoryId }
        } ?: return emptyList()
        val explicit = group.second.mapNotNull(::extractLevel).distinct().sorted()
        return explicit
    }

    /** 独立按 LV 等级筛选（未选类别）时，返回全站所有该等级的子版块列表。 */
    fun categoriesForLevel(categories: List<Category>, level: Int): List<Category> {
        val result = mutableListOf<Category>()
        compactCategoryGroups(categories).forEach { (parent, levels) ->
            val matched = levels.firstOrNull { extractLevel(it) == level }
            if (matched != null) {
                result.add(matched)
            } else if (level == 0 && levels.isNotEmpty() && extractLevel(parent) == null) {
                // 没有独立 Lv.0 子分类时，不混入普通无等级版块，仅收录真实存在的 Lv.0 子版块
            }
        }
        return result.distinctBy { it.id }
    }

    /** 根据当前筛选状态生成搜索范围，用于首页右上角搜索按钮。 */
    fun toSearchScope(filter: HomeFilterState, categories: List<Category>): TopicSearchScope? {
        if (!filter.isActive) return null
        val resolvedCat = if (filter.categoryId != null) {
            resolveCategoryWithLevel(categories, filter.categoryId, filter.level)
        } else null
        val catId = resolvedCat?.id ?: filter.categoryId
        val catName = when {
            resolvedCat != null -> resolvedCat.name
            filter.categoryName != null && filter.level != null -> "${filter.categoryName} · Lv.${filter.level}"
            filter.categoryName != null -> filter.categoryName
            filter.level != null -> "Lv.${filter.level}"
            else -> null
        }
        val levelIds = if (filter.categoryId == null && filter.level != null) {
            categoriesForLevel(categories, filter.level).map { it.id }
        } else emptyList()
        return TopicSearchScope(
            categoryId = catId,
            categoryName = catName,
            tag = filter.tag?.takeIf { it.isNotBlank() },
            categoryIds = levelIds,
        )
    }
}

object TopicListKey {
    fun feed(feed: HomeFeedType) = "feed:${feed.name}"
    fun category(id: Int) = "category:$id"
    fun tag(tag: String) = "tag:$tag"
}

/** 由入口携带范围，输入框、历史关键词和翻页不能丢掉该约束。 */
data class TopicSearchScope(
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val tag: String? = null,
    val categoryIds: List<Int> = emptyList(),
) : Serializable {
    val label: String
        get() = buildList {
            if (!categoryName.isNullOrBlank()) add(categoryName)
            if (!tag.isNullOrBlank()) add("# $tag")
        }.joinToString(" · ")

    fun applyTo(query: String): String {
        // 先清掉用户输入中同类范围，最后追加不可编辑的入口范围。
        val clean = query.replace(Regex("(?i)(?<!\\S)(?:categor(?:y|ies):|tags?:|#)\\S+"), " ").trim()
        val parts = mutableListOf<String>()
        if (clean.isNotEmpty()) parts.add(clean)
        when {
            categoryId != null -> parts.add("category:$categoryId")
            categoryIds.isNotEmpty() -> parts.add("category:${categoryIds.joinToString(",")}")
        }
        if (!tag.isNullOrBlank()) parts.add("tags:$tag")
        return if (parts.isEmpty()) query else parts.joinToString(" ")
    }
}

/** 对类别分组应用「置顶优先 + 自定义顺序」：置顶项排在最前，各自内部遵循自定义顺序。 */
fun arrangeCategoryGroups(
    categories: List<Category>,
    pinnedIds: List<Long>,
    customOrder: List<Long>,
): List<Pair<Category, List<Category>>> {
    val base = orderedCategoryGroups(categories, customOrder)
    if (pinnedIds.isEmpty()) return base
    val pinnedRank = pinnedIds.distinct().withIndex().associate { it.value to it.index }
    val pinned = base.filter { it.first.id.toLong() in pinnedRank }
        .sortedBy { pinnedRank[it.first.id.toLong()] ?: Int.MAX_VALUE }
    val unpinned = base.filterNot { it.first.id.toLong() in pinnedRank }
    return pinned + unpinned
}

/** 对标签列表应用「置顶优先 + 自定义顺序」：置顶项排在最前，其余按自定义顺序再接原热门顺序。 */
fun arrangeTags(
    tags: List<DiscourseTag>,
    pinnedTags: List<String>,
    customOrder: List<String>,
): List<DiscourseTag> {
    if (pinnedTags.isEmpty() && customOrder.isEmpty()) return tags
    val byKey = tags.associateBy { it.displayName.lowercase() }
    val pinnedRank = pinnedTags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        .distinct().withIndex().associate { it.value to it.index }
    val orderRank = customOrder.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        .distinct().withIndex().associate { it.value to it.index }

    val pinned = pinnedTags.mapNotNull { name ->
        val k = name.trim().lowercase()
        byKey[k] ?: name.trim().takeIf { it.isNotEmpty() }?.let { DiscourseTag(id = it, name = it, text = it) }
    }.distinctBy { it.displayName.lowercase() }
        .sortedBy { pinnedRank[it.displayName.lowercase()] ?: Int.MAX_VALUE }

    val unpinned = tags.filterNot { it.displayName.lowercase() in pinnedRank }
        .sortedBy { orderRank[it.displayName.lowercase()] ?: Int.MAX_VALUE }
    return pinned + unpinned
}

/** 合并独立 LV 等级聚合拉取的多页话题列表，去重并按当前排序规则排序。 */
fun mergeAggregatedTopicPages(pages: List<TopicPage>, sort: TopicListSort): List<TopicListItem> {
    val deduped = pages.flatMap { it.items }.distinctBy { it.topic.id }
    val dir = if (sort.ascending && sort.order.supportsAscending) 1 else -1
    return when (sort.order) {
        TopicSortOrder.Created -> deduped.sortedWith(
            compareBy<TopicListItem> { it.topic.createdAt ?: "" }
                .let { if (dir < 0) it.reversed() else it },
        )
        TopicSortOrder.Likes -> deduped.sortedWith(
            compareBy<TopicListItem> { it.topic.likeCount }
                .thenBy { it.topic.bumpedAt ?: it.topic.lastPostedAt ?: "" }
                .let { if (dir < 0) it.reversed() else it },
        )
        TopicSortOrder.Views -> deduped.sortedWith(
            compareBy<TopicListItem> { it.topic.views }
                .thenBy { it.topic.bumpedAt ?: it.topic.lastPostedAt ?: "" }
                .let { if (dir < 0) it.reversed() else it },
        )
        TopicSortOrder.Hot -> deduped.sortedWith(
            compareByDescending<TopicListItem> { it.topic.likeCount * 3 + it.topic.replyCount * 2 + it.topic.views / 100 }
                .thenByDescending { it.topic.bumpedAt ?: it.topic.lastPostedAt ?: "" },
        )
        TopicSortOrder.Default -> deduped.sortedByDescending {
            it.topic.bumpedAt ?: it.topic.lastPostedAt ?: it.topic.createdAt ?: ""
        }
    }
}
