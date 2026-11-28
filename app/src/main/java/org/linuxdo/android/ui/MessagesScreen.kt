package org.linuxdo.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.linuxdo.android.data.ChatChannelSummary
import org.linuxdo.android.data.EditorSuggestion
import org.linuxdo.android.data.NotificationFilter
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.design.FullscreenDialog
import org.linuxdo.android.ui.design.HeaderActionPill
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.LocalListBars
import org.linuxdo.android.ui.design.MessageListRefresh
import org.linuxdo.android.ui.design.MessageSearchField
import org.linuxdo.android.ui.design.ScrollToTopOnRequest
import org.linuxdo.android.ui.design.UserAvatar
import org.linuxdo.android.ui.design.rememberTabBoundaryFeedback
import org.linuxdo.android.ui.design.scrollListBars
import org.linuxdo.android.ui.design.tabSwipeGesture
import org.linuxdo.android.ui.screen.ChatList
import org.linuxdo.android.ui.screen.DirectMessageFilter
import org.linuxdo.android.ui.screen.ChannelListFilter
import org.linuxdo.android.ui.screen.EmptyNotice
import org.linuxdo.android.ui.screen.ErrorNotice
import kotlin.math.abs

/** 消息板块三个平级主入口：通知置最左，右侧依次为直接消息与频道。 */
internal enum class MessageSection(val label: String) {
    Notifications("通知"),
    DirectMessages("直接消息"),
    Channels("频道"),
}

@Composable
internal fun MessagesScreen(
    feedDisplayMode: String,
    section: MessageSection,
    onSection: (MessageSection) -> Unit,
    leading: @Composable () -> Unit,
    showDirectMessageQuickBar: Boolean,
    currentUsername: String?,
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
    onStartDirectMessage: (String) -> Unit,
    onSearchUsers: suspend (String) -> List<EditorSuggestion>,
) {
    var confirmRead by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable(section) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(section) { mutableStateOf("") }
    var dmFilter by rememberSaveable { mutableStateOf(DirectMessageFilter.All) }
    var channelFilter by rememberSaveable { mutableStateOf(ChannelListFilter.All) }
    var showNewDmSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showSearch) {
        if (!showSearch) searchQuery = ""
    }

    val dmChannels = remember(chat.channels) { chat.channels.filter { it.isDirectMessage } }
    val publicChannels = remember(chat.channels) { chat.channels.filter { !it.isDirectMessage } }
    val unreadDmTotal = remember(dmChannels) {
        dmChannels.filter { it.membership?.muted != true }.sumOf { it.unreadCount }
    }
    val unreadDmConversationCount = remember(dmChannels) {
        dmChannels.count { it.membership?.muted != true && it.unreadCount > 0 }
    }
    val dmGroupCount = remember(dmChannels) {
        dmChannels.count { !it.isOneOnOneDm && it.dmUsers.size > 1 }
    }
    val hasUnreadPublicChannel = remember(publicChannels) {
        publicChannels.any { it.membership?.muted != true && it.unreadCount > 0 }
    }
    val unreadChannelCount = remember(publicChannels) {
        publicChannels.count { it.membership?.muted != true && it.unreadCount > 0 }
    }
    val mutedChannelCount = remember(publicChannels) {
        publicChannels.count { it.membership?.muted == true }
    }

    val isChatSection = section != MessageSection.Notifications
    LaunchedEffect(active, section) {
        if (active) {
            if (isChatSection) onRefreshChat() else onRefreshNotifications()
        }
    }

    val pager = rememberPagerState(initialPage = section.ordinal) { MessageSection.entries.size }
    val scope = rememberCoroutineScope()
    val latestOnSection by rememberUpdatedState(onSection)
    val latestSection by rememberUpdatedState(section)
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var swipeAnchor by remember { mutableIntStateOf(section.ordinal) }
    val boundaryFeedback = rememberTabBoundaryFeedback()

    LaunchedEffect(section) {
        settleJob?.cancel()
        if (pager.currentPage != section.ordinal || pager.currentPageOffsetFraction != 0f) {
            pager.scrollToPage(section.ordinal)
        }
    }

    fun settleTo(targetIndex: Int, tapped: Boolean = false) {
        settleJob?.cancel()
        settleJob = scope.launch {
            val distance = abs(targetIndex - pager.currentPage - pager.currentPageOffsetFraction)
            val duration = (220 * distance).toInt().coerceIn(85, 220)
            pager.animateScrollToPage(targetIndex, animationSpec = tween(duration, easing = FastOutSlowInEasing))
            val targetSection = MessageSection.entries[targetIndex]
            if (targetSection != latestSection || tapped) {
                latestOnSection(targetSection)
            }
        }
    }

    val swipeModifier = Modifier.tabSwipeGesture(
        enabled = active,
        onStart = {
            settleJob?.cancel()
            swipeAnchor = pager.currentPage
        },
        onDrag = { delta ->
            val width = pager.layoutInfo.pageSize
            if (width > 0) {
                val position = pager.currentPage + pager.currentPageOffsetFraction
                val target = (position - delta / width).coerceIn(
                    (swipeAnchor - 1).coerceAtLeast(0).toFloat(),
                    (swipeAnchor + 1).coerceAtMost(MessageSection.entries.lastIndex).toFloat(),
                )
                pager.dispatchRawDelta((target - position) * width)
            }
        },
        onEnd = { step ->
            val target = swipeAnchor + step
            if (target !in MessageSection.entries.indices) boundaryFeedback()
            settleTo(target.coerceIn(MessageSection.entries.indices))
        },
    )

    val topBarVisible = LocalListBars.current?.value != false || showSearch
    val filters = NotificationFilter.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosTheme.colors.groupedBackground)
            .then(swipeModifier),
    ) {
        // 统一两层顶栏（与首页 HomeTopFilterHeader 视觉同源）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(IosTheme.colors.groupedBackground),
        ) {
            // 第一行：严格复用首页 HomeTopFilterHeader 第一行尺寸与边距（48dp 高，start=10dp, end=6dp，右端搜索 44dp），
            // 保证从首页切到消息页时左上角头像与右上角搜索图标像素级零位移。
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
                        modifier = Modifier
                            .weight(1f)
                            .selectableGroup()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MessageSection.entries.forEachIndexed { index, item ->
                            val isSelected = section == item
                            val badgeCount = when (item) {
                                MessageSection.Notifications -> unreadNotifications
                                MessageSection.DirectMessages -> unreadDmTotal
                                MessageSection.Channels -> 0
                            }
                            val showDot = item == MessageSection.Channels && hasUnreadPublicChannel
                            MessageTopTabItem(
                                label = item.label,
                                selected = isSelected,
                                badgeCount = badgeCount,
                                showDot = showDot,
                                onClick = { settleTo(index, tapped = true) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { confirmRead = true },
                            modifier = Modifier
                                .size(38.dp)
                                .semantics {
                                    contentDescription = if (isChatSection) "将聊天全部标为已读" else "将通知全部标为已读"
                                },
                        ) {
                            BroomGlyph(IosTheme.colors.label)
                        }
                        IconButton(
                            onClick = { showSearch = !showSearch },
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = if (showSearch) "关闭搜索" else "搜索消息",
                                tint = if (showSearch) IosTheme.colors.accent else IosTheme.colors.label,
                            )
                        }
                    }
                }
            }

            // 可选行内搜索栏（统一胶囊样式）
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically(tween(180), expandFrom = Alignment.Top) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(160), shrinkTowards = Alignment.Top) + fadeOut(tween(130)),
            ) {
                val placeholder = when (section) {
                    MessageSection.Notifications -> "搜索已加载的通知…"
                    MessageSection.DirectMessages -> "搜索直接消息、用户名…"
                    MessageSection.Channels -> "搜索公共频道…"
                }
                MessageSearchField(
                    query = searchQuery,
                    onQuery = { searchQuery = it },
                    placeholder = placeholder,
                )
            }

            // 第二行：严格复用首页筛选栏内边距（horizontal=12dp, vertical=6dp）与 32dp 胶囊高度，保持两级栏总高度一致
            val subBarActive = when (section) {
                MessageSection.Notifications -> filter != NotificationFilter.All
                MessageSection.DirectMessages -> dmFilter != DirectMessageFilter.All
                MessageSection.Channels -> channelFilter != ChannelListFilter.All
            }
            AnimatedVisibility(
                visible = topBarVisible || subBarActive,
                enter = expandVertically(tween(200), expandFrom = Alignment.Top) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(200), shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
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
                                .horizontalScroll(rememberScrollState())
                                .selectableGroup(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            when (section) {
                                MessageSection.Notifications -> {
                                    filters.forEach { tab ->
                                        val tabUnread = tab != NotificationFilter.All && (
                                            tab in unreadFilters ||
                                                notifications.content.entries.any { it.unread && tab.includes(it.notificationType) }
                                            )
                                        MessageFilterPill(
                                            label = tab.label,
                                            selected = filter == tab,
                                            showDot = tabUnread,
                                            onClick = { onFilter(tab) },
                                        )
                                    }
                                }

                                MessageSection.DirectMessages -> {
                                    MessageFilterPill(
                                        label = "全部",
                                        selected = dmFilter == DirectMessageFilter.All,
                                        count = dmChannels.size.takeIf { it > 0 },
                                        onClick = { dmFilter = DirectMessageFilter.All },
                                    )
                                    MessageFilterPill(
                                        label = "未读",
                                        selected = dmFilter == DirectMessageFilter.Unread,
                                        count = unreadDmConversationCount.takeIf { it > 0 },
                                        showDot = unreadDmConversationCount > 0 && dmFilter != DirectMessageFilter.Unread,
                                        onClick = { dmFilter = DirectMessageFilter.Unread },
                                    )
                                    MessageFilterPill(
                                        label = "群聊",
                                        selected = dmFilter == DirectMessageFilter.Group,
                                        count = dmGroupCount.takeIf { it > 0 },
                                        onClick = { dmFilter = DirectMessageFilter.Group },
                                    )
                                }

                                MessageSection.Channels -> {
                                    MessageFilterPill(
                                        label = "全部频道",
                                        selected = channelFilter == ChannelListFilter.All,
                                        count = publicChannels.size.takeIf { it > 0 },
                                        onClick = { channelFilter = ChannelListFilter.All },
                                    )
                                    MessageFilterPill(
                                        label = "有新消息",
                                        selected = channelFilter == ChannelListFilter.Unread,
                                        count = unreadChannelCount.takeIf { it > 0 },
                                        showDot = unreadChannelCount > 0 && channelFilter != ChannelListFilter.Unread,
                                        onClick = { channelFilter = ChannelListFilter.Unread },
                                    )
                                    MessageFilterPill(
                                        label = "免打扰",
                                        selected = channelFilter == ChannelListFilter.Muted,
                                        count = mutedChannelCount.takeIf { it > 0 },
                                        onClick = { channelFilter = ChannelListFilter.Muted },
                                    )
                                }
                            }
                        }

                        if (section == MessageSection.DirectMessages) {
                            HeaderActionPill(
                                text = "+ 新对话",
                                emphasized = true,
                                onClick = { showNewDmSheet = true },
                            )
                        }
                    }
                    IosFullDivider()
                }
            }
        }

        // 主体内容分页（通知 / 直接消息 / 频道）
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top,
        ) { page ->
            when (MessageSection.entries[page]) {
                MessageSection.Notifications -> {
                    val cached = notificationPages[filter]
                    val all = notificationPages[NotificationFilter.All]
                    val pageState = remember(filter, notifications, cached, all) {
                        when {
                            notifications.loaded || notifications.loading -> notifications
                            cached != null && (all == null || cached.loadedAt >= all.loadedAt) -> cached
                            all != null -> all.copy(
                                content = all.content.copy(
                                    entries = all.content.entries.filter { filter.includes(it.notificationType) },
                                ),
                            )
                            else -> ProfileUiState(loading = true)
                        }
                    }
                    NotificationList(
                        state = pageState,
                        active = active && section == MessageSection.Notifications,
                        scrollToTopRequest = if (section == MessageSection.Notifications) scrollToTopRequest else 0,
                        onRefresh = onRefreshNotifications,
                        onMore = onMore,
                        onRead = onRead,
                        onOpenTopic = onOpenTopic,
                        onOpenUser = onOpenUser,
                        searchQuery = if (showSearch && section == MessageSection.Notifications) searchQuery else "",
                    )
                }

                MessageSection.DirectMessages -> {
                    ChatList(
                        state = chat,
                        directMessages = true,
                        dmFilter = dmFilter,
                        channelFilter = ChannelListFilter.All,
                        searchQuery = if (showSearch && section == MessageSection.DirectMessages) searchQuery else "",
                        showQuickContactsBar = showDirectMessageQuickBar,
                        currentUsername = currentUsername,
                        scrollToTopRequest = if (section == MessageSection.DirectMessages) scrollToTopRequest else 0,
                        onRefresh = onRefreshChat,
                        onOpenChannel = onOpenChannel,
                        onNewDirectMessage = { showNewDmSheet = true },
                        active = active && section == MessageSection.DirectMessages,
                    )
                }

                MessageSection.Channels -> {
                    ChatList(
                        state = chat,
                        directMessages = false,
                        dmFilter = DirectMessageFilter.All,
                        channelFilter = channelFilter,
                        searchQuery = if (showSearch && section == MessageSection.Channels) searchQuery else "",
                        showQuickContactsBar = false,
                        currentUsername = currentUsername,
                        scrollToTopRequest = if (section == MessageSection.Channels) scrollToTopRequest else 0,
                        onRefresh = onRefreshChat,
                        onOpenChannel = onOpenChannel,
                        onNewDirectMessage = {},
                        active = active && section == MessageSection.Channels,
                    )
                }
            }
        }
    }

    if (confirmRead) {
        if (isChatSection) {
            AlertDialog(
                onDismissRequest = { confirmRead = false },
                title = { Text("标记为已读") },
                text = { Text("将所有频道和直接消息中的当前未读消息标记为已读？") },
                confirmButton = {
                    TextButton(onClick = { confirmRead = false; onMarkAllChatRead() }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmRead = false }) { Text("取消") }
                },
                containerColor = IosTheme.colors.card,
            )
        } else {
            ConfirmReadNotifications(onDismiss = { confirmRead = false }, onConfirm = onMarkAllRead)
        }
    }

    if (showNewDmSheet) {
        NewDirectMessageSheet(
            recentChannels = dmChannels,
            currentUsername = currentUsername,
            onSearchUsers = onSearchUsers,
            onSelectChannel = { channel ->
                showNewDmSheet = false
                onOpenChannel(channel)
            },
            onSelectUsername = { username ->
                showNewDmSheet = false
                onStartDirectMessage(username)
            },
            onDismiss = { showNewDmSheet = false },
        )
    }
}

@Composable
private fun MessageTopTabItem(
    label: String,
    selected: Boolean,
    badgeCount: Int,
    showDot: Boolean,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    Column(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .then(
                if (badgeCount > 0 || showDot) {
                    Modifier.semantics { stateDescription = "有未读消息" }
                } else Modifier,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = if (selected) 17.sp else 14.5.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) colors.label else colors.secondaryLabel,
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.destructive)
                        .padding(horizontal = 4.5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp,
                        maxLines = 1,
                    )
                }
            } else if (showDot) {
                Spacer(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.destructive),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (selected) colors.accent else Color.Transparent),
        )
    }
}

@Composable
private fun MessageFilterPill(
    label: String,
    selected: Boolean,
    count: Int? = null,
    showDot: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    val isDark = colors.isDark
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (selected) {
        colors.accent.copy(alpha = if (isDark) 0.19f else 0.11f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF767680).copy(alpha = 0.11f)
    }
    val borderColor = if (selected) {
        colors.accent.copy(alpha = if (isDark) 0.44f else 0.30f)
    } else {
        Color.Transparent
    }
    val textColor = if (selected) colors.accent else colors.label
    val countColor = if (selected) colors.accent.copy(alpha = 0.88f) else colors.secondaryLabel

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(0.9.dp, borderColor, shape) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            style = IosTheme.type.footnote.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = textColor,
            maxLines = 1,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = IosTheme.type.caption.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFeatureSettings = "tnum",
                ),
                color = countColor,
                maxLines = 1,
            )
        }
        if (showDot) {
            Spacer(
                modifier = Modifier
                    .size(5.5.dp)
                    .clip(CircleShape)
                    .background(colors.destructive),
            )
        }
    }
}

@Composable
private fun NotificationList(
    state: ProfileUiState,
    active: Boolean,
    scrollToTopRequest: Int,
    onRefresh: () -> Unit,
    onMore: () -> Unit,
    onRead: (Long) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: (String) -> Unit,
    searchQuery: String,
) {
    val list = rememberLazyListState()
    var refreshRequested by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading) { if (!state.loading) refreshRequested = false }
    ScrollToTopOnRequest(list, scrollToTopRequest)
    val cleanQuery = searchQuery.trim()
    val entries = remember(state.content.entries, cleanQuery) {
        state.content.entries.filter { entry ->
            cleanQuery.isEmpty() ||
                entry.title.contains(cleanQuery, ignoreCase = true) ||
                entry.actor?.contains(cleanQuery, ignoreCase = true) == true ||
                entry.subtitle.contains(cleanQuery, ignoreCase = true)
        }
    }
    LaunchedEffect(cleanQuery) { list.scrollToItem(0) }
    val nearEnd by remember {
        derivedStateOf {
            val layout = list.layoutInfo
            layout.visibleItemsInfo.any { it.index >= layout.totalItemsCount - 3 }
        }
    }
    val canLoadMore = state.loaded && !state.loading && state.error == null && state.content.hasMore
    LaunchedEffect(active, nearEnd, canLoadMore, entries.size, cleanQuery) {
        if (active && cleanQuery.isEmpty() && nearEnd && canLoadMore && entries.size >= 20) onMore()
    }

    MessageListRefresh(
        refreshing = state.loading && refreshRequested,
        active = active,
        onRefresh = {
            if (!state.loading) {
                refreshRequested = true
                onRefresh()
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .scrollListBars(list, active),
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                NotificationRow(entry, onOpenTopic, onRead, onOpenUser)
                if (index < entries.lastIndex) {
                    Box(Modifier.fillMaxWidth().background(IosTheme.colors.card)) {
                        IosDivider(startIndent = 65.dp)
                    }
                }
            }
            state.error?.let { message -> item { ErrorNotice(message, onRefresh) } }
            if (state.loading && !refreshRequested) item { IosLoadingBox() }
            if (canLoadMore && (entries.size < 20 || cleanQuery.isNotEmpty())) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onMore) { Text("加载更早的记录") }
                    }
                }
            }
            if (state.loaded && entries.isEmpty() && !state.loading && state.error == null) {
                item {
                    if (cleanQuery.isNotEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("已加载的通知中没有匹配项", color = IosTheme.colors.secondaryLabel)
                        }
                    } else if (!state.content.hasMore) {
                        EmptyNotice()
                    }
                }
            }
        }
    }
}

@Composable
private fun NewDirectMessageSheet(
    recentChannels: List<ChatChannelSummary>,
    currentUsername: String?,
    onSearchUsers: suspend (String) -> List<EditorSuggestion>,
    onSelectChannel: (ChatChannelSummary) -> Unit,
    onSelectUsername: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IosTheme.colors
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<EditorSuggestion>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val cleanQuery = query.trim().removePrefix("@")
    LaunchedEffect(cleanQuery) {
        searchError = null
        if (cleanQuery.isEmpty()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(220)
        searching = true
        try {
            results = onSearchUsers(cleanQuery)
                .filterNot { suggestion ->
                    currentUsername != null &&
                        suggestion.insertion.removePrefix("@").equals(currentUsername, ignoreCase = true)
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            searchError = org.linuxdo.android.net.UserErrorMessage.format(e, "搜索用户失败")
            results = emptyList()
        } finally {
            searching = false
        }
    }

    val recentOneOnOne = remember(recentChannels) {
        recentChannels.filter { it.isOneOnOneDm }.take(6)
    }

    FullscreenDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (colors.isDark) 0.56f else 0.36f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(colors.card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(colors.separator),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "发起直接消息",
                        style = IosTheme.type.headline.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = colors.label,
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.fieldBackground)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = colors.secondaryLabel,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 搜索输入框
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.fieldBackground)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.secondaryLabel,
                        modifier = Modifier.size(16.dp),
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = IosTheme.type.body.copy(fontSize = 14.sp, color = colors.label),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { field ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text("输入用户名搜索佬友…", fontSize = 14.sp, color = colors.tertiaryLabel)
                                }
                                field()
                            }
                        },
                    )
                    if (searching) {
                        IosActivityIndicator(diameter = 14.dp, color = colors.accent)
                    } else if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除",
                            tint = colors.secondaryLabel,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { query = "" },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (cleanQuery.isNotEmpty()) {
                    // 支持直接按完整用户名发起（即使搜索接口限流或尚未返回）
                    val validDirectHandle = cleanQuery.matches(Regex("[A-Za-z0-9_.-]{2,32}"))
                    if (results.isEmpty() && !searching) {
                        if (searchError != null) {
                            Text(
                                text = searchError!!,
                                style = IosTheme.type.footnote,
                                color = colors.secondaryLabel,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        if (validDirectHandle) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectUsername(cleanQuery) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "发起与 @$cleanQuery 的对话",
                                            style = IosTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                            color = colors.label,
                                        )
                                        Text(
                                            text = "直接创建或打开 1v1 直接消息",
                                            style = IosTheme.type.caption,
                                            color = colors.secondaryLabel,
                                        )
                                    }
                                }
                                HeaderActionPill(
                                    text = "发消息",
                                    emphasized = true,
                                    onClick = { onSelectUsername(cleanQuery) },
                                )
                            }
                        } else if (searchError == null) {
                            Text(
                                text = "未找到匹配的用户",
                                style = IosTheme.type.footnote,
                                color = colors.secondaryLabel,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 16.dp),
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                        ) {
                            itemsIndexed(results, key = { _, item -> item.insertion }) { index, item ->
                                val username = item.insertion.removePrefix("@")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectUsername(username) }
                                        .padding(vertical = 9.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        UserAvatar(
                                            avatarUrl = item.avatarUrl,
                                            username = username,
                                            modifier = Modifier.size(36.dp),
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = item.label.ifBlank { username },
                                                style = IosTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                                color = colors.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "@$username",
                                                style = IosTheme.type.caption,
                                                color = colors.secondaryLabel,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    HeaderActionPill(
                                        text = "发消息",
                                        emphasized = true,
                                        onClick = { onSelectUsername(username) },
                                    )
                                }
                                if (index < results.lastIndex) {
                                    IosDivider(startIndent = 48.dp)
                                }
                            }
                        }
                    }
                } else if (recentOneOnOne.isNotEmpty()) {
                    Text(
                        text = "最近联系人",
                        style = IosTheme.type.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                    ) {
                        itemsIndexed(recentOneOnOne, key = { _, ch -> ch.id }) { index, channel ->
                            val user = channel.dmUsers.firstOrNull()
                            val username = user?.username ?: channel.displayTitle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectChannel(channel) }
                                    .padding(vertical = 9.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    UserAvatar(
                                        avatarUrl = avatarUrlOf(user?.avatarTemplate, 96),
                                        username = username,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = channel.displayTitle,
                                            style = IosTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                            color = colors.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        user?.username?.takeIf { it.isNotBlank() }?.let { handle ->
                                            Text(
                                                text = "@$handle",
                                                style = IosTheme.type.caption,
                                                color = colors.secondaryLabel,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                HeaderActionPill(
                                    text = "进入",
                                    emphasized = true,
                                    onClick = { onSelectChannel(channel) },
                                )
                            }
                            if (index < recentOneOnOne.lastIndex) {
                                IosDivider(startIndent = 48.dp)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "输入用户名即可快速发起直接消息",
                        style = IosTheme.type.footnote,
                        color = colors.secondaryLabel,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 18.dp),
                    )
                }
            }
        }
    }
}
