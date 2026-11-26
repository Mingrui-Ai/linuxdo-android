package org.linuxdo.android.ui.screen

import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.ui.design.ForumTagText
import org.linuxdo.android.ui.html.CookedParser

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import org.linuxdo.android.data.avatarUrlOf

import org.linuxdo.android.data.TopicPage
import org.linuxdo.android.data.TopicNotificationLevel
import org.linuxdo.android.data.PostFlagReason
import org.linuxdo.android.data.FloatingAction
import org.linuxdo.android.data.ExcerptCache
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import kotlinx.coroutines.Job
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.min
import kotlin.math.roundToInt
import org.jsoup.Jsoup
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.Topic
import org.linuxdo.android.data.originalImageUrl
import org.linuxdo.android.data.thumbnailUrlForList
import org.linuxdo.android.net.IMAGE_AUTO_RETRY_LIMIT
import org.linuxdo.android.net.awaitImageRetrySlot
import org.linuxdo.android.net.isTransientImageError
import org.linuxdo.android.data.TopicSortOrder
import org.linuxdo.android.data.HomeFeedType
import org.linuxdo.android.data.TopicListItem
import org.linuxdo.android.ui.TopicUnreadDotPosition
import org.linuxdo.android.ui.design.SectionHeader
import org.linuxdo.android.ui.design.rememberTabBoundaryFeedback
import org.linuxdo.android.ui.design.tabSwipeGesture
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.FeedItemDivider
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosLargeTitleScaffold
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.IosMetrics
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.UserAvatar
import org.linuxdo.android.ui.design.avatarShape
import org.linuxdo.android.ui.format.RelativeTime

data class TopicListUiState(
    val items: List<TopicListItem> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val initialLoading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val pendingPage: TopicPage? = null,
    val incomingTopicIds: Set<Long> = emptySet(),
    val highlightedTopicIds: Set<Long> = emptySet(),
    val needsRefresh: Boolean = false,
)

/** "最新"与"某分类下的帖子"用同一个列表页,只是标题与数据源不同。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopicListScreen(
    title: String,
    state: TopicListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onOpenItem: ((TopicListItem) -> Unit)? = null,
    onRemoveBookmark: ((TopicListItem, (String) -> Unit) -> Unit)? = null,
    onOpenUser: (String, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showLargeTitle: Boolean = true,
    active: Boolean = true,
    scrollToTopRequest: Int = 0,
    trailing: @Composable () -> Unit = {},
    showHeaderActions: Boolean = false,
    homeLeading: @Composable () -> Unit = {},
    homeFilter: org.linuxdo.android.data.HomeFilterState? = null,
    homeCategories: List<Category> = emptyList(),
    homeCategoriesLoading: Boolean = false,
    homeCategoryOrder: List<Long> = emptyList(),
    homePinnedCategoryIds: List<Long> = emptyList(),
    homeTags: List<org.linuxdo.android.data.DiscourseTag> = emptyList(),
    homeTagsLoading: Boolean = false,
    homeSearchedTags: List<org.linuxdo.android.data.DiscourseTag> = emptyList(),
    homePinnedTags: List<String> = emptyList(),
    homeTagOrder: List<String> = emptyList(),
    homeTagSearchQuery: String = "",
    onSelectHomeCategory: ((Category?) -> Unit)? = null,
    onSelectHomeLevel: ((Int?) -> Unit)? = null,
    onSelectHomeTag: ((String?) -> Unit)? = null,
    onClearHomeFilters: (() -> Unit)? = null,
    onTogglePinCategory: ((Long) -> Unit)? = null,
    onMoveCategoryOrder: ((Long, Int) -> Unit)? = null,
    onSaveCategoryOrder: ((List<Long>) -> Unit)? = null,
    onResetCategoryOrder: (() -> Unit)? = null,
    onTogglePinTag: ((String) -> Unit)? = null,
    onMoveTagOrder: ((String, Int) -> Unit)? = null,
    onSaveTagOrder: ((List<String>) -> Unit)? = null,
    onResetTagOrder: (() -> Unit)? = null,
    onEnsureHomeCategories: (() -> Unit)? = null,
    onEnsureHomeTags: (() -> Unit)? = null,
    onHomeTagSearchQueryChange: ((String) -> Unit)? = null,
    currentFeed: HomeFeedType? = null,
    miniAppHomePullEnabled: Boolean = false,
    onOpenMiniApps: (() -> Unit)? = null,
    feedStates: Map<HomeFeedType, TopicListUiState> = emptyMap(),
    onFeedSelect: ((HomeFeedType) -> Unit)? = null,
    onPreloadFeed: ((HomeFeedType) -> Unit)? = null,
    hasUnread: Boolean = false,
    categoryVariants: List<Category> = emptyList(),
    tagTitle: String? = null,
    currentCategoryId: Int? = null,
    onSelectCategoryVariant: ((Category) -> Unit)? = null,
    onApplyIncoming: (() -> Unit)? = null,
    onSetNotificationLevel: ((Topic, TopicNotificationLevel) -> Unit)? = null,
    onCreateTopic: (() -> Unit)? = null,
    onBookmarkTopic: ((Topic, (String) -> Unit) -> Unit)? = null,
    onFlagTopic: ((Topic, Int, String?, () -> Unit, (String) -> Unit) -> Unit)? = null,
    sortOrder: TopicSortOrder = TopicSortOrder.Default,
    sortAscending: Boolean = false,
    onSortChange: (TopicSortOrder) -> Unit = {},
    sortEnabled: Boolean = true,
    floatingBallEnabled: Boolean = false,
    floatingBallActions: List<FloatingAction> = FloatingAction.DEFAULT_ACTIONS,
    floatingBallXRatio: Float = 0.95f,
    floatingBallYRatio: Float = 1.0f,
    floatingBallOnRight: Boolean = true,
    onFloatingBallPositionChange: (Float, Float) -> Unit = { _, _ -> },
    onNavigateToDrafts: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    onNavigateToCategories: (() -> Unit)? = null,
    onNavigateToHistory: (() -> Unit)? = null,
    onNavigateToConnect: (() -> Unit)? = null,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToLdcStore: (() -> Unit)? = null,
    onNavigateToLdcMail: (() -> Unit)? = null,
    onNavigateToBookmarks: (() -> Unit)? = null,
    onNavigateToFollowing: (() -> Unit)? = null,
    onNavigateToLikes: (() -> Unit)? = null,
    onNavigateToReplies: (() -> Unit)? = null,
    feedGraphicMode: Boolean = true,
    feedDisplayMode: String = "no_excerpt",
    showTopicUnreadDot: Boolean = true,
    topicUnreadDotPosition: TopicUnreadDotPosition = TopicUnreadDotPosition.Avatar,
    onPrefetchExcerpt: ((Any, List<Long>, () -> Set<Long>) -> Unit)? = null,
    onScrollBusyChanged: ((Any, Boolean) -> Unit)? = null,
    blockedUsernames: Set<String> = emptySet(),
) {
    // 每个 feed 的滚动位置用 rememberLazyListState（内部走 rememberSaveable），
    // 确保在 recomposition 或临时移出组合树后仍能还原 firstVisibleItemIndex + scrollOffset。
    // 旧方案 remember { mutableMapOf() } + LazyListState() 不走 saveable，回复后
    // refreshTopic silent 触发大量 state 更新时极易丢失位置。
    // 首页仅保留 Latest 与 Hot，各自保存滚动位置。
    // 滑过的卡片短暂保留供回滑复用，避免额外预量屏幕外的卡片争抢当前帧。
    val homeCacheWindow = remember { LazyLayoutCacheWindow(ahead = 0.dp, behind = 224.dp) }
    val feedStateLatest = rememberLazyListState(cacheWindow = homeCacheWindow)
    val feedStateHot = rememberLazyListState(cacheWindow = homeCacheWindow)
    val categoryListStates = remember { mutableMapOf<Int, LazyListState>() }
    val defaultListState = rememberLazyListState()
    var pendingSettledFeed by remember { mutableStateOf<HomeFeedType?>(null) }
    LaunchedEffect(currentFeed) {
        if (currentFeed != null && currentFeed == pendingSettledFeed) {
            pendingSettledFeed = null
        }
    }
    val effectiveFeed = pendingSettledFeed ?: currentFeed
    val effectiveState = if (effectiveFeed != null && effectiveFeed != currentFeed) {
        feedStates[effectiveFeed] ?: TopicListUiState(initialLoading = true)
    } else {
        state
    }
    // 将各 feed 的 rememberLazyListState 对象放入一个普通 Map，供统一访问。
    // 不用 getOrPut 动态创建，每个已知 feed 提前声明好对应的 saveable state。
    val feedListStates: Map<HomeFeedType, LazyListState> = remember(
        feedStateLatest, feedStateHot
    ) {
        mapOf(
            HomeFeedType.Latest to feedStateLatest,
            HomeFeedType.Hot to feedStateHot,
        )
    }
    val listState = when {
        effectiveFeed != null -> feedListStates[effectiveFeed] ?: feedStateLatest
        currentCategoryId != null -> categoryListStates.getOrPut(currentCategoryId) { LazyListState() }
        else -> defaultListState
    }
    val scrollBusySource = remember { Any() }
    val reportScrollBusy by rememberUpdatedState(onScrollBusyChanged)
    LaunchedEffect(active, listState) {
        if (!active) {
            reportScrollBusy?.invoke(scrollBusySource, false)
            return@LaunchedEffect
        }
        snapshotFlow { listState.isScrollInProgress }.collect { busy ->
            reportScrollBusy?.invoke(scrollBusySource, busy)
        }
    }
    DisposableEffect(scrollBusySource) {
        onDispose { onScrollBusyChanged?.invoke(scrollBusySource, false) }
    }

    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var noticeToast by remember { mutableStateOf<String?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var flagTargetTopic by remember { mutableStateOf<Topic?>(null) }
    var flagSubmitting by remember { mutableStateOf(false) }
    var flagError by remember { mutableStateOf<String?>(null) }

    fun showNotice(message: String) {
        noticeToast = message
        scope.launch {
            delay(2500)
            if (noticeToast == message) noticeToast = null
        }
    }

    val copyTopicLink: (Topic) -> Unit = { topic ->
        clipboardManager.setText(AnnotatedString("https://linux.do/t/topic/${topic.id}"))
        showNotice("已复制话题链接")
    }
    val bookmarkTopicAction: ((Topic) -> Unit)? = if (onBookmarkTopic != null) {
        { topic -> onBookmarkTopic(topic) { msg -> showNotice(msg) } }
    } else null
    val flagTopicAction: ((Topic) -> Unit)? = if (onFlagTopic != null) {
        { topic ->
            flagError = null
            flagSubmitting = false
            flagTargetTopic = topic
        }
    } else null

    val hasCategoryDropdown = categoryVariants.size > 1 && onSelectCategoryVariant != null

    // 距底部还有 4 项时就开始取下一页,滚到底时数据通常已经就位。
    // 必须要求列表已有真实数据:空列表时占位项也会让 lastVisible 命中阈值,
    // 配合"失败不更新 page"就会变成每秒重打同一页的死循环。
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(active, nearEnd, state.hasMore, state.loadingMore, state.items.size, state.error) {
        val canLoadMore = active && state.hasMore && !state.refreshing && !state.initialLoading &&
            !state.loadingMore &&
            state.items.isNotEmpty() &&
            state.error == null
        if (nearEnd && canLoadMore) onLoadMore()
    }


    val filteredItems = remember(effectiveState.items, blockedUsernames) {
        if (blockedUsernames.isEmpty()) effectiveState.items
        else effectiveState.items.filterNot { it.authorUsername?.lowercase() in blockedUsernames }
    }

    val (pinnedGroups, plainItems) = remember(filteredItems) { pinnedPrefixGroups(filteredItems) }
    val previewKeyPrefix = effectiveFeed?.name ?: currentCategoryId ?: "topic"
    val previewTopicsByKey = remember(plainItems, previewKeyPrefix) {
        plainItems.associate { "${previewKeyPrefix}_${it.listKey}" to it.topic }
    }
    // 只在预取协程内读取视口，滚动每一帧无需重组列表。
    val visiblePreviewTopics = remember(listState, previewTopicsByKey) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .filter { it.offset < layout.viewportEndOffset && it.offset + it.size > layout.viewportStartOffset }
                .sortedBy { kotlin.math.abs(it.offset + it.size / 2 - center) }
                .mapNotNull { item -> (item.key as? String)?.let(previewTopicsByKey::get) }
        }
    }
    val currentVisibleTopics by rememberUpdatedState(visiblePreviewTopics)
    val currentPrefetchExcerpt by rememberUpdatedState(onPrefetchExcerpt)
    LaunchedEffect(active, listState, feedDisplayMode, state.refreshing, state.initialLoading, visiblePreviewTopics) {
        if (!active || state.refreshing || state.initialLoading) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }.collectLatest { scrolling ->
            if (scrolling) return@collectLatest
            try {
                delay(200L)
                // 一次查询当前屏幕所需缓存，避免滚动时每个新组合的卡片分别查库、改高度。
                val visible = currentVisibleTopics.value
                ExcerptCache.loadBatchFromStore(visible.map { it.id })
                if (feedDisplayMode == "no_excerpt") return@collectLatest
                delay(200L)
                val candidates = currentVisibleTopics.value
                    .filter { cleanExcerpt(it.excerpt) == null && ExcerptCache.peekExcerpt(it.id) == null }
                    .take(6).map { it.id }
                currentPrefetchExcerpt?.invoke(scrollBusySource, candidates) {
                    currentVisibleTopics.value.map { it.id }.toSet()
                }
                awaitCancellation()
            } finally {
                currentPrefetchExcerpt?.invoke(scrollBusySource, emptyList()) { emptySet() }
            }
        }
    }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var feedOffset by remember { mutableFloatStateOf(0f) }
    var incomingFeed by remember { mutableStateOf<HomeFeedType?>(null) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    val latestEffectiveFeed by rememberUpdatedState(effectiveFeed)
    val latestOnFeedSelect by rememberUpdatedState(onFeedSelect)
    val latestContainerWidthPx by rememberUpdatedState(containerWidthPx)
    val feedBoundary = rememberTabBoundaryFeedback()

    LaunchedEffect(incomingFeed) { incomingFeed?.let { onPreloadFeed?.invoke(it) } }
    DisposableEffect(active, containerWidthPx) {
        onDispose {
            settleJob?.cancel()
            feedOffset = 0f
            incomingFeed = null
        }
    }

    // 拖动时只改位移。位移由列表图层和指示线读取，不让整屏帖子随每一帧重组。
    fun dragTo(rawOffset: Float, width: Float) {
        val feed = latestEffectiveFeed ?: return
        val next = rawOffset.coerceIn(-width, width)
        val step = if (next < 0f) 1 else -1
        incomingFeed = HomeFeedType.entries.getOrNull(feed.ordinal + step).takeIf { next != 0f }
        feedOffset = if (incomingFeed != null) next else 0f
    }

    fun settle(target: HomeFeedType?, end: Float, width: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            val duration = (220 * abs(end - feedOffset) / width).toInt().coerceIn(85, 220)
            animate(feedOffset, end, animationSpec = tween(duration, easing = FastOutSlowInEasing)) { value, _ ->
                feedOffset = value
            }
            // 同一帧交接页面和位移，不让已选中的新页再吃到上一页的满屏偏移。
            if (target != null) pendingSettledFeed = target
            incomingFeed = null
            feedOffset = 0f
            if (target != null) latestOnFeedSelect?.invoke(target)
        }
    }

    val slideProgress = remember {
        { if (containerWidthPx > 0f) (abs(feedOffset) / containerWidthPx).coerceIn(0f, 1f) else 0f }
    }
    val previewFeed = incomingFeed
    val secondaryListState = previewFeed?.let { feedListStates[it] ?: feedStateLatest }
    val incomingFeedState = incomingFeed?.let { feedStates[it] ?: TopicListUiState(initialLoading = true) }
    val incomingFilteredItems = remember(incomingFeedState?.items, blockedUsernames) {
        val raw = incomingFeedState?.items.orEmpty()
        if (blockedUsernames.isEmpty()) raw
        else raw.filterNot { it.authorUsername?.lowercase() in blockedUsernames }
    }
    val (incomingPinnedGroups, incomingPlainItems) = remember(incomingFilteredItems) {
        pinnedPrefixGroups(incomingFilteredItems)
    }

    val animateSelectFeed: (HomeFeedType) -> Unit = { targetFeed ->
        val cur = latestEffectiveFeed
        val width = latestContainerWidthPx
        val select = latestOnFeedSelect
        if (cur == null || width <= 0f || select == null) {
            select?.invoke(targetFeed)
        } else if (targetFeed == cur) {
            if (incomingFeed != null) settle(null, 0f, width)
        } else {
            if (incomingFeed != targetFeed) feedOffset = 0f
            incomingFeed = targetFeed
            val end = if (targetFeed.ordinal > cur.ordinal) -width else width
            settle(targetFeed, end, width)
        }
    }

    var rawDragOffset by remember { mutableFloatStateOf(0f) }
    val useHomeFilterHeader = homeFilter != null && onSelectHomeCategory != null &&
        onSelectHomeLevel != null && onSelectHomeTag != null && onClearHomeFilters != null
    val homeFeedSwipeModifier = Modifier.tabSwipeGesture(
        enabled = !useHomeFilterHeader && active && currentFeed != null && onFeedSelect != null,
        onStart = {
            settleJob?.cancel()
            rawDragOffset = feedOffset
        },
        onDrag = { dx ->
            rawDragOffset += dx
            dragTo(rawDragOffset, latestContainerWidthPx)
        },
        onEnd = { step ->
            val width = latestContainerWidthPx
            val target = incomingFeed
            val feed = latestEffectiveFeed
            val commit = step != 0 && target != null && feed != null && target.ordinal == feed.ordinal + step
            if (step != 0 && feed != null && feed.ordinal + step !in HomeFeedType.entries.indices) {
                feedBoundary()
            }
            if (width > 0f && (feedOffset != 0f || commit)) {
                settle(if (commit) target else null, if (commit) -step * width else 0f, width)
            }
        },
    )

    val miniAppPull = remember(listState, effectiveFeed) { MiniAppPullGesture() }
    val miniAppPullEnabled = miniAppHomePullEnabled && onOpenMiniApps != null && currentFeed != null
    val pullActionsEnabled = active && !effectiveState.initialLoading && !effectiveState.refreshing &&
        !showSortDialog && flagTargetTopic == null && incomingFeed == null && pendingSettledFeed == null
    // 保持修饰节点位置稳定，避免切换分类时移除节点打断后方的横滑处理器。
    val observeMiniAppPull = Modifier.pointerInput(miniAppPull, listState, miniAppPullEnabled, pullActionsEnabled) {
        try {
            if (miniAppPullEnabled && pullActionsEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val edge = 24.dp.toPx()
                    miniAppPull.begin(
                        !listState.canScrollBackward && !listState.isScrollInProgress && !pullState.isAnimating &&
                            down.type == PointerType.Touch && down.position.x in edge..(size.width - edge),
                    )
                    while (true) {
                        // 先观察松手，再由原下拉刷新组件处理释放；不消费事件或改变滚动距离。
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.singleOrNull()
                        if (change == null || change.id != down.id) {
                            miniAppPull.cancel()
                            break
                        }
                        val distance = change.position - down.position
                        miniAppPull.move(distance.x, distance.y, viewConfiguration.touchSlop)
                        if (!change.pressed) {
                            miniAppPull.release()
                            break
                        }
                    }
                }
            }
        } finally {
            miniAppPull.cancel()
        }
    }

    fun LazyListScope.renderFeedItems(
        pageState: TopicListUiState,
        pagePinnedGroups: List<List<TopicListItem>>,
        pagePlainItems: List<TopicListItem>,
        keyPrefix: String,
    ) {
        pageState.error?.let { message ->
            item(key = "${keyPrefix}_error") { ErrorNotice(message, onRefresh) }
        }
        if (pageState.initialLoading && pageState.items.isEmpty()) {
            item(key = "${keyPrefix}_loading") { IosLoadingBox() }
        }
        if (!pageState.initialLoading && pageState.items.isEmpty() && pageState.error == null) {
            item(key = "${keyPrefix}_empty") { EmptyNotice() }
        }

        itemsIndexed(
            items = pagePinnedGroups,
            key = { _, group -> "${keyPrefix}_pinned_${group.first().topic.id}" },
        ) { _, group ->
            Column(modifier = Modifier.fillMaxWidth()) {
                PinnedGroupCard(
                    items = group,
                    isHighlighted = group.any { it.topic.id in pageState.highlightedTopicIds },
                    onOpenTopic = onOpenTopic,
                    onSetNotificationLevel = onSetNotificationLevel,
                    onCopyLink = copyTopicLink,
                    onBookmark = bookmarkTopicAction,
                    onFlag = flagTopicAction,
                    showTopicUnreadDot = showTopicUnreadDot,
                    topicUnreadDotPosition = topicUnreadDotPosition,
                )
                FeedItemDivider(feedDisplayMode)
            }
        }

        itemsIndexed(
            items = pagePlainItems,
            key = { _, item -> "${keyPrefix}_${item.listKey}" },
            contentType = { _, item -> if (item.bookmarkId == null) feedDisplayMode else "bookmark" },
        ) { index, item ->
            val isHighlighted = item.topic.id in pageState.highlightedTopicIds
            val isGraphic = feedDisplayMode == "graphic"
            Column(modifier = Modifier.fillMaxWidth()) {
                if (item.bookmarkId != null) {
                    var removing by remember(item.bookmarkId) { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.targetPostNumber?.let { "收藏于第 $it 楼" } ?: "话题书签",
                            style = IosTheme.type.caption, color = IosTheme.colors.accent)
                        Spacer(Modifier.weight(1f))
                        if (onRemoveBookmark != null) androidx.compose.material3.TextButton(
                            enabled = !removing,
                            onClick = {
                                removing = true
                                onRemoveBookmark(item) { message -> removing = false; showNotice(message) }
                            },
                        ) { Text(if (removing) "处理中…" else "取消收藏", style = IosTheme.type.caption) }
                    }
                }
                TopicRow(
                    item = item,
                    isHighlighted = isHighlighted,
                    loadCachedPreview = false,
                    onSetNotificationLevel = if (onSetNotificationLevel != null) { level ->
                        onSetNotificationLevel(item.topic, level)
                    } else null,
                    onCopyLink = { copyTopicLink(item.topic) },
                    onBookmark = bookmarkTopicAction?.let { fn -> { fn(item.topic) } },
                    onFlag = flagTopicAction?.let { fn -> { fn(item.topic) } },
                    onSwipeMute = if (onSetNotificationLevel != null && item.bookmarkId == null) {
                        {
                            onSetNotificationLevel(item.topic, TopicNotificationLevel.Muted)
                            showNotice("已设为免打扰")
                        }
                    } else null,
                    onSwipeWatch = if (onSetNotificationLevel != null && item.bookmarkId == null) {
                        {
                            val target = if (item.notificationLevel == TopicNotificationLevel.Watching) {
                                TopicNotificationLevel.Regular
                            } else {
                                TopicNotificationLevel.Watching
                            }
                            onSetNotificationLevel(item.topic, target)
                            showNotice(if (target == TopicNotificationLevel.Watching) "已关注话题" else "已取消关注")
                        }
                    } else null,
                    onOpenUser = onOpenUser,
                    onClick = { if (onOpenItem != null) onOpenItem(item) else onOpenTopic(item.topic) },
                    feedGraphicMode = isGraphic,
                    feedDisplayMode = feedDisplayMode,
                    showTopicUnreadDot = showTopicUnreadDot,
                    topicUnreadDotPosition = topicUnreadDotPosition,
                )
                if (index < pagePlainItems.lastIndex) {
                    FeedItemDivider(feedDisplayMode)
                }
            }
        }

        if (pageState.loadingMore) {
            item(key = "${keyPrefix}_load_more") { IosLoadingBox() }
        }
    }

    PullToRefreshBox(
        isRefreshing = effectiveState.refreshing,
        onRefresh = {
            if (!miniAppPullEnabled) {
                onRefresh()
            } else if (pullActionsEnabled) {
                when (miniAppPull.finish(pullState.distanceFraction, !listState.canScrollBackward)) {
                    MiniAppPullGesture.Action.Open -> onOpenMiniApps?.invoke()
                    MiniAppPullGesture.Action.Refresh -> onRefresh()
                    MiniAppPullGesture.Action.Ignore -> Unit
                }
            }
        },
        state = pullState,
        modifier = modifier
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .then(observeMiniAppPull)
            .then(homeFeedSwipeModifier),
        indicator = {
            val fraction = pullState.distanceFraction
            val showPullHint = miniAppPullEnabled && pullActionsEnabled && fraction >= 1f
            // 初始加载时列表中央已有加载指示，这里再叠一个下拉转圈就成了两个圈。
            if (!effectiveState.initialLoading && (effectiveState.refreshing || fraction > 0f)) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (useHomeFilterHeader) 98.dp else IosMetrics.navBarHeight + 10.dp)
                        .graphicsLayer {
                            alpha = if (effectiveState.refreshing) 1f else min(1f, fraction * 1.4f)
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = if (showPullHint) Modifier
                            .shadow(3.dp, RoundedCornerShape(20.dp))
                            .background(IosTheme.colors.card, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp) else Modifier,
                    ) {
                        IosActivityIndicator(diameter = if (showPullHint) 18.dp else 22.dp)
                        if (showPullHint) {
                            Text(
                                if (miniAppPull.ready(fraction)) "松开进入应用广场" else "松开刷新",
                                style = IosTheme.type.footnote,
                                color = IosTheme.colors.label,
                            )
                        }
                    }
                }
            }
        },
    ) {
        var visibleHeaderHeight by remember(useHomeFilterHeader, onBack) {
            mutableStateOf(if (useHomeFilterHeader) 94.dp else if (onBack == null) 54.dp else IosMetrics.navBarHeight)
        }
        IosLargeTitleScaffold(
            title = title,
            listState = listState,
            onBack = onBack,
            compactTop = onBack == null,
            showLargeTitle = showLargeTitle,
            scrollToTopRequest = scrollToTopRequest,
            trailingWidth = if (showHeaderActions) 88.dp else 0.dp,
            onVisibleHeaderHeightChanged = { visibleHeaderHeight = it },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    trailing()
                    if (showHeaderActions) TopicListHeaderActions(
                        order = sortOrder,
                        ascending = sortAscending,
                        onSelectOrder = { order ->
                            scope.launch { listState.scrollToItem(0) }
                            onSortChange(order)
                        },
                        onSearch = onNavigateToSearch,
                    )
                }
            },
            stickySubHeader = if (useHomeFilterHeader) {
                {
                    HomeTopFilterHeader(
                        topBarVisible = org.linuxdo.android.ui.design.LocalListBars.current?.value != false,
                        filter = homeFilter!!,
                        sortOrder = sortOrder,
                        sortAscending = sortAscending,
                        categories = homeCategories,
                        categoriesLoading = homeCategoriesLoading,
                        categoryOrder = homeCategoryOrder,
                        pinnedCategoryIds = homePinnedCategoryIds,
                        tags = homeTags,
                        tagsLoading = homeTagsLoading,
                        searchedTags = homeSearchedTags,
                        pinnedTags = homePinnedTags,
                        tagOrder = homeTagOrder,
                        tagSearchQuery = homeTagSearchQuery,
                        leading = homeLeading,
                        onSelectCategory = { cat ->
                            scope.launch { listState.scrollToItem(0) }
                            onSelectHomeCategory!!.invoke(cat)
                        },
                        onSelectLevel = { lv ->
                            scope.launch { listState.scrollToItem(0) }
                            onSelectHomeLevel!!.invoke(lv)
                        },
                        onSelectTag = { tg ->
                            scope.launch { listState.scrollToItem(0) }
                            onSelectHomeTag!!.invoke(tg)
                        },
                        onClearFilters = {
                            scope.launch { listState.scrollToItem(0) }
                            onClearHomeFilters!!.invoke()
                        },
                        onTogglePinCategory = { id -> onTogglePinCategory?.invoke(id) },
                        onSaveCategoryOrder = { ids -> onSaveCategoryOrder?.invoke(ids) },
                        onResetCategoryOrder = { onResetCategoryOrder?.invoke() },
                        onTogglePinTag = { name -> onTogglePinTag?.invoke(name) },
                        onSaveTagOrder = { names -> onSaveTagOrder?.invoke(names) },
                        onResetTagOrder = { onResetTagOrder?.invoke() },
                        onEnsureCategories = { onEnsureHomeCategories?.invoke() },
                        onEnsureTags = { onEnsureHomeTags?.invoke() },
                        onTagSearchQueryChange = { onHomeTagSearchQueryChange?.invoke(it) },
                        onSortChange = { order ->
                            scope.launch { listState.scrollToItem(0) }
                            onSortChange(order)
                        },
                        onSearch = onNavigateToSearch,
                    )
                }
            } else null,
            pageOffsetXProvider = if (!useHomeFilterHeader && effectiveFeed != null) ({ feedOffset }) else null,
            secondaryListState = if (useHomeFilterHeader) null else secondaryListState,
            secondaryPageOffsetXProvider = if (!useHomeFilterHeader && incomingFeed != null) {
                {
                    val offset = feedOffset
                    if (offset < 0f) containerWidthPx + offset else -containerWidthPx + offset
                }
            } else null,
            secondaryContent = if (!useHomeFilterHeader && previewFeed != null && incomingFeedState != null) {
                {
                    renderFeedItems(
                        pageState = incomingFeedState,
                        pagePinnedGroups = incomingPinnedGroups,
                        pagePlainItems = incomingPlainItems,
                        keyPrefix = previewFeed.name,
                    )
                }
            } else null,
            customLargeTitle = when {
                useHomeFilterHeader -> null
                effectiveFeed != null && onFeedSelect != null -> {
                    {
                        SectionHeader(
                            labels = remember { HomeFeedType.entries.map { it.label } },
                            selected = effectiveFeed.ordinal,
                            onSelect = { animateSelectFeed(HomeFeedType.entries[it]) },
                            indicatorPosition = {
                                effectiveFeed.ordinal + ((previewFeed ?: effectiveFeed).ordinal - effectiveFeed.ordinal) * slideProgress()
                            },
                            actionWidth = if (showHeaderActions) 88.dp else 48.dp,
                            actions = {
                                if (showHeaderActions) TopicListHeaderActions(
                                    order = sortOrder,
                                    ascending = sortAscending,
                                    onSelectOrder = { order ->
                                        scope.launch { listState.scrollToItem(0) }
                                        onSortChange(order)
                                    },
                                    onSearch = onNavigateToSearch,
                                )
                                else trailing()
                            },
                            leading = homeLeading,
                        )
                    }
                }
                hasCategoryDropdown -> {
                    {
                        CategoryVariantSelector(
                            title = title,
                            variants = categoryVariants,
                            currentCategoryId = currentCategoryId,
                            collapsed = false,
                            onSelect = { onSelectCategoryVariant?.invoke(it) },
                        )
                    }
                }
                tagTitle != null -> { { ForumTagText(tagTitle, IosTheme.type.largeTitle, IosTheme.colors.label) } }
                else -> null
            },
            customCollapsedTitle = when {
                hasCategoryDropdown -> {
                    {
                        CategoryVariantSelector(
                            title = title,
                            variants = categoryVariants,
                            currentCategoryId = currentCategoryId,
                            collapsed = true,
                            onSelect = { onSelectCategoryVariant?.invoke(it) },
                        )
                    }
                }
                tagTitle != null -> { { ForumTagText(tagTitle, IosTheme.type.subheadline, IosTheme.colors.label) } }
                else -> null
            },
            floatingOverlay = {
                var lastApplyIncomingClickTime by remember { mutableLongStateOf(0L) }
                val incomingTopPadding by animateDpAsState(
                    targetValue = visibleHeaderHeight + 8.dp,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "incomingBannerTopPadding",
                )
                val incomingAvatars = remember(effectiveState.pendingPage, effectiveState.incomingTopicIds) {
                    val pending = effectiveState.pendingPage?.items.orEmpty()
                    val incomingSet = effectiveState.incomingTopicIds
                    val prioritized = pending.filter { it.topic.id in incomingSet }.ifEmpty { pending }
                    prioritized
                        .mapNotNull { item ->
                            val primary = item.avatarUrl?.takeIf { it.isNotBlank() }
                            val fallback = item.staticAvatarUrl?.takeIf { it.isNotBlank() }
                            if (primary == null && fallback == null) null
                            else Triple(item.authorUsername, primary, fallback)
                        }
                        .distinctBy { it.first?.lowercase() ?: it.second ?: it.third }
                        .take(3)
                }

                AnimatedVisibility(
                    visible = effectiveState.incomingTopicIds.isNotEmpty() &&
                        pullState.distanceFraction == 0f && !effectiveState.refreshing,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { -it },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = incomingTopPadding),
                ) {
                    var bannerAlphaTarget by remember { mutableFloatStateOf(1f) }
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.isScrollInProgress }.collectLatest { scrolling ->
                            bannerAlphaTarget = 1f
                            if (!scrolling) {
                                delay(2200L)
                                bannerAlphaTarget = 0.88f
                            }
                        }
                    }
                    val bannerAlpha by animateFloatAsState(
                        targetValue = bannerAlphaTarget,
                        animationSpec = tween(durationMillis = 300),
                        label = "incomingBannerAlpha",
                    )
                    IncomingTopicsBanner(
                        count = effectiveState.incomingTopicIds.size,
                        avatars = incomingAvatars,
                        onClick = {
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastApplyIncomingClickTime > 400L) {
                                lastApplyIncomingClickTime = now
                                onApplyIncoming?.invoke()
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        },
                        modifier = Modifier.graphicsLayer { alpha = bannerAlpha },
                    )
                }
                if (floatingBallEnabled && floatingBallActions.isNotEmpty()) {
                    FloatingAssistiveBall(
                        actions = if (sortEnabled) floatingBallActions else floatingBallActions.filter { it != FloatingAction.Sort },
                        sortOrder = sortOrder,
                        sortAscending = sortAscending,
                        initialXRatio = floatingBallXRatio,
                        initialYRatio = floatingBallYRatio,
                        onPositionChange = onFloatingBallPositionChange,
                        onActionClick = { action ->
                            when (action) {
                                FloatingAction.CreateTopic -> onCreateTopic?.invoke()
                                FloatingAction.Bookmarks -> onNavigateToBookmarks?.invoke()
                                FloatingAction.Following -> onNavigateToFollowing?.invoke()
                                FloatingAction.Likes -> onNavigateToLikes?.invoke()
                                FloatingAction.Replies -> onNavigateToReplies?.invoke()
                                FloatingAction.Sort -> showSortDialog = true
                                FloatingAction.Drafts -> onNavigateToDrafts?.invoke()
                                FloatingAction.Search -> onNavigateToSearch?.invoke()
                                FloatingAction.Categories -> onNavigateToCategories?.invoke()
                                FloatingAction.History -> onNavigateToHistory?.invoke()
                                FloatingAction.Connect -> onNavigateToConnect?.invoke()
                                FloatingAction.Chat -> onNavigateToChat?.invoke()
                                FloatingAction.LdcStore -> onNavigateToLdcStore?.invoke()
                                FloatingAction.LdcMail -> onNavigateToLdcMail?.invoke()
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (currentFeed == null && currentCategoryId == null && !showHeaderActions) {
                    // 分类、标签话题页的排序位于顶栏，不再补显示独立发帖/排序悬浮按钮。
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 14.dp, bottom = 68.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (sortEnabled) TopicSortButton(
                            sortOrder = sortOrder,
                            ascending = sortAscending,
                            onClick = { showSortDialog = true },
                        )
                        if (onCreateTopic != null) {
                            CreateTopicFab(onClick = onCreateTopic)
                        }
                    }
                }
                if (sortEnabled && showSortDialog) {
                    TopicSortDialog(
                        currentOrder = sortOrder,
                        ascending = sortAscending,
                        onDismiss = { showSortDialog = false },
                        onSelectOrder = { order ->
                            scope.launch { listState.scrollToItem(0) }
                            onSortChange(order)
                            if (!order.supportsAscending || order != sortOrder) {
                                showSortDialog = false
                            }
                        },
                    )
                }
                noticeToast?.let { notice ->
                    Text(
                        notice,
                        style = IosTheme.type.footnote,
                        color = Color(0xFF30B85B),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosTheme.colors.card)
                            .border(1.dp, Color(0xFF34C759).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            },
        ) {
            val keyPrefix = effectiveFeed?.name ?: currentCategoryId?.toString() ?: "topic"
            renderFeedItems(
                pageState = effectiveState,
                pagePinnedGroups = pinnedGroups,
                pagePlainItems = plainItems,
                keyPrefix = keyPrefix,
            )
        }
    }

    flagTargetTopic?.let { topic ->
        TopicFlagDialog(
            topic = topic,
            submitting = flagSubmitting,
            error = flagError,
            onDismiss = {
                if (!flagSubmitting) {
                    flagTargetTopic = null
                    flagError = null
                }
            },
            onSubmit = { reasonId, message ->
                val handler = onFlagTopic ?: return@TopicFlagDialog
                flagSubmitting = true
                flagError = null
                handler(
                    topic,
                    reasonId,
                    message,
                    {
                        flagSubmitting = false
                        flagTargetTopic = null
                        showNotice("举报已提交，感谢您的反馈")
                    },
                    { err ->
                        flagSubmitting = false
                        flagError = err
                    },
                )
            },
        )
    }
}

/** 列表右下角创建新话题悬浮按钮（笔状图标，与详情页回复按钮风格一致）。 */
@Composable
private fun CreateTopicFab(onClick: () -> Unit) {
    val tintColor = IosTheme.colors.accent
    val bgBase = IosTheme.colors.card.copy(alpha = 0.95f)
    val bgTint = tintColor.copy(alpha = 0.12f)
    val borderColor = tintColor.copy(alpha = 0.28f)
    val fabShape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(48.dp)
            .shadow(elevation = 3.dp, shape = fabShape, clip = false)
            .clip(fabShape)
            .background(bgBase)
            .background(bgTint)
            .border(1.dp, borderColor, fabShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "创建新话题" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val pen = Path().apply {
                    moveTo(4f, 20f)
                    lineTo(8.5f, 19f)
                    lineTo(19.5f, 8f)
                    lineTo(16f, 4.5f)
                    lineTo(5f, 15.5f)
                    close()
                }
                drawPath(pen, tintColor, style = stroke)
                drawLine(tintColor, Offset(13.5f, 7f), Offset(17f, 10.5f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                drawLine(tintColor, Offset(12.5f, 20f), Offset(20f, 20f), strokeWidth = 1.9f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun TopicListHeaderActions(
    order: TopicSortOrder,
    ascending: Boolean,
    onSelectOrder: (TopicSortOrder) -> Unit,
    onSearch: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            TopicSortDynamicButton(order, ascending, onClick = { expanded = true }, floating = false)
            TopicSortDropdownMenu(
                expanded = expanded,
                currentOrder = order,
                ascending = ascending,
                onDismiss = { expanded = false },
                onSelectOrder = { selected ->
                    expanded = false
                    onSelectOrder(selected)
                },
            )
        }
        if (onSearch != null) IconButton(onClick = onSearch, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Search, contentDescription = "搜索", tint = IosTheme.colors.label)
        }
    }
}

/** 复用同一语义排序图标与操作；悬浮模式使用圆形卡片外观。 */
@Composable
private fun TopicSortButton(
    sortOrder: TopicSortOrder,
    ascending: Boolean,
    onClick: () -> Unit,
    floating: Boolean = true,
) {
    TopicSortDynamicButton(
        sortOrder = sortOrder,
        ascending = ascending,
        onClick = onClick,
        floating = floating,
    )
}

/** 排序选项图标绘制：直接复用新的 5 项动态语义图标。 */
@Composable
private fun TopicSortIcon(
    order: TopicSortOrder,
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp),
) {
    TopicSortDynamicGlyph(order = order, tint = tint, modifier = modifier)
}

/**
 * 列表排序选择弹出框（供辅助悬浮球触发时使用，5 项排序与右上角下拉菜单保持一致）。
 */
@Composable
private fun TopicSortDialog(
    currentOrder: TopicSortOrder,
    ascending: Boolean,
    onDismiss: () -> Unit,
    onSelectOrder: (TopicSortOrder) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(IosTheme.colors.card)
                .border(0.6.dp, IosTheme.colors.separator.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopicSortIcon(
                    order = currentOrder,
                    tint = IosTheme.colors.label,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(12.dp))
                val sortSuffix = if (currentOrder.supportsAscending) {
                    if (ascending) " (升序)" else " (降序)"
                } else ""
                Text(
                    text = "排序：${currentOrder.label}$sortSuffix",
                    style = IosTheme.type.headline.copy(fontWeight = FontWeight.SemiBold),
                    color = IosTheme.colors.label,
                    modifier = Modifier.weight(1f),
                )
                Canvas(Modifier.size(14.dp)) {
                    val chevron = Path().apply {
                        moveTo(2f, 9.5f)
                        lineTo(7f, 4.5f)
                        lineTo(12f, 9.5f)
                    }
                    drawPath(chevron, Color(0xFF8E8E93), style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            IosDivider(startIndent = 14.dp, modifier = Modifier.padding(end = 14.dp))

            TopicSortOrder.entries.forEach { order ->
                val isSelected = order == currentOrder
                val itemColor = if (isSelected) IosTheme.colors.accent else IosTheme.colors.label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectOrder(order) }
                        .background(if (isSelected) IosTheme.colors.accent.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TopicSortIcon(
                        order = order,
                        tint = itemColor,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = order.label,
                            style = IosTheme.type.body.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = itemColor,
                        )
                        Text(
                            text = order.description,
                            style = IosTheme.type.caption.copy(fontSize = 11.sp),
                            color = if (isSelected) itemColor.copy(alpha = 0.8f) else IosTheme.colors.tertiaryLabel,
                        )
                    }
                    if (isSelected) {
                        if (order.supportsAscending) {
                            Text(
                                text = if (ascending) "↑ 升序" else "↓ 降序",
                                style = IosTheme.type.caption.copy(fontWeight = FontWeight.Medium),
                                color = itemColor,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        Canvas(Modifier.size(16.dp)) {
                            val check = Path().apply {
                                moveTo(2.5f, 8.5f)
                                lineTo(6f, 12f)
                                lineTo(13.5f, 4.5f)
                            }
                            drawPath(check, itemColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 话题长按操作菜单（提醒四档 + 复制链接、添加书签、举报）。[TopicRow] 与 [PinnedGroupCard] 共用。
 */
@Composable
private fun NotificationLevelMenu(
    expanded: Boolean,
    currentLevel: TopicNotificationLevel?,
    updating: Boolean,
    onDismiss: () -> Unit,
    onPick: ((TopicNotificationLevel) -> Unit)? = null,
    onCopyLink: (() -> Unit)? = null,
    onBookmark: (() -> Unit)? = null,
    onFlag: (() -> Unit)? = null,
    previewTopic: Topic? = null,
    previewCategory: Category? = null,
    onOpenTopic: (() -> Unit)? = null,
) {
    if (!expanded) return
    val previewLoader = org.linuxdo.android.ui.LocalTopicPreviewLoader.current
    if (previewTopic != null && onOpenTopic != null && previewLoader != null) {
        TopicPreviewDialog(previewTopic, previewCategory, previewLoader, onDismiss, onOpenTopic,
            currentLevel, updating, onPick, onCopyLink, onBookmark, onFlag)
        return
    }
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(IosTheme.colors.card),
    ) {
        if (onPick != null) {
            val current = currentLevel ?: TopicNotificationLevel.Regular
            TopicNotificationLevel.entries.forEach { level ->
                val isSelected = level == current
                DropdownMenuItem(
                    enabled = !updating,
                    modifier = if (isSelected) Modifier.background(IosTheme.colors.accent.copy(alpha = 0.12f)) else Modifier,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TopicNotificationIcon(level, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = level.title,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) IosTheme.colors.accent else IosTheme.colors.label,
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Text("✓", color = IosTheme.colors.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    onClick = {
                        onDismiss()
                        onPick(level)
                    },
                )
            }
        }
        val hasExtraActions = onCopyLink != null || onBookmark != null || onFlag != null
        if (onPick != null && hasExtraActions) {
            IosDivider()
        }
        if (onCopyLink != null) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PostActionIcon(PostAction.CopyLink, false, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("复制链接", color = IosTheme.colors.label)
                    }
                },
                onClick = {
                    onDismiss()
                    onCopyLink()
                },
            )
        }
        if (onBookmark != null) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PostActionIcon(PostAction.Bookmark, false, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("添加书签", color = IosTheme.colors.label)
                    }
                },
                onClick = {
                    onDismiss()
                    onBookmark()
                },
            )
        }
        if (onFlag != null) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PostActionIcon(PostAction.Flag, false, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("举报", color = IosTheme.colors.label)
                    }
                },
                onClick = {
                    onDismiss()
                    onFlag()
                },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TopicPreviewDialog(
    topic: Topic,
    category: Category?,
    loader: suspend (Long) -> org.linuxdo.android.data.TopicDetail,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    notificationLevel: TopicNotificationLevel?,
    updating: Boolean,
    onPick: ((TopicNotificationLevel) -> Unit)?,
    onCopy: (() -> Unit)?,
    onBookmark: (() -> Unit)?,
    onFlag: (() -> Unit)?,
) {
    var detail by remember(topic.id) { mutableStateOf<org.linuxdo.android.data.TopicDetail?>(null) }
    var loading by remember(topic.id) { mutableStateOf(true) }
    var failed by remember(topic.id) { mutableStateOf(false) }
    var retry by remember(topic.id) { mutableIntStateOf(0) }

    LaunchedEffect(topic.id, retry) {
        loading = true
        failed = false
        try { detail = loader(topic.id) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    val first = detail?.postStream?.posts?.firstOrNull { it.postNumber == 1 }
    val cookedBlocks = remember(first?.cooked) {
        val cookedHtml = first?.cooked
        if (!cookedHtml.isNullOrBlank()) {
            CookedParser.parseCached(cookedHtml)
        } else emptyList()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(24.dp))
                .background(IosTheme.colors.card)
        ) {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 大标题 (单击也可跳转)
                item {
                    Text(
                        text = detail?.title ?: topic.title,
                        style = IosTheme.type.title3.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp),
                        color = IosTheme.colors.label,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss(); onOpen() }
                    )
                }

                // 2. 作者信息行 (头像 · 名字 · 分类色块 · 分类名 · 时间)
                item {
                    val firstPosterUser = topic.posters.firstOrNull()?.user
                    val posterName = first?.displayName?.takeIf { it.isNotBlank() }
                        ?: firstPosterUser?.name?.takeIf { it.isNotBlank() }
                        ?: firstPosterUser?.username?.takeIf { it.isNotBlank() }
                        ?: "社区用户"
                    val posterAvatar = first?.avatarTemplate ?: firstPosterUser?.avatarTemplate
                    val effectiveCategory = category
                    val catColor = effectiveCategory?.color?.let { parseCategoryColor(it) } ?: IosTheme.colors.accent

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!posterAvatar.isNullOrBlank()) {
                            AsyncImage(
                                avatarUrlOf(posterAvatar),
                                posterName,
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                            )
                        }
                        Text(
                            text = posterName,
                            style = IosTheme.type.footnote,
                            fontWeight = FontWeight.Medium,
                            color = IosTheme.colors.label
                        )
                        Text("·", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                        if (effectiveCategory != null) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(catColor)
                            )
                            Text(
                                text = effectiveCategory.name,
                                style = IosTheme.type.caption,
                                color = IosTheme.colors.secondaryLabel
                            )
                            Text("·", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                        }
                        Text(
                            text = RelativeTime.format(first?.createdAt ?: topic.createdAt),
                            style = IosTheme.type.caption,
                            color = IosTheme.colors.secondaryLabel
                        )
                    }
                }

                // 3. 标签列表 (#快问快答 #人工智能)
                val tags = detail?.tags ?: topic.tags
                if (tags.isNotEmpty()) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            tags.forEach { tag ->
                                ForumTagText(
                                    tag = tag,
                                    style = IosTheme.type.caption,
                                    color = IosTheme.colors.secondaryLabel
                                )
                            }
                        }
                    }
                }

                // 4. 正文富文本与图片渲染
                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            IosActivityIndicator(diameter = 24.dp)
                        }
                    }
                } else if (cookedBlocks.isNotEmpty()) {
                    item {
                        CookedContent(
                            blocks = cookedBlocks,
                            onPicture = { _, _ -> },
                            compact = true,
                        )
                    }
                } else if (!topic.excerpt.isNullOrBlank()) {
                    item {
                        Text(
                            text = topic.excerpt!!,
                            style = IosTheme.type.body,
                            color = IosTheme.colors.label,
                            lineHeight = 24.sp
                        )
                    }
                }

                // 5. 失败重试提示
                if (failed && cookedBlocks.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(IosTheme.colors.groupedBackground)
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("详情暂未加载完整，可直接打开话题查看", style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel)
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.material3.TextButton(onClick = { retry++ }) {
                                Text("重试加载", color = IosTheme.colors.accent)
                            }
                        }
                    }
                }
            }

            // 6. 话题提醒设置 (按图设计分段选择器，靠下独立展示，不贴着正文)
            if (onPick != null) {
                var currentSelectedLevel by remember(notificationLevel, topic.notificationLevel) {
                    mutableStateOf(notificationLevel ?: TopicNotificationLevel.entries.firstOrNull { it.code == topic.notificationLevel } ?: TopicNotificationLevel.Regular)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "话题提醒设置",
                        style = IosTheme.type.caption,
                        color = IosTheme.colors.secondaryLabel,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(IosTheme.colors.groupedBackground)
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val levels = listOf(
                            TopicNotificationLevel.Watching to "关注",
                            TopicNotificationLevel.Tracking to "跟踪",
                            TopicNotificationLevel.Regular to "常规",
                            TopicNotificationLevel.Muted to "免打扰"
                        )
                        levels.forEach { (level, title) ->
                            val isSelected = level == currentSelectedLevel
                            val itemBg = if (isSelected) IosTheme.colors.accent else Color.Transparent
                            val itemTextColor = if (isSelected) Color.White else IosTheme.colors.label

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(itemBg)
                                    .clickable(enabled = !updating) {
                                        currentSelectedLevel = level
                                        onPick(level)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓ ", style = IosTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = itemTextColor)
                                    } else {
                                        TopicNotificationIcon(level, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = title,
                                        style = IosTheme.type.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                        color = itemTextColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. 优雅卡片底栏 (左侧统计数据 💬🤍👁 + 右侧 分享按钮 & 查看详情胶囊)
            IosDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 回复数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${topic.totalReplies}", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                }
                Spacer(Modifier.width(14.dp))
                // 点赞数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤍", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${topic.likeCount}", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                }
                Spacer(Modifier.width(14.dp))
                // 浏览数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👁", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${topic.views}", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                }

                Spacer(Modifier.weight(1f))

                // “查看详情”胶囊按钮
                androidx.compose.material3.Button(
                    onClick = { onDismiss(); onOpen() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = IosTheme.colors.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 38.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PostActionIcon(PostAction.Reply, false, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("查看详情", style = IosTheme.type.footnote, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * 把列表**头部连续**的置顶项按作者切成若干组,返回 (置顶分组, 其余话题)。
 *
 * 只吃头部前缀而不是全列表 filter:站方的置顶本来就排在最前,取前缀足以解决"置顶吃光首屏";
 * 全列表 filter 会把翻页后零散冒出来的置顶帖也抽到顶上,列表会在加载更多时自己跳一下。
 */
private fun pinnedPrefixGroups(
    items: List<TopicListItem>,
): Pair<List<List<TopicListItem>>, List<TopicListItem>> {
    val prefix = items.takeWhile { it.topic.pinned }
    if (prefix.isEmpty()) return emptyList<List<TopicListItem>>() to items
    // LinkedHashMap 保持作者首次出现的顺序,不打乱站方的置顶排序。
    val grouped = LinkedHashMap<String, MutableList<TopicListItem>>()
    prefix.forEach { entry ->
        grouped.getOrPut(entry.authorUsername ?: "") { mutableListOf() }.add(entry)
    }
    return grouped.values.map { it.toList() } to items.drop(prefix.size)
}

@Composable
private fun topicRowBackground(isHighlighted: Boolean): Color {
    val background by animateColorAsState(
        targetValue = if (isHighlighted) IosTheme.colors.accent.copy(alpha = 0.12f) else IosTheme.colors.card,
        animationSpec = tween(if (isHighlighted) 100 else 450), label = "topic-highlight",
    )
    return background
}

/**
 * 置顶帖合并卡。
 *
 * 站方同时挂多条全局置顶时,每条各铺一张完整卡片会把首屏吃光 —— 下拉刷新后想看的新帖只剩一条。
 * 这里把同一作者的连续置顶折成一张卡:只保留作者身份(头像 + 用户名 + Lv 徽章)和标题行,
 * 摘要、缩略图、回复/浏览/时间与标签全部省掉。标题仍走 [titleWithBadges],
 * 所以未读蓝点、"置顶""已关闭"标记不必重写一套。
 *
 * 不做左滑四档抽屉:一张卡对应多个话题,左滑该落到哪一条没有明确语义;
 * 要单独设免打扰就长按对应那一行标题。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedGroupCard(
    items: List<TopicListItem>,
    onOpenTopic: (Topic) -> Unit,
    onSetNotificationLevel: ((Topic, TopicNotificationLevel) -> Unit)? = null,
    onCopyLink: ((Topic) -> Unit)? = null,
    onBookmark: ((Topic) -> Unit)? = null,
    onFlag: ((Topic) -> Unit)? = null,
    isHighlighted: Boolean = false,
    showTopicUnreadDot: Boolean = true,
    topicUnreadDotPosition: TopicUnreadDotPosition = TopicUnreadDotPosition.Avatar,
) {
    val head = items.firstOrNull() ?: return
    val rowBackground = topicRowBackground(isHighlighted)
    var menuTopicId by remember { mutableStateOf<Long?>(null) }
    val hasAnyLongPress = org.linuxdo.android.ui.LocalTopicPreviewLoader.current != null ||
        onSetNotificationLevel != null || onCopyLink != null || onBookmark != null || onFlag != null
    Column(
        Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .padding(horizontal = IosMetrics.screenPadding, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    UserAvatar(
                        avatarUrl = head.avatarUrl,
                        fallbackUrl = head.staticAvatarUrl,
                        username = head.authorUsername,
                        modifier = Modifier.size(24.dp),
                        cornerRadius = 3.5.dp,
                    )
                    if (showTopicUnreadDot && topicUnreadDotPosition == TopicUnreadDotPosition.Avatar && items.any { it.topic.unseen }) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                                .clip(CircleShape)
                                .background(IosTheme.colors.card)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(IosTheme.colors.accent)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = head.displayName.ifBlank { "匿名" },
                    style = IosTheme.type.footnote,
                    color = IosTheme.colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                head.trustLevel?.takeIf { it > 0 }?.let { level ->
                    Spacer(Modifier.width(4.dp))
                    LevelBadge(level)
                }
            }
            Text(
                text = "置顶 ${items.size}",
                style = IosTheme.type.footnote,
                color = IosTheme.colors.secondaryLabel,
            )
        }
        items.forEach { entry ->
            Spacer(Modifier.height(6.dp))
            Box {
                Text(
                    text = titleWithBadges(
                        entry.topic,
                        IosTheme.colors.accent,
                        includeUnseenDot = showTopicUnreadDot && topicUnreadDotPosition == TopicUnreadDotPosition.TitleLeft,
                    ),
                    style = IosTheme.type.body,
                    color = IosTheme.colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = unseenDotContent(IosTheme.colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpenTopic(entry.topic) },
                            onLongClick = if (hasAnyLongPress && !entry.notificationLevelUpdating) {
                                { menuTopicId = entry.topic.id }
                            } else null,
                        ),
                )
                NotificationLevelMenu(
                    expanded = menuTopicId == entry.topic.id && hasAnyLongPress,
                    currentLevel = entry.notificationLevel,
                    updating = entry.notificationLevelUpdating,
                    previewTopic = entry.topic,
                    previewCategory = entry.category,
                    onOpenTopic = { onOpenTopic(entry.topic) },
                    onDismiss = { menuTopicId = null },
                    onPick = onSetNotificationLevel?.let { fn -> { level -> fn(entry.topic, level) } },
                    onCopyLink = onCopyLink?.let { fn -> { fn(entry.topic) } },
                    onBookmark = onBookmark?.let { fn -> { fn(entry.topic) } },
                    onFlag = onFlag?.let { fn -> { fn(entry.topic) } },
                )
            }
        }
    }
}

/**
 * 清洗话题正文摘录：
 * 彻底过滤掉开头自动生成的图片属性、文件名、分辨率与体积信息（如 "image1540×734 54.4 KB"、"截图... 120KB" 等），
 * 直接将真实正文字符串呈现给用户。
 */
internal val EMOJI_SHORTCODE_MAP = mapOf(
    "laughing" to "😆", "satisfied" to "😆", "smile" to "😄", "smiley" to "😃", "grinning" to "😀",
    "grin" to "😁", "joy" to "😂", "rofl" to "🤣", "sweat_smile" to "😅", "wink" to "😉",
    "blush" to "😊", "innocent" to "😇", "slight_smile" to "🙂", "upside_down_face" to "🙃",
    "yum" to "😋", "relieved" to "😌", "heart_eyes" to "😍", "kissing_heart" to "😘",
    "kissing" to "😗", "stuck_out_tongue_winking_eye" to "😜", "stuck_out_tongue_closed_eyes" to "😝",
    "stuck_out_tongue" to "😛", "money_mouth_face" to "🤑", "hugs" to "🤗", "hug" to "🤗",
    "shush" to "🤫", "thinking" to "🤔", "zipper_mouth_face" to "🤐", "face_with_raised_eyebrow" to "🤨",
    "neutral_face" to "😐", "expressionless" to "😑", "no_mouth" to "😶", "smirk" to "😏",
    "unamused" to "😒", "rolling_eyes" to "🙄", "grimacing" to "😬", "lying_face" to "🤥",
    "sleepy" to "😪", "drooling_face" to "🤤", "sleeping" to "😴", "mask" to "😷",
    "face_with_thermometer" to "🤒", "head_bandage" to "🤕", "nauseated_face" to "🤢",
    "vomiting_face" to "🤮", "sneezing_face" to "🤧", "hot_face" to "🥵", "cold_face" to "🥶",
    "woozy_face" to "🥴", "dizzy_face" to "😵", "exploding_head" to "🤯", "sunglasses" to "😎",
    "nerd_face" to "🤓", "monocle_face" to "🧐", "confused" to "😕", "worried" to "😟",
    "slight_frown" to "🙁", "frowning_face" to "☹️", "open_mouth" to "😮", "hushed" to "😯",
    "astonished" to "😲", "flushed" to "😳", "pleading_face" to "🥺", "frowning" to "😦",
    "anguished" to "😧", "fearful" to "😨", "cold_sweat" to "😰", "disappointed_relieved" to "😥",
    "cry" to "😢", "sob" to "😭", "scream" to "😱", "confounded" to "😖", "persevere" to "😣",
    "disappointed" to "😞", "sweat" to "😓", "weary" to "😩", "tired_face" to "😫",
    "yawning_face" to "🥱", "triumph" to "😤", "rage" to "😡", "pout" to "😡", "angry" to "😠",
    "cursing_face" to "🤬", "smiling_imp" to "😈", "imp" to "👿", "skull" to "💀", "poop" to "💩",
    "clown_face" to "🤡", "ghost" to "👻", "alien" to "👽", "wave" to "👋", "ok_hand" to "👌",
    "victory" to "✌️", "v" to "✌️", "+1" to "👍", "thumbsup" to "👍", "-1" to "👎", "thumbsdown" to "👎",
    "clap" to "👏", "pray" to "🙏", "muscle" to "💪", "eyes" to "👀", "heart" to "❤️", "broken_heart" to "💔",
    "fire" to "🔥", "100" to "💯", "tada" to "🎉", "sparkles" to "✨", "star" to "⭐", "rocket" to "🚀",
    "salute" to "🫡", "saluting_face" to "🫡", "party" to "🥳", "partying_face" to "🥳",
    "melting_face" to "🫠", "face_holding_back_tears" to "🥹", "handshake" to "🤝",
    "dog" to "🐶", "cat" to "🐱", "beer" to "🍺", "beers" to "🍻", "coffee" to "☕", "tea" to "🍵",
    "check" to "✅", "white_check_mark" to "✅", "x" to "❌", "cross_mark" to "❌",
    "sun" to "☀️", "sunny" to "☀️", "moon" to "🌙", "rose" to "🌹", "warning" to "⚠️",
    "pin" to "📌", "pushpin" to "📌", "red_heart" to "❤️", "sparkling_heart" to "💖",
    "two_hearts" to "💕", "gift" to "🎁", "bulb" to "💡", "memo" to "📝", "pencil" to "📝"
)

internal val CLOSED_EMOJI_PATTERN = Regex("""(?<![a-zA-Z0-9]):([a-zA-Z0-9_\-+]+):""")
internal val UNCLOSED_EMOJI_PATTERN = Regex("""(?<![a-zA-Z0-9]):([a-zA-Z0-9_\-+]{2,30})(?=[,\s，。！!？?；;]|\Z)""")

/**
 * 智能渲染或过滤 Discourse 表情代码（如 :laughing:, :joy:, :heart:, :+1: 以及截断的 :laughing 等）：
 * - [keepUnknown] 为 true 时（如聊天场景），无法识别的代码保留原貌；为 false 时（如摘要场景）剔除，避免显示生硬难看的冒号字符。
 */
fun renderEmojiShortcodes(text: String?, keepUnknown: Boolean = false): String? {
    if (text.isNullOrBlank()) return text
    var res = CLOSED_EMOJI_PATTERN.replace(text) { matchResult ->
        val code = matchResult.groupValues[1].lowercase()
        EMOJI_SHORTCODE_MAP[code] ?: if (keepUnknown) matchResult.value else ""
    }
    res = UNCLOSED_EMOJI_PATTERN.replace(res) { matchResult ->
        val code = matchResult.groupValues[1].lowercase()
        EMOJI_SHORTCODE_MAP[code] ?: if (keepUnknown) matchResult.value else ""
    }
    return res
}

/** 兼容旧接口：默认过滤无法识别的代码 */
fun renderOrFilterEmojiShortcodes(text: String?): String? = renderEmojiShortcodes(text, keepUnknown = false)

/**
 * 清洗摘要文本。
 * 深度过滤掉各种图片占位符、相机相册自动生成的文件名、十六进制哈希、分辨率大小以及无意义的元信息；
 * 并将 :laughing: 等表情代码自动转为真实 Emoji（未识别的表情代码自动过滤消除），保留真实有意义正文。
 */
internal fun cleanExcerpt(rawHtmlOrText: String?): String? {
    if (rawHtmlOrText.isNullOrBlank()) return null
    val parsed = try {
        val doc = Jsoup.parseBodyFragment(rawHtmlOrText)
        doc.select(".lightbox-wrapper, .lightbox, .meta, .informations, .filename, img:not(.emoji), svg, video, audio").remove()
        doc.select("img.emoji").forEach { img ->
            val alt = img.attr("alt")
            val converted = renderOrFilterEmojiShortcodes(alt).orEmpty()
            img.replaceWith(org.jsoup.nodes.TextNode(converted))
        }
        doc.text().trim()
    } catch (_: Exception) {
        rawHtmlOrText.trim()
    }

    if (parsed.isEmpty()) return null

    var cleaned = parsed

    // 1. 先进行 Emoji Shortcode 渲染与过滤（如 :laughing: -> 😆，未知代码过滤）
    cleaned = renderOrFilterEmojiShortcodes(cleaned).orEmpty()

    // 2. 清理占位符：[image], [图片], [图], [photo], [attachment], [video], [audio] 等（包括首部、中间、尾部）
    cleaned = cleaned.replace(
        Regex("""\[(?:image|img|图片|贴图|图|photo|attachment|video|audio)\]""", RegexOption.IGNORE_CASE),
        " "
    )

    // 3. 清理常见相机、相册、截图默认命名前缀及完整文件名：
    // 如 IMG_5509, PXL_20230501, Screenshot_2023..., 微信截图_..., mmexport1234..., wx_camera_...
    cleaned = cleaned.replace(
        Regex("""\b(?:IMG_\d+|PXL_\d+|Screenshot[^\s]*|截屏[^\s]*|微信截图[^\s]*|QQ截图[^\s]*|mmexport\d+|wx_camera_\d+|Pasted\s+image[^\s]*)\b""", RegexOption.IGNORE_CASE),
        " "
    )

    // 4. 清理 16~64 位的纯十六进制哈希字符串（如 MD5: E577F8EF97E51E7BAD93488ED6435DCA）与标准 UUID
    cleaned = cleaned.replace(Regex("""\b[0-9a-fA-F]{16,64}\b"""), " ")
    cleaned = cleaned.replace(Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b"""), " ")

    // 5. 清理图片扩展名文件：xxx.png, xxx.jpg, xxx.jpeg, xxx.webp 等
    cleaned = cleaned.replace(
        Regex("""\b[\w\-_.]+\.(?:png|jpe?g|gif|webp|bmp|svg)\b""", RegexOption.IGNORE_CASE),
        " "
    )

    // 6. 清理分辨率与体积元信息：image1540×734, 1920x1080, 54.4 KB, 1.2MB 等
    cleaned = cleaned.replace(
        Regex("""\b(?:image|img|photo|贴图)?\s*\d{2,5}\s*[×xX*]\s*\d{2,5}\b""", RegexOption.IGNORE_CASE),
        " "
    )
    cleaned = cleaned.replace(
        Regex("""\b[\d.]+\s*(?:KB|MB|GB|B|千字节|字节)\b""", RegexOption.IGNORE_CASE),
        " "
    )
    cleaned = cleaned.replace(
        Regex("""\b(?:image|img|photo|贴图)\s*\d*\b""", RegexOption.IGNORE_CASE),
        " "
    )

    // 7. 规整空白与清理多余残留标点
    cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
    cleaned = cleaned.trim(',', '，', ':', '：', '|', '丨', '/', '\\', '、', '-', '—', ' ')

    // 8. 必须包含字符、数字或大于 127 的符号（如中文、Emoji 等），否则认定无有效正文
    if (cleaned.isBlank() || cleaned.none { it.isLetterOrDigit() || it.code > 127 }) {
        return null
    }

    return cleaned
}

/**
 * 获取话题真实正文摘录:
 * 1. 优先使用服务端下发的真实 excerpt (置顶帖、搜索结果)，并剥离首部图片元属性
 * 2. 检查用户浏览过或后台轻量预取到的真实首楼正文
 * 3. 严格遵循真实内容原则，绝不拼凑合成假话
 */
private fun getTopicRealExcerpt(topic: Topic): String? {
    val fromTopic = cleanExcerpt(topic.excerpt)
    if (fromTopic != null) return fromTopic

    return cleanExcerpt(ExcerptCache.get(topic.id))
}

@Composable
private fun rememberTopicThumbnailRequest(
    url: String?,
    widthPx: Int,
    heightPx: Int,
    retry: Int,
): ImageRequest? {
    val context = LocalContext.current
    return remember(context, url, widthPx, heightPx, retry) {
        url?.takeIf { it.isNotBlank() }?.let { thumbnailUrl ->
            ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(widthPx, heightPx)
                // 同一资源在大图、小图模式下各自保持合适的内存解码尺寸。
                .memoryCacheKey("topic-thumbnail:$thumbnailUrl:${widthPx}x${heightPx}")
                // 磁盘缓存按完整 URL 存（Coil 默认键），与详情页、大图查看器共用：
                // 任一处拉成功，另一处直接命中本地，不再额外打一次站内请求。
                .setParameter("retry", retry, memoryCacheKey = null)
                .crossfade(false)
                .build()
        }
    }
}

@Composable
private fun TopicThumbnailImage(
    thumbnailUrl: String,
    fallbackUrl: String?,
    widthPx: Int,
    heightPx: Int,
    modifier: Modifier,
) {
    var useFallback by remember(thumbnailUrl, fallbackUrl) { mutableStateOf(false) }
    var retry by remember(thumbnailUrl, fallbackUrl) { mutableIntStateOf(0) }
    var pendingRetry by remember(thumbnailUrl, fallbackUrl) { mutableStateOf(false) }
    val requestUrl = if (useFallback) fallbackUrl else thumbnailUrl
    val request = rememberTopicThumbnailRequest(requestUrl, widthPx, heightPx, retry)
    val canFallback = !fallbackUrl.isNullOrBlank() && fallbackUrl != thumbnailUrl

    // 列表在详情页下方始终保持组合，失败态不会因"返回列表"自然刷新；
    // 瞬时失败（门禁冷却、429、网络抖动）等冷却结束后自己补拉，次数封顶避免风控。
    if (pendingRetry) {
        LaunchedEffect(retry) {
            awaitImageRetrySlot(retry)
            pendingRetry = false
            retry++
        }
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onError = { state ->
            when {
                isTransientImageError(state.result.throwable) && retry < IMAGE_AUTO_RETRY_LIMIT -> pendingRetry = true
                !useFallback && canFallback -> useFallback = true
            }
        },
    )
}

/**
 * 现代知乎风列表卡片。
 * 严格按照【大标题置顶 -> 作者行 -> 2行正文摘要 -> 底部操作行】分层排版。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopicRow(
    item: TopicListItem,
    query: String? = null,
    isHighlighted: Boolean = false,
    loadCachedPreview: Boolean = true,
    onSetNotificationLevel: ((TopicNotificationLevel) -> Unit)? = null,
    onCopyLink: (() -> Unit)? = null,
    onBookmark: (() -> Unit)? = null,
    onFlag: (() -> Unit)? = null,
    onSwipeMute: (() -> Unit)? = null,
    onSwipeWatch: (() -> Unit)? = null,
    onOpenUser: (String, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onClick: () -> Unit,
    feedGraphicMode: Boolean = true,
    feedDisplayMode: String = "no_excerpt",
    showTopicUnreadDot: Boolean = true,
    topicUnreadDotPosition: TopicUnreadDotPosition = TopicUnreadDotPosition.Avatar,
) {
    var showLevelMenu by remember { mutableStateOf(false) }
    val hasAnyLongPress = org.linuxdo.android.ui.LocalTopicPreviewLoader.current != null ||
        onSetNotificationLevel != null || onCopyLink != null || onBookmark != null || onFlag != null
    val topic = item.topic
    val newReplyCount = topic.newReplyCount(item.notificationLevel)
    val showsThumbnail = feedDisplayMode == "no_excerpt" || feedDisplayMode == "small_image" || feedDisplayMode == "graphic"
    val thumbnailUrl = if (showsThumbnail) {
        val coverImageUrl = ExcerptCache.peekCoverImage(topic.id)
        val width = if (feedDisplayMode == "small_image") 320 else 960
        remember(topic.imageUrl, topic.thumbnails, coverImageUrl, width) {
            topic.thumbnailUrlForList(targetWidth = width) ?: coverImageUrl
        }
    } else null
    val originalImageUrl = if (thumbnailUrl != null) remember(topic.imageUrl) { topic.originalImageUrl() } else null
    val needle = query?.trim().orEmpty()
    val accent = IosTheme.colors.accent
    val titleUnreadDot = showTopicUnreadDot && topicUnreadDotPosition == TopicUnreadDotPosition.TitleLeft && topic.unseen
    val title = remember(topic.title, topic.pinned, topic.closed, needle, titleUnreadDot, accent) {
        titleWithBadges(topic, accent, needle, includeUnseenDot = titleUnreadDot)
    }
    val titleInlineContent = if (titleUnreadDot) unseenDotContent(accent) else emptyMap()

    val usesCachedExcerpt = feedDisplayMode != "no_excerpt" && query == null && item.bookmarkId == null
    val cachedExcerpt = if (usesCachedExcerpt) ExcerptCache.peekExcerpt(topic.id) else null
    val realExcerpt = when {
        feedDisplayMode == "no_excerpt" -> null
        !usesCachedExcerpt -> topic.excerpt
        else -> remember(topic.excerpt, cachedExcerpt) {
            cleanExcerpt(topic.excerpt) ?: cleanExcerpt(cachedExcerpt)
        }
    }
    val needsCachedExcerpt = usesCachedExcerpt && realExcerpt == null && cachedExcerpt == null
    val needsCachedCover = showsThumbnail && thumbnailUrl == null
    if (loadCachedPreview && (needsCachedExcerpt || needsCachedCover)) {
        LaunchedEffect(topic.id) { ExcerptCache.loadBatchFromStore(listOf(topic.id)) }
    }

    val excerpt = remember(realExcerpt, needle, accent) {
        val raw = realExcerpt
        if (raw.isNullOrBlank()) null
        else if (needle.isEmpty()) buildAnnotatedString { append(raw) }
        else buildAnnotatedString {
            var start = raw.indexOf(needle, ignoreCase = true)
            var cursor = 0
            while (start >= 0) {
                append(raw.substring(cursor, start))
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                    append(raw.substring(start, start + needle.length))
                }
                cursor = start + needle.length
                start = raw.indexOf(needle, cursor, ignoreCase = true)
            }
            append(raw.substring(cursor))
        }
    }
    val density = LocalDensity.current
    val swipeScope = rememberCoroutineScope()
    val hasSwipeActions = onSwipeMute != null || onSwipeWatch != null
    val actionCount = (if (onSwipeWatch != null) 1 else 0) + (if (onSwipeMute != null) 1 else 0)
    val actionWidthDp = 76.dp
    val maxSwipePx = remember(density, actionCount) { with(density) { (actionWidthDp * actionCount).toPx() } }
    val revealThresholdPx = remember(density, actionCount) {
        with(density) { (if (actionCount >= 2) 56.dp else 44.dp).toPx() }
    }
    val edgeGuardPx = remember(density) { with(density) { 24.dp.toPx() } }
    var swipeOffsetX by remember(topic.id) { mutableFloatStateOf(0f) }
    var swipeSettleJob by remember(topic.id) { mutableStateOf<Job?>(null) }
    fun settleSwipeTo(target: Float) {
        swipeSettleJob?.cancel()
        swipeSettleJob = swipeScope.launch {
            animate(
                initialValue = swipeOffsetX,
                targetValue = target,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            ) { value, _ -> swipeOffsetX = value }
        }
    }
    LaunchedEffect(item.notificationLevelUpdating, topic.id) {
        if (item.notificationLevelUpdating && swipeOffsetX < 0f) settleSwipeTo(0f)
    }
    val swipeModifier = if (hasSwipeActions && maxSwipePx > 0f) {
        Modifier.pointerInput(topic.id, maxSwipePx, revealThresholdPx, item.notificationLevelUpdating) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (item.notificationLevelUpdating) return@awaitEachGesture
                // 右边缘留给系统返回手势，避免冲突
                if (down.position.x > size.width - edgeGuardPx && swipeOffsetX >= 0f) return@awaitEachGesture
                var totalDx = 0f
                var totalDy = 0f
                var lockedHorizontal = false
                val startOffset = swipeOffsetX
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (lockedHorizontal) {
                            val target = if (swipeOffsetX <= -revealThresholdPx) -maxSwipePx else 0f
                            settleSwipeTo(target)
                        }
                        break
                    }
                    val delta = change.positionChange()
                    totalDx += delta.x
                    totalDy += delta.y
                    if (!lockedHorizontal) {
                        val absX = abs(totalDx)
                        val absY = abs(totalDy)
                        if (absY > viewConfiguration.touchSlop && absY > absX) {
                            break
                        }
                        if (absX > viewConfiguration.touchSlop && absX > absY * 1.25f) {
                            // 左滑展开（totalDx < 0），已展开时允许右滑收回（startOffset < 0）
                            val canDrag = (startOffset < 0f) || (totalDx < 0f)
                            if (!canDrag) break
                            lockedHorizontal = true
                            swipeSettleJob?.cancel()
                        }
                    }
                    if (lockedHorizontal) {
                        change.consume()
                        swipeOffsetX = (startOffset + totalDx).coerceIn(-maxSwipePx * 1.12f, 0f)
                    }
                }
            }
        }
    } else Modifier
    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .then(swipeModifier),
    ) {
        if (hasSwipeActions && swipeOffsetX < -0.5f) {
            val isWatching = item.notificationLevel == TopicNotificationLevel.Watching
            val watchBg = if (isWatching) Color(0xFF5E5CE6) else Color(0xFF007AFF)
            val muteBg = Color(0xFFFF3B30)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(if (onSwipeMute != null) muteBg else watchBg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(modifier = Modifier.fillMaxHeight()) {
                    if (onSwipeWatch != null) {
                        Box(
                            modifier = Modifier
                                .width(actionWidthDp)
                                .fillMaxHeight()
                                .background(watchBg)
                                .clickable(enabled = !item.notificationLevelUpdating) {
                                    settleSwipeTo(0f)
                                    onSwipeWatch()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TopicNotificationIcon(
                                    level = TopicNotificationLevel.Watching,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White,
                                )
                                Text(
                                    text = if (isWatching) "取消关注" else "关注",
                                    style = IosTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    if (onSwipeMute != null) {
                        Box(
                            modifier = Modifier
                                .width(actionWidthDp)
                                .fillMaxHeight()
                                .background(muteBg)
                                .clickable(enabled = !item.notificationLevelUpdating) {
                                    settleSwipeTo(0f)
                                    onSwipeMute()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TopicNotificationIcon(
                                    level = TopicNotificationLevel.Muted,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White,
                                )
                                Text(
                                    text = "免打扰",
                                    style = IosTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        val isGraphic = feedDisplayMode == "graphic"
        val isNoExcerpt = feedDisplayMode == "no_excerpt"
        val isCardStyle = isGraphic || isNoExcerpt
        Column(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                .background(if (isHighlighted) IosTheme.colors.card.copy(alpha = 0.85f) else IosTheme.colors.card)
                .combinedClickable(
                    onClick = {
                        if (swipeOffsetX < -1f) settleSwipeTo(0f) else onClick()
                    },
                    onLongClick = if (hasAnyLongPress && !item.notificationLevelUpdating) {
                        {
                            if (swipeOffsetX < -1f) settleSwipeTo(0f)
                            else showLevelMenu = true
                        }
                    } else null,
                )
                .padding(
                    horizontal = if (isCardStyle) 16.dp else IosMetrics.screenPadding,
                    vertical = if (isCardStyle) 12.dp else 10.dp,
                ),
        ) {
            if (isNoExcerpt) {
                // ── 无正文模式（微博样式）：从上到下：用户图标 -> 标题 -> 缩略图 ──
                // 1. 用户图标与作者信息行（微博风格双行头部）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        UserAvatar(
                            avatarUrl = item.avatarUrl,
                            fallbackUrl = item.staticAvatarUrl,
                            username = item.authorUsername,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(enabled = !item.authorUsername.isNullOrBlank()) {
                                    item.authorUsername?.let { u ->
                                        onOpenUser(u, item.avatarUrl, item.staticAvatarUrl, item.authorName ?: item.displayName)
                                    }
                                },
                            cornerRadius = 16.dp,
                        )
                        if (showTopicUnreadDot && topicUnreadDotPosition == TopicUnreadDotPosition.Avatar && topic.unseen) {
                            Box(
                                Modifier
                                    .size(7.5.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .clip(CircleShape)
                                    .background(IosTheme.colors.card)
                                    .padding(1.dp)
                                    .clip(CircleShape)
                                    .background(IosTheme.colors.accent)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.displayName.ifBlank { "匿名" },
                            style = IosTheme.type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
                            color = IosTheme.colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = !item.authorUsername.isNullOrBlank()) {
                                item.authorUsername?.let { u ->
                                    onOpenUser(u, item.avatarUrl, item.staticAvatarUrl, item.authorName ?: item.displayName)
                                }
                            },
                        )
                        item.trustLevel?.takeIf { it > 0 }?.let { level ->
                            Spacer(Modifier.width(5.dp))
                            LevelBadge(level)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.notificationLevel?.takeIf { it != TopicNotificationLevel.Regular }?.let { level ->
                            TopicNotificationLevelTag(level)
                            Spacer(Modifier.width(6.dp))
                        }
                        item.category?.let { CategoryTag(it) }
                    }
                }

                NotificationLevelMenu(
                    expanded = showLevelMenu && hasAnyLongPress,
                    currentLevel = item.notificationLevel,
                    updating = item.notificationLevelUpdating,
                    previewTopic = topic,
                    previewCategory = item.category,
                    onOpenTopic = onClick,
                    onDismiss = { showLevelMenu = false },
                    onPick = onSetNotificationLevel,
                    onCopyLink = onCopyLink,
                    onBookmark = onBookmark,
                    onFlag = onFlag,
                )

                // 2. 标题（紧随作者行，精致克制主标题，契合主流App信息流）
                Spacer(Modifier.height(7.5.dp))
                Text(
                    text = title,
                    style = IosTheme.type.body.copy(
                        fontSize = 14.6.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.8.sp,
                        letterSpacing = 0.1.sp,
                    ),
                    color = IosTheme.colors.label,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = titleInlineContent,
                )

                // 3. 缩略图（如果有配图，上下微留白舒展，不团在一起）
                if (thumbnailUrl != null) {
                    Spacer(Modifier.height(11.dp))
                    TopicThumbnailImage(
                        thumbnailUrl = thumbnailUrl,
                        fallbackUrl = originalImageUrl,
                        widthPx = 960,
                        heightPx = 540,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, IosTheme.colors.separator.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .background(IosTheme.colors.fieldBackground),
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    Spacer(Modifier.height(11.dp))
                }

                // 4. 底部互动栏（左侧：评论 / 浏览 / 活跃时间；右侧：新回复数）
                TopicCardMetaBar(
                    topic = topic,
                    newReplyCount = newReplyCount,
                    compact = false,
                )
            } else if (isGraphic) {
                // ── 图文模式：标题置顶（知乎级粗体大标题，首字平齐无毛刺） ──
                Text(
                    text = title,
                    style = IosTheme.type.body.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        letterSpacing = 0.15.sp,
                    ),
                    color = IosTheme.colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = titleInlineContent,
                )

                Spacer(Modifier.height(8.dp))

                // ── 标题下方作者与分类信息行 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            UserAvatar(
                                avatarUrl = item.avatarUrl,
                                fallbackUrl = item.staticAvatarUrl,
                                username = item.authorUsername,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable(enabled = !item.authorUsername.isNullOrBlank()) {
                                        item.authorUsername?.let { u ->
                                            onOpenUser(u, item.avatarUrl, item.staticAvatarUrl, item.authorName ?: item.displayName)
                                        }
                                    },
                                cornerRadius = 13.dp,
                            )
                            if (showTopicUnreadDot && topicUnreadDotPosition == TopicUnreadDotPosition.Avatar && topic.unseen) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 1.dp, y = (-1).dp)
                                        .clip(CircleShape)
                                        .background(IosTheme.colors.card)
                                        .padding(1.dp)
                                        .clip(CircleShape)
                                        .background(IosTheme.colors.accent)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.displayName.ifBlank { "匿名" },
                            style = IosTheme.type.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
                            color = IosTheme.colors.label.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = !item.authorUsername.isNullOrBlank()) {
                                item.authorUsername?.let { u ->
                                    onOpenUser(u, item.avatarUrl, item.staticAvatarUrl, item.authorName ?: item.displayName)
                                }
                            },
                        )
                        item.trustLevel?.takeIf { it > 0 }?.let { level ->
                            Spacer(Modifier.width(5.dp))
                            LevelBadge(level)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.notificationLevel?.takeIf { it != TopicNotificationLevel.Regular }?.let { level ->
                            TopicNotificationLevelTag(level)
                            Spacer(Modifier.width(6.dp))
                        }
                        item.category?.let { CategoryTag(it) }
                    }
                }

                NotificationLevelMenu(
                    expanded = showLevelMenu && hasAnyLongPress,
                    currentLevel = item.notificationLevel,
                    updating = item.notificationLevelUpdating,
                    previewTopic = topic,
                    previewCategory = item.category,
                    onOpenTopic = onClick,
                    onDismiss = { showLevelMenu = false },
                    onPick = onSetNotificationLevel,
                    onCopyLink = onCopyLink,
                    onBookmark = onBookmark,
                    onFlag = onFlag,
                )

                // 3. 正文两行摘录 (严格保证两行知乎排版结构，固定最小高度防抖)
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = excerpt,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) togetherWith
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                        },
                        label = "excerpt-fade",
                    ) { targetExcerpt ->
                        if (targetExcerpt != null) {
                            Text(
                                text = targetExcerpt,
                                style = IosTheme.type.footnote.copy(fontSize = 14.sp, lineHeight = 20.5.sp, fontWeight = FontWeight.Normal),
                                color = IosTheme.colors.secondaryLabel,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(IosTheme.colors.separator.copy(alpha = 0.22f))
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.58f)
                                        .height(13.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(IosTheme.colors.separator.copy(alpha = 0.16f))
                                )
                            }
                        }
                    }
                }

                // 4. 封面大图预览 (16:9 高清全宽展示)
                thumbnailUrl?.let { url ->
                    Spacer(Modifier.height(9.dp))
                    TopicThumbnailImage(
                        thumbnailUrl = url,
                        fallbackUrl = originalImageUrl,
                        widthPx = 960,
                        heightPx = 540,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.5.dp, IosTheme.colors.separator.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .background(IosTheme.colors.fieldBackground),
                    )
                }

                // 5. 底部互动栏（左侧：评论 / 浏览 / 发布时间；右侧：新回复数）
                Spacer(Modifier.height(10.dp))
                TopicCardMetaBar(
                    topic = topic,
                    newReplyCount = newReplyCount,
                    compact = false,
                )
            } else {
                // ── 小图与纯文模式: 统一知乎结构 (大标题置顶 -> 作者行 -> 2行正文摘要 -> 底部操作行) ──
                // 1. 标题置顶 (大一号半粗)
                Text(
                    text = title,
                    style = IosTheme.type.body.copy(
                        fontSize = 15.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 21.5.sp,
                        letterSpacing = 0.15.sp,
                    ),
                    color = IosTheme.colors.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = titleInlineContent,
                )

                Spacer(Modifier.height(6.dp))

                // 2. 标题下方作者与分类行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            UserAvatar(
                                avatarUrl = item.avatarUrl,
                                fallbackUrl = item.staticAvatarUrl,
                                username = item.authorUsername,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable(enabled = !item.authorUsername.isNullOrBlank()) {
                                        item.authorUsername?.let { u ->
                                            onOpenUser(u, item.avatarUrl, item.staticAvatarUrl, item.authorName ?: item.displayName)
                                        }
                                    },
                                cornerRadius = 4.dp,
                            )
                            if (showTopicUnreadDot && topicUnreadDotPosition == TopicUnreadDotPosition.Avatar && topic.unseen) {
                                Box(
                                    Modifier
                                        .size(6.5.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 1.dp, y = (-1).dp)
                                        .clip(CircleShape)
                                        .background(IosTheme.colors.card)
                                        .padding(1.dp)
                                        .clip(CircleShape)
                                        .background(IosTheme.colors.accent)
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.displayName.ifBlank { "匿名" },
                            style = IosTheme.type.footnote.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            color = IosTheme.colors.secondaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = !item.authorUsername.isNullOrBlank()) {
                                item.authorUsername?.let { u ->
                                    onOpenUser(u, item.avatarUrl, item.staticAvatarUrl, item.authorName ?: item.displayName)
                                }
                            },
                        )
                        item.trustLevel?.takeIf { it > 0 }?.let { level ->
                            Spacer(Modifier.width(4.dp))
                            LevelBadge(level)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.notificationLevel?.takeIf { it != TopicNotificationLevel.Regular }?.let { level ->
                            TopicNotificationLevelTag(level)
                            Spacer(Modifier.width(6.dp))
                        }
                        item.category?.let { CategoryTag(it) }
                    }
                }

                NotificationLevelMenu(
                    expanded = showLevelMenu && hasAnyLongPress,
                    currentLevel = item.notificationLevel,
                    updating = item.notificationLevelUpdating,
                    previewTopic = topic,
                    previewCategory = item.category,
                    onOpenTopic = onClick,
                    onDismiss = { showLevelMenu = false },
                    onPick = onSetNotificationLevel,
                    onCopyLink = onCopyLink,
                    onBookmark = onBookmark,
                    onFlag = onFlag,
                )

                Spacer(Modifier.height(6.dp))

                // 3. 两行正文摘要 (有图时小图右置混排，无图整宽，永远保持两行正文)
                if (feedDisplayMode == "small_image" && thumbnailUrl != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (excerpt != null) {
                                Text(
                                    text = excerpt,
                                    style = IosTheme.type.footnote.copy(fontSize = 13.sp, lineHeight = 18.5.sp, fontWeight = FontWeight.Normal),
                                    color = IosTheme.colors.secondaryLabel,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(IosTheme.colors.separator.copy(alpha = 0.22f))
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(IosTheme.colors.separator.copy(alpha = 0.16f))
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        TopicThumbnailImage(
                            thumbnailUrl = thumbnailUrl,
                            fallbackUrl = originalImageUrl,
                            widthPx = 240,
                            heightPx = 168,
                            modifier = Modifier
                                .size(width = 74.dp, height = 52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(IosTheme.colors.fieldBackground),
                        )
                    }
                } else {
                    if (excerpt != null) {
                        Text(
                            text = excerpt,
                            style = IosTheme.type.footnote.copy(fontSize = 13.sp, lineHeight = 18.5.sp, fontWeight = FontWeight.Normal),
                            color = IosTheme.colors.secondaryLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(IosTheme.colors.separator.copy(alpha = 0.22f))
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.58f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(IosTheme.colors.separator.copy(alpha = 0.16f))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(7.dp))

                // 4. 底部紧凑互动行（左侧：评论 / 浏览 / 时间；右侧：标签与新回复数）
                TopicCardMetaBar(
                    topic = topic,
                    newReplyCount = newReplyCount,
                    compact = true,
                    trailingTags = topic.tags.take(2),
                )
            }
        }
    }
}

/** 一档信任等级的配色。描边与文字都留 Brush 口子,好让高等级上金属渐变和彩虹。 */
internal data class LevelPalette(
    val background: Brush,
    val border: Brush,
    val content: Color,
    val contentBrush: Brush? = null,
)

internal val RAINBOW = listOf(
    Color(0xFFFF4D6D), Color(0xFFFF9F1C), Color(0xFFFFD60A),
    Color(0xFF34C759), Color(0xFF0A84FF), Color(0xFFAF52DE),
)

/**
 * 作者信任等级徽章。颜色本身就是身份:Lv1 蓝、Lv2 琥珀橙、Lv3 鎏金、Lv4 彩虹加金冠。
 *
 * Lv1/Lv2 的色值与「分类」页的 Lv 勋章同源,免得同一个 Lv 在两个页面是两种颜色。
 * Lv3 起刻意改成**深底亮字**:Lv2 和 Lv3 都在黄色系里,只调深浅在 10sp 的徽章上根本分不开,
 * 明暗反转才一眼可辨 —— 顺带让等级越高的牌子越有金属分量。
 * 这里一律用静态渐变、不挂流光动画:列表每行都要画一遍,动画那点光泽不值这个开销。
 */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    val isDark = IosTheme.colors.isDark
    val shape = RoundedCornerShape(6.dp)
    val palette = when {
        level >= 4 -> LevelPalette(
            background = Brush.horizontalGradient(
                listOf(Color(0xFF241233), Color(0xFF32124A), Color(0xFF1A1030)),
            ),
            border = Brush.horizontalGradient(RAINBOW),
            content = Color(0xFFFFF1C2),
            contentBrush = Brush.horizontalGradient(RAINBOW),
        )
        level == 3 -> LevelPalette(
            background = Brush.horizontalGradient(
                listOf(Color(0xFF3E2B08), Color(0xFF6B4A12), Color(0xFF4A3410)),
            ),
            border = Brush.horizontalGradient(
                listOf(Color(0xFFFFE9A8), Color(0xFFD4AF37), Color(0xFFFFD76A)),
            ),
            content = Color(0xFFFFD873),
        )
        level == 2 -> LevelPalette(
            background = Brush.horizontalGradient(
                if (isDark) listOf(Color(0xFF33230A), Color(0xFF47310E))
                else listOf(Color(0xFFFFF6DE), Color(0xFFFFE7B0)),
            ),
            border = SolidColor(if (isDark) Color(0xFF8F651A) else Color(0xFFEFC163)),
            content = if (isDark) Color(0xFFFFD166) else Color(0xFF824B05),
        )
        else -> LevelPalette(
            background = Brush.horizontalGradient(
                if (isDark) listOf(Color(0xFF13263E), Color(0xFF193354))
                else listOf(Color(0xFFEAF3FF), Color(0xFFDCEBFF)),
            ),
            border = SolidColor(if (isDark) Color(0xFF2B548C) else Color(0xFFBBD7FF)),
            content = if (isDark) Color(0xFF6EAEFF) else Color(0xFF155FD0),
        )
    }
    val weight = if (level >= 3) FontWeight.ExtraBold else FontWeight.SemiBold
    val textStyle = if (palette.contentBrush != null) {
        TextStyle(brush = palette.contentBrush, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = weight)
    } else {
        TextStyle(color = palette.content, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = weight)
    }
    Row(
        modifier
            .clip(shape)
            .background(palette.background)
            .border(0.8.dp, palette.border, shape)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (level >= 4) {
            CrownGlyph(Color(0xFFFFD60A))
            Spacer(Modifier.width(2.dp))
        }
        Text(text = "Lv$level", style = textStyle)
    }
}

/** Lv4 专属小金冠。只有最高等级才画,所以列表里的数量天然很少。 */
@Composable
internal fun CrownGlyph(color: Color) {
    Canvas(Modifier.size(8.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val crown = Path().apply {
                moveTo(3f, 19f)
                lineTo(1.5f, 7f)
                lineTo(7.5f, 12f)
                lineTo(12f, 4f)
                lineTo(16.5f, 12f)
                lineTo(22.5f, 7f)
                lineTo(21f, 19f)
                close()
            }
            drawPath(crown, color)
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    ForumTagText(
        tag = tag,
        fallbackHash = false,
        style = IosTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
        color = IosTheme.colors.secondaryLabel,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(IosTheme.colors.fieldBackground)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/**
 * 列表卡片统一底部互动栏：
 * - 左侧定宽槽位显示：评论数、浏览数、活跃时间（每个指标使用定宽容器 + `tnum` 等宽数字，
 *   确保不管数字是 1 位还是 4 位，上下相邻卡片的三个图标永远严格垂直对齐在同一直线上，且间距舒展不拥挤）；
 * - 右侧居右显示：跟踪/关注话题的新增未读回复数（主题色胶囊显著标出）。
 */
@Composable
private fun TopicCardMetaBar(
    topic: Topic,
    newReplyCount: Int = 0,
    compact: Boolean = false,
    trailingTags: List<String> = emptyList(),
) {
    val iconColor = IosTheme.colors.secondaryLabel.copy(alpha = 0.78f)
    val textColor = IosTheme.colors.secondaryLabel
    val iconSize = if (compact) 12.5.dp else 14.dp
    val fontSize = if (compact) 11.5.sp else 12.2.sp
    val textStyle = IosTheme.type.caption.copy(
        fontSize = fontSize,
        lineHeight = fontSize,
        fontFeatureSettings = "tnum",
    )
    val replySlotWidth = if (compact) 58.dp else 68.dp
    val viewSlotWidth = if (compact) 66.dp else 76.dp
    val iconTextGap = if (compact) 4.5.dp else 5.5.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 评论数（固定槽位宽度，确保下一列“浏览图标”在所有卡片中严格上下对齐）
        Row(
            modifier = Modifier.width(replySlotWidth),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReplyGlyph(iconColor, iconSize = iconSize)
            Spacer(Modifier.width(iconTextGap))
            Text(
                text = formatCount(topic.totalReplies),
                style = textStyle,
                color = textColor,
                maxLines = 1,
            )
        }

        // 2. 浏览数（固定槽位宽度，确保下一列“时钟图标”在所有卡片中严格上下对齐）
        Row(
            modifier = Modifier.width(viewSlotWidth),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewGlyph(iconColor, iconSize = iconSize)
            Spacer(Modifier.width(iconTextGap))
            Text(
                text = formatCount(topic.views),
                style = textStyle,
                color = textColor,
                maxLines = 1,
            )
        }

        // 3. 活跃时间
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClockGlyph(iconColor, iconSize = iconSize)
            Spacer(Modifier.width(iconTextGap))
            Text(
                text = RelativeTime.format(topic.bumpedAt ?: topic.lastPostedAt ?: topic.createdAt),
                style = textStyle,
                color = textColor,
                maxLines = 1,
            )
        }

        Spacer(Modifier.weight(1f))

        if (trailingTags.isNotEmpty()) {
            trailingTags.forEachIndexed { index, tag ->
                TagChip(tag)
                if (index < trailingTags.lastIndex || newReplyCount > 0) {
                    Spacer(Modifier.width(6.dp))
                }
            }
        }

        // 4. 居右新回复数徽标（仅在跟踪/关注且存在未读新回复时显示，主题色显著标出）
        if (newReplyCount > 0) {
            val badgeText = if (newReplyCount > 99) "99+" else "+$newReplyCount"
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = if (compact) 26.dp else 28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(IosTheme.colors.accent)
                    .padding(horizontal = if (compact) 7.dp else 8.dp, vertical = if (compact) 2.dp else 2.5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeText,
                    style = IosTheme.type.caption.copy(
                        fontSize = if (compact) 11.sp else 11.5.sp,
                        lineHeight = if (compact) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

private val replyGlyphPath by lazy {
    Path().apply {
        moveTo(6.2f, 4.8f)
        lineTo(17.8f, 4.8f)
        quadraticTo(21f, 4.8f, 21f, 8.0f)
        lineTo(21f, 14.6f)
        quadraticTo(21f, 17.8f, 17.8f, 17.8f)
        lineTo(11.2f, 17.8f)
        lineTo(7.2f, 21.0f)
        lineTo(7.2f, 17.8f)
        lineTo(6.2f, 17.8f)
        quadraticTo(3f, 17.8f, 3f, 14.6f)
        lineTo(3f, 8.0f)
        quadraticTo(3f, 4.8f, 6.2f, 4.8f)
        close()
    }
}

/** 回复气泡（一体化空心圆角对话框 + 左下角微折角，垂直重心校准在 Y=12f）。 */
@Composable
private fun ReplyGlyph(color: Color, iconSize: Dp = 13.5.dp) {
    Canvas(Modifier.size(iconSize)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            drawPath(
                path = replyGlyphPath,
                color = color,
                style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private val viewGlyphPath by lazy {
    Path().apply {
        moveTo(2.2f, 12f)
        cubicTo(6f, 5.5f, 18f, 5.5f, 21.8f, 12f)
        cubicTo(18f, 18.5f, 6f, 18.5f, 2.2f, 12f)
        close()
    }
}

/** 浏览眼睛图标（杏仁形轮廓 + 空心瞳孔）。 */
@Composable
private fun ViewGlyph(color: Color, iconSize: Dp = 13.5.dp) {
    Canvas(Modifier.size(iconSize)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            drawPath(
                path = viewGlyphPath,
                color = color,
                style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawCircle(
                color = color,
                radius = 2.7f,
                center = Offset(12f, 12f),
                style = Stroke(width = 2.0f),
            )
        }
    }
}

/** 活跃时间小钟表图标。 */
@Composable
private fun ClockGlyph(color: Color, iconSize: Dp = 13.5.dp) {
    Canvas(Modifier.size(iconSize)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            drawCircle(color, radius = 9f, center = Offset(12f, 12f), style = Stroke(2.0f))
            drawLine(color, Offset(12f, 7.5f), Offset(12f, 12f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            drawLine(color, Offset(12f, 12f), Offset(15.5f, 12f), strokeWidth = 2.0f, cap = StrokeCap.Round)
        }
    }
}

/**
 * 未读蓝点与置顶/关闭标记都内联进标题文本流,和标题共占 2 行配额,
 * 不在标题上方额外占一行 —— 紧凑卡片经不起多一行的开销。
 */
private fun titleWithBadges(
    topic: Topic,
    accent: Color,
    query: String? = null,
    includeUnseenDot: Boolean = true,
) = buildAnnotatedString {
    if (includeUnseenDot && topic.unseen) {
        appendInlineContent(UNSEEN_DOT, "•")
        append(" ")
    }
    if (topic.pinned) {
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
            append("置顶 ")
        }
    }
    if (topic.closed) {
        withStyle(SpanStyle(color = accent.copy(alpha = 0.7f), fontSize = 14.sp)) { append("已关闭 ") }
    }
    val title = topic.title
    val needle = query?.trim().orEmpty()
    if (needle.isNotEmpty()) {
        var start = title.indexOf(needle, ignoreCase = true)
        var cursor = 0
        while (start >= 0) {
            append(title.substring(cursor, start))
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                append(title.substring(start, start + needle.length))
            }
            cursor = start + needle.length
            start = title.indexOf(needle, cursor, ignoreCase = true)
        }
        append(title.substring(cursor))
    } else {
        append(title)
    }
}

private const val UNSEEN_DOT = "unseen-dot"

@Composable
private fun unseenDotContent(accent: Color): Map<String, InlineTextContent> = remember(accent) {
    mapOf(
        UNSEEN_DOT to InlineTextContent(
            Placeholder(9.sp, 9.sp, PlaceholderVerticalAlign.TextCenter),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            }
        },
    )
}

/** 浏览量这类大数按千位缩写,免得把这一行挤到换行。 */
internal fun formatCount(value: Int): String {
    if (value < 1_000) return value.coerceAtLeast(0).toString()
    if (value >= 100_000) return "${(value.toLong() + 500L) / 1_000L}k"
    val tenths = (value + 50) / 100
    val decimal = tenths % 10
    return if (decimal == 0) "${tenths / 10}k" else "${tenths / 10}.${decimal}k"
}

/**
 * 计算类别在当前浅/深色背景上的高可读前景色：
 * - 浅色模式下，像亮黄(#e0d900)、浅青(#32c3c3)、淡蓝(#a8c6fe)等原色在白底上对比度过低，
 *   保留色相并适度压暗至相对亮度 <= 0.155（确保白底对比度 >= 4.8:1），护眼且保留色系辨识度。
 * - 深色模式下，极暗色（如深绿 #38571A）适当提亮，避免在深灰卡片上发黑难辨。
 */
internal fun readableCategoryForeground(raw: Color, isDark: Boolean): Color {
    val lum = 0.2126f * linearChannel(raw.red) +
        0.7152f * linearChannel(raw.green) +
        0.0722f * linearChannel(raw.blue)
    if (!isDark) {
        if (lum <= 0.155f) return raw
        var lo = 0f
        var hi = 1f
        var best = raw
        repeat(10) {
            val mid = (lo + hi) * 0.5f
            // 与深中性色混合而非纯黑，保留色相饱和度同时稳定压暗亮度
            val candidate = Color(
                red = raw.red * mid,
                green = raw.green * mid,
                blue = raw.blue * mid,
                alpha = 1f,
            )
            val cLum = 0.2126f * linearChannel(candidate.red) +
                0.7152f * linearChannel(candidate.green) +
                0.0722f * linearChannel(candidate.blue)
            if (cLum <= 0.155f) {
                best = candidate
                lo = mid
            } else {
                hi = mid
            }
        }
        return best
    } else {
        if (lum >= 0.24f) return raw
        var lo = 0f
        var hi = 1f
        var best = raw
        repeat(10) {
            val mid = (lo + hi) * 0.5f
            val candidate = Color(
                red = raw.red + (1f - raw.red) * mid,
                green = raw.green + (1f - raw.green) * mid,
                blue = raw.blue + (1f - raw.blue) * mid,
                alpha = 1f,
            )
            val cLum = 0.2126f * linearChannel(candidate.red) +
                0.7152f * linearChannel(candidate.green) +
                0.0722f * linearChannel(candidate.blue)
            if (cLum >= 0.24f) {
                best = candidate
                hi = mid
            } else {
                lo = mid
            }
        }
        return best
    }
}

private fun linearChannel(c: Float): Float =
    if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

@Composable
internal fun CategoryTag(category: Category, prominent: Boolean = false, onClick: (() -> Unit)? = null) {
    val rawColor = parseCategoryColor(category.color) ?: IosTheme.colors.accent
    val isDark = IosTheme.colors.isDark
    val fgColor = remember(rawColor, isDark) { readableCategoryForeground(rawColor, isDark) }
    val chipShape = RoundedCornerShape(8.dp)
    val borderColor = fgColor.copy(alpha = if (isDark) 0.38f else 0.26f)
    val pillBg = rawColor.copy(alpha = if (isDark) 0.16f else 0.11f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(chipShape)
            .then(if (onClick != null) Modifier.clickable(
                role = Role.Button, onClickLabel = "浏览${category.name}分类", onClick = onClick,
            ) else Modifier)
            .border(width = 0.75.dp, color = borderColor, shape = chipShape)
            .background(pillBg)
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
    ) {
        ForumCategoryIcon(category, if (prominent) 15.dp else 12.dp, fgColor, directory = prominent)
        Spacer(Modifier.width(4.5.dp))
        Text(
            text = category.name,
            style = if (prominent) IosTheme.type.body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium) else IosTheme.type.footnote.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
            color = fgColor,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ErrorNotice(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = IosMetrics.screenPadding, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            style = IosTheme.type.subheadline,
            color = IosTheme.colors.secondaryLabel,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "重试",
                style = IosTheme.type.body,
                color = IosTheme.colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            org.linuxdo.android.ui.design.NetworkSettingsLink()
        }
    }
}

@Composable
internal fun EmptyNotice() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "这里还没有内容",
            style = IosTheme.type.subheadline,
            color = IosTheme.colors.tertiaryLabel,
        )
    }
}

/** Discourse 下发的分类色通常是 6 位十六进制，主题配置里也可能出现 3 位简写或带 # 前缀。 */
fun parseCategoryColor(hex: String?): Color? =
    org.linuxdo.android.data.parseHexRgbColor(hex)?.let(::Color)

private val CATEGORY_LEVEL_REGEX = Regex("[,，\\s]*Lv\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)

internal fun categoryLevelOf(category: Category): Int =
    CATEGORY_LEVEL_REGEX.find(category.name)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 3) ?: 0

internal fun categoryBaseNameOf(name: String): String =
    name.replace(CATEGORY_LEVEL_REGEX, "").trim().ifBlank { name }

@Composable
private fun CategoryVariantSelector(
    title: String,
    variants: List<Category>,
    currentCategoryId: Int?,
    collapsed: Boolean,
    onSelect: (Category) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentCategory = variants.firstOrNull { it.id == currentCategoryId }
    val currentLevel = currentCategory?.let(::categoryLevelOf)
        ?: CATEGORY_LEVEL_REGEX.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 3)
        ?: 0
    val baseTitle = categoryBaseNameOf(currentCategory?.name ?: title)
    val isDark = IosTheme.colors.isDark

    val (pillBg, pillBorder, pillText) = levelPillColors(currentLevel, isDark)

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(
                    bottom = if (collapsed) 0.dp else 6.dp,
                    end = 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (currentCategory != null) ForumCategoryIcon(currentCategory, if (collapsed) 16.dp else 22.dp,
                parseCategoryColor(currentCategory.color) ?: IosTheme.colors.accent)
            Text(
                text = baseTitle,
                style = if (collapsed) {
                    IosTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    IosTheme.type.largeTitle
                },
                color = IosTheme.colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(pillBg)
                    .border(0.8.dp, pillBorder, RoundedCornerShape(6.dp))
                    .padding(
                        horizontal = if (collapsed) 6.dp else 8.dp,
                        vertical = if (collapsed) 2.dp else 3.5.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Lv.$currentLevel",
                    style = IosTheme.type.caption.copy(
                        fontSize = if (collapsed) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = pillText,
                )
                DropdownChevronGlyph(
                    color = pillText,
                    expanded = expanded,
                    size = if (collapsed) 9.dp else 10.dp,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = IosTheme.colors.card,
            shape = RoundedCornerShape(14.dp),
        ) {
            variants.forEachIndexed { idx, variant ->
                val level = categoryLevelOf(variant)
                val selected = variant.id == currentCategoryId
                val (itemBg, itemBorder, itemText) = levelPillColors(level, isDark)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = false
                            onSelect(variant)
                        }
                        .background(
                            if (selected) IosTheme.colors.accent.copy(alpha = 0.08f) else Color.Transparent,
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(parseCategoryColor(variant.color) ?: IosTheme.colors.tertiaryLabel),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = categoryBaseNameOf(variant.name),
                        style = IosTheme.type.subheadline,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) IosTheme.colors.accent else IosTheme.colors.label,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(itemBg)
                            .border(0.7.dp, itemBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp),
                    ) {
                        Text(
                            text = "Lv.$level",
                            style = IosTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = itemText,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(14.dp))
                    if (selected) {
                        Text(
                            text = "✓",
                            style = IosTheme.type.subheadline,
                            fontWeight = FontWeight.Bold,
                            color = IosTheme.colors.accent,
                        )
                    } else if (variant.topicCount > 0) {
                        Text(
                            text = "${variant.topicCount}",
                            style = IosTheme.type.caption,
                            color = IosTheme.colors.tertiaryLabel,
                        )
                    }
                }
                if (idx < variants.lastIndex) {
                    IosDivider(startIndent = 14.dp)
                }
            }
        }
    }
}

internal fun levelPillColors(level: Int, isDark: Boolean): Triple<Color, Color, Color> = when (level) {
    0 -> if (isDark) {
        Triple(Color(0xFF25282E), Color(0xFF3E434D), Color(0xFFD5D8E0))
    } else {
        Triple(Color(0xFFF0F2F6), Color(0xFFD5DAE3), Color(0xFF4A5160))
    }
    1 -> if (isDark) {
        Triple(Color(0xFF152942), Color(0xFF2B548C), Color(0xFF6EAEFF))
    } else {
        Triple(Color(0xFFEAF3FF), Color(0xFFBBD7FF), Color(0xFF155FD0))
    }
    2 -> if (isDark) {
        Triple(Color(0xFF36250B), Color(0xFF8F651A), Color(0xFFFFD166))
    } else {
        Triple(Color(0xFFFFF4D9), Color(0xFFEFC163), Color(0xFF824B05))
    }
    else -> Triple(Color(0xFF4A3410), Color(0xFFD4AF37), Color(0xFFFFE28A))
}

@Composable
private fun DropdownChevronGlyph(
    color: Color,
    expanded: Boolean,
    size: Dp,
) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (expanded) {
                moveTo(w * 0.18f, h * 0.68f)
                lineTo(w * 0.5f, h * 0.32f)
                lineTo(w * 0.82f, h * 0.68f)
            } else {
                moveTo(w * 0.18f, h * 0.34f)
                lineTo(w * 0.5f, h * 0.70f)
                lineTo(w * 0.82f, h * 0.34f)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = w * 0.2f, cap = StrokeCap.Round),
        )
    }
}

/**
 * X（Twitter）风格的新发布悬浮胶囊：左侧向上箭头 + 右侧叠放大尺寸发帖者头像（无冗余文字），
 * 位置紧贴当前可见顶栏（或二级筛选栏）正下方，随顶栏收起平滑上移。
 */
@Composable
fun IncomingTopicsBanner(
    count: Int,
    avatars: List<Triple<String?, String?, String?>> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillBg = IosTheme.colors.accent
    val pillContent = Color.White
    val avatarSize = 28.dp
    val avatarStep = 18.dp

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = pillBg.copy(alpha = 0.42f),
                ambientColor = pillBg.copy(alpha = 0.25f),
            )
            .clip(CircleShape)
            .background(pillBg)
            .clickable(role = Role.Button, onClickLabel = "查看 $count 条新发布", onClick = onClick)
            .padding(
                start = 11.dp,
                end = if (avatars.isNotEmpty()) 6.dp else 12.dp,
                top = 5.dp,
                bottom = 5.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 左侧向上箭头
            Canvas(Modifier.size(16.dp)) {
                scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                    val stroke = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawLine(pillContent, Offset(12f, 19.5f), Offset(12f, 4.8f), strokeWidth = 2.6f, cap = StrokeCap.Round)
                    val head = Path().apply {
                        moveTo(5.8f, 11.0f)
                        lineTo(12f, 4.8f)
                        lineTo(18.2f, 11.0f)
                    }
                    drawPath(head, pillContent, style = stroke)
                }
            }

            if (avatars.isNotEmpty()) {
                // 右侧叠放至多 3 位新帖发帖者大头像（带与胶囊同色的描边分隔，纯 X 样式无文字）
                val stackWidth = avatarSize + avatarStep * (avatars.size - 1)
                Box(
                    modifier = Modifier
                        .width(stackWidth)
                        .height(avatarSize),
                ) {
                    avatars.forEachIndexed { idx, (username, primaryUrl, fallbackUrl) ->
                        Box(
                            modifier = Modifier
                                .offset(x = avatarStep * idx)
                                .size(avatarSize)
                                .clip(CircleShape)
                                .background(pillBg)
                                .padding(1.6.dp)
                                .clip(CircleShape),
                        ) {
                            UserAvatar(
                                avatarUrl = primaryUrl,
                                fallbackUrl = fallbackUrl,
                                username = username,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = avatarSize / 2,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "+$count",
                    style = IosTheme.type.footnote.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = pillContent,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun TopicNotificationLevelTag(
    level: TopicNotificationLevel,
    modifier: Modifier = Modifier,
) {
    if (level == TopicNotificationLevel.Regular) return
    val color = notificationLevelColor(level)
    val isDark = IosTheme.colors.isDark
    val chipShape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(chipShape)
            .border(0.75.dp, color.copy(alpha = if (isDark) 0.35f else 0.22f), chipShape)
            .background(color.copy(alpha = if (isDark) 0.16f else 0.08f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.5.dp))
        Text(
            text = level.badgeLabel,
            style = IosTheme.type.footnote.copy(fontSize = 11.5.sp),
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun notificationLevelColor(level: TopicNotificationLevel): Color = when (level) {
    TopicNotificationLevel.Watching -> Color(0xFF007AFF)
    TopicNotificationLevel.Tracking -> Color(0xFF34C759)
    TopicNotificationLevel.Regular -> Color(0xFF8E8E93)
    TopicNotificationLevel.Muted -> Color(0xFFFF3B30)
}

@Composable
internal fun TopicNotificationIcon(
    level: TopicNotificationLevel,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val color = tint ?: notificationLevelColor(level)
    Canvas(modifier.size(18.dp)) {
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            val bell = Path().apply {
                moveTo(5f, 17f)
                lineTo(7f, 14f)
                lineTo(7f, 9f)
                cubicTo(7f, 2.5f, 17f, 2.5f, 17f, 9f)
                lineTo(17f, 14f)
                lineTo(19f, 17f)
                close()
            }
            drawPath(bell, color, style = Stroke(1.7f, cap = StrokeCap.Round))
            drawLine(color, Offset(10f, 20f), Offset(14f, 20f), 1.7f, StrokeCap.Round)
            if (level == TopicNotificationLevel.Muted) {
                drawLine(color, Offset(3f, 3f), Offset(21f, 21f), 1.7f, StrokeCap.Round)
            } else if (level == TopicNotificationLevel.Watching) {
                drawCircle(color, 2f, Offset(20f, 4f))
            }
        }
    }
}

@Composable
private fun TopicFlagDialog(
    topic: Topic,
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (reasonId: Int, message: String?) -> Unit,
) {
    var selectedReason by remember(topic.id) { mutableStateOf(PostFlagReason.Spam) }
    var customMessage by rememberSaveable(topic.id) { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!submitting) onDismiss() }) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(IosTheme.colors.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "举报话题",
                style = IosTheme.type.headline,
                fontWeight = FontWeight.SemiBold,
                color = IosTheme.colors.label,
            )
            Text(
                topic.title,
                style = IosTheme.type.footnote,
                color = IosTheme.colors.secondaryLabel,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PostFlagReason.entries.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) IosTheme.colors.fieldBackground else Color.Transparent)
                            .clickable(enabled = !submitting) { selectedReason = reason }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(
                                    1.5.dp,
                                    if (isSelected) IosTheme.colors.accent else IosTheme.colors.tertiaryLabel,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(IosTheme.colors.accent, CircleShape),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(reason.title, style = IosTheme.type.subheadline, fontWeight = FontWeight.Medium)
                            Text(reason.description, style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                        }
                    }
                }
            }

            if (selectedReason.requiresMessage) {
                BasicTextField(
                    value = customMessage,
                    enabled = !submitting,
                    onValueChange = { customMessage = it },
                    textStyle = IosTheme.type.subheadline.copy(color = IosTheme.colors.label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosTheme.colors.fieldBackground)
                        .padding(8.dp),
                    decorationBox = { innerTextField ->
                        if (customMessage.isEmpty()) {
                            Text("请详细说明需要版主处理的问题...", style = IosTheme.type.subheadline, color = IosTheme.colors.tertiaryLabel)
                        }
                        innerTextField()
                    },
                )
            }
            error?.let { Text(it, style = IosTheme.type.footnote, color = IosTheme.colors.destructive) }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IosTheme.colors.fieldBackground)
                        .clickable(enabled = !submitting, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("取消", style = IosTheme.type.subheadline, color = IosTheme.colors.secondaryLabel)
                }
                val canSubmit = !submitting && (!selectedReason.requiresMessage || customMessage.isNotBlank())
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canSubmit) IosTheme.colors.accent else IosTheme.colors.accent.copy(alpha = 0.4f))
                        .clickable(enabled = canSubmit) {
                            onSubmit(selectedReason.id, customMessage.trim().takeIf { selectedReason.requiresMessage })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (submitting) "提交中…" else "提交",
                        style = IosTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private const val FLOATING_BALL_IDLE_ALPHA = 0.18f

/**
 * 辅助悬浮按钮设计方案（单功能直达 / 多功能伞型自适应展开，支持自由拖拽与边缘吸附记忆）。
 */
@Composable
private fun FloatingAssistiveBall(
    actions: List<FloatingAction>,
    sortOrder: TopicSortOrder,
    sortAscending: Boolean,
    initialXRatio: Float,
    initialYRatio: Float,
    onPositionChange: (Float, Float) -> Unit,
    onActionClick: (FloatingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "expandProgress",
    )

    val isSingle = actions.size == 1

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val mainBallSize = 52.dp
        val mainBallSizePx = with(density) { mainBallSize.toPx() }

        // 自由拖拽安全边界：避开最顶部状态栏（32dp）与底栏（72dp），四周留 6dp 防切边
        val minX = with(density) { 6.dp.toPx() }
        val maxX = (screenWidthPx - mainBallSizePx - with(density) { 6.dp.toPx() }).coerceAtLeast(minX)
        val minY = with(density) { 32.dp.toPx() }
        val maxY = (screenHeightPx - mainBallSizePx - with(density) { 72.dp.toPx() }).coerceAtLeast(minY)

        // 采用真实绝对物理像素坐标：手指拖到哪停在哪，完全消除相对基准点重置与强行吸附造成的乱跳
        var currentXPx by remember {
            mutableFloatStateOf(
                (minX + initialXRatio.coerceIn(0f, 1f) * (maxX - minX)).coerceIn(minX, maxX)
            )
        }
        var currentYPx by remember {
            mutableFloatStateOf(
                (minY + initialYRatio.coerceIn(0f, 1f) * (maxY - minY)).coerceIn(minY, maxY)
            )
        }

        val mainCenter = Offset(currentXPx + mainBallSizePx / 2f, currentYPx + mainBallSizePx / 2f)

        // 四象限智能空间判定：
        // 1. 水平方向：右半区朝左展开，左半区朝右展开
        val isRightSide = mainCenter.x >= screenWidthPx / 2f
        // 2. 垂直方向：上半区（上方不足 142dp+56dp）朝下展开，下半区朝上展开
        val needExpandDownward = mainCenter.y < (with(density) { 142.dp.toPx() } + with(density) { 56.dp.toPx() })

        // 静止淡出透明度：未交互时近乎透明（0.18f），触碰、拖拽或展开时平滑升为 1.0f，绝不遮挡阅读
        val idleAlpha by animateFloatAsState(
            targetValue = if (isExpanded || isDragging) 1f else FLOATING_BALL_IDLE_ALPHA,
            animationSpec = tween(220),
            label = "fabIdleAlpha",
        )

        // 1. 全屏轻柔半透明蒙层（展开时淡出）
        if (expandProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.26f * expandProgress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false },
                    ),
            )
        }

        // 2. 触控涟漪光环（展开时从主球周围扩散出的淡蓝水纹圆环，参照设计图右侧）
        if (expandProgress > 0.01f) {
            val outerRadiusPx = with(density) { 38.dp.toPx() }
            val innerRadiusPx = with(density) { 32.dp.toPx() }

            // 外层水纹圆环
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (mainCenter.x - outerRadiusPx).roundToInt(),
                            (mainCenter.y - outerRadiusPx).roundToInt(),
                        )
                    }
                    .size(76.dp)
                    .border(
                        width = 1.2.dp,
                        color = Color(0xFF2196F3).copy(alpha = 0.22f * expandProgress),
                        shape = CircleShape,
                    ),
            )
            // 内层水纹圆环
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (mainCenter.x - innerRadiusPx).roundToInt(),
                            (mainCenter.y - innerRadiusPx).roundToInt(),
                        )
                    }
                    .size(64.dp)
                    .border(
                        width = 1.4.dp,
                        color = Color(0xFF2196F3).copy(alpha = 0.38f * expandProgress),
                        shape = CircleShape,
                    ),
            )
        }

        // 3. 静止底晕（静止时主球周围有极浅的微蓝晕圈，柔和贴边，不突兀）
        if (expandProgress < 0.05f) {
            val haloRadiusPx = with(density) { 29.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (mainCenter.x - haloRadiusPx).roundToInt(),
                            (mainCenter.y - haloRadiusPx).roundToInt(),
                        )
                    }
                    .size(58.dp)
                    .graphicsLayer { alpha = idleAlpha }
                    .background(Color(0xFF2196F3).copy(alpha = 0.12f), CircleShape),
            )
        }

        // 4. 展开的子按钮群（四象限智能朝向：右上向左下，右下向左上，左上向右下，左下向右上）
        if (!isSingle && expandProgress > 0.01f) {
            val count = actions.size
            val subRadiusDp = when {
                count >= 8 -> 168.dp
                count >= 6 -> 154.dp
                else -> 142.dp
            }
            val subRadiusPx = with(density) { subRadiusDp.toPx() } * expandProgress
            val subBallSize = 42.dp
            val subBallSizePx = with(density) { subBallSize.toPx() }

            val startAngle = if (isRightSide) {
                if (needExpandDownward) 184.0 else 84.0
            } else {
                if (needExpandDownward) 356.0 else 96.0
            }
            val endAngle = if (isRightSide) {
                if (needExpandDownward) 266.0 else 176.0
            } else {
                if (needExpandDownward) 274.0 else 4.0
            }

            actions.forEachIndexed { i, action ->
                val angle = if (count <= 1) 0.0 else startAngle + i.toDouble() * (endAngle - startAngle) / (count - 1).toDouble()
                val rad = Math.toRadians(angle)
                val subX = mainCenter.x + (subRadiusPx * cos(rad)).toFloat()
                val subY = mainCenter.y - (subRadiusPx * sin(rad)).toFloat()

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (subX - subBallSizePx / 2f).roundToInt(),
                                (subY - subBallSizePx / 2f).roundToInt(),
                            )
                        }
                        .graphicsLayer {
                            alpha = expandProgress.coerceIn(0f, 1f)
                            scaleX = 0.35f + 0.65f * expandProgress
                            scaleY = 0.35f + 0.65f * expandProgress
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 子按钮圆球
                    Box(
                        modifier = Modifier
                            .size(subBallSize)
                            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(floatingActionColor(action))
                            .border(1.2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                isExpanded = false
                                onActionClick(action)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        FloatingActionIcon(action = action, tint = Color.White, modifier = Modifier.size(19.dp))
                    }

                    // 精致白底药丸标签（文字小巧上移，避免相互压盖与遮挡）
                    Box(
                        modifier = Modifier
                            .offset(y = 21.dp)
                            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(8.dp), clip = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosTheme.colors.card.copy(alpha = 0.96f))
                            .border(0.5.dp, IosTheme.colors.separator.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 5.5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = action.shortLabel,
                            color = IosTheme.colors.label,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // 5. 主悬浮球（采用绝对像素平移，松手直接驻留目标坐标，支持全屏幕自由拖放）
        val ballRotation = expandProgress * 45f
        val isDark = IosTheme.colors.isDark
        val fabDarkBg = if (isDark) Color(0xFF383A3E) else Color(0xFF242629)

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(currentXPx.roundToInt(), currentYPx.roundToInt())
                }
                .size(mainBallSize)
                .graphicsLayer {
                    alpha = idleAlpha
                }
                .shadow(elevation = if (isExpanded) 5.dp else 2.5.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .then(
                    if (isSingle) {
                        Modifier
                            .background(IosTheme.colors.card.copy(alpha = 0.92f))
                            .border(1.dp, IosTheme.colors.separator, CircleShape)
                    } else if (isExpanded) {
                        Modifier
                            .background(if (isDark) Color(0xFF222426) else Color(0xFF141517))
                            .border(1.2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    } else {
                        Modifier
                            .background(fabDarkBg)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    }
                )
                .pointerInput(isSingle, actions) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        var totalDragDistance = 0f
                        val pointerId = down.id
                        var hasMoved = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                isDragging = false
                                if (!hasMoved) {
                                    if (isSingle) {
                                        onActionClick(actions.first())
                                    } else {
                                        isExpanded = !isExpanded
                                    }
                                } else {
                                    val finalXRatio = if (maxX > minX) ((currentXPx - minX) / (maxX - minX)).coerceIn(0f, 1f) else 0.5f
                                    val finalYRatio = if (maxY > minY) ((currentYPx - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0.5f
                                    onPositionChange(finalXRatio, finalYRatio)
                                }
                                break
                            }

                            val drag = change.positionChange()
                            if (drag != Offset.Zero) {
                                totalDragDistance += drag.getDistance()
                                if (totalDragDistance > 8f) {
                                    hasMoved = true
                                    change.consume()
                                    currentXPx = (currentXPx + drag.x).coerceIn(minX, maxX)
                                    currentYPx = (currentYPx + drag.y).coerceIn(minY, maxY)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isSingle) {
                // 单功能展示：图标 + 下方小字
                val singleAction = actions.first()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    FloatingActionIcon(
                        action = singleAction,
                        tint = if (isDark) Color.White else Color(0xFF181818),
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = singleAction.shortLabel,
                        style = IosTheme.type.caption.copy(fontWeight = FontWeight.Medium, fontSize = 9.sp),
                        color = IosTheme.colors.label,
                    )
                }
            } else {
                // 多功能主球：旋转动画，+ 号平滑过渡为 ×
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = ballRotation
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(20.dp)) {
                        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                            val stroke = 2.4f
                            // 水平横线
                            drawLine(Color.White, Offset(5.5f, 12f), Offset(18.5f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
                            // 垂直竖线
                            drawLine(Color.White, Offset(12f, 5.5f), Offset(12f, 18.5f), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

/** 辅助悬浮按钮各功能的柔和雅致专属配色（低饱和度，对齐系统级设计图）。 */
private fun floatingActionColor(action: FloatingAction): Color = when (action) {
    FloatingAction.CreateTopic -> Color(0xFFF06A55) // 暖珊瑚西柚橙
    FloatingAction.Bookmarks -> Color(0xFFFAAD14)   // 典雅琥珀金
    FloatingAction.Following -> Color(0xFF2979FF)   // 皇家明亮蓝
    FloatingAction.Likes -> Color(0xFFFF4D4F)       // 活力点赞红
    FloatingAction.Replies -> Color(0xFF00BFA5)     // 翠湖薄荷青
    FloatingAction.Sort -> Color(0xFF2196F3)        // 纯净主题天蓝
    FloatingAction.Drafts -> Color(0xFF8C9BB0)      // 雅致灰紫蓝
    FloatingAction.Search -> Color(0xFF68A398)      // 柔和灰海青
    FloatingAction.Categories -> Color(0xFFEAA63B)  // 暖金琥珀
    FloatingAction.History -> Color(0xFF7E57C2)     // 典雅紫罗兰
    FloatingAction.Connect -> Color(0xFF1E88E5)     // LinuxDo 极客蓝
    FloatingAction.Chat -> Color(0xFFE91E63)        // 活力珊瑚粉
    FloatingAction.LdcStore -> Color(0xFFF59E0B)
    FloatingAction.LdcMail -> Color(0xFF007AFF)
}

/** 辅助悬浮按钮各功能的纯矢量精致手绘图标。 */
@Composable
private fun FloatingActionIcon(
    action: FloatingAction,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp),
) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (action) {
                FloatingAction.CreateTopic -> {
                    // 铅笔 ✏️
                    val pen = Path().apply {
                        moveTo(4f, 20f)
                        lineTo(8.5f, 19f)
                        lineTo(19.5f, 8f)
                        lineTo(16f, 4.5f)
                        lineTo(5f, 15.5f)
                        close()
                    }
                    drawPath(pen, tint, style = stroke)
                    drawLine(tint, Offset(13.5f, 7f), Offset(17f, 10.5f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(12.5f, 20f), Offset(20f, 20f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                }
                FloatingAction.Bookmarks -> {
                    // 燕尾书签 🔖
                    val bookmark = Path().apply {
                        moveTo(6.5f, 4.5f)
                        lineTo(17.5f, 4.5f)
                        lineTo(17.5f, 19.5f)
                        lineTo(12f, 15.5f)
                        lineTo(6.5f, 19.5f)
                        close()
                    }
                    drawPath(bookmark, tint, style = stroke)
                    drawLine(tint, Offset(9.5f, 9.5f), Offset(14.5f, 9.5f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                }
                FloatingAction.Following -> {
                    // 关注用户群组人像 👤⁺
                    drawCircle(tint, 3.2f, Offset(9f, 7.5f), style = stroke)
                    val shoulder = Path().apply {
                        moveTo(3.5f, 19.5f)
                        cubicTo(3.5f, 15f, 6.5f, 13.5f, 9f, 13.5f)
                        cubicTo(11.5f, 13.5f, 14.5f, 15f, 14.5f, 19.5f)
                    }
                    drawPath(shoulder, tint, style = stroke)
                    drawLine(tint, Offset(18.5f, 8f), Offset(18.5f, 14f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(15.5f, 11f), Offset(21.5f, 11f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                }
                FloatingAction.Likes -> {
                    // 竖大拇指点赞 👍
                    val thumb = Path().apply {
                        moveTo(4.5f, 12f)
                        lineTo(4.5f, 19.5f)
                        lineTo(7.5f, 19.5f)
                        lineTo(7.5f, 12f)
                        close()
                    }
                    drawPath(thumb, tint, style = stroke)
                    val palm = Path().apply {
                        moveTo(7.5f, 13f)
                        lineTo(10.5f, 11f)
                        lineTo(12.5f, 4.5f)
                        cubicTo(13.2f, 3.5f, 14.5f, 3.5f, 15f, 4.5f)
                        cubicTo(15.2f, 5.5f, 14.2f, 8f, 13.5f, 10.5f)
                        lineTo(17.5f, 10.5f)
                        cubicTo(19f, 10.5f, 19.5f, 11.5f, 19.5f, 12.8f)
                        lineTo(19f, 17f)
                        cubicTo(18.8f, 18.2f, 17.8f, 19.5f, 16.5f, 19.5f)
                        lineTo(7.5f, 19.5f)
                    }
                    drawPath(palm, tint, style = stroke)
                }
                FloatingAction.Replies -> {
                    // 弯曲回折回复 ↩️
                    val reply = Path().apply {
                        moveTo(18.5f, 18f)
                        cubicTo(18.5f, 11.5f, 14.5f, 8.5f, 9.5f, 8.5f)
                    }
                    drawPath(reply, tint, style = stroke)
                    val arrowHead = Path().apply {
                        moveTo(13.5f, 5f)
                        lineTo(9f, 8.5f)
                        lineTo(13.5f, 12f)
                    }
                    drawPath(arrowHead, tint, style = stroke)
                    drawLine(tint, Offset(5f, 19.5f), Offset(19f, 19.5f), strokeWidth = 1.9f, cap = StrokeCap.Round)
                }
                FloatingAction.Sort -> {
                    // 垂直双向箭头 ⇅
                    drawLine(tint, Offset(8f, 18.5f), Offset(8f, 5.5f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                    val leftArrow = Path().apply {
                        moveTo(4.5f, 9.5f)
                        lineTo(8f, 5.5f)
                        lineTo(11.5f, 9.5f)
                    }
                    drawPath(leftArrow, tint, style = stroke)

                    drawLine(tint, Offset(16f, 5.5f), Offset(16f, 18.5f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                    val rightArrow = Path().apply {
                        moveTo(12.5f, 14.5f)
                        lineTo(16f, 18.5f)
                        lineTo(19.5f, 14.5f)
                    }
                    drawPath(rightArrow, tint, style = stroke)
                }
                FloatingAction.Drafts -> {
                    // 文件夹 📁
                    val folder = Path().apply {
                        moveTo(3.5f, 7f)
                        lineTo(8.5f, 7f)
                        lineTo(10.5f, 9.5f)
                        lineTo(20.5f, 9.5f)
                        lineTo(20.5f, 18.5f)
                        lineTo(3.5f, 18.5f)
                        close()
                    }
                    drawPath(folder, tint, style = stroke)
                    drawLine(tint, Offset(3.5f, 10.5f), Offset(20.5f, 10.5f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                }
                FloatingAction.Search -> {
                    // 放大镜 🔍
                    drawCircle(tint, 5.5f, Offset(10.5f, 10.5f), style = stroke)
                    drawLine(tint, Offset(14.8f, 14.8f), Offset(20f, 20f), strokeWidth = 2.3f, cap = StrokeCap.Round)
                }
                FloatingAction.Categories -> {
                    // 分类板块 🗂（4 个圆角小卡片）
                    val r = 1.5f
                    drawRoundRect(tint, Offset(4.5f, 4.5f), Size(6.5f, 6.5f), CornerRadius(r, r), style = stroke)
                    drawRoundRect(tint, Offset(13f, 4.5f), Size(6.5f, 6.5f), CornerRadius(r, r), style = stroke)
                    drawRoundRect(tint, Offset(4.5f, 13f), Size(6.5f, 6.5f), CornerRadius(r, r), style = stroke)
                    drawRoundRect(tint, Offset(13f, 13f), Size(6.5f, 6.5f), CornerRadius(r, r), style = stroke)
                }
                FloatingAction.History -> {
                    // 历史时钟回旋箭头 🕒 ⟲
                    drawArc(
                        color = tint,
                        startAngle = 40f,
                        sweepAngle = 280f,
                        useCenter = false,
                        topLeft = Offset(4f, 4f),
                        size = Size(16f, 16f),
                        style = stroke,
                    )
                    val arrow = Path().apply {
                        moveTo(9.5f, 2.5f)
                        lineTo(4f, 4.5f)
                        lineTo(5.5f, 10f)
                    }
                    drawPath(arrow, tint, style = stroke)
                    drawLine(tint, Offset(12f, 12f), Offset(12f, 7.5f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(12f, 12f), Offset(15.5f, 12f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                }
                FloatingAction.Connect -> {
                    // 极客互联双链 🔗
                    val l1 = Path().apply {
                        moveTo(10f, 13f)
                        lineTo(8f, 15f)
                        cubicTo(6.5f, 16.5f, 6.5f, 19f, 8f, 20.5f)
                        cubicTo(9.5f, 22f, 12f, 22f, 13.5f, 20.5f)
                        lineTo(15.5f, 18.5f)
                    }
                    val l2 = Path().apply {
                        moveTo(14f, 11f)
                        lineTo(16f, 9f)
                        cubicTo(17.5f, 7.5f, 17.5f, 5f, 16f, 3.5f)
                        cubicTo(14.5f, 2f, 12f, 2f, 10.5f, 3.5f)
                        lineTo(8.5f, 5.5f)
                    }
                    drawPath(l1, tint, style = stroke)
                    drawPath(l2, tint, style = stroke)
                    drawLine(tint, Offset(9.5f, 14.5f), Offset(14.5f, 9.5f), strokeWidth = 2.1f, cap = StrokeCap.Round)
                }
                FloatingAction.Chat -> {
                    // 实时聊天对话气泡 💬
                    val bubble = Path().apply {
                        moveTo(4.5f, 6f)
                        cubicTo(4.5f, 4.5f, 5.5f, 4f, 7f, 4f)
                        lineTo(17f, 4f)
                        cubicTo(18.5f, 4f, 19.5f, 4.5f, 19.5f, 6f)
                        lineTo(19.5f, 14f)
                        cubicTo(19.5f, 15.5f, 18.5f, 16f, 17f, 16f)
                        lineTo(9.5f, 16f)
                        lineTo(5.5f, 19.5f)
                        lineTo(5.5f, 16f)
                        lineTo(4.5f, 16f)
                        close()
                    }
                    drawPath(bubble, tint, style = stroke)
                    drawCircle(tint, 1.1f, Offset(8.5f, 10f))
                    drawCircle(tint, 1.1f, Offset(12f, 10f))
                    drawCircle(tint, 1.1f, Offset(15.5f, 10f))
                }
                FloatingAction.LdcStore -> {
                    val bag = Path().apply {
                        moveTo(5f, 8f); lineTo(19f, 8f); lineTo(20.5f, 20f); lineTo(3.5f, 20f); close()
                    }
                    drawPath(bag, tint, style = stroke)
                    drawArc(tint, 180f, 180f, false, Offset(8f, 3f), Size(8f, 10f), style = stroke)
                }
                FloatingAction.LdcMail -> {
                    drawRoundRect(tint, Offset(3.5f, 5.5f), Size(17f, 13f), CornerRadius(2f, 2f), style = stroke)
                    val flap = Path().apply {
                        moveTo(4.5f, 7f); lineTo(12f, 13f); lineTo(19.5f, 7f)
                    }
                    drawPath(flap, tint, style = stroke)
                }
            }
        }
    }
}


