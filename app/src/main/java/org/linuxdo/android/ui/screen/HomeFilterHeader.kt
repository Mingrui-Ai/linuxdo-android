package org.linuxdo.android.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.DiscourseTag
import org.linuxdo.android.data.HomeFilterResolver
import org.linuxdo.android.data.HomeFilterState
import org.linuxdo.android.data.TopicSortOrder
import org.linuxdo.android.data.arrangeCategoryGroups
import org.linuxdo.android.data.arrangeTags
import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.ui.design.ForumTagIcon
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosTheme

/**
 * 首页两层导航头：
 * - 第一行（主顶栏）：头像 + LINUX DO + 动态排序图标 + 搜索（随上滑收起，下滑展开）
 * - 第二行（筛选栏）：类别 + 等级 + 标签 + 重置（默认随主顶栏一起收起；激活筛选时保留常驻，便于随时调整或清除）
 */
@Composable
internal fun HomeTopFilterHeader(
    topBarVisible: Boolean,
    filter: HomeFilterState,
    sortOrder: TopicSortOrder,
    sortAscending: Boolean,
    categories: List<Category>,
    categoriesLoading: Boolean,
    categoryOrder: List<Long>,
    pinnedCategoryIds: List<Long>,
    tags: List<DiscourseTag>,
    tagsLoading: Boolean,
    searchedTags: List<DiscourseTag>,
    pinnedTags: List<String>,
    tagOrder: List<String>,
    tagSearchQuery: String,
    leading: @Composable () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onSelectLevel: (Int?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onTogglePinCategory: (Long) -> Unit,
    onSaveCategoryOrder: (List<Long>) -> Unit,
    onResetCategoryOrder: () -> Unit,
    onTogglePinTag: (String) -> Unit,
    onSaveTagOrder: (List<String>) -> Unit,
    onResetTagOrder: () -> Unit,
    onEnsureCategories: () -> Unit,
    onEnsureTags: () -> Unit,
    onTagSearchQueryChange: (String) -> Unit,
    onSortChange: (TopicSortOrder) -> Unit,
    onSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showLevelMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // 激活任一筛选时，即使列表上滑隐藏了主顶栏，二级筛选栏依然保留可见
    val filterRowVisible = topBarVisible || filter.isActive

    val selectedCategory = remember(categories, filter.categoryId) {
        val id = filter.categoryId ?: return@remember null
        HomeFilterResolver.resolveCategoryWithLevel(categories, id, null)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IosTheme.colors.groupedBackground),
    ) {
        // 第一行：头像 + LINUX DO + 动态排序按钮 + 搜索（上滑时收起）
        AnimatedVisibility(
            visible = topBarVisible,
            enter = expandVertically(tween(220), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top) + fadeOut(tween(160)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 10.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading()
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    LinuxDoBrandWordmark()
                    if (sortOrder != TopicSortOrder.Default) {
                        val badgeText = sortOrder.label + if (sortAscending && sortOrder.supportsAscending) " ↑" else ""
                        Text(
                            text = badgeText,
                            style = IosTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = IosTheme.colors.accent,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(IosTheme.colors.accent.copy(alpha = 0.12f))
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TopicSortDynamicButton(
                            sortOrder = sortOrder,
                            ascending = sortAscending,
                            onClick = { showSortMenu = true },
                        )
                        TopicSortDropdownMenu(
                            expanded = showSortMenu,
                            currentOrder = sortOrder,
                            ascending = sortAscending,
                            onDismiss = { showSortMenu = false },
                            onSelectOrder = { order ->
                                showSortMenu = false
                                onSortChange(order)
                            },
                        )
                    }
                    if (onSearch != null) {
                        IconButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = IosTheme.colors.label)
                        }
                    }
                }
            }
        }

        // 第二行：类别 + LV 等级 + 标签 + 重置（未筛选时随顶栏一起收起；筛选中始终保留）
        AnimatedVisibility(
            visible = filterRowVisible,
            enter = expandVertically(tween(220), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top) + fadeOut(tween(160)),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        // 1. 类别胶囊 + 锚定下拉栏（选中时复用高对比度类别原色）
                        Box {
                            val isDark = IosTheme.colors.isDark
                            val catActive = filter.categoryId != null
                            val rawCatColor = selectedCategory?.let { parseCategoryColor(it.color) } ?: IosTheme.colors.accent
                            val catFgColor = remember(rawCatColor, isDark) { readableCategoryForeground(rawCatColor, isDark) }
                            HomeFilterPill(
                                text = filter.categoryName ?: selectedCategory?.let { HomeFilterResolver.baseCategoryName(it.name) } ?: "全部类别",
                                active = catActive,
                                accentColor = if (catActive) catFgColor else null,
                                leading = if (catActive) {
                                    {
                                        ForumCategoryIcon(
                                            category = selectedCategory,
                                            size = 13.dp,
                                            color = catFgColor,
                                            name = filter.categoryName,
                                            id = filter.categoryId,
                                        )
                                    }
                                } else null,
                                onClick = {
                                    onEnsureCategories()
                                    showCategoryMenu = true
                                },
                                onClear = if (catActive) ({ onSelectCategory(null) }) else null,
                            )
                            HomeCategoryDropdownMenu(
                                expanded = showCategoryMenu,
                                categories = categories,
                                loading = categoriesLoading,
                                categoryOrder = categoryOrder,
                                pinnedCategoryIds = pinnedCategoryIds,
                                selectedCategoryId = filter.categoryId,
                                activeFilterCount = filter.activeCount,
                                onDismiss = { showCategoryMenu = false },
                                onSelectCategory = { cat ->
                                    showCategoryMenu = false
                                    onSelectCategory(cat)
                                },
                                onTogglePin = onTogglePinCategory,
                                onSaveOrder = onSaveCategoryOrder,
                                onResetOrder = onResetCategoryOrder,
                                onClearAll = {
                                    showCategoryMenu = false
                                    onClearFilters()
                                },
                            )
                        }

                        // 2. 独立 LV 等级胶囊 + 锚定下拉栏（外框/背景/文字同步对应等级卡片配色）
                        Box {
                            val lvActive = filter.level != null
                            HomeFilterPill(
                                text = filter.level?.let { "Lv.$it" } ?: "等级",
                                active = lvActive,
                                selectedLevel = filter.level,
                                onClick = {
                                    onEnsureCategories()
                                    showLevelMenu = true
                                },
                                onClear = if (lvActive) ({ onSelectLevel(null) }) else null,
                            )
                            HomeLevelDropdownMenu(
                                expanded = showLevelMenu,
                                filter = filter,
                                categories = categories,
                                onDismiss = { showLevelMenu = false },
                                onSelectLevel = { lv ->
                                    showLevelMenu = false
                                    onSelectLevel(lv)
                                },
                            )
                        }

                        // 3. 标签胶囊 + 锚定下拉栏
                        Box {
                            val tagActive = !filter.tag.isNullOrBlank()
                            HomeFilterPill(
                                text = filter.tag ?: "标签",
                                active = tagActive,
                                leading = if (tagActive && filter.tag != null) {
                                    { ForumTagIcon(tag = filter.tag, size = 13.dp, fallbackHash = true) }
                                } else null,
                                onClick = {
                                    onEnsureTags()
                                    showTagMenu = true
                                },
                                onClear = if (tagActive) ({ onSelectTag(null) }) else null,
                            )
                            HomeTagDropdownMenu(
                                expanded = showTagMenu,
                                tags = tags,
                                searchedTags = searchedTags,
                                loading = tagsLoading,
                                pinnedTags = pinnedTags,
                                tagOrder = tagOrder,
                                query = tagSearchQuery,
                                selectedTag = filter.tag,
                                selectedCategory = selectedCategory,
                                activeFilterCount = filter.activeCount,
                                onQueryChange = onTagSearchQueryChange,
                                onTogglePin = onTogglePinTag,
                                onSaveOrder = onSaveTagOrder,
                                onResetOrder = onResetTagOrder,
                                onDismiss = {
                                    showTagMenu = false
                                    if (tagSearchQuery.isNotEmpty()) onTagSearchQueryChange("")
                                },
                                onSelectTag = { tag ->
                                    showTagMenu = false
                                    if (tagSearchQuery.isNotEmpty()) onTagSearchQueryChange("")
                                    onSelectTag(tag)
                                },
                                onClearAll = {
                                    showTagMenu = false
                                    if (tagSearchQuery.isNotEmpty()) onTagSearchQueryChange("")
                                    onClearFilters()
                                },
                            )
                        }
                    }

                    // 选中 >= 2 项时右侧显示轻巧「重置」按钮，避免冗余第三行
                    if (filter.activeCount >= 2) {
                        Text(
                            text = "重置",
                            style = IosTheme.type.footnote.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = IosTheme.colors.secondaryLabel,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(role = Role.Button, onClick = onClearFilters)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
                IosFullDivider()
            }
        }
    }
}

/** LINUX DO 品牌艺术标题：浅深色均采用明亮经典 L 站琥珀金黄（#FFB800 / #FFC41F），清晰明快。 */
@Composable
private fun LinuxDoBrandWordmark() {
    val isDark = IosTheme.colors.isDark
    val linuxColor = IosTheme.colors.label
    val doColor = if (isDark) Color(0xFFFFC929) else Color(0xFFFFB300)
    val accentStart = Color(0xFFFFC41F)
    val accentEnd = if (isDark) Color(0xFFFFE066) else Color(0xFFFFD026)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "LINUX",
            style = IosTheme.type.headline.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
            ),
            color = linuxColor,
            maxLines = 1,
        )
        // 中间一枚明亮经典黄 45° 斜切菱形点缀
        Canvas(Modifier.size(width = 5.5.dp, height = 5.5.dp)) {
            val diamond = Path().apply {
                moveTo(size.width * 0.5f, 0f)
                lineTo(size.width, size.height * 0.5f)
                lineTo(size.width * 0.5f, size.height)
                lineTo(0f, size.height * 0.5f)
                close()
            }
            drawPath(diamond, accentStart)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DO",
                style = IosTheme.type.headline.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                ),
                color = doColor,
                maxLines = 1,
            )
            Spacer(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(accentStart, accentEnd))),
            )
        }
    }
}

@Composable
private fun HomeFilterPill(
    text: String,
    active: Boolean,
    selectedLevel: Int? = null,
    accentColor: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val isDark = IosTheme.colors.isDark
    val shape = RoundedCornerShape(16.dp)
    val resolvedAccent = accentColor ?: IosTheme.colors.accent
    val levelStyle = if (active && selectedLevel != null) levelPillColors(selectedLevel, isDark) else null
    val isLv3 = active && selectedLevel == 3

    val bgBrush = when {
        isLv3 -> Brush.horizontalGradient(listOf(Color(0xFF3E2B08), Color(0xFF6B4A12), Color(0xFF4A3410)))
        else -> null
    }
    val borderBrush = when {
        isLv3 -> Brush.horizontalGradient(listOf(Color(0xFFFFE9A8), Color(0xFFD4AF37), Color(0xFFFFD76A)))
        else -> null
    }
    val bgColor = when {
        isLv3 -> Color.Transparent
        levelStyle != null -> levelStyle.first
        active -> resolvedAccent.copy(alpha = if (isDark) 0.18f else 0.11f)
        else -> if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF767680).copy(alpha = 0.11f)
    }
    val borderColor = when {
        isLv3 -> Color.Transparent
        levelStyle != null -> levelStyle.second
        active -> resolvedAccent.copy(alpha = if (isDark) 0.42f else 0.30f)
        else -> Color.Transparent
    }
    val textColor = when {
        isLv3 -> Color(0xFFFFD873)
        levelStyle != null -> levelStyle.third
        active -> resolvedAccent
        else -> IosTheme.colors.label
    }

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier.background(bgColor))
            .then(
                when {
                    borderBrush != null -> Modifier.border(1.dp, borderBrush, shape)
                    borderColor != Color.Transparent -> Modifier.border(0.9.dp, borderColor, shape)
                    else -> Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = if (onClear != null) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leading != null) leading()
        Text(
            text = text,
            style = IosTheme.type.footnote.copy(
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClear != null) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.18f))
                    .clickable(role = Role.Button, onClickLabel = "清除$text", onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "×",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 11.sp,
                )
            }
        } else {
            FilterChevronDown(color = textColor.copy(alpha = 0.65f), size = 10.dp)
        }
    }
}

@Composable
private fun FilterChevronDown(color: Color, size: Dp = 10.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.34f)
            lineTo(w * 0.50f, h * 0.68f)
            lineTo(w * 0.82f, h * 0.34f)
        }
        drawPath(
            path,
            color = color,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * 动态排序按钮：去掉小圆点，直接切换为当前排序方式的语义图标。
 * 默认「最新」为双箭头（中性色），切到其他排序时高亮显示对应图标（时钟/火焰/爱心/眼睛）。
 */
@Composable
internal fun TopicSortDynamicButton(
    sortOrder: TopicSortOrder,
    ascending: Boolean,
    onClick: () -> Unit,
    floating: Boolean = false,
) {
    val isCustom = sortOrder != TopicSortOrder.Default
    val accent = IosTheme.colors.accent
    val tint = if (isCustom) accent else if (floating) IosTheme.colors.secondaryLabel else IosTheme.colors.label
    val bgColor = when {
        floating -> IosTheme.colors.card.copy(alpha = 0.94f)
        isCustom -> accent.copy(alpha = if (IosTheme.colors.isDark) 0.18f else 0.11f)
        else -> Color.Transparent
    }
    val borderColor = if (isCustom) accent.copy(alpha = 0.42f) else IosTheme.colors.separator.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .size(if (floating) 44.dp else 38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (floating) Modifier.border(1.dp, borderColor, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "列表排序：${sortOrder.label}" +
                    if (!sortOrder.supportsAscending) "" else if (ascending) "，升序" else "，降序"
            },
        contentAlignment = Alignment.Center,
    ) {
        TopicSortDynamicGlyph(order = sortOrder, tint = tint, modifier = Modifier.size(19.dp))
        if (ascending && sortOrder.supportsAscending) {
            Text(
                text = "↑",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 1.dp),
            )
        }
    }
}

/** 排序语义图标：网站默认、最新动态、最新发布、热门、点赞和浏览。 */
@Composable
internal fun TopicSortDynamicGlyph(
    order: TopicSortOrder,
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp),
) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (order) {
                TopicSortOrder.Default -> {
                    // 双向排序箭头
                    drawLine(tint, Offset(8f, 18.5f), Offset(8f, 5.5f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                    val up = Path().apply {
                        moveTo(4.5f, 9.5f); lineTo(8f, 5.5f); lineTo(11.5f, 9.5f)
                    }
                    drawPath(up, tint, style = stroke)
                    drawLine(tint, Offset(16f, 5.5f), Offset(16f, 18.5f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                    val down = Path().apply {
                        moveTo(12.5f, 14.5f); lineTo(16f, 18.5f); lineTo(19.5f, 14.5f)
                    }
                    drawPath(down, tint, style = stroke)
                }
                TopicSortOrder.Activity -> {
                    // 动态脉冲，区别于发帖时间的时钟。
                    val activity = Path().apply {
                        moveTo(2.5f, 12f)
                        lineTo(6.5f, 12f)
                        lineTo(9.2f, 5f)
                        lineTo(14f, 19f)
                        lineTo(17f, 12f)
                        lineTo(21.5f, 12f)
                    }
                    drawPath(activity, tint, style = stroke)
                }
                TopicSortOrder.Created -> {
                    // 时钟图标
                    drawCircle(tint, radius = 8.6f, center = Offset(12f, 12f), style = stroke)
                    drawLine(tint, Offset(12f, 7.2f), Offset(12f, 12f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(12f, 12f), Offset(15.5f, 14.0f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                }
                TopicSortOrder.Hot -> {
                    // 热度上升趋势折线箭头（简洁利落、辨识度高，替代旧火苗）
                    val trend = Path().apply {
                        moveTo(3.2f, 17.2f)
                        lineTo(9.2f, 11.2f)
                        lineTo(13.2f, 14.8f)
                        lineTo(20.6f, 6.8f)
                    }
                    drawPath(trend, tint, style = stroke)
                    val head = Path().apply {
                        moveTo(15.2f, 6.8f)
                        lineTo(20.6f, 6.8f)
                        lineTo(20.6f, 12.2f)
                    }
                    drawPath(head, tint, style = stroke)
                }
                TopicSortOrder.Likes -> {
                    // 爱心图标
                    val heart = Path().apply {
                        moveTo(12f, 20.2f)
                        cubicTo(10.5f, 18.8f, 4f, 13.5f, 4f, 8.6f)
                        cubicTo(4f, 5.4f, 6.7f, 3.9f, 9.4f, 4.9f)
                        cubicTo(10.8f, 5.4f, 11.6f, 6.3f, 12f, 7.3f)
                        cubicTo(12.4f, 6.3f, 13.2f, 5.4f, 14.6f, 4.9f)
                        cubicTo(17.3f, 3.9f, 20f, 5.4f, 20f, 8.6f)
                        cubicTo(20f, 13.5f, 13.5f, 18.8f, 12f, 20.2f)
                        close()
                    }
                    drawPath(heart, tint, style = stroke)
                }
                TopicSortOrder.Views -> {
                    // 眼睛图标
                    val eye = Path().apply {
                        moveTo(2.8f, 12f)
                        cubicTo(6.2f, 6.2f, 17.8f, 6.2f, 21.2f, 12f)
                        cubicTo(17.8f, 17.8f, 6.2f, 17.8f, 2.8f, 12f)
                        close()
                    }
                    drawPath(eye, tint, style = stroke)
                    drawCircle(tint, radius = 2.8f, center = Offset(12f, 12f), style = stroke)
                }
            }
        }
    }
}

/** 锚定右上角的排序菜单；支持双向排序的项目可再次点击切换方向。 */
@Composable
internal fun TopicSortDropdownMenu(
    expanded: Boolean,
    currentOrder: TopicSortOrder,
    ascending: Boolean,
    onDismiss: () -> Unit,
    onSelectOrder: (TopicSortOrder) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = IosTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(196.dp),
    ) {
        TopicSortOrder.entries.forEachIndexed { idx, order ->
            val selected = order == currentOrder
            val tint = if (selected) IosTheme.colors.accent else IosTheme.colors.label
            val iconBg = if (selected) IosTheme.colors.accent.copy(alpha = 0.12f)
            else if (IosTheme.colors.isDark) Color.White.copy(alpha = 0.07f)
            else Color(0xFF767680).copy(alpha = 0.10f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) IosTheme.colors.accent.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { onSelectOrder(order) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    TopicSortDynamicGlyph(
                        order = order,
                        tint = if (selected) IosTheme.colors.accent else IosTheme.colors.secondaryLabel,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = order.label,
                        style = IosTheme.type.subheadline.copy(
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = tint,
                    )
                    Text(
                        text = order.description,
                        style = IosTheme.type.caption.copy(fontSize = 11.sp),
                        color = if (selected) IosTheme.colors.accent.copy(alpha = 0.8f) else IosTheme.colors.tertiaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (selected) {
                    Text(
                        text = when {
                            order.supportsAscending -> if (ascending) "↑ 升序" else "↓ 降序"
                            else -> "✓"
                        },
                        style = IosTheme.type.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.accent,
                    )
                }
            }
            if (idx < TopicSortOrder.entries.lastIndex) {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}

/** 极简独立 LV 等级下拉栏：复用用户名旁 LevelBadge 样式，右侧带轻巧「可见」提示。 */
@Composable
private fun HomeLevelDropdownMenu(
    expanded: Boolean,
    filter: HomeFilterState,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSelectLevel: (Int?) -> Unit,
) {
    val allowedLevels = remember(categories, filter.categoryId) {
        if (filter.categoryId != null && categories.isNotEmpty()) {
            HomeFilterResolver.supportedLevelsForCategory(categories, filter.categoryId)
        } else {
            listOf(0, 1, 2, 3)
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
        containerColor = IosTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.width(118.dp),
    ) {
        (0..3).forEach { lv ->
            val supported = filter.categoryId == null || categories.isEmpty() || lv in allowedLevels
            val selected = filter.level == lv
            val (lvBg, lvBorder, lvText) = levelPillColors(lv, IosTheme.colors.isDark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) lvBg.copy(alpha = if (lv == 3) 0.22f else 0.75f) else Color.Transparent)
                    .then(
                        if (selected) Modifier.border(0.8.dp, lvBorder, RoundedCornerShape(9.dp))
                        else Modifier,
                    )
                    .clickable(enabled = supported) { onSelectLevel(lv) }
                    .padding(horizontal = 9.dp, vertical = 7.dp)
                    .alpha(if (supported) 1f else 0.38f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (lv == 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(lvBg)
                                .border(0.8.dp, lvBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "Lv0",
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = lvText,
                            )
                        }
                    } else {
                        LevelBadge(level = lv)
                    }
                    Text(
                        text = "可见",
                        style = IosTheme.type.caption.copy(fontSize = 11.5.sp),
                        color = if (selected) lvText else IosTheme.colors.secondaryLabel,
                    )
                }
                if (selected) {
                    Text("✓", color = lvText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

/** 首页类别锚定下拉栏：窄宽原色胶囊，长按置顶、再次长按取消置顶，无多余按钮。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCategoryDropdownMenu(
    expanded: Boolean,
    categories: List<Category>,
    loading: Boolean,
    categoryOrder: List<Long>,
    pinnedCategoryIds: List<Long>,
    selectedCategoryId: Int?,
    activeFilterCount: Int,
    onDismiss: () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onTogglePin: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSaveOrder: (List<Long>) -> Unit,
    @Suppress("UNUSED_PARAMETER") onResetOrder: () -> Unit,
    onClearAll: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val pinnedSet = remember(pinnedCategoryIds) { pinnedCategoryIds.toSet() }
    val groups = remember(categories, pinnedCategoryIds, categoryOrder) {
        arrangeCategoryGroups(categories, pinnedCategoryIds, categoryOrder)
    }
    val q = query.trim()
    val filtered = remember(groups, q) {
        if (q.isEmpty()) groups
        else groups.filter { (parent, _) ->
            parent.name.contains(q, ignoreCase = true) || parent.slug.contains(q, ignoreCase = true)
        }
    }
    val isDark = IosTheme.colors.isDark
    val haptic = LocalHapticFeedback.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            query = ""
            onDismiss()
        },
        offset = DpOffset(0.dp, 4.dp),
        containerColor = IosTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(174.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosTheme.colors.groupedBackground)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = IosTheme.type.footnote.copy(fontSize = 12.sp, color = IosTheme.colors.label),
                    cursorBrush = SolidColor(IosTheme.colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("搜索…", style = IosTheme.type.footnote.copy(fontSize = 12.sp), color = IosTheme.colors.tertiaryLabel)
                        }
                        inner()
                    },
                )
            }
        }
        IosFullDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 5.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (loading && filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                    IosActivityIndicator(diameter = 20.dp)
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text("没有匹配的类别", style = IosTheme.type.footnote, color = IosTheme.colors.tertiaryLabel)
                }
            } else {
                filtered.forEach { (parent, levels) ->
                    val idLong = parent.id.toLong()
                    val isPinned = idLong in pinnedSet
                    val selected = selectedCategoryId == parent.id
                    val rawCatColor = parseCategoryColor(parent.color) ?: IosTheme.colors.accent
                    val fgColor = remember(rawCatColor, isDark) { readableCategoryForeground(rawCatColor, isDark) }
                    val lvNums = levels.mapNotNull { HomeFilterResolver.extractLevel(it) }.distinct().sorted()

                    // 浅色模式用低饱和柔和底 + 高对比压暗同色系前景/图标，清晰护眼
                    val itemBg = when {
                        selected -> rawCatColor.copy(alpha = if (isDark) 0.25f else 0.18f)
                        else -> rawCatColor.copy(alpha = if (isDark) 0.12f else 0.09f)
                    }
                    val itemBorder = when {
                        selected -> fgColor.copy(alpha = if (isDark) 0.70f else 0.58f)
                        isPinned -> fgColor.copy(alpha = if (isDark) 0.44f else 0.36f)
                        else -> fgColor.copy(alpha = if (isDark) 0.24f else 0.22f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(itemBg)
                            .border(0.75.dp, itemBorder, RoundedCornerShape(9.dp))
                            .combinedClickable(
                                onClick = {
                                    query = ""
                                    onSelectCategory(parent)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePin(idLong)
                                },
                            )
                            .padding(horizontal = 7.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        ForumCategoryIcon(category = parent, size = 13.5.dp, color = fgColor, directory = true)
                        Text(
                            text = HomeFilterResolver.baseCategoryName(parent.name),
                            style = IosTheme.type.footnote.copy(
                                fontSize = 12.5.sp,
                                fontWeight = if (selected || isPinned) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = fgColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (isPinned) {
                            PinGlyph(color = fgColor.copy(alpha = 0.90f), size = 9.5.dp)
                        }
                        if (lvNums.isNotEmpty()) {
                            val lvLabel = if (lvNums.size == 1) "Lv${lvNums.first()}" else "Lv${lvNums.first()}-${lvNums.last()}"
                            Text(
                                text = lvLabel,
                                style = IosTheme.type.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                color = fgColor.copy(alpha = if (isDark) 0.78f else 0.82f),
                            )
                        }
                        if (selected) {
                            Text("✓", color = fgColor, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        IosFullDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "长按置顶 / 取消",
                style = IosTheme.type.caption.copy(fontSize = 10.sp),
                color = IosTheme.colors.tertiaryLabel,
            )
            if (activeFilterCount > 0) {
                Text(
                    text = "清空筛选",
                    style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = IosTheme.colors.destructive,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable {
                            query = ""
                            onClearAll()
                        }
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
    }
}

/** 首页标签锚定下拉栏：恢复流式胶囊样式，支持长按置顶、再次长按取消置顶，无置顶按钮。 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeTagDropdownMenu(
    expanded: Boolean,
    tags: List<DiscourseTag>,
    searchedTags: List<DiscourseTag>,
    loading: Boolean,
    pinnedTags: List<String>,
    tagOrder: List<String>,
    query: String,
    selectedTag: String?,
    selectedCategory: Category?,
    activeFilterCount: Int,
    onQueryChange: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSaveOrder: (List<String>) -> Unit,
    @Suppress("UNUSED_PARAMETER") onResetOrder: () -> Unit,
    onDismiss: () -> Unit,
    onSelectTag: (String?) -> Unit,
    onClearAll: () -> Unit,
) {
    val trimmed = query.trim()
    val pinnedLowerSet = remember(pinnedTags) { pinnedTags.map { it.lowercase() }.toSet() }
    val arrangedTags = remember(tags, pinnedTags, tagOrder) {
        arrangeTags(tags, pinnedTags, tagOrder)
    }
    val displayTags = remember(arrangedTags, searchedTags, trimmed) {
        if (trimmed.isEmpty()) {
            arrangedTags.take(60)
        } else {
            val local = arrangedTags.filter {
                it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.name.contains(trimmed, ignoreCase = true)
            }
            (searchedTags + local).distinctBy { it.displayName.lowercase() }.take(60)
        }
    }
    val categorySuggested = remember(selectedCategory, tags) {
        val allowed = selectedCategory?.allowedTags.orEmpty().filter { it.isNotBlank() }
        if (allowed.isEmpty()) emptyList()
        else {
            val byName = tags.associateBy { it.displayName.lowercase() }
            allowed.map { name -> byName[name.lowercase()] ?: DiscourseTag(id = name, name = name, text = name) }
        }
    }
    val haptic = LocalHapticFeedback.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
        containerColor = IosTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(214.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(29.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosTheme.colors.groupedBackground)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = IosTheme.type.footnote.copy(fontSize = 12.sp, color = IosTheme.colors.label),
                    cursorBrush = SolidColor(IosTheme.colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("搜索标签…", style = IosTheme.type.footnote.copy(fontSize = 12.sp), color = IosTheme.colors.tertiaryLabel)
                        }
                        inner()
                    },
                )
            }
        }
        IosFullDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (loading && displayTags.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                    IosActivityIndicator(diameter = 20.dp)
                }
            } else {
                val pinnedList = displayTags.filter { it.displayName.lowercase() in pinnedLowerSet }
                val regularList = displayTags.filterNot { it.displayName.lowercase() in pinnedLowerSet }

                if (pinnedList.isNotEmpty()) {
                    Text(
                        text = "已置顶",
                        style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.accent,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        pinnedList.forEach { tag ->
                            HomeTagOptionChip(
                                tag = tag,
                                selected = selectedTag.equals(tag.displayName, ignoreCase = true),
                                pinned = true,
                                onClick = { onSelectTag(tag.displayName) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePin(tag.displayName)
                                },
                            )
                        }
                    }
                }

                if (trimmed.isEmpty() && categorySuggested.isNotEmpty() && selectedCategory != null) {
                    Text(
                        text = "「${HomeFilterResolver.baseCategoryName(selectedCategory.name)}」常用",
                        style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.secondaryLabel,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        categorySuggested.forEach { tag ->
                            HomeTagOptionChip(
                                tag = tag,
                                selected = selectedTag.equals(tag.displayName, ignoreCase = true),
                                pinned = tag.displayName.lowercase() in pinnedLowerSet,
                                onClick = { onSelectTag(tag.displayName) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePin(tag.displayName)
                                },
                            )
                        }
                    }
                }

                Text(
                    text = if (trimmed.isEmpty()) "热门标签" else "匹配标签",
                    style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = IosTheme.colors.secondaryLabel,
                )

                if (regularList.isEmpty() && pinnedList.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        Text("没有匹配的标签", style = IosTheme.type.footnote, color = IosTheme.colors.tertiaryLabel)
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        regularList.forEach { tag ->
                            HomeTagOptionChip(
                                tag = tag,
                                selected = selectedTag.equals(tag.displayName, ignoreCase = true),
                                pinned = false,
                                onClick = { onSelectTag(tag.displayName) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePin(tag.displayName)
                                },
                            )
                        }
                    }
                }
            }
        }

        IosFullDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "长按置顶 / 取消",
                style = IosTheme.type.caption.copy(fontSize = 10.sp),
                color = IosTheme.colors.tertiaryLabel,
            )
            if (activeFilterCount > 0) {
                Text(
                    text = "清空筛选",
                    style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = IosTheme.colors.destructive,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTagOptionChip(
    tag: DiscourseTag,
    selected: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                when {
                    selected -> IosTheme.colors.accent.copy(alpha = 0.14f)
                    pinned -> IosTheme.colors.accent.copy(alpha = 0.06f)
                    else -> IosTheme.colors.groupedBackground
                },
            )
            .border(
                0.7.dp,
                when {
                    selected -> IosTheme.colors.accent.copy(alpha = 0.45f)
                    pinned -> IosTheme.colors.accent.copy(alpha = 0.25f)
                    else -> IosTheme.colors.separator.copy(alpha = 0.35f)
                },
                shape,
            )
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ForumTagIcon(tag = tag.displayName, size = 12.dp, fallbackHash = true)
        Text(
            text = tag.displayName,
            style = IosTheme.type.caption.copy(
                fontSize = 12.sp,
                fontWeight = if (selected || pinned) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) IosTheme.colors.accent else IosTheme.colors.label,
            maxLines = 1,
        )
        if (tag.effectiveCount > 0) {
            Text(
                text = formatHomeFilterTagCount(tag.effectiveCount),
                style = IosTheme.type.caption.copy(fontSize = 10.sp),
                color = IosTheme.colors.tertiaryLabel,
            )
        }
    }
}

@Composable
private fun PinGlyph(color: Color, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val head = Path().apply {
                moveTo(15f, 4f)
                lineTo(20f, 9f)
                lineTo(17.5f, 11.5f)
                lineTo(15.5f, 10.5f)
                lineTo(11.5f, 14.5f)
                lineTo(12.5f, 17f)
                lineTo(10f, 19.5f)
                lineTo(4.5f, 14f)
                lineTo(7f, 11.5f)
                lineTo(9.5f, 12.5f)
                lineTo(13.5f, 8.5f)
                lineTo(12.5f, 6.5f)
                close()
            }
            drawPath(head, color, style = stroke)
            drawLine(color, Offset(7.2f, 16.8f), Offset(3.5f, 20.5f), strokeWidth = 2.1f, cap = StrokeCap.Round)
        }
    }
}

private fun formatHomeFilterTagCount(value: Int): String {
    if (value < 1_000) return value.coerceAtLeast(0).toString()
    if (value >= 100_000) return "${(value.toLong() + 500L) / 1_000L}k"
    val tenths = (value + 50) / 100
    val decimal = tenths % 10
    return if (decimal == 0) "${tenths / 10}k" else "${tenths / 10}.${decimal}k"
}
