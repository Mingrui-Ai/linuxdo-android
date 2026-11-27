package org.linuxdo.android.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.DiscourseTagSearchResult
import org.linuxdo.android.data.HomeFilterResolver
import org.linuxdo.android.data.compactCategoryGroups
import org.linuxdo.android.data.creatableCategories
import org.linuxdo.android.ui.CategoriesUiState
import org.linuxdo.android.ui.IosTextAction
import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.ui.design.ForumTagIcon
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosTheme

internal enum class TopicCreatorMenu { Category, Level, Tags }

@Composable
internal fun TopicCreatorFields(
    categories: CategoriesUiState,
    categoryId: Int?,
    tags: List<String>,
    menu: TopicCreatorMenu?,
    onMenuChange: (TopicCreatorMenu?) -> Unit,
    onTogglePinCategory: (Long) -> Unit,
    onCategoryChange: (Int?) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onSearchTags: suspend (Int?, String, List<String>) -> DiscourseTagSearchResult,
) {
    val writable = remember(categories.items) { creatableCategories(categories.items) }
    val groups = remember(writable) { compactCategoryGroups(writable) }
    val group = remember(groups, categoryId) {
        groups.firstOrNull { (parent, levels) -> parent.id == categoryId || levels.any { it.id == categoryId } }
    }
    val variants = remember(group) { group?.let { listOf(it.first) + it.second }.orEmpty() }
    val levels = remember(variants) { variants.associateBy(HomeFilterResolver::effectiveLevel) }
    val current = variants.firstOrNull { it.id == categoryId }
    val level = current?.let(HomeFilterResolver::effectiveLevel)
    val colors = IosTheme.colors

    val select: (Category) -> Unit = { category ->
        onMenuChange(null)
        if (category.id != categoryId) {
            if (!category.allowGlobalTags && category.allowedTags.isNotEmpty() &&
                category.allowedTagGroups.isEmpty() && category.requiredTagGroups.isEmpty()) {
                val valid = tags.filter { it in category.allowedTags }
                if (valid != tags) onTagsChange(valid)
            }
            onCategoryChange(category.id)
        }
    }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box {
            val color = readableCategoryForeground(current?.color?.let(::parseCategoryColor) ?: colors.accent, colors.isDark)
            HomeFilterPill(
                text = current?.let { HomeFilterResolver.baseCategoryName(it.name) } ?: "选择分类",
                active = current != null,
                accentColor = color,
                leading = current?.let { { ForumCategoryIcon(it, 13.dp, color) } },
                onClick = { onMenuChange(TopicCreatorMenu.Category) },
            )
            HomeCategoryDropdownMenu(
                expanded = menu == TopicCreatorMenu.Category,
                categories = writable,
                loading = categories.loading,
                categoryOrder = categories.order,
                pinnedCategoryIds = categories.pinnedIds,
                selectedCategoryId = group?.first?.id,
                onDismiss = { onMenuChange(null) },
                onSelectCategory = { select(if (it.id == group?.first?.id && current != null) current else it) },
                onTogglePin = onTogglePinCategory,
            )
        }
        if (current != null) {
            Box {
                HomeFilterPill(text = "Lv.$level", active = true, selectedLevel = level,
                    onClick = { onMenuChange(TopicCreatorMenu.Level) })
                CategoryLevelDropdownMenu(
                    expanded = menu == TopicCreatorMenu.Level,
                    selectedLevel = level,
                    levelEnabled = { it in levels },
                    onDismiss = { onMenuChange(null) },
                    onSelectLevel = { lv -> levels[lv]?.let(select) },
                    optionLabel = { if (it in levels) "可发帖" else "不可选" },
                )
            }
        }
        Box {
            HomeFilterPill(
                text = when (tags.size) {
                    0 -> "标签"
                    1 -> tags.first()
                    else -> "${tags.size} 个标签"
                },
                active = tags.isNotEmpty(),
                leading = tags.firstOrNull()?.let { { ForumTagIcon(tag = it, size = 13.dp, fallbackHash = true) } },
                onClick = { onMenuChange(TopicCreatorMenu.Tags) },
            )
            if (menu == TopicCreatorMenu.Tags) {
                TopicTagMenu(current, tags, onTagsChange, onSearchTags, onDismiss = { onMenuChange(null) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicTagMenu(
    category: Category?,
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    onSearch: suspend (Int?, String, List<String>) -> DiscourseTagSearchResult,
    onDismiss: () -> Unit,
) {
    var query by remember(category?.id) { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    var result by remember(category?.id, query, tags, retry) { mutableStateOf<DiscourseTagSearchResult?>(null) }
    var failed by remember(category?.id, query, tags, retry) { mutableStateOf(false) }
    LaunchedEffect(category?.id, query, tags, retry) {
        if (query.isNotBlank()) delay(300)
        try {
            result = onSearch(category?.id, query.trim(), tags)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            failed = true
        }
    }
    val colors = IosTheme.colors
    DropdownMenu(expanded = true, onDismissRequest = onDismiss, offset = DpOffset(0.dp, 4.dp),
        shape = RoundedCornerShape(16.dp), containerColor = colors.card,
        modifier = Modifier.width(266.dp).heightIn(max = 330.dp)) {
        FilterMenuSearch(query, "搜索标签…", onChange = { query = it })
        IosFullDivider()
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val requirements = category?.requiredTagGroups.orEmpty().associate { it.name to it.minCount }.toMutableMap()
            result?.let { response ->
                response.requiredTagName?.takeIf { it.isNotBlank() }?.let { requirements[it] = response.requiredTagMinCount }
            }
            for ((name, minimum) in requirements) {
                Text("至少选择 $minimum 个「$name」标签", style = IosTheme.type.caption, color = colors.accent)
            }
            if ((category?.minimumRequiredTags ?: 0) > 0) {
                Text("此分类至少需要 ${category?.minimumRequiredTags} 个标签", style = IosTheme.type.caption, color = colors.secondaryLabel)
            }
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    tags.forEach { name -> HomeTagOptionChip(name, selected = true, onClick = { onTagsChange(tags - name) }) }
                }
                IosFullDivider()
            }
            val candidates = result?.results.orEmpty().filterNot { it.name in tags }
            when {
                failed -> IosTextAction("加载失败，点此重试", textStyle = IosTheme.type.footnote) { retry++ }
                result == null -> IosActivityIndicator(diameter = 16.dp)
                candidates.isEmpty() -> Text(if (query.isBlank()) "没有更多可选标签" else "未找到匹配标签",
                    style = IosTheme.type.footnote, color = colors.secondaryLabel)
                else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    candidates.forEach { tag -> HomeTagOptionChip(tag.text.ifBlank { tag.name }, tag.count,
                        selected = false, onClick = { onTagsChange(tags + tag.name) }) }
                }
            }
        }
    }
}
