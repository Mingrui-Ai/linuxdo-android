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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.DisableSelection
import org.linuxdo.android.ui.design.ContinuousTextSelection
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.linuxdo.android.Config
import org.linuxdo.android.data.AttachedMedia
import org.linuxdo.android.data.prepareChatMessage
import org.linuxdo.android.data.ChatChannelSummary
import org.linuxdo.android.data.ChatMessageSummary
import org.linuxdo.android.data.ChatUpload
import org.linuxdo.android.data.ChatUserSummary
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.ChatUiState
import org.linuxdo.android.ui.design.FullscreenDialog
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosFullDivider
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.MessageListRefresh
import org.linuxdo.android.ui.design.UserAvatar
import org.linuxdo.android.ui.design.scrollListBars
import org.linuxdo.android.ui.format.RelativeTime
import org.linuxdo.android.ui.html.CookedBlock
import org.linuxdo.android.ui.html.CookedParser
import org.linuxdo.android.ui.html.InlineText
import org.linuxdo.android.ui.html.contentUrl
import org.linuxdo.android.ui.html.replyMarkdownPreview
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

/**
 * 解析聊天消息的 Markdown / cooked HTML 内容为结构化块，供气泡渲染（支持粗体、斜体、删除线、行内/围栏代码、
 * 链接、引用、列表、表格、折叠详情 [details]、剧透 [spoiler]、表情短码及图片）。
 */
internal fun parseChatBubbleBlocks(
    message: ChatMessageSummary,
    emojiUrls: Map<String, String> = emptyMap(),
): List<CookedBlock> {
    if (message.cooked.isNotBlank()) {
        val parsed = runCatching { CookedParser().parse(message.cooked) }.getOrNull().orEmpty()
        if (parsed.isNotEmpty()) return parsed
    }
    if (message.message.isNotBlank()) {
        // 先解析 Markdown，再处理正文表情；不能改写代码块、行内代码或链接中的短码。
        val parsed = runCatching {
            replyMarkdownPreview(message.message, emojiUrls)
        }.getOrNull().orEmpty()
        if (parsed.isNotEmpty()) return parsed
        return listOf(CookedBlock.Paragraph(listOf(InlineText(message.message))))
    }
    return emptyList()
}

/** cooked / Markdown 已显示的图片和附件不再从 uploads 追加一遍。 */
internal fun unrenderedChatUploads(uploads: List<ChatUpload>, blocks: List<CookedBlock>): List<ChatUpload> {
    val rendered = mutableSetOf<String>()
    fun add(url: String?) { url?.let(::contentUrl)?.let(rendered::add) }
    fun visit(items: List<CookedBlock>) {
        items.forEach { block ->
            when (block) {
                is CookedBlock.Paragraph -> block.runs.forEach { add(it.href) }
                is CookedBlock.Picture -> { add(block.url); add(block.originalUrl) }
                is CookedBlock.ImageGrid -> block.pictures.forEach { add(it.url); add(it.originalUrl) }
                is CookedBlock.Quote -> visit(block.blocks)
                is CookedBlock.Details -> visit(block.blocks)
                is CookedBlock.ListBlock -> block.entries.forEach(::visit)
                is CookedBlock.Table -> block.rows.forEach { row -> row.forEach { visit(it.blocks) } }
                is CookedBlock.Video -> add(block.url)
                is CookedBlock.Audio -> add(block.url)
                else -> Unit
            }
        }
    }
    visit(blocks)
    return uploads.filter { upload ->
        listOfNotNull(contentUrl(upload.url), upload.thumbnailUrl?.let(::contentUrl)).none { it in rendered }
    }
}

@Composable
fun ChatRoomScreen(
    channel: ChatChannelSummary,
    state: ChatUiState,
    active: Boolean,
    currentUsername: String? = null,
    emojiUrls: Map<String, String> = emptyMap(),
    onLoad: (Long, Boolean) -> Unit,
    onLoadPast: (Long) -> Unit = {},
    onSetChannelMuted: (Long, Boolean) -> Unit,
    onMessagesVisible: (Long, Long) -> Unit = { _, _ -> },
    onSend: (Long, String, List<Long>, () -> Unit) -> Unit,
    onUploadAttachment: (Long, Uri, (AttachedMedia) -> Unit, (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onOpenUser: (String) -> Unit = {},
    onOpenTopic: (Long, String, Int?) -> Unit = { _, _, _ -> },
    onOpenExternalUrl: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    val channelId = channel.id
    val messages = state.messagesByChannel[channelId].orEmpty()
    val listState = rememberLazyListState()
    var scrollToBottomRequest by remember(channelId, currentUsername) { mutableIntStateOf(0) }
    var draft by rememberSaveable(channelId) { mutableStateOf("") }
    var showComposerSheet by remember(channelId) { mutableStateOf(false) }
    var composerInitialFullscreen by remember(channelId) { mutableStateOf(false) }
    var pendingAttachments by remember(channelId) { mutableStateOf<List<AttachedMedia>>(emptyList()) }
    var isUploading by remember(channelId) { mutableStateOf(false) }
    var chatGallery by remember(channelId) { mutableStateOf<Pair<List<ImageViewerItem>, Int>?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun handleAttachUri(uri: Uri) {
        isUploading = true
        onUploadAttachment(channelId, uri, { media ->
            isUploading = false
            pendingAttachments = pendingAttachments + media
        }, {
            isUploading = false
        })
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) handleAttachUri(uri)
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) handleAttachUri(uri)
    }

    LaunchedEffect(active, channelId) {
        if (!active) return@LaunchedEffect
        onLoad(channelId, false)
    }

    LaunchedEffect(active, channelId, messages, channel.membership?.lastReadMessageId) {
        if (!active || channel.isDirectMessage) return@LaunchedEffect
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
    var appliedBottomRequest by remember(channelId, currentUsername) { mutableIntStateOf(0) }
    val canLoadMorePast = state.canLoadMorePastByChannel[channelId] ?: true
    val isLoadingPast = state.loadingPastChannelId == channelId

    LaunchedEffect(messages, scrollToBottomRequest) {
        if (channel.isDirectMessage) return@LaunchedEffect
        if (messages.isEmpty()) return@LaunchedEffect
        val currentFirstId = messages.first().id
        val currentLastId = messages.last().id
        if (!initialScrollDone || scrollToBottomRequest != appliedBottomRequest) {
            initialScrollDone = true
            val headerCount = if (isLoadingPast || (!canLoadMorePast && messages.size >= 50)) 1 else 0
            listState.scrollToItem(messages.lastIndex + headerCount)
            appliedBottomRequest = scrollToBottomRequest
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
        if (channel.isDirectMessage) return@LaunchedEffect
        if (firstVisibleIndex <= 2 && canLoadMorePast && !isLoadingPast && messages.isNotEmpty() && initialScrollDone) {
            onLoadPast(channelId)
        }
    }

    val showSenderNames = !channel.isOneOnOneDm
    val canSendInline = (draft.isNotBlank() || pendingAttachments.isNotEmpty()) &&
        !isUploading && state.sendingChannelId == null

    fun submitChatDraft(raw: String) {
        // 编辑器导出后 Compose 可能尚未重组，不能用上一帧 canSendInline 拦截新正文。
        if (isUploading || state.sendingChannelId != null) return
        val submittedAttachments = pendingAttachments
        val submission = prepareChatMessage(raw, submittedAttachments) ?: return
        onSend(channelId, submission.message, submission.uploadIds) {
            // 主动发送成功应定位新消息，不受之前阅读位置或输入法收起时序限制。
            scrollToBottomRequest++
            if (draft == raw) draft = ""
            val sentIds = submittedAttachments.map { it.id }.toSet()
            pendingAttachments = pendingAttachments.filterNot { it.id in sentIds }
            if (draft.isBlank() && pendingAttachments.isEmpty()) {
                showComposerSheet = false
                composerInitialFullscreen = false
                keyboard?.hide()
            }
        }
    }

    @Composable
    fun MessageEntry(index: Int, message: ChatMessageSummary) {
        val prev = messages.getOrNull(index - 1)
        val next = messages.getOrNull(index + 1)
        val showDivider = shouldShowTimeDivider(prev?.createdAt, message.createdAt)
        val sameAsPrev = !showDivider && isSameBubbleGroup(prev, message)
        val sameAsNext = next != null && isSameBubbleGroup(message, next)
        val isMe = currentUsername != null && message.user?.username == currentUsername

        Column(Modifier.fillMaxWidth()) {
            if (showDivider) {
                DisableSelection {
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
            }
            ChatMessageBubble(
                message = message,
                isMe = isMe,
                emojiUrls = emojiUrls,
                showSenderName = showSenderNames && !sameAsPrev,
                groupedWithPrevious = sameAsPrev,
                groupedWithNext = sameAsNext,
                onPicture = { images, idx -> chatGallery = images to idx },
                onOpenTopic = onOpenTopic,
                onOpenExternalUrl = onOpenExternalUrl,
                onOpenUser = onOpenUser,
            )
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
                    Text(
                        org.linuxdo.android.net.UserErrorMessage.sanitize(state.error, "加载失败，请稍后重试"),
                        color = IosTheme.colors.secondaryLabel,
                    )
                    org.linuxdo.android.ui.design.NetworkSettingsLink()
                }
            }

            // 消息气泡列表
            if (state.loadingChannelId == channelId && messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IosActivityIndicator(diameter = 24.dp, color = IosTheme.colors.accent)
                }
            } else if (channel.isDirectMessage) {
                key(channelId, currentUsername) {
                    SelectableChatMessages(
                        messageIds = remember(messages) { messages.map { it.id } },
                        scrollToBottomRequest = scrollToBottomRequest,
                        active = active,
                        loadingPast = isLoadingPast,
                        canLoadPast = canLoadMorePast,
                        onLoadPast = { onLoadPast(channelId) },
                        onMessagesVisible = { onMessagesVisible(channelId, it) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        header = {
                            if (isLoadingPast || (!canLoadMorePast && messages.size >= 50)) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    if (isLoadingPast) {
                                        IosActivityIndicator(diameter = 18.dp, color = IosTheme.colors.accent)
                                    } else {
                                        Text("已加载全部历史消息", style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
                                    }
                                }
                            }
                        },
                    ) { index -> MessageEntry(index, messages[index]) }
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
                        MessageEntry(index, message)
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
                                    pickFile.launch(arrayOf("*/*"))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ComposerAttachmentIcon(tint = if (isUploading) IosTheme.colors.tertiaryLabel else IosTheme.colors.label)
                        }

                        // 底部精简输入条：图片 / 附件 + 点击直接拉起回帖同款 MarkdownComposerSheet（支持常态精简、Aa、全屏工具栏、实时预览）+ 发送
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(IosTheme.colors.fieldBackground)
                                .clickable(enabled = state.sendingChannelId == null) {
                                    composerInitialFullscreen = false
                                    showComposerSheet = true
                                }
                                .padding(horizontal = 13.dp, vertical = 8.dp)
                                .semantics { contentDescription = "输入聊天消息" },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = if (draft.isEmpty()) "发送消息… 支持 Markdown" else draft.replace('\n', ' '),
                                    style = TextStyle(
                                        fontSize = 14.5.sp,
                                        color = if (draft.isEmpty()) IosTheme.colors.tertiaryLabel else IosTheme.colors.label,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "Aa",
                                    style = TextStyle(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IosTheme.colors.secondaryLabel,
                                    ),
                                )
                            }
                        }

                        // 发送药丸按钮
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (canSendInline) IosTheme.colors.accent else IosTheme.colors.accent.copy(alpha = 0.35f))
                                .clickable(enabled = canSendInline) { submitChatDraft(draft) }
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

        // 复用回帖 MarkdownComposerSheet（常态精简 Aa 工具条 + 全屏云端工具栏 + 实时预览）
        if (showComposerSheet) {
            MarkdownComposerSheet(
                sessionKey = "chat_$channelId",
                heading = "发送给 ${channel.displayTitle.ifBlank { "直接消息" }}",
                sendLabel = "发送",
                placeholder = "发送消息… 支持 Markdown",
                draft = draft,
                attachments = pendingAttachments,
                emojiUrls = emojiUrls,
                submitting = state.sendingChannelId == channelId,
                uploading = isUploading,
                canSubmit = canSendInline,
                error = state.error,
                allowPoll = false,
                initialFullScreen = composerInitialFullscreen,
                onDraftChange = { draft = it },
                onSubmit = ::submitChatDraft,
                onClose = {
                    keyboard?.hide()
                    showComposerSheet = false
                },
                onAttach = ::handleAttachUri,
                onRemoveAttachment = { removeId ->
                    pendingAttachments = pendingAttachments.filterNot { it.id == removeId }
                },
            )
        }

        chatGallery?.let { (images, idx) ->
            ImageViewer(images = images, initialIndex = idx) { chatGallery = null }
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
    emojiUrls: Map<String, String>,
    showSenderName: Boolean,
    groupedWithPrevious: Boolean,
    groupedWithNext: Boolean,
    onPicture: (List<ImageViewerItem>, Int) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val user = message.user
    val blocks = remember(message.id, message.message, message.cooked, emojiUrls) {
        parseChatBubbleBlocks(message, emojiUrls)
    }
    val timeStr = remember(message.createdAt) { formatBubbleClock(message.createdAt) }
    val remainingUploads = remember(message.uploads, blocks) { unrenderedChatUploads(message.uploads, blocks) }
    val imageUploads = remainingUploads.filter { it.isImage }
    val fileUploads = remainingUploads.filterNot { it.isImage }
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
                    .background(if (IosTheme.colors.isDark) Color(0xFF1E40AF) else Color(0xFF1D4ED8))
                    .padding(horizontal = 13.dp, vertical = 8.5.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (blocks.isNotEmpty()) {
                        ChatMarkdownContent(
                            blocks = blocks,
                            onAccent = true,
                            onPicture = onPicture,
                            onOpenTopic = onOpenTopic,
                            onOpenExternalUrl = onOpenExternalUrl,
                        )
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
                    DisableSelection {
                        fileUploads.forEach { upload ->
                            Text(
                                text = "📎 ${upload.originalFilename}",
                                style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.88f)),
                            )
                        }
                    }
                }
            }
            if (!groupedWithNext && timeStr.isNotBlank()) {
                DisableSelection {
                    Text(
                        text = timeStr,
                        style = TextStyle(fontSize = 10.5.sp, color = IosTheme.colors.tertiaryLabel),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
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
                    DisableSelection {
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
                }

                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(if (IosTheme.colors.isDark) Color(0xFF30343B) else Color.White)
                        .border(
                            0.8.dp,
                            if (IosTheme.colors.isDark) Color(0xFF555D69) else Color(0xFFCBD5E1),
                            bubbleShape,
                        )
                        .padding(horizontal = 13.dp, vertical = 8.5.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (blocks.isNotEmpty()) {
                            ChatMarkdownContent(
                                blocks = blocks,
                                onAccent = false,
                                onPicture = onPicture,
                                onOpenTopic = onOpenTopic,
                                onOpenExternalUrl = onOpenExternalUrl,
                            )
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
                        DisableSelection {
                            fileUploads.forEach { upload ->
                                Text(
                                    text = "📎 ${upload.originalFilename}",
                                    style = TextStyle(fontSize = 13.sp, color = IosTheme.colors.secondaryLabel),
                                )
                            }
                        }
                    }
                }

                if (!groupedWithNext && timeStr.isNotBlank()) {
                    DisableSelection {
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
}

/**
 * 聊天气泡内 Markdown 渲染组件：
 * - 我方蓝色气泡（onAccent = true）自动适配纯白文字、半透明浅白代码块/引用条与下划线链接；
 * - 对方气泡（onAccent = false）适配主题正文色、强调色链接与浅底代码块/引用块；
 * - 完整支持段落富文本（粗/斜/删/行内代码/小字/剧透点击展开/表情/链接）、标题、代码块、引用、列表、表格、折叠详情、分割线与图片预览。
 */
@Composable
private fun ChatMarkdownContent(
    blocks: List<CookedBlock>,
    onAccent: Boolean,
    onPicture: (List<ImageViewerItem>, Int) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
) {
    val colors = IosTheme.colors
    val textColor = if (onAccent) Color.White else colors.label
    val secondaryColor = if (onAccent) Color.White.copy(alpha = 0.80f) else colors.secondaryLabel
    val codeBg = if (onAccent) Color.White.copy(alpha = 0.18f) else colors.fieldBackground
    val quoteBg = if (onAccent) Color.White.copy(alpha = 0.14f) else colors.fieldBackground
    val quoteBar = if (onAccent) Color.White.copy(alpha = 0.72f) else colors.accent.copy(alpha = 0.65f)
    val dividerColor = if (onAccent) Color.White.copy(alpha = 0.28f) else colors.separator
    val allImages = remember(blocks) { extractChatBubbleImages(blocks) }

    ContinuousTextSelection(onCopied = {}, onQuote = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is CookedBlock.Paragraph -> ChatBubbleRichParagraph(
                    runs = block.runs,
                    onAccent = onAccent,
                    onPicture = onPicture,
                    onOpenTopic = onOpenTopic,
                    onOpenExternalUrl = onOpenExternalUrl,
                )

                is CookedBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 17.5.sp
                        2 -> 16.5.sp
                        3 -> 15.5.sp
                        else -> 14.8.sp
                    }
                    Text(
                        text = block.title,
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            lineHeight = (fontSize.value + 5f).sp,
                        ),
                    )
                }

                is CookedBlock.Code -> {
                    Text(
                        text = block.text,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            fontSize = 12.8.sp,
                            lineHeight = 18.sp,
                            color = textColor,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBg)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }

                is CookedBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(quoteBg)
                            .padding(end = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .heightIn(min = 24.dp)
                                .background(quoteBar),
                        )
                        Box(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
                            ChatMarkdownContent(block.blocks, onAccent, onPicture, onOpenTopic, onOpenExternalUrl)
                        }
                    }
                }

                is CookedBlock.Details -> {
                    var expanded by remember(block.id) { mutableStateOf(block.initiallyOpen) }
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(quoteBg)
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.clickable { expanded = !expanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            DisableSelection {
                                Text(
                                    text = if (expanded) "▾" else "▸",
                                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = secondaryColor),
                                )
                            }
                            ChatBubbleRichParagraph(
                                runs = block.summary.ifEmpty { listOf(InlineText("总结")) },
                                onAccent = onAccent,
                                onPicture = onPicture,
                                onOpenTopic = onOpenTopic,
                                onOpenExternalUrl = onOpenExternalUrl,
                                boldOverride = true,
                            )
                        }
                        if (expanded && block.blocks.isNotEmpty()) {
                            ChatMarkdownContent(block.blocks, onAccent, onPicture, onOpenTopic, onOpenExternalUrl)
                        }
                    }
                }

                is CookedBlock.ListBlock -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.entries.forEachIndexed { idx, entryBlocks ->
                            val marker = block.start?.let { "${it + idx}." } ?: "•"
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = marker,
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = secondaryColor),
                                )
                                Box(Modifier.weight(1f, fill = false)) {
                                    ChatMarkdownContent(entryBlocks, onAccent, onPicture, onOpenTopic, onOpenExternalUrl)
                                }
                            }
                        }
                    }
                }

                is CookedBlock.Table -> {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBg)
                            .horizontalScroll(rememberScrollState())
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        block.rows.forEachIndexed { rIdx, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { cell ->
                                    Box(Modifier.width(128.dp * cell.columnSpan)) {
                                        ChatMarkdownContent(cell.blocks, onAccent, onPicture, onOpenTopic, onOpenExternalUrl)
                                    }
                                }
                            }
                            if (rIdx < block.rows.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(dividerColor))
                            }
                        }
                    }
                }

                is CookedBlock.Picture -> DisableSelection {
                    val idx = allImages.indexOfFirst { it.url == block.originalUrl || it.previewUrl == block.url }.coerceAtLeast(0)
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.description.ifBlank { "图片" },
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val gallery = allImages.ifEmpty {
                                    listOf(ImageViewerItem(block.originalUrl, block.url, block.description))
                                }
                                onPicture(gallery, idx.coerceIn(0, gallery.lastIndex))
                            },
                        contentScale = ContentScale.FillWidth,
                    )
                }

                is CookedBlock.ImageGrid -> DisableSelection {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.pictures.forEach { pic ->
                            val idx = allImages.indexOfFirst { it.url == pic.originalUrl || it.previewUrl == pic.url }.coerceAtLeast(0)
                            AsyncImage(
                                model = pic.url,
                                contentDescription = pic.description.ifBlank { "图片" },
                                modifier = Modifier
                                    .widthIn(max = 200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val gallery = allImages.ifEmpty {
                                            listOf(ImageViewerItem(pic.originalUrl, pic.url, pic.description))
                                        }
                                        onPicture(gallery, idx.coerceIn(0, gallery.lastIndex))
                                    },
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                    }
                }

                is CookedBlock.Onebox -> DisableSelection {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(quoteBg)
                            .clickable { onOpenExternalUrl(block.url) }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = block.title.ifBlank { block.url },
                            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = textColor),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        block.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                text = desc,
                                style = TextStyle(fontSize = 12.sp, color = secondaryColor),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                is CookedBlock.Video -> DisableSelection {
                    Text(
                        text = "🎬 ${block.title}",
                        style = TextStyle(fontSize = 13.5.sp, color = textColor, textDecoration = TextDecoration.Underline),
                        modifier = Modifier.clickable { onOpenExternalUrl(block.url) },
                    )
                }

                is CookedBlock.Audio -> DisableSelection {
                    Text(
                        text = "🎵 ${block.title}",
                        style = TextStyle(fontSize = 13.5.sp, color = textColor, textDecoration = TextDecoration.Underline),
                        modifier = Modifier.clickable { onOpenExternalUrl(block.url) },
                    )
                }

                is CookedBlock.Poll -> DisableSelection {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(quoteBg)
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "📊 ${block.title.ifBlank { "投票" }}",
                            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = textColor),
                        )
                        block.options.forEach { opt ->
                            Text(
                                text = "• ${opt.title}",
                                style = TextStyle(fontSize = 12.5.sp, color = secondaryColor),
                            )
                        }
                    }
                }

                CookedBlock.Divider -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(dividerColor),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ChatBubbleRichParagraph(
    runs: List<InlineText>,
    onAccent: Boolean,
    onPicture: (List<ImageViewerItem>, Int) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    boldOverride: Boolean = false,
) {
    val colors = IosTheme.colors
    val context = LocalContext.current
    val updatedOpenExternal by rememberUpdatedState(onOpenExternalUrl)
    val updatedOpenTopic by rememberUpdatedState(onOpenTopic)
    val hasSpoiler = remember(runs) { runs.any { it.spoiler } }
    var revealedSpoilers by remember(runs) { mutableStateOf(false) }

    val textColor = if (onAccent) Color.White else colors.label
    val secondaryColor = if (onAccent) Color.White.copy(alpha = 0.80f) else colors.secondaryLabel
    val codeBg = if (onAccent) Color.White.copy(alpha = 0.22f) else colors.fieldBackground
    val spoilerHiddenBg = if (onAccent) Color.White.copy(alpha = 0.55f) else colors.secondaryLabel.copy(alpha = 0.78f)
    val linkColor = if (onAccent) Color.White else colors.accent

    val annotated = remember(runs, onAccent, colors, revealedSpoilers, boldOverride) {
        buildAnnotatedString {
            runs.forEach { run ->
                val emoji = run.emojiUrl
                if (emoji != null) {
                    appendInlineContent(emoji, run.text.ifBlank { "表情" })
                    return@forEach
                }
                val isHiddenSpoiler = run.spoiler && !revealedSpoilers
                val span = SpanStyle(
                    fontWeight = if (run.bold || boldOverride) FontWeight.Bold else null,
                    fontStyle = if (run.italic) FontStyle.Italic else null,
                    fontFamily = if (run.code) FontFamily.Monospace else null,
                    fontSize = if (run.small) 12.sp else TextUnit.Unspecified,
                    textDecoration = if (run.strikethrough) TextDecoration.LineThrough else null,
                    color = when {
                        isHiddenSpoiler -> Color.Transparent
                        run.small -> secondaryColor
                        else -> Color.Unspecified
                    },
                    background = when {
                        isHiddenSpoiler -> spoilerHiddenBg
                        run.spoiler -> codeBg
                        run.code -> codeBg
                        else -> Color.Unspecified
                    },
                )
                withStyle(span) {
                    val link = run.href
                    if (link == null || isHiddenSpoiler) {
                        append(run.text)
                    } else {
                        withLink(
                            LinkAnnotation.Url(
                                url = link,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = if (onAccent) TextDecoration.Underline else null,
                                    ),
                                ),
                                linkInteractionListener = {
                                    if (run.isAttachment) {
                                        AttachmentDownloadManager.enqueue(
                                            context = context,
                                            url = link,
                                            suggestedName = run.text.trim(),
                                        )
                                    } else {
                                        val internalTarget = org.linuxdo.android.data.parseInternalTopicUrl(link)
                                        if (internalTarget != null) {
                                            updatedOpenTopic(internalTarget.topicId, run.text.ifBlank { "话题 #${internalTarget.topicId}" }, internalTarget.postNumber)
                                        } else {
                                            updatedOpenExternal(link)
                                        }
                                    }
                                },
                            ),
                        ) {
                            append(run.text)
                        }
                    }
                }
            }
        }
    }

    val inline = remember(runs) {
        runs.mapNotNull { it.emojiUrl }.distinct().associateWith { url ->
            InlineTextContent(Placeholder(18.sp, 18.sp, PlaceholderVerticalAlign.TextCenter)) {
                AsyncImage(url, "表情", modifier = Modifier.fillMaxSize())
            }
        }
    }

    val spoilerModifier = if (hasSpoiler) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { revealedSpoilers = !revealedSpoilers }
    } else {
        Modifier
    }

    Text(
        text = annotated,
        style = TextStyle(fontSize = 14.8.sp, color = textColor, lineHeight = 20.5.sp),
        inlineContent = inline,
        modifier = spoilerModifier,
    )
}

private fun extractChatBubbleImages(blocks: List<CookedBlock>): List<ImageViewerItem> {
    val result = mutableListOf<ImageViewerItem>()
    fun walk(list: List<CookedBlock>) {
        list.forEach { b ->
            when (b) {
                is CookedBlock.Picture -> result += ImageViewerItem(b.originalUrl, b.url, b.description)
                is CookedBlock.ImageGrid -> b.pictures.forEach { p ->
                    result += ImageViewerItem(p.originalUrl, p.url, p.description)
                }
                is CookedBlock.Quote -> walk(b.blocks)
                is CookedBlock.Details -> walk(b.blocks)
                is CookedBlock.ListBlock -> b.entries.forEach(::walk)
                is CookedBlock.Table -> b.rows.forEach { row -> row.forEach { walk(it.blocks) } }
                else -> Unit
            }
        }
    }
    walk(blocks)
    return result
}
