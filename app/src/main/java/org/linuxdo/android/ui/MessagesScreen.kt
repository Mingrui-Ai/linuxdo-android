package org.linuxdo.android.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import org.linuxdo.android.data.ChatChannelSummary
import org.linuxdo.android.data.NotificationFilter
import org.linuxdo.android.ui.design.*
import org.linuxdo.android.ui.screen.ChatList
import org.linuxdo.android.ui.screen.EmptyNotice
import org.linuxdo.android.ui.screen.ErrorNotice

internal enum class MessageSection(val label: String) { Notifications("通知"), Chat("聊天") }

@Composable
internal fun MessagesScreen(
    feedDisplayMode: String,
    section: MessageSection,
    onSection: (MessageSection) -> Unit,
    directMessages: Boolean,
    onDirectMessages: (Boolean) -> Unit,
    notifications: ProfileUiState,
    notificationPages: Map<NotificationFilter, ProfileUiState>,
    chat: ChatUiState,
    filter: NotificationFilter,
    unreadFilters: Set<NotificationFilter>,
    unreadNotifications: Int,
    active: Boolean,
    scrollToTopRequest: Int,
    onFilter: (NotificationFilter) -> Unit,
    onRefreshNotifications: () -> Unit,
    onMore: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkAllChatRead: () -> Unit,
    onRead: (Long) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: (String) -> Unit,
    onRefreshChat: () -> Unit,
    onOpenChannel: (ChatChannelSummary) -> Unit,
) {
    var confirmRead by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable(section) { mutableStateOf(false) }
    var notificationQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(showSearch) { if (!showSearch) notificationQuery = "" }
    val headerState = rememberCollapsingHeaderState()
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) headerState.expand()
    }
    val chatOpen = section == MessageSection.Chat
    val unreadChannels = remember(chat.channels) { chat.channels.filter { it.unreadCount > 0 && it.membership?.muted != true } }
    LaunchedEffect(active, section) {
        if (active) {
            if (chatOpen) onRefreshChat() else onRefreshNotifications()
        }
    }
    val filters = NotificationFilter.entries
    MessagePages(
        labels = remember { MessageSection.entries.map { it.label } },
        selected = section.ordinal,
        onSelect = { onSection(MessageSection.entries[it]) },
        active = active,
        swipeContent = false,
        collapsingHeader = headerState,
        unread = { if (it == MessageSection.Chat.ordinal) unreadChannels.isNotEmpty() else unreadNotifications > 0 },
        actionWidth = 76.dp,
        actions = {
            Row {
                IconButton(onClick = { confirmRead = true },
                    modifier = Modifier.width(38.dp).semantics {
                        contentDescription = if (chatOpen) "将聊天全部标为已读" else "将通知全部标为已读"
                    }) {
                    BroomGlyph(IosTheme.colors.label)
                }
                IconButton(onClick = { showSearch = !showSearch }, modifier = Modifier.width(38.dp)) {
                    Icon(Icons.Default.Search,
                        if (showSearch) "关闭搜索" else if (chatOpen) "搜索聊天" else "搜索通知",
                        tint = if (showSearch) IosTheme.colors.accent else IosTheme.colors.label)
                }
            }
        },
    ) { group ->
        if (group == MessageSection.Chat.ordinal) {
            MessagePages(
                labels = listOf("频道", "直接消息"),
                selected = if (directMessages) 1 else 0,
                onSelect = { onDirectMessages(it == 1) },
                active = active && chatOpen,
                compactHeader = true,
                unread = { index -> unreadChannels.any { it.isDirectMessage == (index == 1) } },
            ) { page ->
                val direct = page == 1
                ChatList(chat, direct, showSearch, feedDisplayMode, if (chatOpen && direct == directMessages) scrollToTopRequest else 0,
                    onRefreshChat, onOpenChannel, active = active && chatOpen && direct == directMessages)
            }
        } else {
            MessagePages(
                labels = remember { filters.map { it.label } },
                selected = filter.ordinal,
                onSelect = { onFilter(filters[it]) },
                active = active && !chatOpen,
                compactHeader = true,
                unread = { index ->
                    val tab = filters[index]
                    tab != NotificationFilter.All && (tab in unreadFilters ||
                        notifications.content.entries.any { it.unread && tab.includes(it.notificationType) })
                },
            ) { page ->
                val pageFilter = filters[page]
                val current = pageFilter == filter
                val cached = notificationPages[pageFilter]
                val all = notificationPages[NotificationFilter.All]
                val pageState = if (current) notifications else remember(pageFilter, cached, all) {
                    when {
                        cached != null && (all == null || cached.loadedAt >= all.loadedAt) -> cached
                        all != null -> all.copy(content = all.content.copy(
                            entries = all.content.entries.filter { pageFilter.includes(it.notificationType) }))
                        else -> ProfileUiState(loading = true)
                    }
                }
                NotificationList(pageState, feedDisplayMode, active && !chatOpen && current,
                    if (!chatOpen && current) scrollToTopRequest else 0,
                    onRefreshNotifications, onMore, onRead, onOpenTopic, onOpenUser,
                    showSearch, notificationQuery, { notificationQuery = it })
            }
        }
    }
    if (confirmRead) {
        if (chatOpen) AlertDialog(
            onDismissRequest = { confirmRead = false },
            title = { Text("标记为已读") },
            text = { Text("将所有频道和直接消息中的当前未读消息标记为已读？") },
            confirmButton = { TextButton(onClick = { confirmRead = false; onMarkAllChatRead() }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { confirmRead = false }) { Text("取消") } },
            containerColor = IosTheme.colors.card,
        ) else ConfirmReadNotifications(onDismiss = { confirmRead = false }, onConfirm = onMarkAllRead)
    }
}

/** Both header and list drive the same pager. Publish the new filter only after settling. */
@Composable
private fun MessagePages(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    active: Boolean,
    swipeContent: Boolean = true,
    compactHeader: Boolean = false,
    collapsingHeader: CollapsingHeaderState? = null,
    unread: (Int) -> Boolean,
    actionWidth: Dp = 0.dp,
    actions: @Composable () -> Unit = {},
    content: @Composable (Int) -> Unit,
) {
    val pager = rememberPagerState(initialPage = selected) { labels.size }
    val scope = rememberCoroutineScope()
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestSelected by rememberUpdatedState(selected)
    var animation by remember { mutableStateOf<Job?>(null) }
    var anchor by remember { mutableIntStateOf(selected) }
    val boundary = rememberTabBoundaryFeedback()

    // 外部入口（如系统通知）可以更改当前板块，恢复时以入口指定的页为准。
    LaunchedEffect(selected) {
        animation?.cancel()
        if (pager.currentPage != selected || pager.currentPageOffsetFraction != 0f) {
            pager.scrollToPage(selected)
        }
    }

    fun settle(target: Int, tapped: Boolean = false) {
        animation?.cancel()
        animation = scope.launch {
            val distance = abs(target - pager.currentPage - pager.currentPageOffsetFraction)
            val duration = (220 * distance).toInt().coerceIn(85, 220)
            pager.animateScrollToPage(target, animationSpec = tween(duration, easing = FastOutSlowInEasing))
            if (target != latestSelected || tapped) latestOnSelect(target)
        }
    }

    val gesture = Modifier.tabSwipeGesture(
        enabled = active,
        onStart = {
            animation?.cancel()
            anchor = pager.currentPage
        },
        onDrag = { delta ->
            val width = pager.layoutInfo.pageSize
            if (width > 0) {
                val position = pager.currentPage + pager.currentPageOffsetFraction
                val target = (position - delta / width).coerceIn(
                    (anchor - 1).coerceAtLeast(0).toFloat(),
                    (anchor + 1).coerceAtMost(labels.lastIndex).toFloat(),
                )
                pager.dispatchRawDelta((target - position) * width)
            }
        },
        onEnd = { step ->
            val target = anchor + step
            if (target !in labels.indices) boundary()
            settle(target.coerceIn(labels.indices))
        },
    )
    val header: @Composable () -> Unit = {
        SectionHeader(
            labels = labels,
            selected = selected,
            onSelect = { settle(it, tapped = true) },
            modifier = if (swipeContent) Modifier else gesture,
            indicatorPosition = { pager.currentPage + pager.currentPageOffsetFraction },
            unread = unread,
            compact = compactHeader,
            actionWidth = actionWidth,
            actions = actions,
        )
    }
    val pages: @Composable () -> Unit = {
        // 内容横滑由上面的单一手势驱动，嵌套分类到边界时不会连带切换父板块。
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false, verticalAlignment = Alignment.Top) { page -> content(page) }
    }
    val modifier = Modifier.fillMaxSize().background(IosTheme.colors.groupedBackground)
        .then(if (swipeContent) gesture else Modifier)
    if (collapsingHeader != null) {
        CollapsingHeaderLayout(state = collapsingHeader, modifier = modifier, header = header, content = pages)
    } else {
        // 二级分类始终可见，不再随上下导航栏的布尔显隐动画一起消失。
        Column(modifier) {
            header()
            Box(Modifier.fillMaxWidth().weight(1f)) { pages() }
        }
    }
}

@Composable
private fun NotificationList(
    state: ProfileUiState,
    feedDisplayMode: String,
    active: Boolean,
    scrollToTopRequest: Int,
    onRefresh: () -> Unit,
    onMore: () -> Unit,
    onRead: (Long) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: (String) -> Unit,
    showSearch: Boolean,
    query: String,
    onQuery: (String) -> Unit,
) {
    val list = rememberLazyListState()
    var refreshRequested by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading) { if (!state.loading) refreshRequested = false }
    ScrollToTopOnRequest(list, scrollToTopRequest)
    val searchQuery = if (showSearch) query.trim() else ""
    val entries = remember(state.content.entries, searchQuery) {
        state.content.entries.filter { entry ->
            searchQuery.isEmpty() || entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.actor?.contains(searchQuery, ignoreCase = true) == true ||
                entry.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }
    LaunchedEffect(searchQuery) { list.scrollToItem(0) }
    val nearEnd by remember { derivedStateOf {
        val layout = list.layoutInfo
        layout.visibleItemsInfo.any { it.index >= layout.totalItemsCount - 3 }
    } }
    val canLoadMore = state.loaded && !state.loading && state.error == null && state.content.hasMore
    LaunchedEffect(active, nearEnd, canLoadMore, entries.size, searchQuery) {
        if (active && searchQuery.isEmpty() && nearEnd && canLoadMore && entries.size >= 20) onMore()
    }
    Column(Modifier.fillMaxSize()) {
        if (showSearch) MessageSearchField(query, onQuery, "搜索已加载的通知…")
        MessageListRefresh(state.loading && refreshRequested, active,
            { if (!state.loading) { refreshRequested = true; onRefresh() } }, Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize().scrollListBars(list, active), contentPadding = PaddingValues(bottom = 72.dp)) {
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    NotificationRow(entry, onOpenTopic, onRead, onOpenUser)
                    if (index < entries.lastIndex) FeedItemDivider(feedDisplayMode)
                }
                state.error?.let { message -> item { ErrorNotice(message, onRefresh) } }
                if (state.loading && !refreshRequested) item { IosLoadingBox() }
                if (canLoadMore && (entries.size < 20 || searchQuery.isNotEmpty())) item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onMore) { Text("加载更早的记录") }
                    }
                }
                if (state.loaded && entries.isEmpty() && !state.loading && state.error == null) {
                    item {
                        if (searchQuery.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("已加载的通知中没有匹配项", color = IosTheme.colors.secondaryLabel)
                        } else if (!state.content.hasMore) EmptyNotice()
                    }
                }
            }
        }
    }
}
