package org.linuxdo.android.ui.screen

import android.net.Uri
import android.util.TypedValue
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.linuxdo.android.data.DiscourseTagSearchResult
import org.linuxdo.android.ui.CategoriesUiState
import org.linuxdo.android.ui.CreatorPollSpan
import org.linuxdo.android.ui.LocalEditorSuggestionSource
import org.linuxdo.android.ui.PollBuilderDialog
import org.linuxdo.android.ui.PollBuilderSpec
import org.linuxdo.android.ui.RichMarkdownEditorView
import org.linuxdo.android.ui.TopicCreatorUiState
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosNavBar
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.nextAvailablePollName

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopicCreatorSheet(
    state: TopicCreatorUiState,
    categories: CategoriesUiState,
    onTogglePinCategory: (Long) -> Unit,
    onTitleChange: (String) -> Unit,
    onRawChange: (String) -> Unit,
    onCategoryChange: (Int?) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onSearchTags: suspend (Int?, String, List<String>) -> DiscourseTagSearchResult,
    onAttach: (Uri) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSaveDraft: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onClearNotice: () -> Unit,
) {
    val colors = IosTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val completionSource = LocalEditorSuggestionSource.current
    var editor by remember { mutableStateOf<RichMarkdownEditorView?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(ComposerExpandedTab.Edit) }
    val preview = tab == ComposerExpandedTab.Preview
    var menu by remember { mutableStateOf<TopicCreatorMenu?>(null) }
    var subToolbar by remember { mutableStateOf<ComposerSubToolbar?>(null) }
    var linkLabel by remember { mutableStateOf<String?>(null) }
    var pollOpen by remember { mutableStateOf(false) }
    var pollTarget by remember { mutableStateOf<Pair<CreatorPollSpan, PollBuilderSpec>?>(null) }
    var gallery by remember { mutableStateOf<Pair<List<ImageViewerItem>, Int>?>(null) }
    var picking by remember { mutableStateOf(false) }
    val busy = state.submitting || state.loadingDraft || state.savingDraft || state.uploading

    fun flushEditor() {
        editor?.exportToMarkdown()?.takeIf { it != state.raw }?.let(onRawChange)
    }
    val canHandleBack = menu == null && linkLabel == null && !pollOpen && gallery == null && !picking
    val dismiss: () -> Unit = {
        if (canHandleBack && !state.submitting && !state.savingDraft) {
            flushEditor()
            keyboard?.hide()
            editor?.clearFocus()
            focusManager.clearFocus(force = true)
            onClose()
        }
    }
    // 整页编辑先让系统处理 IME 返回；收起键盘本身不关闭页面或触发草稿同步。
    // 部分输入法将返回交给 Activity，此时也只隐藏键盘，再次返回才退出编辑。
    BackHandler(enabled = canHandleBack) {
        if (imeVisible) keyboard?.hide() else dismiss()
    }
    DisposableEffect(keyboard) {
        onDispose { keyboard?.hide() }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        picking = false
        uri?.let(onAttach)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picking = false
        uri?.let(onAttach)
    }

    LaunchedEffect(state.loadingDraft, state.remoteRevision) {
        if (!state.loadingDraft) editor?.setInitialMarkdown(state.raw, state.attachments)
    }
    LaunchedEffect(state.attachments) { editor?.syncAttachments(state.attachments) }
    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(2200)
            onClearNotice()
        }
    }

    Column(Modifier.fillMaxSize().background(colors.card)
        // 这是覆盖在导航栈上的整页，空白处的触摸也应落在发帖页。
        .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)).imePadding()) {
        IosNavBar(
            title = if (expanded && state.title.isNotBlank()) state.title.trim()
                else if (state.isReplyDraft) "回复草稿" else "新话题",
            onBack = dismiss,
            trailingWidth = 114.dp,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (state.savingDraft) "保存中" else "存草稿", style = IosTheme.type.footnote,
                        color = if (state.canSaveDraft) colors.accent else colors.tertiaryLabel,
                        modifier = Modifier.clickable(enabled = state.canSaveDraft) { flushEditor(); onSaveDraft(false) }
                            .padding(horizontal = 6.dp, vertical = 8.dp))
                    ComposerSendPill(if (state.isReplyDraft) "发送" else "发布", state.canSubmit, state.submitting) {
                        flushEditor()
                        onSubmit()
                    }
                }
            },
        )
        if (!expanded) {
            if (state.isReplyDraft) {
                Text(state.title.ifBlank { "回复话题 #${state.replyTopicId}" }, style = IosTheme.type.headline,
                    color = colors.label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            } else {
                val titleStyle = IosTheme.type.headline.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = state.title, onValueChange = onTitleChange, enabled = !state.loadingDraft && !state.submitting,
                    textStyle = titleStyle.copy(color = colors.label), cursorBrush = SolidColor(colors.accent), maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        tab = ComposerExpandedTab.Edit
                        editor?.post { editor?.requestInputFocus() }
                    }),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp),
                    decorationBox = { inner -> Box {
                        if (state.title.isEmpty()) Text("话题标题", style = titleStyle, color = colors.tertiaryLabel)
                        inner()
                    } },
                )
                TopicCreatorFields(categories, state.categoryId, state.tags, menu,
                    onMenuChange = { if (it == null || !busy) menu = it }, onTogglePinCategory = onTogglePinCategory,
                    onCategoryChange = onCategoryChange, onTagsChange = onTagsChange, onSearchTags = onSearchTags)
            }
            IosFullDivider()
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("正文", style = IosTheme.type.headline, color = colors.label)
            Text("${state.raw.length} 字", style = IosTheme.type.caption, color = colors.tertiaryLabel, modifier = Modifier.weight(1f))
            ComposerModeSegmentedControl(tab) { next ->
                if (next != tab) {
                    flushEditor()
                    subToolbar = null
                    tab = next
                    if (next == ComposerExpandedTab.Preview) keyboard?.hide()
                    else editor?.post { editor?.requestInputFocus() }
                }
            }
            ComposerHeaderIconButton(if (expanded) "显示话题信息" else "专注正文编辑", { expanded = !expanded }) {
                FullscreenToggleIcon(isFullScreen = expanded, tint = colors.secondaryLabel)
            }
        }
        val textColor = colors.label.toArgb()
        val hintColor = colors.tertiaryLabel.toArgb()
        val sizePx = with(LocalDensity.current) { 16.sp.toPx() }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
            AndroidView(
                // 发帖是整页编辑，正文使用工具栏上方的全部余量，长文在原生编辑器内滚动。
                modifier = if (preview) Modifier.size(0.dp) else Modifier.fillMaxSize(),
                factory = { ctx -> RichMarkdownEditorView(ctx).apply {
                    hint = "写下正文…"
                    val horizontalPadding = (4 * resources.displayMetrics.density).toInt()
                    setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
                    setInitialMarkdown(state.raw, state.attachments)
                    setOnMarkdownChangeListener(onRawChange)
                    setOnRemoveAttachmentListener(onRemoveAttachment)
                    setOnPreviewImageListener { url -> gallery = listOf(ImageViewerItem(url, url, "图片")) to 0 }
                    setOnEditPollListener { span, spec -> pollTarget = span to spec; pollOpen = true }
                    editor = this
                } },
                update = { view ->
                    view.isEnabled = !state.loadingDraft && !state.submitting && !preview
                    view.updateThemeColors(textColor, hintColor)
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
                    view.setCompletionSource(completionSource, categoryId = state.categoryId, topicId = state.replyTopicId)
                },
            )
            if (preview) ComposerMarkdownPreviewPane(state.raw, state.emojiUrls, state.attachments,
                onPicture = { images, index -> gallery = images to index }, modifier = Modifier.fillMaxSize())
        }
        if (state.loadingDraft || state.uploading) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IosActivityIndicator(diameter = 14.dp)
                Text(if (state.uploading) "正在上传附件…" else "正在读取草稿…", style = IosTheme.type.caption, color = colors.secondaryLabel)
            }
        }
        val status = state.error ?: state.notice
        if (status != null) Text(status, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
            style = IosTheme.type.footnote, color = if (state.error != null) colors.destructive else colors.secondaryLabel)
        if (!preview) ExpandedMarkdownToolbar(
            enabled = !busy, allowPoll = true, editor = editor, openSub = subToolbar,
            onToggleSub = { subToolbar = if (subToolbar == it) null else it }, onCloseSub = { subToolbar = null },
            onLink = { subToolbar = null; linkLabel = editor?.currentSelectedText().orEmpty() },
            onPickImage = {
                subToolbar = null
                picking = true
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickFile = { subToolbar = null; picking = true; filePicker.launch(arrayOf("*/*")) },
            onOpenPoll = { subToolbar = null; pollTarget = null; pollOpen = true; keyboard?.hide() },
        )
    }
    linkLabel?.let { initial -> MarkdownLinkInsertDialog(initial, onDismiss = { linkLabel = null }, onConfirm = { label, url ->
        linkLabel = null
        editor?.applyMarkdownLink(label, url)
    }) }
    if (pollOpen) {
        PollBuilderDialog(suggestedPollName = remember(state.raw) { nextAvailablePollName(state.raw) }, initialSpec = pollTarget?.second,
            onDismiss = { pollOpen = false; pollTarget = null }, onConfirm = { spec ->
                val target = pollTarget
                if (target == null) editor?.insertPoll(spec) else editor?.updatePollSpan(target.first, spec)
                pollOpen = false
                pollTarget = null
            })
    }
    gallery?.let { (images, index) -> ImageViewer(images = images, initialIndex = index) { gallery = null } }
}
