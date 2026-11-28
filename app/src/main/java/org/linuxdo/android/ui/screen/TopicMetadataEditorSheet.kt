package org.linuxdo.android.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.HomeFilterResolver
import org.linuxdo.android.data.compactCategoryGroups
import org.linuxdo.android.net.UserErrorMessage
import org.linuxdo.android.ui.CategoriesUiState
import org.linuxdo.android.ui.IosTextAction
import org.linuxdo.android.ui.TopicMetadataEditorUiState
import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.ui.design.ForumTagIcon
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.IosNavBar
import org.linuxdo.android.ui.design.IosTheme

internal enum class TopicMetadataMenu { Category, Level, Tags }

internal data class TopicMetadataHeaderMeta(
    val currentCategory: Category?,
    val currentGroupParentId: Int?,
    val level: Int?,
    val levelCategories: Map<Int, Category>,
)

internal fun resolveTopicMetadataCategoryState(
    allCategories: List<Category>,
    selectableCategories: List<Category>,
    selectedCategoryId: Int?,
): TopicMetadataHeaderMeta {
    val selectableGroups = compactCategoryGroups(selectableCategories)
    val selectableGroup = selectableGroups.firstOrNull { (parent, levels) ->
        parent.id == selectedCategoryId || levels.any { it.id == selectedCategoryId }
    }
    val variants = selectableGroup?.let { listOf(it.first) + it.second }.orEmpty()
    val levels = variants.associateBy(HomeFilterResolver::effectiveLevel)
    val current = variants.firstOrNull { it.id == selectedCategoryId }
        ?: allCategories.firstOrNull { it.id == selectedCategoryId }
    val level = current?.let(HomeFilterResolver::effectiveLevel)
    return TopicMetadataHeaderMeta(
        currentCategory = current,
        currentGroupParentId = selectableGroup?.first?.id,
        level = level,
        levelCategories = levels,
    )
}

internal fun metadataTitleCountHint(
    title: String,
    changed: Boolean,
    minimum: Int,
): Pair<String, Boolean> {
    val length = title.trim().length
    val invalid = changed && (length < minimum || length > 200)
    val hint = when {
        changed && length < minimum -> "还差 ${minimum - length} 字（至少 $minimum 字）"
        changed && length > 200 -> "超出 ${length - 200} 字（最多 200 字）"
        changed -> "至少 $minimum 个字符"
        else -> ""
    }
    return hint to invalid
}

/**
 * 修改话题元信息（标题 / 分类 / 等级 / 标签）整页编辑器。
 * 与新版发帖页（TopicCreatorSheet）和首页筛选胶囊（HomeFilterHeader）保持统一视觉与交互语言。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopicMetadataEditorSheet(
    state: TopicMetadataEditorUiState,
    categories: CategoriesUiState,
    onTogglePinCategory: (Long) -> Unit,
    onTitle: (String) -> Unit,
    onCategory: (Int) -> Unit,
    onTags: (List<String>) -> Unit,
    onTagQuery: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = IosTheme.colors
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var discard by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<TopicMetadataMenu?>(null) }

    val editable = state.original?.canEdit == true && !state.saving && !state.loading
    val selectableCategories = state.rules?.selectableCategories.orEmpty()
    val allCategories = state.rules?.categories.orEmpty()
    val categoryMeta = remember(allCategories, selectableCategories, state.categoryId) {
        resolveTopicMetadataCategoryState(allCategories, selectableCategories, state.categoryId)
    }
    val currentCategory = categoryMeta.currentCategory
    val canEditCategory = editable && selectableCategories.isNotEmpty()
    val canEditTags = editable && state.rules?.canTagTopics == true

    LaunchedEffect(editable) {
        if (!editable) menu = null
    }

    val requestClose: () -> Unit = {
        if (!state.saving) {
            menu = null
            if (state.dirty) {
                keyboard?.hide()
                focus.clearFocus(force = true)
                discard = true
            } else {
                keyboard?.hide()
                focus.clearFocus(force = true)
                onClose()
            }
        }
    }

    // 整页编辑先收起软键盘，再响应退出，保持与 TopicCreatorSheet 一致的返回手感。
    BackHandler(enabled = !discard && menu == null) {
        if (imeVisible) {
            keyboard?.hide()
        } else {
            requestClose()
        }
    }
    DisposableEffect(keyboard) {
        onDispose { keyboard?.hide() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.card)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
            .imePadding(),
    ) {
        IosNavBar(
            title = if (state.original?.privateMessage == true) "编辑私信标题" else "编辑话题",
            onBack = requestClose,
            trailingWidth = 86.dp,
            trailing = {
                ComposerSendPill(
                    label = if (state.saving) "保存中" else "保存",
                    enabled = state.canSave,
                    submitting = state.saving,
                    onClick = {
                        menu = null
                        focus.clearFocus()
                        keyboard?.hide()
                        onSave()
                    },
                )
            },
        )
        IosFullDivider()

        if (state.loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                IosLoadingBox()
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                state.error?.let { err ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        TopicMetadataErrorBanner(
                            message = UserErrorMessage.sanitize(err, "操作失败，请稍后重试"),
                            onRetry = if (state.saving) null else onRetry,
                        )
                    }
                }

                if (state.original != null) {
                    val titleStyle = IosTheme.type.headline.copy(
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    BasicTextField(
                        value = state.title,
                        onValueChange = onTitle,
                        enabled = editable,
                        minLines = 2,
                        maxLines = 5,
                        textStyle = titleStyle.copy(color = if (editable) colors.label else colors.secondaryLabel),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                        decorationBox = { inner ->
                            Box {
                                if (state.title.isEmpty()) {
                                    Text("话题标题", style = titleStyle, color = colors.tertiaryLabel)
                                }
                                inner()
                            }
                        },
                    )

                    val changedTitle = state.patch?.title != null
                    val minimum = if (state.original.privateMessage) {
                        state.rules?.minPmTitleLength ?: 2
                    } else {
                        state.rules?.minTitleLength ?: 15
                    }
                    val length = state.title.trim().length
                    val (countHint, invalidLength) = metadataTitleCountHint(state.title, changedTitle, minimum)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = countHint,
                            style = IosTheme.type.caption,
                            color = if (invalidLength) colors.destructive else colors.tertiaryLabel,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$length / 200",
                            style = IosTheme.type.caption,
                            color = if (invalidLength) colors.destructive else colors.tertiaryLabel,
                        )
                    }

                    if (!state.original.privateMessage) {
                        val selectCategory: (Category) -> Unit = { chosen ->
                            menu = null
                            if (chosen.id != state.categoryId) {
                                onCategory(chosen.id)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            // 1. 分类胶囊 + 首页同款分类下拉菜单
                            Box {
                                val catColor = readableCategoryForeground(
                                    currentCategory?.color?.let(::parseCategoryColor) ?: colors.accent,
                                    colors.isDark,
                                )
                                Box(Modifier.alpha(if (canEditCategory) 1f else 0.6f)) {
                                    HomeFilterPill(
                                        text = currentCategory?.let { HomeFilterResolver.baseCategoryName(it.name) } ?: "选择分类",
                                        active = currentCategory != null,
                                        accentColor = catColor,
                                        leading = currentCategory?.let { { ForumCategoryIcon(it, 13.dp, catColor) } },
                                        onClick = {
                                            if (canEditCategory) {
                                                keyboard?.hide()
                                                menu = TopicMetadataMenu.Category
                                            }
                                        },
                                    )
                                }
                                HomeCategoryDropdownMenu(
                                    expanded = menu == TopicMetadataMenu.Category,
                                    categories = selectableCategories,
                                    loading = false,
                                    categoryOrder = categories.order,
                                    pinnedCategoryIds = categories.pinnedIds,
                                    selectedCategoryId = categoryMeta.currentGroupParentId,
                                    onDismiss = { menu = null },
                                    onSelectCategory = { pickedParent ->
                                        val target = if (pickedParent.id == categoryMeta.currentGroupParentId && currentCategory != null) {
                                            currentCategory
                                        } else {
                                            pickedParent
                                        }
                                        selectCategory(target)
                                    },
                                    onTogglePin = onTogglePinCategory,
                                )
                            }

                            // 2. 等级胶囊 + 首页同款等级下拉菜单
                            if (currentCategory != null && categoryMeta.level != null) {
                                val canPickLevel = canEditCategory && categoryMeta.levelCategories.isNotEmpty()
                                Box {
                                    Box(Modifier.alpha(if (canPickLevel) 1f else 0.6f)) {
                                        HomeFilterPill(
                                            text = "Lv.${categoryMeta.level}",
                                            active = true,
                                            selectedLevel = categoryMeta.level,
                                            onClick = {
                                                if (canPickLevel) {
                                                    keyboard?.hide()
                                                    menu = TopicMetadataMenu.Level
                                                }
                                            },
                                        )
                                    }
                                    CategoryLevelDropdownMenu(
                                        expanded = menu == TopicMetadataMenu.Level,
                                        selectedLevel = categoryMeta.level,
                                        levelEnabled = { it in categoryMeta.levelCategories },
                                        onDismiss = { menu = null },
                                        onSelectLevel = { lv -> categoryMeta.levelCategories[lv]?.let(selectCategory) },
                                        optionLabel = { if (it in categoryMeta.levelCategories) "可选" else "不可选" },
                                    )
                                }
                            }

                            // 3. 标签胶囊 + 与发帖页一致的锚定搜索下拉菜单
                            Box {
                                Box(Modifier.alpha(if (canEditTags) 1f else 0.6f)) {
                                    HomeFilterPill(
                                        text = when (state.tags.size) {
                                            0 -> "标签"
                                            1 -> state.tags.first()
                                            else -> "${state.tags.size} 个标签"
                                        },
                                        active = state.tags.isNotEmpty(),
                                        leading = state.tags.firstOrNull()?.let {
                                            { ForumTagIcon(tag = it, size = 13.dp, fallbackHash = true) }
                                        },
                                        onClick = {
                                            if (canEditTags) {
                                                menu = TopicMetadataMenu.Tags
                                            }
                                        },
                                    )
                                }
                                if (menu == TopicMetadataMenu.Tags && canEditTags) {
                                    TopicMetadataTagDropdown(
                                        state = state,
                                        category = currentCategory,
                                        onTagQuery = onTagQuery,
                                        onTags = onTags,
                                        onDismiss = { menu = null },
                                    )
                                }
                            }
                        }

                        IosFullDivider()

                        // 已选标签与标签规则明细区（直接可见并可一键移除，兼顾直观管理与下拉筛选）
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "已选标签",
                                    style = IosTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.label,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${state.tags.size} / 5",
                                    style = IosTheme.type.caption,
                                    color = colors.tertiaryLabel,
                                )
                            }

                            if (state.tags.isEmpty()) {
                                Text(
                                    text = if (canEditTags) "未选择标签，点击上方「标签」胶囊搜索添加" else "未设置标签",
                                    style = IosTheme.type.footnote,
                                    color = colors.tertiaryLabel,
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    state.tags.forEach { tag ->
                                        HomeTagOptionChip(
                                            name = if (canEditTags) "$tag  ×" else tag,
                                            selected = true,
                                            onClick = {
                                                if (canEditTags) onTags(state.tags - tag)
                                            },
                                        )
                                    }
                                }
                            }

                            state.requiredTagName?.takeIf { it.isNotBlank() }?.let { groupName ->
                                Text(
                                    text = "请从「$groupName」组至少选择 ${state.requiredTagMin} 个标签",
                                    style = IosTheme.type.caption,
                                    color = colors.accent,
                                )
                            }
                            currentCategory?.minimumRequiredTags?.takeIf { it > 0 }?.let { minTags ->
                                Text(
                                    text = "该分类至少需要 $minTags 个标签",
                                    style = IosTheme.type.caption,
                                    color = colors.secondaryLabel,
                                )
                            }
                            if (state.rules?.canTagTopics != true) {
                                Text(
                                    text = "当前账号不能修改标签，已有标签会保留",
                                    style = IosTheme.type.caption,
                                    color = colors.secondaryLabel,
                                )
                            }
                            state.tagError?.let { tagErr ->
                                TopicMetadataErrorBanner(
                                    message = UserErrorMessage.sanitize(tagErr, "标签加载失败，请重试"),
                                    onRetry = if (canEditTags) ({ onTagQuery(state.tagQuery) }) else null,
                                )
                            }
                        }
                    } else {
                        IosFullDivider()
                        Text(
                            text = "私信仅支持修改标题",
                            style = IosTheme.type.caption,
                            color = colors.secondaryLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }

                    if (state.dirty) {
                        state.validationError?.let { validationErr ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                TopicMetadataErrorBanner(message = validationErr)
                            }
                        }
                    }
                }
            }
        }
    }

    if (discard) {
        TopicMetadataDiscardDialog(
            onDiscard = {
                discard = false
                onClose()
            },
            onCancel = { discard = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicMetadataTagDropdown(
    state: TopicMetadataEditorUiState,
    category: Category?,
    onTagQuery: (String) -> Unit,
    onTags: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IosTheme.colors
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.card,
        modifier = Modifier
            .width(266.dp)
            .heightIn(max = 330.dp),
    ) {
        FilterMenuSearch(state.tagQuery, "搜索标签…", onChange = onTagQuery)
        IosFullDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 276.dp)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.requiredTagName?.takeIf { it.isNotBlank() }?.let { groupName ->
                Text(
                    text = "至少选择 ${state.requiredTagMin} 个「$groupName」标签",
                    style = IosTheme.type.caption,
                    color = colors.accent,
                )
            }
            if ((category?.minimumRequiredTags ?: 0) > 0) {
                Text(
                    text = "此分类至少需要 ${category?.minimumRequiredTags} 个标签",
                    style = IosTheme.type.caption,
                    color = colors.secondaryLabel,
                )
            }
            if (state.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    state.tags.forEach { name ->
                        HomeTagOptionChip(
                            name = name,
                            selected = true,
                            onClick = { onTags(state.tags - name) },
                        )
                    }
                }
                IosFullDivider()
            }
            val candidates = state.tagOptions.filterNot { it.name in state.tags }
            when {
                state.loadingTags -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IosActivityIndicator(diameter = 15.dp)
                        Text("正在加载…", style = IosTheme.type.footnote, color = colors.secondaryLabel)
                    }
                }
                state.tagError != null -> {
                    IosTextAction("加载失败，点此重试", textStyle = IosTheme.type.footnote) {
                        onTagQuery(state.tagQuery)
                    }
                }
                candidates.isEmpty() -> {
                    Text(
                        text = if (state.tagQuery.isBlank()) "没有更多可选标签" else "未找到匹配标签",
                        style = IosTheme.type.footnote,
                        color = colors.secondaryLabel,
                    )
                }
                else -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        candidates.forEach { tag ->
                            HomeTagOptionChip(
                                name = tag.text.ifBlank { tag.name },
                                count = tag.count,
                                selected = false,
                                onClick = { onTags(state.tags + tag.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicMetadataErrorBanner(message: String, onRetry: (() -> Unit)? = null) {
    val colors = IosTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.destructive.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            style = IosTheme.type.footnote,
            color = colors.destructive,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "重试",
                    style = IosTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                )
            }
        }
    }
}

@Composable
private fun TopicMetadataDiscardDialog(onDiscard: () -> Unit, onCancel: () -> Unit) {
    val colors = IosTheme.colors
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.card)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("放弃本次修改？", style = IosTheme.type.headline, color = colors.label)
            Text("尚未保存的话题信息将不会提交。", style = IosTheme.type.footnote, color = colors.secondaryLabel)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.fieldBackground)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("继续编辑", style = IosTheme.type.footnote, color = colors.label)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.destructive)
                        .clickable(onClick = onDiscard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("放弃修改", style = IosTheme.type.footnote, color = Color.White)
                }
            }
        }
    }
}
