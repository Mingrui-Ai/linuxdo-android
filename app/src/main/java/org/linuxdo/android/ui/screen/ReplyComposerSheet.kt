package org.linuxdo.android.ui.screen

import android.net.Uri
import android.util.TypedValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.linuxdo.android.ui.ComposerTarget
import org.linuxdo.android.ui.ComposerUiState
import org.linuxdo.android.ui.CreatorPollSpan
import org.linuxdo.android.ui.LocalEditorSuggestionSource
import org.linuxdo.android.ui.PollBuilderDialog
import org.linuxdo.android.ui.PollBuilderSpec
import org.linuxdo.android.ui.RichMarkdownEditorView
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.composerBackKeys
import org.linuxdo.android.ui.design.rememberComposerDismiss
import org.linuxdo.android.ui.html.CookedBlock
import org.linuxdo.android.ui.html.replyMarkdownPreview
import org.linuxdo.android.ui.nextAvailablePollName

internal enum class ComposerExpandedTab { Edit, Preview }

internal enum class ComposerSubToolbar { Heading, Quote, Code, List, More }

/**
 * 回复 / 编辑 / 实时聊天共用的 Markdown 输入弹层。
 * 对齐 LINUX DO 云端 Discourse 新版编辑器（composer-redesign）底部工具栏体系，并兼顾移动端体验：
 * - 常态精简：低矮自适应输入框；底栏左起与云端一致（图片 / 附件 / 可选投票 | Aa），点 Aa 展开发言快捷条；
 * - 展开全屏：顶栏左侧收起 + 标题 + 字数，右侧「编辑 / 预览」分段胶囊 + 发表/发送胶囊；
 *   编辑态底部常驻与云端顺序一致的横向可滚工具栏：
 *   粗体(B) / 斜体(I) / 文本大小(T▾: 标题1-4、小) | 链接 / 引用(▾: 块引用、隐藏详细信息 [details="总结"]) / 代码(</>▾) / 图片 / 附件 | 列表(▾: 项目符号/编号/待办) / 更多(⊕: 隐藏详细信息、模糊剧透、表格、分割线、可选投票、脚注)；
 * - 预览态本地实时渲染 Markdown + Discourse 扩展语法。
 */
@Composable
internal fun ReplyComposerSheet(
    composer: ComposerUiState,
    emojiUrls: Map<String, String>,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onAttach: (Uri) -> Unit,
    onRemoveAttachment: (String) -> Unit = {},
) {
    val target = composer.target ?: return
    val heading = when (target) {
        is ComposerTarget.NewReply -> "发表评论"
        is ComposerTarget.ReplyTo -> "回复 @${target.username}"
        is ComposerTarget.Edit -> "编辑 #${target.postNumber}"
    }
    val sendText = if (target is ComposerTarget.Edit) "保存" else "发表"
    MarkdownComposerSheet(
        sessionKey = target,
        heading = heading,
        sendLabel = sendText,
        placeholder = "说点什么… 支持 Markdown",
        draft = composer.draft,
        attachments = composer.attachments,
        emojiUrls = emojiUrls,
        loadingDraft = composer.loadingDraft,
        remoteRevision = composer.remoteRevision,
        submitting = composer.submitting,
        uploading = composer.uploading,
        canSubmit = composer.canSubmit,
        error = composer.error,
        topicId = target.topicId,
        allowPoll = true,
        onDraftChange = onDraftChange,
        onSubmit = onSubmit,
        onClose = onClose,
        onAttach = onAttach,
        onRemoveAttachment = onRemoveAttachment,
    )
}

@Composable
internal fun MarkdownComposerSheet(
    sessionKey: Any,
    heading: String,
    sendLabel: String,
    placeholder: String,
    draft: String,
    attachments: List<org.linuxdo.android.data.AttachedMedia>,
    emojiUrls: Map<String, String>,
    loadingDraft: Boolean = false,
    remoteRevision: Long = 0L,
    submitting: Boolean = false,
    uploading: Boolean = false,
    canSubmit: Boolean = (draft.isNotBlank() || attachments.isNotEmpty()) && !submitting && !loadingDraft && !uploading,
    error: String? = null,
    topicId: Long? = null,
    allowPoll: Boolean = true,
    initialFullScreen: Boolean = false,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onAttach: (Uri) -> Unit,
    onRemoveAttachment: (String) -> Unit = {},
) {
    val completionSource = LocalEditorSuggestionSource.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isFullScreen by rememberSaveable(sessionKey) { mutableStateOf(initialFullScreen) }
    var expandedTab by rememberSaveable(sessionKey) { mutableStateOf(ComposerExpandedTab.Edit) }
    var quickMdOpen by rememberSaveable(sessionKey) { mutableStateOf(false) }
    var openSubToolbar by remember { mutableStateOf<ComposerSubToolbar?>(null) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkInitialText by remember { mutableStateOf("") }
    var showPollDialog by remember { mutableStateOf(false) }
    var editingPollTarget by remember { mutableStateOf<Pair<CreatorPollSpan, PollBuilderSpec>?>(null) }
    var previewGallery by remember { mutableStateOf<Pair<List<ImageViewerItem>, Int>?>(null) }

    var editorView by remember(sessionKey) { mutableStateOf<RichMarkdownEditorView?>(null) }
    var editorContentHeight by remember(sessionKey) { mutableIntStateOf(0) }
    val compactEditorHeight = with(LocalDensity.current) { editorContentHeight.toDp().coerceIn(48.dp, 152.dp) }
    var pickingAttachment by remember { mutableStateOf(false) }

    val isPreview = isFullScreen && expandedTab == ComposerExpandedTab.Preview
    val busy = submitting || uploading || loadingDraft

    val dismiss = rememberComposerDismiss(
        enabled = !showPollDialog && !showLinkDialog && previewGallery == null,
        canDismiss = !submitting,
        ignoreKeyboardHide = pickingAttachment || isFullScreen || showLinkDialog || showPollDialog,
    ) {
        editorView?.exportToMarkdown()?.let(onDraftChange)
        onClose()
    }

    // 切换回编辑态时确保同步展开态子菜单与键盘焦点
    LaunchedEffect(isFullScreen, expandedTab) {
        if (!isFullScreen) {
            expandedTab = ComposerExpandedTab.Edit
            openSubToolbar = null
        } else if (expandedTab == ComposerExpandedTab.Preview) {
            openSubToolbar = null
            editorView?.exportToMarkdown()?.let(onDraftChange)
            keyboardController?.hide()
        }
    }

    // 服务端草稿恢复或冲突选择后刷新内容，普通输入不重置光标。
    LaunchedEffect(sessionKey, loadingDraft, remoteRevision) {
        if (!loadingDraft) {
            editorView?.setInitialMarkdown(draft, attachments)
        }
    }

    // 弹框打开时立刻聚焦输入框并呼出软键盘，消除额外延迟避免卡顿感
    LaunchedEffect(editorView, loadingDraft) {
        if (!loadingDraft && editorView != null && !isPreview) {
            editorView?.requestInputFocus()
        }
    }

    LaunchedEffect(attachments) {
        editorView?.syncAttachments(attachments)
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickingAttachment = false
        uri?.let(onAttach)
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickingAttachment = false
        uri?.let(onAttach)
    }

    fun openLinkInsert() {
        linkInitialText = editorView?.currentSelectedText().orEmpty()
        openSubToolbar = null
        showLinkDialog = true
    }

    fun openPollInsert() {
        keyboardController?.hide()
        openSubToolbar = null
        editingPollTarget = null
        showPollDialog = true
    }

    fun triggerPickImage() {
        pickingAttachment = true
        openSubToolbar = null
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun triggerPickFile() {
        pickingAttachment = true
        openSubToolbar = null
        pickFile.launch(arrayOf("*/*"))
    }

    fun toggleSub(sub: ComposerSubToolbar) {
        openSubToolbar = if (openSubToolbar == sub) null else sub
    }

    val colors = IosTheme.colors
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .composerBackKeys(dismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // 背景半透明暗色遮罩，点击收起弹层
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isFullScreen) 0.55f else 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismiss,
                ),
        )

        val sheetHeightModifier = if (isFullScreen) {
            Modifier
                .imePadding()
                .height(maxHeight * 0.94f)
        } else {
            // 常态使用 graphicsLayer 跟随 IME 逐帧平移（0 relayout），收放软键盘丝滑贴合不掉帧；
            // 点击空白区域收起或键盘下滑时直接跟随键盘回落，无二次跳变。
            Modifier
                .graphicsLayer {
                    val imeBottom = imeInsets.getBottom(density)
                    val navBottom = navInsets.getBottom(density)
                    translationY = -(imeBottom - navBottom).coerceAtLeast(0).toFloat()
                }
                .heightIn(max = maxHeight * 0.85f)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(sheetHeightModifier)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(colors.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}, // 阻止点击穿透到遮罩
                ),
        ) {
            // 顶部极简抓手条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.tertiaryLabel.copy(alpha = 0.55f)),
                )
            }

            // 顶栏：常态 [标题 + 展开按钮]；展开态 [收起按钮 + 标题 + 字数 + 编辑/预览分段胶囊 + 发表胶囊]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isFullScreen) {
                    ComposerHeaderIconButton(
                        contentDescription = "收起为精简模式",
                        onClick = {
                            isFullScreen = false
                            expandedTab = ComposerExpandedTab.Edit
                            openSubToolbar = null
                        },
                    ) {
                        ComposerCollapseExpandIcon(expanded = true, tint = colors.label)
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = heading,
                        style = IosTheme.type.headline.copy(fontSize = 15.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isFullScreen && draft.isNotEmpty()) {
                        Text(
                            text = "${draft.length} 字",
                            style = IosTheme.type.caption.copy(fontSize = 11.sp),
                            color = colors.secondaryLabel,
                            maxLines = 1,
                        )
                    }
                }

                if (isFullScreen) {
                    ComposerModeSegmentedControl(
                        selectedTab = expandedTab,
                        onSelect = { tab ->
                            expandedTab = tab
                            if (tab == ComposerExpandedTab.Edit) {
                                editorView?.post { editorView?.requestInputFocus() }
                            }
                        },
                    )
                    ComposerSendPill(
                        label = sendLabel,
                        enabled = canSubmit,
                        submitting = submitting,
                        onClick = {
                            editorView?.exportToMarkdown()?.let(onDraftChange)
                            onSubmit()
                        },
                    )
                } else {
                    ComposerHeaderIconButton(
                        contentDescription = "展开全屏编辑器",
                        onClick = { isFullScreen = true },
                    ) {
                        ComposerCollapseExpandIcon(expanded = false, tint = colors.label)
                    }
                }
            }

            if (isFullScreen) {
                IosFullDivider()
            }

            // 中间编辑 / 预览正文区域
            val textColorInt = colors.label.toArgb()
            val hintColorInt = colors.tertiaryLabel.toArgb()
            val editorTextSizePx = with(LocalDensity.current) { 15.5.sp.toPx() }

            val editorBoxModifier = if (isFullScreen) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(compactEditorHeight)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.fieldBackground)
            }

            Box(modifier = editorBoxModifier) {
                if (loadingDraft) {
                    IosLoadingBox()
                } else {
                    // 始终挂载 AndroidView 避免切到「预览」再切回「编辑」时丢失光标、输入法状态与媒体/投票 Span
                    val editorVisibilityModifier = if (isPreview) {
                        Modifier.size(0.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }
                    AndroidView(
                        modifier = editorVisibilityModifier,
                        factory = { ctx ->
                            RichMarkdownEditorView(ctx).apply {
                                hint = placeholder
                                setOnDismissInputListener(dismiss)
                                updateThemeColors(textColorInt, hintColorInt)
                                setTextSize(TypedValue.COMPLEX_UNIT_PX, editorTextSizePx)
                                setCompletionSource(completionSource, topicId = topicId)
                                setOnContentHeightChangeListener { editorContentHeight = it }
                                setInitialMarkdown(draft, attachments)
                                setOnMarkdownChangeListener { newRaw -> onDraftChange(newRaw) }
                                setOnRemoveAttachmentListener { mediaId -> onRemoveAttachment(mediaId) }
                                setOnPreviewImageListener { previewUrl ->
                                    previewGallery = listOf(ImageViewerItem(previewUrl, previewUrl, "图片")) to 0
                                }
                                if (allowPoll) {
                                    setOnEditPollListener { span, spec ->
                                        editingPollTarget = Pair(span, spec)
                                        showPollDialog = true
                                    }
                                }
                                editorView = this
                                post { requestInputFocus() }
                            }
                        },
                        update = { view ->
                            view.hint = placeholder
                            view.setOnDismissInputListener(dismiss)
                            view.updateThemeColors(textColorInt, hintColorInt)
                            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, editorTextSizePx)
                            view.setCompletionSource(completionSource, topicId = topicId)
                            if (allowPoll) {
                                view.setOnEditPollListener { span, spec ->
                                    editingPollTarget = Pair(span, spec)
                                    showPollDialog = true
                                }
                            }
                            val density = view.resources.displayMetrics.density
                            val padH = ((if (isFullScreen) 4 else 13) * density).toInt()
                            val padV = ((if (isFullScreen) 12 else 11) * density).toInt()
                            if (view.paddingLeft != padH || view.paddingTop != padV) {
                                view.setPadding(padH, padV, padH, padV)
                            }
                        },
                    )

                    if (isPreview) {
                        ComposerMarkdownPreviewPane(
                            raw = draft,
                            emojiUrls = emojiUrls,
                            attachments = attachments,
                            onPicture = { images, idx -> previewGallery = images to idx },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // 仅显示必要错误或展开态上传中，不再展示草稿同步状态提示行（如“已与网页同步”“已保存到手机”）
            val hasStatusRow = error != null || (isFullScreen && uploading)
            if (hasStatusRow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (isFullScreen && uploading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IosActivityIndicator(diameter = 14.dp)
                            Text(
                                text = "上传中…",
                                style = IosTheme.type.caption,
                                color = colors.secondaryLabel,
                            )
                        }
                    }
                    error?.let { err ->
                        Text(
                            text = err,
                            style = IosTheme.type.footnote,
                            color = colors.destructive,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // 常态模式：底部工具栏与云端对齐（上滑出快捷格式 / 子菜单，底行放操作按钮 + 取消/发表）
            if (!isFullScreen) {
                AnimatedVisibility(
                    visible = quickMdOpen,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    CompactQuickMdStrip(
                        enabled = !busy,
                        onBold = { editorView?.applyMarkdownWrap("**", "**", "粗体文字") },
                        onItalic = { editorView?.applyMarkdownWrap("*", "*", "斜体文字") },
                        onHeading = { editorView?.applyMarkdownLinePrefix("## ", "标题") },
                        onLink = ::openLinkInsert,
                        onQuote = { editorView?.applyMarkdownLinePrefix("> ", "块引用") },
                        onDetails = {
                            editorView?.applyMarkdownBlock(
                                prefix = "[details=\"总结\"]\n",
                                suffix = "\n[/details]",
                                placeholder = "此文本将被隐藏",
                            )
                        },
                        onCode = { editorView?.applyMarkdownWrap("`", "`", "code") },
                        onList = { editorView?.applyMarkdownLinePrefix("- ", "列表项") },
                    )
                }

                IosFullDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        ComposerBarIconButton(
                            contentDescription = "插入图片",
                            enabled = !busy,
                            onClick = ::triggerPickImage,
                        ) {
                            ComposerImageIcon(tint = if (busy) colors.tertiaryLabel else colors.label)
                        }
                        ComposerBarIconButton(
                            contentDescription = "添加附件",
                            enabled = !busy,
                            onClick = ::triggerPickFile,
                        ) {
                            ComposerAttachmentIcon(tint = if (busy) colors.tertiaryLabel else colors.label)
                        }
                        if (allowPoll) {
                            ComposerBarIconButton(
                                contentDescription = "插入投票",
                                enabled = !busy,
                                active = showPollDialog,
                                onClick = ::openPollInsert,
                            ) {
                                ComposerPollIcon(
                                    tint = when {
                                        busy -> colors.tertiaryLabel
                                        showPollDialog -> colors.accent
                                        else -> colors.label
                                    },
                                )
                            }
                        }

                        ComposerVerticalDivider()

                        ComposerBarIconButton(
                            contentDescription = "常用 Markdown 格式",
                            enabled = !busy,
                            active = quickMdOpen,
                            onClick = { quickMdOpen = !quickMdOpen },
                        ) {
                            Text(
                                text = "Aa",
                                style = IosTheme.type.subheadline.copy(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = when {
                                    busy -> colors.tertiaryLabel
                                    quickMdOpen -> colors.accent
                                    else -> colors.label
                                },
                            )
                        }

                        if (uploading) {
                            Spacer(Modifier.width(6.dp))
                            IosActivityIndicator(diameter = 15.dp)
                            Text(
                                text = "上传中…",
                                style = IosTheme.type.caption,
                                color = colors.secondaryLabel,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }

                    ComposerSendPill(
                        label = sendLabel,
                        enabled = canSubmit,
                        submitting = submitting,
                        onClick = {
                            editorView?.exportToMarkdown()?.let(onDraftChange)
                            onSubmit()
                        },
                    )
                }
            }

            // 展开编辑态：严格对齐 LINUX DO 云端（Discourse composer-redesign）底部工具栏与分组顺序
            if (isFullScreen && !isPreview) {
                ExpandedMarkdownToolbar(
                    enabled = !busy,
                    allowPoll = allowPoll,
                    openSub = openSubToolbar,
                    onToggleSub = ::toggleSub,
                    onCloseSub = { openSubToolbar = null },
                    editor = editorView,
                    onLink = ::openLinkInsert,
                    onPickImage = ::triggerPickImage,
                    onPickFile = ::triggerPickFile,
                    onOpenPoll = ::openPollInsert,
                )
            }
        }
    }

    if (showLinkDialog) {
        MarkdownLinkInsertDialog(
            initialLabel = linkInitialText,
            onDismiss = {
                showLinkDialog = false
                editorView?.post { editorView?.requestInputFocus() }
            },
            onConfirm = { label, url ->
                showLinkDialog = false
                editorView?.applyMarkdownLink(label, url)
            },
        )
    }

    if (allowPoll && showPollDialog) {
        val currentRaw = editorView?.exportToMarkdown() ?: draft
        val suggestedPollName = remember(currentRaw) {
            nextAvailablePollName(currentRaw)
        }
        PollBuilderDialog(
            suggestedPollName = suggestedPollName,
            initialSpec = editingPollTarget?.second,
            onDismiss = {
                showPollDialog = false
                editingPollTarget = null
            },
            onConfirm = { resultSpec ->
                val targetPoll = editingPollTarget
                if (targetPoll != null) {
                    editorView?.updatePollSpan(targetPoll.first, resultSpec)
                } else {
                    editorView?.insertPoll(resultSpec)
                }
                showPollDialog = false
                editingPollTarget = null
            },
        )
    }

    previewGallery?.let { (images, idx) ->
        ImageViewer(images = images, initialIndex = idx) { previewGallery = null }
    }
}

@Composable
internal fun ComposerModeSegmentedControl(
    selectedTab: ComposerExpandedTab,
    onSelect: (ComposerExpandedTab) -> Unit,
) {
    val colors = IosTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.fieldBackground)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            ComposerExpandedTab.Edit to "编辑",
            ComposerExpandedTab.Preview to "预览",
        ).forEach { (tab, label) ->
            val active = selectedTab == tab
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) colors.card else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 11.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = IosTheme.type.caption.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) colors.label else colors.secondaryLabel,
                )
            }
        }
    }
}

@Composable
internal fun ComposerSendPill(
    label: String,
    enabled: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (enabled) colors.accent else colors.accent.copy(alpha = 0.36f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (submitting) {
                IosActivityIndicator(diameter = 14.dp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = IosTheme.type.subheadline.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
internal fun ComposerHeaderIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ComposerBarIconButton(
    contentDescription: String,
    enabled: Boolean,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = IosTheme.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (active) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ComposerVerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(16.dp)
            .background(IosTheme.colors.separator.copy(alpha = 0.55f)),
    )
}

@Composable
private fun CompactQuickMdStrip(
    enabled: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHeading: () -> Unit,
    onLink: () -> Unit,
    onQuote: () -> Unit,
    onDetails: () -> Unit,
    onCode: () -> Unit,
    onList: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickMdChip(enabled = enabled, contentDescription = "粗体", onClick = onBold) {
            Text("B", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "斜体", onClick = onItalic) {
            Text("I", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "文本大小", onClick = onHeading) {
            Text("T 标题", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "插入链接", onClick = onLink) {
            MdLinkIcon(tint = it, modifier = Modifier.size(13.dp))
            Text("链接", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "块引用", onClick = onQuote) {
            MdQuoteIcon(tint = it, modifier = Modifier.size(13.dp))
            Text("引用", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "隐藏详细信息", onClick = onDetails) {
            Text("▸ 折叠", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "代码", onClick = onCode) {
            MdCodeIcon(tint = it, modifier = Modifier.size(13.dp))
            Text("代码", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = it)
        }
        QuickMdChip(enabled = enabled, contentDescription = "列表", onClick = onList) {
            Text("• 列表", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = it)
        }
    }
}

@Composable
private fun QuickMdChip(
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    val colors = IosTheme.colors
    val tint = if (enabled) colors.label else colors.tertiaryLabel
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.card)
            .border(0.6.dp, colors.separator.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content(tint)
    }
}

@Composable
internal fun ExpandedMarkdownToolbar(
    enabled: Boolean,
    allowPoll: Boolean,
    openSub: ComposerSubToolbar?,
    onToggleSub: (ComposerSubToolbar) -> Unit,
    onCloseSub: () -> Unit,
    editor: RichMarkdownEditorView?,
    onLink: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onOpenPoll: () -> Unit,
) {
    val colors = IosTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card),
    ) {
        AnimatedVisibility(
            visible = openSub != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            openSub?.let { sub ->
                Column {
                    IosFullDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.groupedBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (sub) {
                            ComposerSubToolbar.Heading -> {
                                SubToolbarPill("标题 1", enabled) { editor?.applyMarkdownLinePrefix("# ", "标题 1"); onCloseSub() }
                                SubToolbarPill("标题 2", enabled) { editor?.applyMarkdownLinePrefix("## ", "标题 2"); onCloseSub() }
                                SubToolbarPill("标题 3", enabled) { editor?.applyMarkdownLinePrefix("### ", "标题 3"); onCloseSub() }
                                SubToolbarPill("标题 4", enabled) { editor?.applyMarkdownLinePrefix("#### ", "标题 4"); onCloseSub() }
                                SubToolbarPill("小", enabled) { editor?.applyMarkdownWrap("<small>", "</small>", "小号文字"); onCloseSub() }
                                SubToolbarPill("删除线", enabled) { editor?.applyMarkdownWrap("~~", "~~", "删除文字"); onCloseSub() }
                            }
                            ComposerSubToolbar.Quote -> {
                                SubToolbarPill("❝ 块引用", enabled) {
                                    editor?.applyMarkdownLinePrefix("> ", "块引用")
                                    onCloseSub()
                                }
                                SubToolbarPill("▸ 隐藏详细信息", enabled) {
                                    editor?.applyMarkdownBlock("[details=\"总结\"]\n", "\n[/details]", "此文本将被隐藏")
                                    onCloseSub()
                                }
                            }
                            ComposerSubToolbar.Code -> {
                                SubToolbarPill("`行内代码`", enabled) { editor?.applyMarkdownWrap("`", "`", "行内代码"); onCloseSub() }
                                SubToolbarPill("```代码块```", enabled) {
                                    editor?.applyMarkdownBlock("```\n", "\n```", "// 代码内容")
                                    onCloseSub()
                                }
                            }
                            ComposerSubToolbar.List -> {
                                SubToolbarPill("• 项目符号列表", enabled) { editor?.applyMarkdownLinePrefix("- ", "列表项"); onCloseSub() }
                                SubToolbarPill("1. 编号列表", enabled) { editor?.applyMarkdownLinePrefix("1. ", "列表项"); onCloseSub() }
                                SubToolbarPill("☑ 待办清单", enabled) { editor?.applyMarkdownLinePrefix("- [ ] ", "待办事项"); onCloseSub() }
                            }
                            ComposerSubToolbar.More -> {
                                SubToolbarPill("▸ 隐藏详细信息", enabled) {
                                    editor?.applyMarkdownBlock("[details=\"总结\"]\n", "\n[/details]", "此文本将被隐藏")
                                    onCloseSub()
                                }
                                SubToolbarPill("◐ 模糊剧透", enabled) {
                                    editor?.applyMarkdownWrap("[spoiler]", "[/spoiler]", "此文本将被模糊处理")
                                    onCloseSub()
                                }
                                SubToolbarPill("▦ 插入表格", enabled) { editor?.applyMarkdownTable(); onCloseSub() }
                                SubToolbarPill("— 分割线", enabled) { editor?.applyMarkdownDivider(); onCloseSub() }
                                if (allowPoll) {
                                    SubToolbarPill("构建投票", enabled) { onCloseSub(); onOpenPoll() }
                                }
                                SubToolbarPill("¹ 添加脚注", enabled) { editor?.applyMarkdownFootnote(); onCloseSub() }
                            }
                        }
                    }
                }
            }
        }

        // 与云端 LINUX DO（Discourse composer-redesign）一致的底部工具栏分组顺序：
        // [fontStyles: B, I, 文本大小(T▾)] | [insertions: 链接, 引用(▾含折叠总结), 代码(▾), 图片, 附件] | [extras: 列表(▾), 更多(⊕)]
        IosFullDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Group 1: fontStyles (bold, italic, heading/text size)
            ExpandedToolButton(
                contentDescription = "粗体",
                enabled = enabled,
                onClick = { onCloseSub(); editor?.applyMarkdownWrap("**", "**", "粗体文字") },
            ) {
                Text("B", fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = it)
            }
            ExpandedToolButton(
                contentDescription = "强调",
                enabled = enabled,
                onClick = { onCloseSub(); editor?.applyMarkdownWrap("*", "*", "斜体文字") },
            ) {
                Text("I", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, color = it)
            }
            ExpandedToolButton(
                contentDescription = "文本大小",
                enabled = enabled,
                active = openSub == ComposerSubToolbar.Heading,
                onClick = { onToggleSub(ComposerSubToolbar.Heading) },
            ) {
                Text("T", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = it)
                Text("▾", fontSize = 9.sp, color = it.copy(alpha = 0.7f))
            }

            ComposerVerticalDivider()

            // Group 2: insertions (link, blockquote + details, code, upload image/file)
            ExpandedToolButton(
                contentDescription = "链接",
                enabled = enabled,
                onClick = onLink,
            ) {
                MdLinkIcon(tint = it, modifier = Modifier.size(17.dp))
            }
            ExpandedToolButton(
                contentDescription = "引用与隐藏详细信息",
                enabled = enabled,
                active = openSub == ComposerSubToolbar.Quote,
                onClick = { onToggleSub(ComposerSubToolbar.Quote) },
            ) {
                MdQuoteIcon(tint = it, modifier = Modifier.size(17.dp))
                Text("▾", fontSize = 9.sp, color = it.copy(alpha = 0.7f))
            }
            ExpandedToolButton(
                contentDescription = "预先格式化的文本",
                enabled = enabled,
                active = openSub == ComposerSubToolbar.Code,
                onClick = { onToggleSub(ComposerSubToolbar.Code) },
            ) {
                MdCodeIcon(tint = it, modifier = Modifier.size(17.dp))
                Text("▾", fontSize = 9.sp, color = it.copy(alpha = 0.7f))
            }
            ExpandedToolButton(
                contentDescription = "上传图片",
                enabled = enabled,
                onClick = onPickImage,
            ) {
                ComposerImageIcon(tint = it, modifier = Modifier.size(19.dp))
            }
            ExpandedToolButton(
                contentDescription = "添加附件",
                enabled = enabled,
                onClick = onPickFile,
            ) {
                ComposerAttachmentIcon(tint = it, modifier = Modifier.size(19.dp))
            }

            ComposerVerticalDivider()

            // Group 3: extras (list, options +)
            ExpandedToolButton(
                contentDescription = "列表",
                enabled = enabled,
                active = openSub == ComposerSubToolbar.List,
                onClick = { onToggleSub(ComposerSubToolbar.List) },
            ) {
                MdListIcon(tint = it, modifier = Modifier.size(17.dp))
                Text("▾", fontSize = 9.sp, color = it.copy(alpha = 0.7f))
            }
            ExpandedToolButton(
                contentDescription = "更多选项",
                enabled = enabled,
                active = openSub == ComposerSubToolbar.More,
                onClick = { onToggleSub(ComposerSubToolbar.More) },
            ) {
                MdPlusCircleIcon(tint = it, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ExpandedToolButton(
    contentDescription: String,
    enabled: Boolean,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    val colors = IosTheme.colors
    val tint = when {
        !enabled -> colors.tertiaryLabel
        active -> colors.accent
        else -> colors.label
    }
    Row(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
        content(tint)
    }
}

@Composable
private fun SubToolbarPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(0.6.dp, colors.separator.copy(alpha = 0.65f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = IosTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = if (enabled) colors.label else colors.tertiaryLabel,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ComposerMarkdownPreviewPane(
    raw: String,
    emojiUrls: Map<String, String>,
    attachments: List<org.linuxdo.android.data.AttachedMedia>,
    onPicture: (List<ImageViewerItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IosTheme.colors
    var blocks by remember(raw, emojiUrls, attachments) { mutableStateOf<List<CookedBlock>?>(null) }
    var parseError by remember(raw) { mutableStateOf(false) }

    LaunchedEffect(raw, emojiUrls, attachments) {
        parseError = false
        if (raw.isBlank()) {
            blocks = emptyList()
            return@LaunchedEffect
        }
        try {
            blocks = withContext(Dispatchers.Default) {
                replyMarkdownPreview(raw, emojiUrls, attachments)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            parseError = true
            blocks = emptyList()
        }
    }

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        when {
            raw.isBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无内容，切回「编辑」输入 Markdown 后即可在此实时预览",
                        style = IosTheme.type.footnote.copy(fontSize = 13.5.sp),
                        color = colors.tertiaryLabel,
                    )
                }
            }
            parseError -> {
                Text(
                    text = "预览解析暂时不可用，请切回编辑检查语法",
                    style = IosTheme.type.footnote,
                    color = colors.secondaryLabel,
                )
            }
            blocks == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IosActivityIndicator(diameter = 18.dp)
                }
            }
            else -> {
                CookedContent(
                    blocks = blocks!!,
                    onPicture = onPicture,
                )
            }
        }
    }
}

@Composable
internal fun MarkdownLinkInsertDialog(
    initialLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (label: String, url: String) -> Unit,
) {
    val colors = IosTheme.colors
    var label by remember { mutableStateOf(initialLabel) }
    var url by remember { mutableStateOf("") }
    val labelFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(60)
        if (initialLabel.isNotBlank()) urlFocus.requestFocus() else labelFocus.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 330.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(0.5.dp, colors.separator.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "插入链接",
                style = IosTheme.type.headline.copy(fontSize = 15.5.sp),
                fontWeight = FontWeight.Bold,
                color = colors.label,
            )

            ComposerDialogField(
                value = label,
                onValueChange = { label = it },
                placeholder = "链接显示文字（可选）",
                modifier = Modifier.focusRequester(labelFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { urlFocus.requestFocus() }),
            )

            ComposerDialogField(
                value = url,
                onValueChange = { url = it },
                placeholder = "https://linux.do",
                modifier = Modifier.focusRequester(urlFocus),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(label, url) }),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.fieldBackground)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "取消",
                        style = IosTheme.type.footnote.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.label,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.accent)
                        .clickable { onConfirm(label, url) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "插入",
                        style = IosTheme.type.footnote.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = IosTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.fieldBackground)
            .border(0.6.dp, colors.separator.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = IosTheme.type.footnote.copy(fontSize = 13.5.sp),
                color = colors.tertiaryLabel,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = IosTheme.type.footnote.copy(fontSize = 13.5.sp, color = colors.label),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── 原型对齐的线性矢量图标（1.9px 圆角描边） ────────────────────────────────────

/** 展开（对角外扩双箭头） / 收起（对角内收双箭头） */
@Composable
private fun ComposerCollapseExpandIcon(expanded: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (!expanded) {
                val p1 = Path().apply { moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f) }
                val p2 = Path().apply { moveTo(9f, 21f); lineTo(3f, 21f); lineTo(3f, 15f) }
                drawPath(p1, tint, style = stroke)
                drawPath(p2, tint, style = stroke)
                drawLine(tint, Offset(21f, 3f), Offset(14f, 10f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                drawLine(tint, Offset(3f, 21f), Offset(10f, 14f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            } else {
                val p1 = Path().apply { moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f) }
                val p2 = Path().apply { moveTo(20f, 10f); lineTo(14f, 10f); lineTo(14f, 4f) }
                drawPath(p1, tint, style = stroke)
                drawPath(p2, tint, style = stroke)
                drawLine(tint, Offset(14f, 10f), Offset(21f, 3f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                drawLine(tint, Offset(3f, 21f), Offset(10f, 14f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun MdLinkIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val left = Path().apply {
                moveTo(10f, 13f)
                cubicTo(12f, 15.6f, 15.8f, 15.6f, 17.8f, 13.6f)
                lineTo(20.6f, 10.8f)
                cubicTo(22.6f, 8.8f, 22.6f, 5.4f, 20.6f, 3.4f)
                cubicTo(18.6f, 1.4f, 15.2f, 1.4f, 13.2f, 3.4f)
                lineTo(11.6f, 5f)
            }
            val right = Path().apply {
                moveTo(14f, 11f)
                cubicTo(12f, 8.4f, 8.2f, 8.4f, 6.2f, 10.4f)
                lineTo(3.4f, 13.2f)
                cubicTo(1.4f, 15.2f, 1.4f, 18.6f, 3.4f, 20.6f)
                cubicTo(5.4f, 22.6f, 8.8f, 22.6f, 10.8f, 20.6f)
                lineTo(12.4f, 19f)
            }
            drawPath(left, tint, style = stroke)
            drawPath(right, tint, style = stroke)
        }
    }
}

@Composable
private fun MdQuoteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val q1 = Path().apply {
                moveTo(4f, 11f)
                lineTo(9.5f, 11f)
                lineTo(9.5f, 5.5f)
                lineTo(4f, 5.5f)
                close()
                moveTo(9.5f, 11f)
                cubicTo(9.5f, 15.5f, 7.5f, 18f, 4.5f, 19f)
            }
            val q2 = Path().apply {
                moveTo(14.5f, 11f)
                lineTo(20f, 11f)
                lineTo(20f, 5.5f)
                lineTo(14.5f, 5.5f)
                close()
                moveTo(20f, 11f)
                cubicTo(20f, 15.5f, 18f, 18f, 15f, 19f)
            }
            drawPath(q1, tint, style = stroke)
            drawPath(q2, tint, style = stroke)
        }
    }
}

@Composable
private fun MdCodeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val right = Path().apply { moveTo(16f, 18f); lineTo(22f, 12f); lineTo(16f, 6f) }
            val left = Path().apply { moveTo(8f, 6f); lineTo(2f, 12f); lineTo(8f, 18f) }
            drawPath(right, tint, style = stroke)
            drawPath(left, tint, style = stroke)
        }
    }
}

@Composable
private fun MdListIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            drawLine(tint, Offset(8f, 6f), Offset(21f, 6f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            drawLine(tint, Offset(8f, 12f), Offset(21f, 12f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            drawLine(tint, Offset(8f, 18f), Offset(21f, 18f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            drawCircle(tint, radius = 1.4f, center = Offset(3.5f, 6f))
            drawCircle(tint, radius = 1.4f, center = Offset(3.5f, 12f))
            drawCircle(tint, radius = 1.4f, center = Offset(3.5f, 18f))
        }
    }
}

@Composable
private fun MdPlusCircleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(tint, radius = 9f, center = Offset(12f, 12f), style = stroke)
            drawLine(tint, Offset(12f, 8f), Offset(12f, 16f), strokeWidth = 1.9f, cap = StrokeCap.Round)
            drawLine(tint, Offset(8f, 12f), Offset(16f, 12f), strokeWidth = 1.9f, cap = StrokeCap.Round)
        }
    }
}
