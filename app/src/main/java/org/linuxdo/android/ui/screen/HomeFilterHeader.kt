package org.linuxdo.android.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.DiscourseTag
import org.linuxdo.android.data.HomeFilterResolver
import org.linuxdo.android.data.HomeFilterState
import org.linuxdo.android.data.TopicSortOrder
import org.linuxdo.android.data.orderedCategoryGroups
import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.ui.design.ForumTagIcon
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosDivider
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
    tags: List<DiscourseTag>,
    tagsLoading: Boolean,
    searchedTags: List<DiscourseTag>,
    tagSearchQuery: String,
    leading: @Composable () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onSelectLevel: (Int?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onEnsureCategories: () -> Unit,
    onEnsureTags: () -> Unit,
    onTagSearchQueryChange: (String) -> Unit,
    onSortChange: (TopicSortOrder) -> Unit,
    onSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var showCategorySheet by remember { mutableStateOf(false) }
    var showLevelMenu by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
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
                    Text(
                        text = "LINUX DO",
                        style = IosTheme.type.headline.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = IosTheme.colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                        // 1. 类别胶囊
                        val catActive = filter.categoryId != null
                        val catColor = selectedCategory?.let { parseCategoryColor(it.color) } ?: IosTheme.colors.accent
                        HomeFilterPill(
                            text = filter.categoryName ?: selectedCategory?.let { HomeFilterResolver.baseCategoryName(it.name) } ?: "全部类别",
                            active = catActive,
                            leading = if (catActive) {
                                {
                                    ForumCategoryIcon(
                                        category = selectedCategory,
                                        size = 13.dp,
                                        color = catColor,
                                        name = filter.categoryName,
                                        id = filter.categoryId,
                                    )
                                }
                            } else null,
                            onClick = {
                                onEnsureCategories()
                                showCategorySheet = true
                            },
                            onClear = if (catActive) ({ onSelectCategory(null) }) else null,
                        )

                        // 2. 独立 LV 等级胶囊 + 锚定下拉菜单
                        Box {
                            val lvActive = filter.level != null
                            HomeFilterPill(
                                text = filter.level?.let { "Lv.$it" } ?: "等级",
                                active = lvActive,
                                isLv3 = filter.level == 3,
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

                        // 3. 标签胶囊
                        val tagActive = !filter.tag.isNullOrBlank()
                        HomeFilterPill(
                            text = filter.tag ?: "标签",
                            active = tagActive,
                            leading = if (tagActive && filter.tag != null) {
                                { ForumTagIcon(tag = filter.tag, size = 13.dp, fallbackHash = true) }
                            } else null,
                            onClick = {
                                onEnsureTags()
                                showTagSheet = true
                            },
                            onClear = if (tagActive) ({ onSelectTag(null) }) else null,
                        )
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

    if (showCategorySheet) {
        HomeCategoryPickerDialog(
            categories = categories,
            loading = categoriesLoading,
            categoryOrder = categoryOrder,
            selectedCategoryId = filter.categoryId,
            activeFilterCount = filter.activeCount,
            onDismiss = { showCategorySheet = false },
            onSelectCategory = { cat ->
                showCategorySheet = false
                onSelectCategory(cat)
            },
            onClearAll = {
                showCategorySheet = false
                onClearFilters()
            },
        )
    }

    if (showTagSheet) {
        HomeTagPickerDialog(
            tags = tags,
            searchedTags = searchedTags,
            loading = tagsLoading,
            query = tagSearchQuery,
            selectedTag = filter.tag,
            selectedCategory = selectedCategory,
            activeFilterCount = filter.activeCount,
            onQueryChange = onTagSearchQueryChange,
            onDismiss = {
                showTagSheet = false
                if (tagSearchQuery.isNotEmpty()) onTagSearchQueryChange("")
            },
            onSelectTag = { tag ->
                showTagSheet = false
                if (tagSearchQuery.isNotEmpty()) onTagSearchQueryChange("")
                onSelectTag(tag)
            },
            onClearAll = {
                showTagSheet = false
                if (tagSearchQuery.isNotEmpty()) onTagSearchQueryChange("")
                onClearFilters()
            },
        )
    }
}

@Composable
private fun HomeFilterPill(
    text: String,
    active: Boolean,
    isLv3: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val isDark = IosTheme.colors.isDark
    val shape = RoundedCornerShape(16.dp)
    val bgBrush = when {
        active && isLv3 -> Brush.linearGradient(listOf(Color(0xFF272219), Color(0xFF3B3019)))
        else -> null
    }
    val bgColor = when {
        active && isLv3 -> Color.Transparent
        active -> IosTheme.colors.accent.copy(alpha = if (isDark) 0.20f else 0.12f)
        else -> if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF767680).copy(alpha = 0.11f)
    }
    val borderColor = when {
        active && isLv3 -> Color(0xFFFFD873).copy(alpha = 0.55f)
        active -> IosTheme.colors.accent.copy(alpha = if (isDark) 0.45f else 0.34f)
        else -> Color.Transparent
    }
    val textColor = when {
        active && isLv3 -> Color(0xFFFFD873)
        active -> IosTheme.colors.accent
        else -> IosTheme.colors.label
    }

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier.background(bgColor))
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape) else Modifier)
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
                    .background(
                        if (isLv3) Color(0xFFFFD873).copy(alpha = 0.22f)
                        else IosTheme.colors.accent.copy(alpha = 0.18f),
                    )
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

/** 5 项排序对应的语义图标：最新(双箭头) / 最新发布(时钟) / 热门(火焰) / 点赞最多(爱心) / 浏览最多(眼睛)。 */
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
                TopicSortOrder.Created -> {
                    // 时钟图标
                    drawCircle(tint, radius = 8.6f, center = Offset(12f, 12f), style = stroke)
                    drawLine(tint, Offset(12f, 7.2f), Offset(12f, 12f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(12f, 12f), Offset(15.5f, 14.0f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                }
                TopicSortOrder.Hot -> {
                    // 火焰图标
                    val flame = Path().apply {
                        moveTo(12.2f, 3.0f)
                        cubicTo(13.2f, 6.2f, 10.8f, 8.2f, 9.2f, 9.8f)
                        cubicTo(7.5f, 11.5f, 6.8f, 13.0f, 6.8f, 14.8f)
                        cubicTo(6.8f, 18.1f, 9.2f, 20.5f, 12.2f, 20.5f)
                        cubicTo(15.3f, 20.5f, 17.6f, 18.0f, 17.6f, 14.7f)
                        cubicTo(17.6f, 11.7f, 15.8f, 9.5f, 14.2f, 8.0f)
                        cubicTo(14.0f, 9.6f, 13.3f, 10.6f, 12.3f, 11.2f)
                        cubicTo(13.1f, 9.0f, 13.4f, 6.2f, 12.2f, 3.0f)
                        close()
                    }
                    drawPath(flame, tint, style = stroke)
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

/** 锚定右上角的 5 项排序下拉菜单：支持再次点击切换升序/降序。 */
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
        modifier = Modifier.widthIn(min = 224.dp, max = 260.dp),
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

/** 独立 LV 等级下拉菜单：既支持与当前类别联动，也支持不选类别时独立筛选全站对应等级。 */
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
    val levelCatCounts = remember(categories) {
        (0..3).associateWith { lv -> HomeFilterResolver.categoriesForLevel(categories, lv).size }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = IosTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.widthIn(min = 216.dp, max = 256.dp),
    ) {
        // 全部等级
        val allSelected = filter.level == null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (allSelected) IosTheme.colors.accent.copy(alpha = 0.10f) else Color.Transparent)
                .clickable { onSelectLevel(null) }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (allSelected) IosTheme.colors.accent.copy(alpha = 0.14f) else IosTheme.colors.groupedBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "ALL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (allSelected) IosTheme.colors.accent else IosTheme.colors.secondaryLabel,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "全部等级",
                    style = IosTheme.type.subheadline.copy(
                        fontSize = 14.sp,
                        fontWeight = if (allSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (allSelected) IosTheme.colors.accent else IosTheme.colors.label,
                )
                Text(
                    text = if (filter.categoryName != null) "「${filter.categoryName}」不限等级" else "全站所有等级",
                    style = IosTheme.type.caption.copy(fontSize = 11.sp),
                    color = IosTheme.colors.tertiaryLabel,
                )
            }
            if (allSelected) {
                Text("✓", color = IosTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        val isDark = IosTheme.colors.isDark
        (0..3).forEach { lv ->
            val supported = filter.categoryId == null || categories.isEmpty() || lv in allowedLevels
            val selected = filter.level == lv
            val (pillBg, pillBorder, pillText) = levelPillColors(lv, isDark)
            val count = levelCatCounts[lv] ?: 0
            val subtitle = when {
                filter.categoryName != null && supported -> "${filter.categoryName} · Lv.$lv"
                filter.categoryName != null -> "「${filter.categoryName}」无此等级"
                count > 0 -> "聚合全站 $count 个 Lv.$lv 子版块"
                else -> "筛选全站 Lv.$lv 子版块"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) IosTheme.colors.accent.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable(enabled = supported) { onSelectLevel(lv) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(pillBg.copy(alpha = if (supported) 1f else 0.4f))
                        .border(0.7.dp, pillBorder.copy(alpha = if (supported) 1f else 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Lv$lv",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = pillText.copy(alpha = if (supported) 1f else 0.45f),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Lv.$lv",
                        style = IosTheme.type.subheadline.copy(
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = when {
                            !supported -> IosTheme.colors.tertiaryLabel.copy(alpha = 0.55f)
                            selected -> IosTheme.colors.accent
                            else -> IosTheme.colors.label
                        },
                    )
                    Text(
                        text = subtitle,
                        style = IosTheme.type.caption.copy(fontSize = 11.sp),
                        color = IosTheme.colors.tertiaryLabel.copy(alpha = if (supported) 1f else 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (selected) {
                    Text("✓", color = IosTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/** 首页类别选择弹层：复用网页原版 SVG 类别图标、支持搜索及显示各版块等级范围。 */
@Composable
private fun HomeCategoryPickerDialog(
    categories: List<Category>,
    loading: Boolean,
    categoryOrder: List<Long>,
    selectedCategoryId: Int?,
    activeFilterCount: Int,
    onDismiss: () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onClearAll: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val groups = remember(categories, categoryOrder) {
        orderedCategoryGroups(categories, categoryOrder)
    }
    val filtered = remember(groups, query) {
        val q = query.trim()
        if (q.isEmpty()) groups
        else groups.filter { (parent, _) ->
            parent.name.contains(q, ignoreCase = true) || parent.slug.contains(q, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(IosTheme.colors.card)
                .border(0.7.dp, IosTheme.colors.separator.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        ) {
            // 顶部搜索 + 全部类别
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IosTheme.colors.groupedBackground)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = IosTheme.type.body.copy(fontSize = 13.5.sp, color = IosTheme.colors.label),
                        cursorBrush = SolidColor(IosTheme.colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("搜索类别…", style = IosTheme.type.body.copy(fontSize = 13.5.sp), color = IosTheme.colors.tertiaryLabel)
                            }
                            inner()
                        },
                    )
                }
                val isAll = selectedCategoryId == null
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAll) IosTheme.colors.accent.copy(alpha = 0.14f) else IosTheme.colors.groupedBackground)
                        .border(
                            0.8.dp,
                            if (isAll) IosTheme.colors.accent.copy(alpha = 0.4f) else IosTheme.colors.separator.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelectCategory(null) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "全部类别",
                        style = IosTheme.type.footnote.copy(fontSize = 12.5.sp, fontWeight = if (isAll) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (isAll) IosTheme.colors.accent else IosTheme.colors.label,
                    )
                }
            }
            IosFullDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (loading && filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        IosActivityIndicator(diameter = 24.dp)
                    }
                } else if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("没有匹配的类别", style = IosTheme.type.subheadline, color = IosTheme.colors.tertiaryLabel)
                    }
                } else {
                    filtered.forEach { (parent, levels) ->
                        val selected = selectedCategoryId == parent.id
                        val catColor = parseCategoryColor(parent.color) ?: IosTheme.colors.accent
                        val lvNums = levels.mapNotNull { HomeFilterResolver.extractLevel(it) }.distinct().sorted()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) IosTheme.colors.accent.copy(alpha = 0.12f)
                                    else IosTheme.colors.groupedBackground,
                                )
                                .border(
                                    0.8.dp,
                                    if (selected) IosTheme.colors.accent.copy(alpha = 0.4f)
                                    else IosTheme.colors.separator.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { onSelectCategory(parent) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ForumCategoryIcon(category = parent, size = 17.dp, color = catColor, directory = true)
                            Text(
                                text = HomeFilterResolver.baseCategoryName(parent.name),
                                style = IosTheme.type.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                                color = if (selected) IosTheme.colors.accent else IosTheme.colors.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (lvNums.isNotEmpty()) {
                                val lvLabel = if (lvNums.size == 1) "Lv.${lvNums.first()}" else "Lv.${lvNums.first()}~${lvNums.last()}"
                                Text(
                                    text = lvLabel,
                                    style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                                    color = IosTheme.colors.secondaryLabel,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(IosTheme.colors.card)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            if (selected) {
                                Text("✓", color = IosTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (activeFilterCount > 0) {
                IosFullDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IosTheme.colors.groupedBackground.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "已启用 $activeFilterCount 项筛选",
                        style = IosTheme.type.caption,
                        color = IosTheme.colors.secondaryLabel,
                    )
                    Text(
                        text = "清空全部筛选",
                        style = IosTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.destructive,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

/** 首页标签选择弹层：支持本地+远程搜索联想，已选类别时置顶该类别常用标签，图标复用网页原版 SVG。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeTagPickerDialog(
    tags: List<DiscourseTag>,
    searchedTags: List<DiscourseTag>,
    loading: Boolean,
    query: String,
    selectedTag: String?,
    selectedCategory: Category?,
    activeFilterCount: Int,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectTag: (String?) -> Unit,
    onClearAll: () -> Unit,
) {
    val trimmed = query.trim()
    val displayTags = remember(tags, searchedTags, trimmed) {
        if (trimmed.isEmpty()) {
            tags.take(80)
        } else {
            val local = tags.filter {
                it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.name.contains(trimmed, ignoreCase = true)
            }
            (searchedTags + local).distinctBy { it.displayName.lowercase() }.take(80)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(IosTheme.colors.card)
                .border(0.7.dp, IosTheme.colors.separator.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IosTheme.colors.groupedBackground)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = IosTheme.type.body.copy(fontSize = 13.5.sp, color = IosTheme.colors.label),
                        cursorBrush = SolidColor(IosTheme.colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("搜索标签（本地 + 远程联想）…", style = IosTheme.type.body.copy(fontSize = 13.5.sp), color = IosTheme.colors.tertiaryLabel)
                            }
                            inner()
                        },
                    )
                }
                val isAll = selectedTag.isNullOrBlank()
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAll) IosTheme.colors.accent.copy(alpha = 0.14f) else IosTheme.colors.groupedBackground)
                        .border(
                            0.8.dp,
                            if (isAll) IosTheme.colors.accent.copy(alpha = 0.4f) else IosTheme.colors.separator.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelectTag(null) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "全部标签",
                        style = IosTheme.type.footnote.copy(fontSize = 12.5.sp, fontWeight = if (isAll) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (isAll) IosTheme.colors.accent else IosTheme.colors.label,
                    )
                }
            }
            IosFullDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (loading && displayTags.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        IosActivityIndicator(diameter = 24.dp)
                    }
                } else {
                    if (trimmed.isEmpty() && categorySuggested.isNotEmpty() && selectedCategory != null) {
                        Text(
                            text = "「${HomeFilterResolver.baseCategoryName(selectedCategory.name)}」常用标签",
                            style = IosTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = IosTheme.colors.secondaryLabel,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            categorySuggested.forEach { tag ->
                                HomeTagOptionChip(
                                    tag = tag,
                                    selected = selectedTag.equals(tag.displayName, ignoreCase = true),
                                    onClick = { onSelectTag(tag.displayName) },
                                )
                            }
                        }
                    }

                    Text(
                        text = if (trimmed.isEmpty()) "热门标签" else "匹配标签",
                        style = IosTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.secondaryLabel,
                    )

                    if (displayTags.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("没有匹配的标签", style = IosTheme.type.subheadline, color = IosTheme.colors.tertiaryLabel)
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            displayTags.forEach { tag ->
                                HomeTagOptionChip(
                                    tag = tag,
                                    selected = selectedTag.equals(tag.displayName, ignoreCase = true),
                                    onClick = { onSelectTag(tag.displayName) },
                                )
                            }
                        }
                    }
                }
            }

            if (activeFilterCount > 0) {
                IosFullDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IosTheme.colors.groupedBackground.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "已启用 $activeFilterCount 项筛选",
                        style = IosTheme.type.caption,
                        color = IosTheme.colors.secondaryLabel,
                    )
                    Text(
                        text = "清空全部筛选",
                        style = IosTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.destructive,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTagOptionChip(
    tag: DiscourseTag,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) IosTheme.colors.accent.copy(alpha = 0.14f)
                else IosTheme.colors.groupedBackground,
            )
            .border(
                0.8.dp,
                if (selected) IosTheme.colors.accent.copy(alpha = 0.45f)
                else IosTheme.colors.separator.copy(alpha = 0.35f),
                shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ForumTagIcon(tag = tag.displayName, size = 13.dp, fallbackHash = true)
        Text(
            text = tag.displayName,
            style = IosTheme.type.footnote.copy(
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) IosTheme.colors.accent else IosTheme.colors.label,
            maxLines = 1,
        )
        if (tag.effectiveCount > 0) {
            Text(
                text = formatHomeFilterTagCount(tag.effectiveCount),
                style = IosTheme.type.caption.copy(fontSize = 11.sp),
                color = IosTheme.colors.tertiaryLabel,
            )
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
