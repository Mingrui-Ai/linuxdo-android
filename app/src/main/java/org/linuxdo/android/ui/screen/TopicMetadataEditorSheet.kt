package org.linuxdo.android.ui.screen

import android.net.Uri
import android.util.TypedValue
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.HomeFilterResolver
import org.linuxdo.android.data.compactCategoryGroups
import org.linuxdo.android.net.UserErrorMessage
import org.linuxdo.android.ui.CategoriesUiState
import org.linuxdo.android.ui.CreatorPollSpan
import org.linuxdo.android.ui.IosTextAction
import org.linuxdo.android.ui.LocalEditorSuggestionSource
import org.linuxdo.android.ui.PollBuilderDialog
import org.linuxdo.android.ui.PollBuilderSpec
import org.linuxdo.android.ui.RichMarkdownEditorView
import org.linuxdo.android.ui.TopicMetadataEditorUiState
import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.ui.design.ForumTagIcon
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.IosNavBar
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.nextAvailablePollName

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
 * 统一的话题编辑整页：参考 Discourse 原站编辑主帖逻辑，并完全对齐发帖页（TopicCreatorSheet）
 * 与首页筛选胶囊（HomeFilterHeader）的布局与交互：
 * - 上半区：大标题输入 + 分类 / Lv 等级 / 标签筛选胶囊（支持按权限单独编辑或只读展示）；
 * - 下半区：支持编辑 1 楼正文时直接接入完整 RichMarkdownEditorView + 预览分段 + 全屏专注 + 云端对齐 Markdown 工具栏；
 *   若当前账号仅可修改元信息（不可改正文），则保留已选标签流式管理区。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopicMetadataEditorSheet(
    state: TopicMetadataEditorUiState,
    categories: CategoriesUiState,
    onTogglePinCategory: (Long) -> Unit,
    onTitle: (String) -> Unit,
    onRawChange: (String) -> Unit,
    onCategory: (Int) -> Unit,
    onTags: (List<String>) -> Unit,
    onTagQuery: (String) -> Unit,
    onAttach: (Uri) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = IosTheme.colors
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val completionSource = LocalEditorSuggestionSource.current

    var discard by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf<TopicMetadataMenu?>(null) }
    var expanded by rememberSaveable(state.topicId) { mutableStateOf(false) }
    var tab by rememberSaveable(state.topicId) { mutableStateOf(ComposerExpandedTab.Edit) }
    val preview = tab == ComposerExpandedTab.Preview
    var subToolbar by remember { mutableStateOf<ComposerSubToolbar?>(null) }
    var editor by remember { mutableStateOf<RichMarkdownEditorView?>(null) }
    var linkLabel by remember { mutableStateOf<String?>(null) }
    var pollOpen by remember { mutableStateOf(false) }
    var pollTarget by remember { mutableStateOf<Pair<CreatorPollSpan, PollBuilderSpec>?>(null) }
    var gallery by remember { mutableStateOf<Pair<List<ImageViewerItem>, Int>?>(null) }
    var picking by remember { mutableStateOf(false) }

    val busy = state.saving || state.loading || state.uploading
    val canEditMeta = state.canEditMetadata && !busy
    val canEditBody = state.canEditBody && !state.loading
    val selectableCategories = state.rules?.selectableCategories.orEmpty()
    val allCategories = state.rules?.categories.orEmpty()
    val categoryMeta = remember(allCategories, selectableCategories, state.categoryId) {
        resolveTopicMetadataCategoryState(allCategories, selectableCategories, state.categoryId)
    }
    val currentCategory = categoryMeta.currentCategory
    val canEditCategory = canEditMeta && selectableCategories.isNotEmpty()
    val canEditTags = canEditMeta && state.rules?.canTagTopics == true

    fun flushEditor() {
        if (state.canEditBody) {
            editor?.exportToMarkdown()?.takeIf { it != state.raw }?.let(onRawChange)
        }
    }

    LaunchedEffect(canEditMeta) {
        if (!canEditMeta) menu = null
    }
    LaunchedEffect(state.loading, state.rawRevision) {
        if (!state.loading && state.canEditBody) {
            editor?.setInitialMarkdown(state.raw, state.attachments)
        }
    }
    LaunchedEffect(state.attachments) {
        if (state.canEditBody) editor?.syncAttachments(state.attachments)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        picking = false
        uri?.let(onAttach)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picking = false
        uri?.let(onAttach)
    }

    val canHandleBack = !discard && menu == null && linkLabel == null && !pollOpen && gallery == null && !picking
    val requestClose: () -> Unit = {
        if (!state.saving && !state.uploading) {
            flushEditor()
            menu = null
            subToolbar = null
            keyboard?.hide()
            editor?.clearFocus()
            focus.clearFocus(force = true)
            if (state.dirty) discard = true else onClose()
        }
    }

    BackHandler(enabled = canHandleBack) {
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
            title = when {
                expanded && state.title.isNotBlank() -> state.title.trim()
                state.original?.privateMessage == true -> "编辑私信"
                else -> "编辑话题"
            },
            onBack = requestClose,
            trailingWidth = 86.dp,
            trailing = {
                ComposerSendPill(
                    label = if (state.saving) "保存中" else "保存",
                    enabled = state.canSave,
                    submitting = state.saving,
                    onClick = {
                        flushEditor()
                        menu = null
                        subToolbar = null
                        focus.clearFocus()
                        keyboard?.hide()
                        onSave()
                    },
                )
            },
        )

        if (state.loading) {
            IosFullDivider()
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                IosLoadingBox()
            }
        } else if (state.original == null) {
            IosFullDivider()
            Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                state.error?.let { err ->
                    TopicMetadataErrorBanner(
                        message = UserErrorMessage.sanitize(err, "编辑信息加载失败"),
                        onRetry = if (state.saving) null else onRetry,
                    )
                }
            }
        } else {
            // 顶部标题 + 分类 / 等级 / 标签区（全屏专注正文编辑时收起，与发帖页完全一致）
            if (!expanded) {
                val titleStyle = IosTheme.type.headline.copy(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                BasicTextField(
                    value = state.title,
                    onValueChange = onTitle,
                    enabled = canEditMeta,
                    maxLines = 2,
                    textStyle = titleStyle.copy(color = if (canEditMeta) colors.label else colors.secondaryLabel),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = if (canEditBody) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (canEditBody) {
                                tab = ComposerExpandedTab.Edit
                                editor?.post { editor?.requestInputFocus() }
                            }
                        },
                        onDone = { focus.clearFocus(); keyboard?.hide() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
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
                if (changedTitle || !canEditBody) {
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        // 1. 分类胶囊 + 首页同款下拉菜单
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

                        // 2. Lv 等级胶囊 + 首页同款下拉菜单
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

                        // 3. 标签胶囊 + 发帖页同款搜索下拉菜单
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
                }
                IosFullDivider()
            }

            if (canEditBody) {
                // 正文头栏：与 TopicCreatorSheet 完全一致（正文 + 字数 + 编辑/预览分段 + 全屏专注）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("正文", style = IosTheme.type.headline, color = colors.label)
                    Text(
                        text = "${state.raw.length} 字",
                        style = IosTheme.type.caption,
                        color = colors.tertiaryLabel,
                        modifier = Modifier.weight(1f),
                    )
                    ComposerModeSegmentedControl(tab) { next ->
                        if (next != tab) {
                            flushEditor()
                            subToolbar = null
                            tab = next
                            if (next == ComposerExpandedTab.Preview) keyboard?.hide()
                            else editor?.post { editor?.requestInputFocus() }
                        }
                    }
                    ComposerHeaderIconButton(
                        contentDescription = if (expanded) "显示话题信息" else "专注正文编辑",
                        onClick = { expanded = !expanded },
                    ) {
                        FullscreenToggleIcon(isFullScreen = expanded, tint = colors.secondaryLabel)
                    }
                }

                val textColor = colors.label.toArgb()
                val hintColor = colors.tertiaryLabel.toArgb()
                val sizePx = with(LocalDensity.current) { 16.sp.toPx() }
                Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
                    AndroidView(
                        modifier = if (preview) Modifier.size(0.dp) else Modifier.fillMaxSize(),
                        factory = { ctx ->
                            RichMarkdownEditorView(ctx).apply {
                                hint = "编辑正文… 支持 Markdown"
                                val horizontalPadding = (4 * resources.displayMetrics.density).toInt()
                                setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
                                setInitialMarkdown(state.raw, state.attachments)
                                setOnMarkdownChangeListener(onRawChange)
                                setOnRemoveAttachmentListener(onRemoveAttachment)
                                setOnPreviewImageListener { url ->
                                    gallery = listOf(ImageViewerItem(url, url, "图片")) to 0
                                }
                                setOnEditPollListener { span, spec ->
                                    pollTarget = span to spec
                                    pollOpen = true
                                }
                                editor = this
                            }
                        },
                        update = { view ->
                            view.isEnabled = !state.loading && !state.saving && !preview
                            view.setInputFrozen(state.saving || state.uploading)
                            view.updateThemeColors(textColor, hintColor)
                            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
                            view.setCompletionSource(
                                completionSource,
                                categoryId = state.categoryId,
                                topicId = state.topicId,
                            )
                        },
                    )
                    if (preview) {
                        ComposerMarkdownPreviewPane(
                            raw = state.raw,
                            emojiUrls = state.emojiUrls,
                            attachments = state.attachments,
                            onPicture = { images, index -> gallery = images to index },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (state.uploading) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IosActivityIndicator(diameter = 14.dp)
                        Text("正在上传附件…", style = IosTheme.type.caption, color = colors.secondaryLabel)
                    }
                }

                val statusError = state.error ?: if (state.dirty) state.validationError else null
                if (statusError != null) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        TopicMetadataErrorBanner(
                            message = UserErrorMessage.sanitize(statusError, "保存失败，请核对后重试"),
                            onRetry = if (state.error != null && !state.saving) onRetry else null,
                        )
                    }
                }

                if (!preview) {
                    ExpandedMarkdownToolbar(
                        enabled = !busy,
                        allowPoll = true,
                        editor = editor,
                        openSub = subToolbar,
                        onToggleSub = { subToolbar = if (subToolbar == it) null else it },
                        onCloseSub = { subToolbar = null },
                        onLink = { subToolbar = null; linkLabel = editor?.currentSelectedText().orEmpty() },
                        onPickImage = {
                            subToolbar = null
                            picking = true
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickFile = {
                            subToolbar = null
                            picking = true
                            filePicker.launch(arrayOf("*/*"))
                        },
                        onOpenPoll = {
                            subToolbar = null
                            pollTarget = null
                            pollOpen = true
                            keyboard?.hide()
                        },
                    )
                }
            } else {
                // 仅可修改元信息（如无权编辑 1 楼正文）时，保留清晰直观的标签与规则区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.error?.let { err ->
                        TopicMetadataErrorBanner(
                            message = UserErrorMessage.sanitize(err, "操作失败，请稍后重试"),
                            onRetry = if (state.saving) null else onRetry,
                        )
                    }
                    if (!state.original.privateMessage) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "已选标签",
                                style = IosTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.label,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${state.tags.size} / 5", style = IosTheme.type.caption, color = colors.tertiaryLabel)
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
                                        onClick = { if (canEditTags) onTags(state.tags - tag) },
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
                    } else {
                        Text("私信仅支持修改标题", style = IosTheme.type.caption, color = colors.secondaryLabel)
                    }
                    if (state.dirty) {
                        state.validationError?.let { validationErr ->
                            TopicMetadataErrorBanner(message = validationErr)
                        }
                    }
                }
            }
        }
    }

    linkLabel?.let { initial ->
        MarkdownLinkInsertDialog(
            initialLabel = initial,
            onDismiss = { linkLabel = null },
            onConfirm = { label, url ->
                linkLabel = null
                editor?.applyMarkdownLink(label, url)
            },
        )
    }
    if (pollOpen) {
        PollBuilderDialog(
            suggestedPollName = remember(state.raw) { nextAvailablePollName(state.raw) },
            initialSpec = pollTarget?.second,
            onDismiss = { pollOpen = false; pollTarget = null },
            onConfirm = { spec ->
                val target = pollTarget
                if (target == null) editor?.insertPoll(spec) else editor?.updatePollSpan(target.first, spec)
                pollOpen = false
                pollTarget = null
            },
        )
    }
    gallery?.let { (images, index) ->
        ImageViewer(images = images, initialIndex = index) { gallery = null }
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
            Text("尚未保存的修改内容将不会提交。", style = IosTheme.type.footnote, color = colors.secondaryLabel)
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
