package org.linuxdo.android.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.linuxdo.android.Config
import org.linuxdo.android.data.AttachedMedia
import org.linuxdo.android.data.ChatChannelSummary
import org.linuxdo.android.data.ChatMessageSummary
import org.linuxdo.android.data.ChatUpload
import org.linuxdo.android.data.ChatUserSummary
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.ChatUiState
import org.linuxdo.android.ui.RichMarkdownEditorView
import org.linuxdo.android.ui.design.FullscreenDialog
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.MessageListRefresh
import org.linuxdo.android.ui.design.UserAvatar
import org.linuxdo.android.ui.design.composerBackKeys
import org.linuxdo.android.ui.design.rememberComposerDismiss
import org.linuxdo.android.ui.design.scrollListBars
import org.linuxdo.android.ui.format.RelativeTime
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal enum class DirectMessageFilter { All, Unread, Group }
internal enum class ChannelListFilter { All, Unread, Muted }

private val CHAT_CHANNEL_ACCENTS = listOf(
    Color(0xFF007AFF),
    Color(0xFFF59E0B),
    Color(0xFF10B981),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF06B6D4),
)

internal fun filterDirectMessageChannels(
    channels: List<ChatChannelSummary>,
    filter: DirectMessageFilter,
    query: String,
): List<ChatChannelSummary> {
    val cleanQuery = query.trim()
    return channels.filter { channel ->
        if (!channel.isDirectMessage) return@filter false
        val matchesFilter = when (filter) {
            DirectMessageFilter.All -> true
            DirectMessageFilter.Unread -> channel.unreadCount > 0 && channel.membership?.muted != true
            DirectMessageFilter.Group -> !channel.isOneOnOneDm && channel.dmUsers.size > 1
        }
        if (!matchesFilter) return@filter false
        cleanQuery.isEmpty() ||
            channel.displayTitle.contains(cleanQuery, ignoreCase = true) ||
            channel.dmUsers.any {
                it.username.contains(cleanQuery, ignoreCase = true) ||
                    it.name?.contains(cleanQuery, ignoreCase = true) == true
            } ||
            channel.lastMessage?.message?.contains(cleanQuery, ignoreCase = true) == true
    }
}

internal fun filterPublicChannels(
    channels: List<ChatChannelSummary>,
    filter: ChannelListFilter,
    query: String,
): List<ChatChannelSummary> {
    val cleanQuery = query.trim()
    return channels.filter { channel ->
        if (channel.isDirectMessage) return@filter false
        val matchesFilter = when (filter) {
            ChannelListFilter.All -> true
            ChannelListFilter.Unread -> channel.unreadCount > 0 && channel.membership?.muted != true
            ChannelListFilter.Muted -> channel.membership?.muted == true
        }
        if (!matchesFilter) return@filter false
        cleanQuery.isEmpty() ||
            channel.displayTitle.contains(cleanQuery, ignoreCase = true) ||
            channel.categoryName?.contains(cleanQuery, ignoreCase = true) == true ||
            channel.lastMessage?.message?.contains(cleanQuery, ignoreCase = true) == true
    }
}

internal fun formatChatConversationPreview(
    channel: ChatChannelSummary,
    currentUsername: String?,
): Pair<String?, String> {
    val last = channel.lastMessage
    if (last == null) {
        return null to if (channel.isDirectMessage) "暂无对话" else (channel.categoryName ?: "暂无新消息")
    }
    val raw = last.message.ifBlank { org.jsoup.Jsoup.parseBodyFragment(last.cooked).text() }
    val rendered = renderEmojiShortcodes(raw, keepUnknown = true).orEmpty().replace('\n', ' ').trim()
    val body = when {
        rendered.isNotBlank() -> rendered
        last.uploads.any { it.isImage } -> "[图片]"
        last.uploads.isNotEmpty() -> "[附件]"
        else -> "新消息"
    }
    val senderUsername = last.user?.username
    val isFromMe = !currentUsername.isNullOrBlank() &&
        !senderUsername.isNullOrBlank() &&
        senderUsername.equals(currentUsername, ignoreCase = true)
    val prefix = when {
        isFromMe -> "我:"
        channel.isOneOnOneDm -> null
        else -> (last.user?.name?.takeIf { it.isNotBlank() } ?: senderUsername)?.let { "$it:" }
    }
    return prefix to body
}

internal fun shouldShowTimeDivider(previousIso: String?, currentIso: String?): Boolean {
    val cur = parseChatInstant(currentIso) ?: return false
    val prev = parseChatInstant(previousIso) ?: return true
    return ChronoUnit.MINUTES.between(prev, cur) >= 15L
}

internal fun isSameBubbleGroup(previous: ChatMessageSummary?, current: ChatMessageSummary): Boolean {
    if (previous == null) return false
    if (shouldShowTimeDivider(previous.createdAt, current.createdAt)) return false
    val prevUser = previous.user?.username?.lowercase() ?: return false
    val curUser = current.user?.username?.lowercase() ?: return false
    return prevUser == curUser
}

private fun parseChatInstant(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(iso).toInstant() }
        .recoverCatching { Instant.parse(iso) }
        .getOrNull()
}

private fun formatChatTimeDivider(iso: String?, now: Instant = Instant.now()): String {
    val instant = parseChatInstant(iso) ?: return ""
    val zone = ZoneId.systemDefault()
    val zdt = instant.atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    val date = zdt.toLocalDate()
    val hm = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (ChronoUnit.DAYS.between(date, today)) {
        0L -> "今天 $hm"
        1L -> "昨天 $hm"
        in 2L..6L -> "${ChronoUnit.DAYS.between(date, today)}天前 $hm"
        else -> if (date.year == today.year) {
            "${date.monthValue}月${date.dayOfMonth}日 $hm"
        } else {
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日 $hm"
        }
    }
}

private fun formatBubbleClock(iso: String?): String {
    val instant = parseChatInstant(iso) ?: return RelativeTime.format(iso)
    val zdt = instant.atZone(ZoneId.systemDefault())
    return zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
internal fun ChatList(
    state: ChatUiState,
    directMessages: Boolean,
    dmFilter: DirectMessageFilter,
    channelFilter: ChannelListFilter,
    searchQuery: String,
    showQuickContactsBar: Boolean,
    currentUsername: String?,
    scrollToTopRequest: Int,
    onRefresh: () -> Unit,
    onOpenChannel: (ChatChannelSummary) -> Unit,
    onNewDirectMessage: () -> Unit,
    active: Boolean,
) {
    val currentList = remember(directMessages, state.channels, dmFilter, channelFilter, searchQuery) {
        if (directMessages) {
            filterDirectMessageChannels(state.channels, dmFilter, searchQuery)
        } else {
            filterPublicChannels(state.channels, channelFilter, searchQuery)
        }
    }
    val allDmChannels = remember(state.channels, directMessages) {
        if (directMessages) state.channels.filter { it.isDirectMessage } else emptyList()
    }
    val list = rememberLazyListState()
    org.linuxdo.android.ui.design.ScrollToTopOnRequest(list, scrollToTopRequest)
    LaunchedEffect(searchQuery, dmFilter, channelFilter) { list.scrollToItem(0) }

    val showQuickStrip = directMessages &&
        showQuickContactsBar &&
        searchQuery.isBlank() &&
        dmFilter == DirectMessageFilter.All

    MessageListRefresh(
        refreshing = state.loadingChannels && state.channels.isNotEmpty(),
        active = active,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(IosTheme.colors.groupedBackground),
    ) {
        LazyColumn(
            state = list,
            contentPadding = PaddingValues(bottom = 72.dp),
            modifier = Modifier
                .fillMaxSize()
                .scrollListBars(list, active),
        ) {
            if (showQuickStrip) {
                item(key = "dm_quick_contacts_strip") {
                    DirectMessageQuickContactsBar(
                        channels = allDmChannels,
                        onOpenChannel = onOpenChannel,
                        onNewDirectMessage = onNewDirectMessage,
                    )
                }
            }

            if (state.loadingChannels && state.channels.isEmpty()) {
                item(key = "chat_loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        IosActivityIndicator(diameter = 24.dp, color = IosTheme.colors.accent)
                    }
                }
            }

            state.channelsError?.let { message ->
                item(key = "chat_error") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(message, style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = onRefresh) { Text("重试") }
                    }
                }
            }

            if (currentList.isEmpty() && !state.loadingChannels && state.channelsError == null) {
                item(key = "chat_empty") {
                    val emptyText = when {
                        searchQuery.isNotBlank() -> "未搜索到匹配项"
                        directMessages && dmFilter == DirectMessageFilter.Unread -> "暂无未读直接消息"
                        directMessages && dmFilter == DirectMessageFilter.Group -> "暂无群聊对话"
                        directMessages -> "暂无直接消息"
                        channelFilter == ChannelListFilter.Unread -> "暂无未读频道消息"
                        channelFilter == ChannelListFilter.Muted -> "暂无免打扰频道"
                        else -> "暂无公共频道"
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyText,
                            style = IosTheme.type.footnote,
                            color = IosTheme.colors.secondaryLabel,
                        )
                    }
                }
            }

            itemsIndexed(currentList, key = { _, channel -> channel.id }) { index, channel ->
                ChatConversationRow(
                    channel = channel,
                    currentUsername = currentUsername,
                    onClick = { onOpenChannel(channel) },
                )
                if (index < currentList.lastIndex) {
                    Box(Modifier.fillMaxWidth().background(IosTheme.colors.card)) {
                        IosDivider(startIndent = 74.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectMessageQuickContactsBar(
    channels: List<ChatChannelSummary>,
    onOpenChannel: (ChatChannelSummary) -> Unit,
    onNewDirectMessage: () -> Unit,
) {
    val colors = IosTheme.colors
    val recent = remember(channels) { channels.take(10) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.groupedBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 新对话入口
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .clickable(onClick = onNewDirectMessage),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(
                            width = 1.dp,
                            color = colors.accent.copy(alpha = if (colors.isDark) 0.45f else 0.34f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "发起新对话",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "新对话",
                    style = IosTheme.type.caption.copy(fontSize = 11.sp),
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            recent.forEach { channel ->
                val isGroup = !channel.isOneOnOneDm && channel.dmUsers.size > 1
                val firstUser = channel.dmUsers.firstOrNull() ?: channel.lastMessage?.user
                val unread = channel.unreadCount > 0 && channel.membership?.muted != true
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .clickable { onOpenChannel(channel) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(44.dp)) {
                        if (isGroup) {
                            GroupAvatarGrid(users = channel.dmUsers, size = 44.dp)
                        } else {
                            UserAvatar(
                                avatarUrl = avatarUrlOf(firstUser?.avatarTemplate, 96),
                                username = firstUser?.username ?: channel.displayTitle,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                        if (unread) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                                    .border(1.5.dp, colors.groupedBackground, CircleShape),
                            )
                        }
                    }
                    Text(
                        text = channel.displayTitle,
                        style = IosTheme.type.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (unread) colors.label else colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        IosFullDivider()
    }
}

@Composable
private fun ChatConversationRow(
    channel: ChatChannelSummary,
    currentUsername: String?,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    val isDirect = channel.isDirectMessage
    val isGroup = isDirect && !channel.isOneOnOneDm && channel.dmUsers.size > 1
    val muted = channel.membership?.muted == true
    val unread = channel.unreadCount
    val hasActiveUnread = unread > 0 && !muted
    val (prefix, body) = remember(channel, currentUsername) {
        formatChatConversationPreview(channel, currentUsername)
    }
    val timeText = remember(channel.lastMessage?.createdAt) {
        channel.lastMessage?.createdAt?.let { RelativeTime.format(it) }.orEmpty()
    }
    val secondaryTag = remember(channel, isDirect, isGroup) {
        when {
            isGroup -> "${channel.dmUsers.size}人"
            isDirect -> channel.dmUsers.firstOrNull()?.username?.takeIf {
                it.isNotBlank() && !it.equals(channel.displayTitle, ignoreCase = true)
            }?.let { "@$it" }
            else -> null
        }
    }
    val rowBg = if (hasActiveUnread) {
        colors.accent.copy(alpha = if (colors.isDark) 0.08f else 0.04f)
    } else {
        colors.card
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 左侧 46dp 头像（1v1 圆形头像 / 群聊 2x2 拼贴 / 频道彩色 # 圆角方块）
        when {
            isGroup -> GroupAvatarGrid(users = channel.dmUsers, size = 46.dp)
            isDirect -> {
                val user = channel.dmUsers.firstOrNull() ?: channel.lastMessage?.user
                UserAvatar(
                    avatarUrl = avatarUrlOf(user?.avatarTemplate, 96),
                    username = user?.username ?: channel.displayTitle,
                    modifier = Modifier.size(46.dp),
                )
            }
            else -> ChannelHashAvatar(channelId = channel.id, size = 46.dp)
        }

        // 右侧双行信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = channel.displayTitle,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = IosTheme.type.body.copy(
                            fontSize = 15.sp,
                            fontWeight = if (hasActiveUnread) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = colors.label,
                    )
                    if (secondaryTag != null) {
                        if (isGroup) {
                            Text(
                                text = secondaryTag,
                                style = IosTheme.type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.secondaryLabel,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colors.fieldBackground)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        } else {
                            Text(
                                text = secondaryTag,
                                style = IosTheme.type.caption.copy(fontSize = 11.5.sp),
                                color = colors.tertiaryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (timeText.isNotBlank()) {
                    Text(
                        text = timeText,
                        style = IosTheme.type.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = if (hasActiveUnread) FontWeight.SemiBold else FontWeight.Normal,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (hasActiveUnread) colors.accent else colors.tertiaryLabel,
                        maxLines = 1,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!prefix.isNullOrBlank()) {
                        Text(
                            text = "$prefix ",
                            style = IosTheme.type.footnote.copy(fontSize = 13.3.sp),
                            color = colors.tertiaryLabel,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = IosTheme.type.footnote.copy(fontSize = 13.3.sp),
                        color = if (hasActiveUnread) colors.label.copy(alpha = 0.86f) else colors.secondaryLabel,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (muted) {
                        MutedBellGlyph(tint = colors.tertiaryLabel)
                    }
                    if (unread > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (muted) colors.fieldBackground else colors.accent)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                .semantics { contentDescription = "${unread}条未读消息" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (unread > 99) "99+" else unread.toString(),
                                style = IosTheme.type.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                color = if (muted) colors.secondaryLabel else Color.White,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAvatarGrid(users: List<ChatUserSummary>, size: Dp) {
    val colors = IosTheme.colors
    val shown = remember(users) { users.take(4) }
    val cellSize = (size - 6.dp) / 2
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.fieldBackground)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                shown.getOrNull(0)?.let { u ->
                    UserAvatar(
                        avatarUrl = avatarUrlOf(u.avatarTemplate, 64),
                        username = u.username,
                        cornerRadius = 4.dp,
                        modifier = Modifier.size(cellSize),
                    )
                }
                shown.getOrNull(1)?.let { u ->
                    UserAvatar(
                        avatarUrl = avatarUrlOf(u.avatarTemplate, 64),
                        username = u.username,
                        cornerRadius = 4.dp,
                        modifier = Modifier.size(cellSize),
                    )
                }
            }
            if (shown.size > 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    shown.getOrNull(2)?.let { u ->
                        UserAvatar(
                            avatarUrl = avatarUrlOf(u.avatarTemplate, 64),
                            username = u.username,
                            cornerRadius = 4.dp,
                            modifier = Modifier.size(cellSize),
                        )
                    }
                    shown.getOrNull(3)?.let { u ->
                        UserAvatar(
                            avatarUrl = avatarUrlOf(u.avatarTemplate, 64),
                            username = u.username,
                            cornerRadius = 4.dp,
                            modifier = Modifier.size(cellSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHashAvatar(channelId: Long, size: Dp) {
    val accent = CHAT_CHANNEL_ACCENTS[(channelId.toInt().let { if (it < 0) -it else it }) % CHAT_CHANNEL_ACCENTS.size]
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(accent.copy(alpha = 0.13f))
            .border(1.dp, accent.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "#",
            style = IosTheme.type.headline.copy(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold),
            color = accent,
        )
    }
}

@Composable
private fun MutedBellGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(13.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val bell = Path().apply {
                moveTo(18f, 8f)
                cubicTo(18f, 4.7f, 15.3f, 2f, 12f, 2f)
                cubicTo(8.7f, 2f, 6f, 4.7f, 6f, 8f)
                cubicTo(6f, 15f, 3f, 17f, 3f, 17f)
                lineTo(17f, 17f)
            }
            drawPath(bell, tint, style = stroke)
            drawLine(tint, Offset(2f, 2f), Offset(22f, 22f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun ChatRoomScreen(
    channel: ChatChannelSummary,
    state: ChatUiState,
    active: Boolean,
    currentUsername: String? = null,
    onLoad: (Long, Boolean) -> Unit,
    onLoadPast: (Long) -> Unit = {},
    onSetChannelMuted: (Long, Boolean) -> Unit,
    onMessagesVisible: (Long, Long) -> Unit = { _, _ -> },
    onSend: (Long, String, List<Long>, () -> Unit) -> Unit,
    onUploadAttachment: (Long, Uri, (AttachedMedia) -> Unit, (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onOpenUser: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    val channelId = channel.id
    val messages = state.messagesByChannel[channelId].orEmpty()
    val listState = rememberLazyListState()
    var draft by rememberSaveable(channelId) { mutableStateOf("") }
    var showComposerSheet by remember(channelId) { mutableStateOf(false) }
    var pendingAttachments by remember(channelId) { mutableStateOf<List<AttachedMedia>>(emptyList()) }
    var isUploading by remember(channelId) { mutableStateOf(false) }
    var pickingAttachment by remember(channelId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val send: (String, List<Long>) -> Unit = { text, uploads ->
        onSend(channelId, text, uploads) {
            draft = ""
            pendingAttachments = emptyList()
            showComposerSheet = false
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickingAttachment = false
        if (uri != null) {
            isUploading = true
            onUploadAttachment(channelId, uri, { media ->
                isUploading = false
                pendingAttachments = pendingAttachments + media
            }, {
                isUploading = false
            })
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickingAttachment = false
        if (uri != null) {
            isUploading = true
            onUploadAttachment(channelId, uri, { media ->
                isUploading = false
                pendingAttachments = pendingAttachments + media
            }, {
                isUploading = false
            })
        }
    }

    LaunchedEffect(active, channelId) {
        if (!active) return@LaunchedEffect
        onLoad(channelId, false)
    }

    LaunchedEffect(active, channelId, messages, channel.membership?.lastReadMessageId) {
        if (!active) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long }.maxOrNull() }
            .distinctUntilChanged().collectLatest { lastVisible ->
                if (lastVisible != null) {
                    delay(400)
                    onMessagesVisible(channelId, lastVisible)
                }
            }
    }

    var initialScrollDone by remember(channelId) { mutableStateOf(false) }
    var prevFirstMessageId by remember(channelId) { mutableStateOf<Long?>(null) }
    var prevLastMessageId by remember(channelId) { mutableStateOf<Long?>(null) }
    val canLoadMorePast = state.canLoadMorePastByChannel[channelId] ?: true
    val isLoadingPast = state.loadingPastChannelId == channelId

    LaunchedEffect(messages) {
        if (messages.isEmpty()) return@LaunchedEffect
        val currentFirstId = messages.first().id
        val currentLastId = messages.last().id
        if (!initialScrollDone) {
            initialScrollDone = true
            listState.scrollToItem(messages.lastIndex)
        } else if (prevFirstMessageId != null && currentFirstId != prevFirstMessageId) {
            val addedCount = messages.indexOfFirst { it.id == prevFirstMessageId }.takeIf { it > 0 } ?: 0
            if (addedCount > 0) {
                val newIndex = listState.firstVisibleItemIndex + addedCount
                val offset = listState.firstVisibleItemScrollOffset
                listState.scrollToItem(newIndex, offset)
            }
        } else if (prevLastMessageId != null && currentLastId != prevLastMessageId) {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (totalItems - lastVisibleIndex <= 3) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
        prevFirstMessageId = currentFirstId
        prevLastMessageId = currentLastId
    }

    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex, canLoadMorePast, isLoadingPast, initialScrollDone) {
        if (firstVisibleIndex <= 2 && canLoadMorePast && !isLoadingPast && messages.isNotEmpty() && initialScrollDone) {
            onLoadPast(channelId)
        }
    }

    val showSenderNames = !channel.isOneOnOneDm
    val canSendInline = (draft.isNotBlank() || pendingAttachments.isNotEmpty()) &&
        !isUploading && state.sendingChannelId == null

    fun submitInline() {
        if (!canSendInline) return
        val text = draft.trim()
        val attachText = pendingAttachments.joinToString("\n") { it.toMarkdown() }
        val fullMessage = when {
            text.isBlank() -> attachText
            attachText.isBlank() -> text
            else -> "$text\n\n$attachText"
        }
        if (fullMessage.isNotBlank()) {
            val uploadIds = pendingAttachments.mapNotNull { it.uploadId }
            send(fullMessage, uploadIds)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(IosTheme.colors.groupedBackground)) {
            // 统一现代聊天顶栏（支持头像/副标题/免打扰）
            ChatRoomHeaderBar(
                channel = channel,
                muteUpdating = channelId in state.updatingMuteChannelIds,
                onBack = onBack,
                onOpenUser = onOpenUser,
                onSetMuted = { onSetChannelMuted(channelId, it) },
            )

            // 错误提示
            if (state.error != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(state.error, color = IosTheme.colors.secondaryLabel)
                    org.linuxdo.android.ui.design.NetworkSettingsLink()
                }
            }

            // 消息气泡列表
            if (state.loadingChannelId == channelId && messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IosActivityIndicator(diameter = 24.dp, color = IosTheme.colors.accent)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                ) {
                    if (isLoadingPast) {
                        item(key = "loading_past_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                IosActivityIndicator(diameter = 18.dp, color = IosTheme.colors.accent)
                            }
                        }
                    } else if (!canLoadMorePast && messages.size >= 50) {
                        item(key = "all_history_loaded") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "已加载全部历史消息",
                                    style = IosTheme.type.caption,
                                    color = IosTheme.colors.secondaryLabel,
                                )
                            }
                        }
                    }

                    itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                        val prev = messages.getOrNull(index - 1)
                        val next = messages.getOrNull(index + 1)
                        val showDivider = shouldShowTimeDivider(prev?.createdAt, message.createdAt)
                        val sameAsPrev = !showDivider && isSameBubbleGroup(prev, message)
                        val sameAsNext = next != null && isSameBubbleGroup(message, next)
                        val isMe = currentUsername != null && message.user?.username == currentUsername

                        Column(Modifier.fillMaxWidth()) {
                            if (showDivider) {
                                val dividerLabel = remember(message.createdAt) {
                                    formatChatTimeDivider(message.createdAt)
                                }
                                if (dividerLabel.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = dividerLabel,
                                            style = IosTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                            color = IosTheme.colors.secondaryLabel,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(IosTheme.colors.fieldBackground)
                                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                            ChatMessageBubble(
                                message = message,
                                isMe = isMe,
                                showSenderName = showSenderNames && !sameAsPrev,
                                groupedWithPrevious = sameAsPrev,
                                groupedWithNext = sameAsNext,
                                onOpenUser = onOpenUser,
                            )
                        }
                    }
                }
            }

            // 底部原地直输栏（无需强制弹窗即可直接打字发送，保留展开全屏富文本按钮）
            Surface(
                color = IosTheme.colors.card,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                Column {
                    IosFullDivider()

                    // 待发送附件预览栏
                    if (pendingAttachments.isNotEmpty() || isUploading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (media in pendingAttachments) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IosTheme.colors.fieldBackground)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        if (media.isImage) {
                                            AsyncImage(
                                                model = media.previewUrl,
                                                contentDescription = media.name,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Text("📄", fontSize = 14.sp)
                                        }
                                        Text(
                                            text = media.name.take(12),
                                            style = IosTheme.type.caption,
                                            color = IosTheme.colors.label,
                                            maxLines = 1,
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除",
                                            tint = IosTheme.colors.secondaryLabel,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    pendingAttachments = pendingAttachments.filterNot { it.id == media.id }
                                                },
                                        )
                                    }
                                }
                            }
                            if (isUploading) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IosTheme.colors.fieldBackground)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    IosActivityIndicator(diameter = 14.dp, color = IosTheme.colors.accent)
                                    Text("正在上传...", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        // 图片按钮
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(IosTheme.colors.fieldBackground)
                                .clickable(enabled = !isUploading && state.sendingChannelId == null) {
                                    pickingAttachment = true
                                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ComposerImageIcon(tint = if (isUploading) IosTheme.colors.tertiaryLabel else IosTheme.colors.label)
                        }

                        // 附件按钮
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(IosTheme.colors.fieldBackground)
                                .clickable(enabled = !isUploading && state.sendingChannelId == null) {
                                    pickingAttachment = true
                                    pickFile.launch(arrayOf("*/*"))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ComposerAttachmentIcon(tint = if (isUploading) IosTheme.colors.tertiaryLabel else IosTheme.colors.label)
                        }

                        // 原地胶囊文本输入框 + 内置展开富文本按钮
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp, max = 112.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(IosTheme.colors.fieldBackground)
                                .padding(start = 13.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                enabled = state.sendingChannelId == null,
                                maxLines = 4,
                                textStyle = TextStyle(
                                    fontSize = 14.5.sp,
                                    lineHeight = 20.sp,
                                    color = IosTheme.colors.label,
                                ),
                                cursorBrush = SolidColor(IosTheme.colors.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                keyboardActions = KeyboardActions(onSend = { submitInline() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "输入聊天消息" },
                                decorationBox = { innerField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (draft.isEmpty()) {
                                            Text(
                                                text = "发送消息…",
                                                style = TextStyle(fontSize = 14.5.sp, color = IosTheme.colors.tertiaryLabel),
                                            )
                                        }
                                        innerField()
                                    }
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = state.sendingChannelId == null) {
                                        showComposerSheet = true
                                    }
                                    .semantics { contentDescription = "展开全屏富文本编辑器" },
                                contentAlignment = Alignment.Center,
                            ) {
                                ChatFullscreenToggleIcon(
                                    isFullScreen = false,
                                    tint = IosTheme.colors.secondaryLabel,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        // 发送药丸按钮
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (canSendInline) IosTheme.colors.accent else IosTheme.colors.accent.copy(alpha = 0.35f))
                                .clickable(enabled = canSendInline, onClick = ::submitInline)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.sendingChannelId == channelId) {
                                IosActivityIndicator(diameter = 15.dp, color = Color.White)
                            } else {
                                Text(
                                    text = "发送",
                                    style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 点击展开图标后弹出的富文本/全屏输入面板
        if (showComposerSheet) {
            ChatComposerBottomSheet(
                channelTitle = channel.displayTitle.ifBlank { "实时聊天" },
                draft = draft,
                onDraftChange = { newDraft: String -> draft = newDraft },
                pendingAttachments = pendingAttachments,
                onRemoveAttachment = { removeId: String -> pendingAttachments = pendingAttachments.filterNot { it.id == removeId } },
                isUploading = isUploading,
                pickingAttachment = pickingAttachment,
                isSending = state.sendingChannelId == channelId,
                error = state.error,
                onPickImage = { pickingAttachment = true; pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onPickFile = { pickingAttachment = true; pickFile.launch(arrayOf("*/*")) },
                onSend = { fullMessage, uploadIds ->
                    if (fullMessage.isNotBlank()) {
                        send(fullMessage, uploadIds)
                    }
                },
                onDismiss = { keyboard?.hide(); showComposerSheet = false },
            )
        }
    }
}

@Composable
private fun ChatRoomHeaderBar(
    channel: ChatChannelSummary,
    muteUpdating: Boolean,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onSetMuted: (Boolean) -> Unit,
) {
    val colors = IosTheme.colors
    val isDirect = channel.isDirectMessage
    val isGroup = isDirect && !channel.isOneOnOneDm && channel.dmUsers.size > 1
    val peerUser = if (channel.isOneOnOneDm) channel.dmUsers.firstOrNull() else null
    val subtitle = remember(channel, isDirect, isGroup, peerUser) {
        when {
            isGroup -> "${channel.dmUsers.size}人 · 直接消息群组"
            peerUser != null -> "@${peerUser.username}"
            !isDirect -> channel.categoryName?.takeIf { it.isNotBlank() } ?: "公共频道"
            else -> "直接消息"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.groupedBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                BackChevronGlyph(tint = colors.accent)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !peerUser?.username.isNullOrBlank()) {
                        peerUser?.username?.let(onOpenUser)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                when {
                    isGroup -> GroupAvatarGrid(users = channel.dmUsers, size = 34.dp)
                    isDirect -> {
                        val user = peerUser ?: channel.lastMessage?.user
                        UserAvatar(
                            avatarUrl = avatarUrlOf(user?.avatarTemplate, 80),
                            username = user?.username ?: channel.displayTitle,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    else -> ChannelHashAvatar(channelId = channel.id, size = 34.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = channel.displayTitle,
                        style = IosTheme.type.headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = IosTheme.type.caption.copy(fontSize = 11.sp),
                            color = colors.secondaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            val membership = channel.membership
            if (membership != null) {
                ChatChannelNotificationMenu(
                    channelId = channel.id,
                    muted = membership.muted,
                    enabled = !muteUpdating,
                    onSetMuted = onSetMuted,
                )
            }
        }
        IosFullDivider()
    }
}

@Composable
private fun BackChevronGlyph(tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val path = Path().apply {
                moveTo(15f, 18f)
                lineTo(9f, 12f)
                lineTo(15f, 6f)
            }
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun ChatChannelNotificationMenu(
    channelId: Long,
    muted: Boolean,
    enabled: Boolean,
    onSetMuted: (Boolean) -> Unit,
) {
    var expanded by remember(channelId) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = if (muted) "消息通知设置，已开启消息免打扰" else "消息通知设置",
                tint = if (enabled && !muted) IosTheme.colors.accent else IosTheme.colors.secondaryLabel,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (muted) "关闭消息免打扰" else "开启消息免打扰") },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onSetMuted(!muted)
                },
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageSummary,
    isMe: Boolean,
    showSenderName: Boolean,
    groupedWithPrevious: Boolean,
    groupedWithNext: Boolean,
    onOpenUser: (String) -> Unit,
) {
    val user = message.user
    val textContent = remember(message.message, message.cooked) {
        val raw = if (message.message.isNotBlank()) message.message
        else android.text.Html.fromHtml(message.cooked, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()
        renderEmojiShortcodes(raw, keepUnknown = true).orEmpty()
    }
    val timeStr = remember(message.createdAt) { formatBubbleClock(message.createdAt) }
    val imageUploads = remember(message.uploads) { message.uploads.filter { it.isImage } }
    val fileUploads = remember(message.uploads) { message.uploads.filter { !it.isImage } }
    var fullscreenImage by remember { mutableStateOf<ChatUpload?>(null) }

    fullscreenImage?.let { upload ->
        FullscreenDialog(
            onDismissRequest = { fullscreenImage = null },
            darkSystemBars = true,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .safeDrawingPadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { fullscreenImage = null },
                contentAlignment = Alignment.Center,
            ) {
                SubcomposeAsyncImage(
                    model = upload.absoluteUrl(Config.BASE_URL),
                    contentDescription = upload.originalFilename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }

    val topPadding = if (groupedWithPrevious) 1.dp else 5.dp
    val bottomPadding = if (groupedWithNext) 1.dp else 4.dp

    if (isMe) {
        val bubbleShape = RoundedCornerShape(
            topStart = 17.dp,
            topEnd = if (groupedWithPrevious) 5.dp else 17.dp,
            bottomStart = 17.dp,
            bottomEnd = 5.dp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, end = 12.dp, top = topPadding, bottom = bottomPadding),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(IosTheme.colors.accent)
                    .padding(horizontal = 13.dp, vertical = 8.5.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (textContent.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = textContent,
                                style = TextStyle(fontSize = 14.8.sp, color = Color.White, lineHeight = 20.5.sp),
                            )
                        }
                    }
                    imageUploads.forEach { upload ->
                        AsyncImage(
                            model = upload.absoluteThumbnailUrl(Config.BASE_URL),
                            contentDescription = upload.originalFilename,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { fullscreenImage = upload },
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                    fileUploads.forEach { upload ->
                        Text(
                            text = "📎 ${upload.originalFilename}",
                            style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.88f)),
                        )
                    }
                }
            }
            if (!groupedWithNext && timeStr.isNotBlank()) {
                Text(
                    text = timeStr,
                    style = TextStyle(fontSize = 10.5.sp, color = IosTheme.colors.tertiaryLabel),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    } else {
        val bubbleShape = RoundedCornerShape(
            topStart = if (groupedWithPrevious) 5.dp else 17.dp,
            topEnd = 17.dp,
            bottomStart = 5.dp,
            bottomEnd = 17.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 48.dp, top = topPadding, bottom = bottomPadding),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (!groupedWithNext) {
                    UserAvatar(
                        avatarUrl = avatarUrlOf(user?.avatarTemplate, 72),
                        username = user?.username,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(enabled = !user?.username.isNullOrBlank()) {
                                user?.username?.let(onOpenUser)
                            },
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showSenderName) {
                    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: user?.username.orEmpty()
                    if (displayName.isNotBlank()) {
                        Text(
                            text = displayName,
                            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = IosTheme.colors.secondaryLabel),
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable(enabled = !user?.username.isNullOrBlank()) {
                                    user?.username?.let(onOpenUser)
                                },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(IosTheme.colors.card)
                        .border(
                            0.5.dp,
                            IosTheme.colors.separator.copy(alpha = 0.5f),
                            bubbleShape,
                        )
                        .padding(horizontal = 13.dp, vertical = 8.5.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (textContent.isNotBlank()) {
                            SelectionContainer {
                                Text(
                                    text = textContent,
                                    style = TextStyle(fontSize = 14.8.sp, color = IosTheme.colors.label, lineHeight = 20.5.sp),
                                )
                            }
                        }
                        imageUploads.forEach { upload ->
                            AsyncImage(
                                model = upload.absoluteThumbnailUrl(Config.BASE_URL),
                                contentDescription = upload.originalFilename,
                                modifier = Modifier
                                    .widthIn(max = 200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { fullscreenImage = upload },
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                        fileUploads.forEach { upload ->
                            Text(
                                text = "📎 ${upload.originalFilename}",
                                style = TextStyle(fontSize = 13.sp, color = IosTheme.colors.secondaryLabel),
                            )
                        }
                    }
                }

                if (!groupedWithNext && timeStr.isNotBlank()) {
                    Text(
                        text = timeStr,
                        style = TextStyle(fontSize = 10.5.sp, color = IosTheme.colors.tertiaryLabel),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/** 实时聊天富文本评论样式输入框 (支持半屏/全屏切换、附件、表情、Markdown) */
@Composable
private fun ChatComposerBottomSheet(
    channelTitle: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    pendingAttachments: List<AttachedMedia>,
    onRemoveAttachment: (String) -> Unit,
    isUploading: Boolean,
    pickingAttachment: Boolean,
    isSending: Boolean,
    error: String?,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onSend: (String, List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val completionSource = org.linuxdo.android.ui.LocalEditorSuggestionSource.current
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var editorView by remember { mutableStateOf<RichMarkdownEditorView?>(null) }
    var editorContentHeight by remember { mutableIntStateOf(0) }
    val compactEditorHeight = with(LocalDensity.current) {
        editorContentHeight.toDp().coerceIn(44.dp, 180.dp)
    }
    val dismiss = rememberComposerDismiss(
        canDismiss = !isSending,
        ignoreKeyboardHide = pickingAttachment,
    ) {
        editorView?.exportToMarkdown()?.let(onDraftChange)
        onDismiss()
    }
    val textColorInt = IosTheme.colors.label.toArgb()
    val hintColorInt = IosTheme.colors.tertiaryLabel.toArgb()

    LaunchedEffect(pendingAttachments) {
        editorView?.syncAttachments(pendingAttachments)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .composerBackKeys(dismiss)
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismiss,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.92f)
                .then(if (isFullScreen) Modifier.height(maxHeight * 0.92f) else Modifier)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(IosTheme.colors.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "发送给 $channelTitle",
                    style = IosTheme.type.headline,
                    fontWeight = FontWeight.SemiBold,
                    color = IosTheme.colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { isFullScreen = !isFullScreen }
                        .semantics { contentDescription = if (isFullScreen) "恢复默认高度" else "全屏显示" },
                    contentAlignment = Alignment.Center,
                ) {
                    ChatFullscreenToggleIcon(isFullScreen = isFullScreen, tint = IosTheme.colors.label)
                }
            }

            val editorTextSizePx = with(LocalDensity.current) { 16.sp.toPx() }
            Box(
                modifier = Modifier
                    .weight(1f, fill = isFullScreen)
                    .fillMaxWidth()
                    .then(
                        if (isFullScreen) Modifier else Modifier.height(
                            compactEditorHeight + (if (pendingAttachments.isNotEmpty() || isUploading) 44.dp else 0.dp),
                        ),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(IosTheme.colors.fieldBackground),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                RichMarkdownEditorView(ctx).apply {
                                    setOnDismissInputListener(dismiss)
                                    hint = "发送消息…"
                                    updateThemeColors(textColorInt, hintColorInt)
                                    setCompletionSource(completionSource)
                                    setOnContentHeightChangeListener { editorContentHeight = it }
                                    setInitialMarkdown(draft, pendingAttachments)
                                    setOnMarkdownChangeListener { newRaw -> onDraftChange(newRaw) }
                                    setOnRemoveAttachmentListener { mediaId -> onRemoveAttachment(mediaId) }
                                    editorView = this
                                    post { requestInputFocus() }
                                }
                            },
                            update = { view ->
                                view.setOnDismissInputListener(dismiss)
                                view.updateThemeColors(textColorInt, hintColorInt)
                                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, editorTextSizePx)
                                view.setCompletionSource(completionSource)
                                view.isEnabled = !isSending
                            },
                        )
                    }

                    if (pendingAttachments.isNotEmpty() || isUploading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (media in pendingAttachments) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IosTheme.colors.card)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        if (media.isImage) {
                                            AsyncImage(
                                                model = media.previewUrl,
                                                contentDescription = media.name,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = IosTheme.colors.accent,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        Text(
                                            text = media.name,
                                            style = IosTheme.type.caption,
                                            color = IosTheme.colors.label,
                                            maxLines = 1,
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除",
                                            tint = IosTheme.colors.secondaryLabel,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onRemoveAttachment(media.id) },
                                        )
                                    }
                                }
                            }
                            if (isUploading) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IosTheme.colors.card)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    IosActivityIndicator(diameter = 14.dp, color = IosTheme.colors.accent)
                                    Text("正在上传...", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                                }
                            }
                        }
                    }
                }
            }

            error?.let { Text(it, style = IosTheme.type.footnote, color = IosTheme.colors.destructive, maxLines = 2) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isUploading && !isSending, onClick = onPickImage),
                        contentAlignment = Alignment.Center,
                    ) {
                        ComposerImageIcon(tint = if (isUploading) IosTheme.colors.tertiaryLabel else IosTheme.colors.label)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isUploading && !isSending, onClick = onPickFile),
                        contentAlignment = Alignment.Center,
                    ) {
                        ComposerAttachmentIcon(tint = if (isUploading) IosTheme.colors.tertiaryLabel else IosTheme.colors.label)
                    }
                }

                val canSend = (draft.isNotBlank() || pendingAttachments.isNotEmpty()) && !isUploading && !isSending
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (canSend) IosTheme.colors.accent else IosTheme.colors.accent.copy(alpha = 0.4f))
                        .clickable(enabled = canSend) {
                            val fullText = editorView?.exportToMarkdown()?.trim() ?: draft.trim()
                            val uploadIds = pendingAttachments.mapNotNull { it.uploadId }
                            onSend(fullText, uploadIds)
                        }
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSending) {
                        IosActivityIndicator(diameter = 16.dp, color = Color.White)
                    } else {
                        Text(
                            text = "发送",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatFullscreenToggleIcon(isFullScreen: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (!isFullScreen) {
                val p1 = Path().apply { moveTo(3f, 8f); lineTo(3f, 3f); lineTo(8f, 3f) }
                val p2 = Path().apply { moveTo(16f, 3f); lineTo(21f, 3f); lineTo(21f, 8f) }
                val p3 = Path().apply { moveTo(21f, 16f); lineTo(21f, 21f); lineTo(16f, 21f) }
                val p4 = Path().apply { moveTo(8f, 21f); lineTo(3f, 21f); lineTo(3f, 16f) }
                drawPath(p1, tint, style = stroke)
                drawPath(p2, tint, style = stroke)
                drawPath(p3, tint, style = stroke)
                drawPath(p4, tint, style = stroke)
            } else {
                val p1 = Path().apply { moveTo(8f, 3f); lineTo(8f, 8f); lineTo(3f, 8f) }
                val p2 = Path().apply { moveTo(16f, 3f); lineTo(16f, 8f); lineTo(21f, 8f) }
                val p3 = Path().apply { moveTo(16f, 21f); lineTo(16f, 16f); lineTo(21f, 16f) }
                val p4 = Path().apply { moveTo(8f, 21f); lineTo(8f, 16f); lineTo(3f, 16f) }
                drawPath(p1, tint, style = stroke)
                drawPath(p2, tint, style = stroke)
                drawPath(p3, tint, style = stroke)
                drawPath(p4, tint, style = stroke)
            }
        }
    }
}
