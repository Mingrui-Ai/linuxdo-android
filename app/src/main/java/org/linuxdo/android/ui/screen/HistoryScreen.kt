package org.linuxdo.android.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosNavBar
import org.linuxdo.android.ui.design.ListNavigationHeader
import org.linuxdo.android.ui.design.MessageListRefresh
import org.linuxdo.android.ui.design.scrollListBars
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.Instant
import org.linuxdo.android.data.ReadingHistoryEntry
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.avatarShape
import org.linuxdo.android.ui.format.RelativeTime

@Composable
fun HistoryScreen(
    entries: List<ReadingHistoryEntry>,
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    hasMore: Boolean,
    error: String?,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onBack: () -> Unit,
) {
    val list = rememberLazyListState()
    var searchExpanded by rememberSaveable { mutableStateOf(query.isNotBlank()) }
    var refreshing by remember { mutableStateOf(false) }
    var loadingOlder by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val canLoadMore by rememberUpdatedState(hasMore && !loading && error == null)
    val loadMore by rememberUpdatedState(onLoadMore)
    val filtered = remember(entries, query) {
        entries.filter { entry ->
            query.isBlank() || entry.title.contains(query.trim(), ignoreCase = true) || entry.topicId.toString().contains(query.trim())
        }
    }

    LaunchedEffect(loading) {
        if (!loading) { refreshing = false; loadingOlder = false }
    }
    LaunchedEffect(list) {
        snapshotFlow {
            val total = list.layoutInfo.totalItemsCount
            val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            canLoadMore && list.isScrollInProgress && total > 0 && last >= total - 3
        }.distinctUntilChanged().filter { it }.collect {
            loadingOlder = true
            loadMore()
        }
    }

    Column(Modifier.fillMaxSize().background(IosTheme.colors.groupedBackground)) {
        ListNavigationHeader {
            IosNavBar(
                title = "浏览历史", onBack = onBack, backLabel = "返回",
                trailing = {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .clickable {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded) { onQueryChange(""); focusManager.clearFocus() }
                            }
                            .semantics { contentDescription = if (searchExpanded) "关闭历史搜索" else "搜索历史" },
                        contentAlignment = Alignment.Center,
                    ) {
                        HistorySearchIcon(
                            if (searchExpanded) IosTheme.colors.accent else IosTheme.colors.label,
                            Modifier.size(22.dp),
                        )
                    }
                },
            )
            if (searchExpanded) {
                LaunchedEffect(Unit) { focus.requestFocus() }
                Row(
                    Modifier.fillMaxWidth().background(IosTheme.colors.card).padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(50)).background(IosTheme.colors.fieldBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query, onValueChange = onQueryChange, singleLine = true,
                        textStyle = IosTheme.type.body.copy(color = IosTheme.colors.label, fontSize = 14.sp),
                        cursorBrush = SolidColor(IosTheme.colors.accent),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        decorationBox = { field ->
                            Box {
                                if (query.isEmpty()) Text("搜索标题或话题编号", style = IosTheme.type.body.copy(fontSize = 14.sp),
                                    color = IosTheme.colors.secondaryLabel)
                                field()
                            }
                        },
                    )
                    if (query.isNotEmpty()) Box(
                        Modifier.size(24.dp).clip(CircleShape).clickable { onQueryChange("") }
                            .semantics { contentDescription = "清除搜索内容" },
                        contentAlignment = Alignment.Center,
                    ) { Text("✕", fontSize = 12.sp, color = IosTheme.colors.secondaryLabel) }
                }
            }
        }
        MessageListRefresh(
            refreshing = refreshing && loading, active = !loading,
            onRefresh = { refreshing = true; loadingOlder = false; onRefresh() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                state = list, modifier = Modifier.fillMaxSize().scrollListBars(list),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                error?.let { message ->
                    item("history-error") {
                        Text("$message · 下拉重试", modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel)
                    }
                }
                if (loading && entries.isEmpty()) {
                    item("history-loading") { IosLoadingBox() }
                } else if (filtered.isEmpty()) {
                    item("history-empty") {
                        Text(if (entries.isEmpty()) "暂无浏览历史，下拉刷新" else "没有匹配的话题",
                            modifier = Modifier.fillMaxWidth().padding(24.dp), style = IosTheme.type.footnote,
                            color = IosTheme.colors.secondaryLabel)
                    }
                }
                if (filtered.isNotEmpty()) item("history-heading") {
                    Text("最近浏览", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        style = IosTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.secondaryLabel)
                }
                items(filtered, key = { it.topicId }) { entry ->
                    HistoryRow(entry = entry, query = query) {
                        onOpenTopic(entry.topicId, entry.title, entry.lastSeenPostNumber.takeIf { it > 1 })
                    }
                }
                if (hasMore || loadingOlder) item("history-more") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (loading && loadingOlder) IosActivityIndicator(diameter = 22.dp)
                        else Text(if (error != null) "下拉重试" else "继续上滑加载更早记录",
                            style = IosTheme.type.footnote, color = IosTheme.colors.tertiaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: ReadingHistoryEntry,
    query: String,
    onClick: () -> Unit,
) {
    val needle = query.trim()
    val accent = IosTheme.colors.accent
    val titleText = remember(entry.title, needle, accent) {
        if (needle.isEmpty()) {
            AnnotatedString(entry.title)
        } else {
            buildAnnotatedString {
                val raw = entry.title
                var start = raw.indexOf(needle, ignoreCase = true)
                var cursor = 0
                while (start >= 0) {
                    append(raw.substring(cursor, start))
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                        append(raw.substring(start, start + needle.length))
                    }
                    cursor = start + needle.length
                    start = raw.indexOf(needle, cursor, ignoreCase = true)
                }
                append(raw.substring(cursor))
            }
        }
    }

    val avatarUrl = remember(entry.avatarTemplate) {
        entry.avatarTemplate?.let { avatarUrlOf(it, size = 96) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IosTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 第一行：作者头像 + 作者名 / 编号 + 右侧分类药丸
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = entry.authorUsername,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(avatarShape(entry.authorUsername, avatarUrl, 3.5.dp))
                            .background(IosTheme.colors.fieldBackground),
                    )
                    Spacer(Modifier.width(6.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(IosTheme.colors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (entry.authorUsername?.firstOrNull()?.uppercase() ?: "#"),
                            style = IosTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = IosTheme.colors.accent,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }

                Text(
                    text = entry.authorUsername ?: "#${entry.topicId}",
                    style = IosTheme.type.footnote,
                    color = IosTheme.colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!entry.categoryName.isNullOrBlank()) {
                val catColor = parseHexColor(entry.categoryColor) ?: IosTheme.colors.accent
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(catColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(catColor),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = entry.categoryName,
                        style = IosTheme.type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = catColor,
                        maxLines = 1,
                    )
                }
            }
        }

        // 第二行：标题 (对齐默认列表与搜索样式)
        Text(
            text = titleText,
            style = IosTheme.type.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
            color = IosTheme.colors.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // 第三行：底部元信息 (读到第 X 楼、回复数、时间、ID 药丸)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.lastSeenPostNumber > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IosTheme.colors.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "读到第 ${entry.lastSeenPostNumber} 楼",
                            style = IosTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = IosTheme.colors.accent,
                        )
                    }
                }

                if (entry.totalReplies > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HistoryReplyGlyph(IosTheme.colors.tertiaryLabel)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "${entry.totalReplies}",
                            style = IosTheme.type.caption.copy(fontSize = 11.sp),
                            color = IosTheme.colors.secondaryLabel,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HistoryClockGlyph(IosTheme.colors.tertiaryLabel)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (entry.lastSeenAt > 0) RelativeTime.format(Instant.ofEpochMilli(entry.lastSeenAt).toString()) else "网页同步",
                        style = IosTheme.type.caption.copy(fontSize = 11.sp),
                        color = IosTheme.colors.secondaryLabel,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (!entry.authorUsername.isNullOrBlank()) {
                Text(
                    text = "#${entry.topicId}",
                    style = IosTheme.type.caption.copy(fontSize = 10.sp),
                    color = IosTheme.colors.tertiaryLabel,
                )
            }
        }
    }
    IosDivider(startIndent = 16.dp)
}

@Composable
private fun HistoryReplyGlyph(color: Color) {
    Canvas(Modifier.size(11.dp)) {
        val bubbleHeight = size.height * 0.74f
        drawRoundRect(
            color = color,
            size = Size(size.width, bubbleHeight),
            cornerRadius = CornerRadius(size.width * 0.24f),
            style = Stroke(size.width * 0.12f),
        )
        val tail = Path().apply {
            moveTo(size.width * 0.26f, bubbleHeight * 0.92f)
            lineTo(size.width * 0.3f, size.height)
            lineTo(size.width * 0.54f, bubbleHeight * 0.92f)
            close()
        }
        drawPath(tail, color)
    }
}

@Composable
private fun HistoryClockGlyph(color: Color) {
    Canvas(Modifier.size(10.dp)) {
        val stroke = Stroke(width = size.width * 0.12f, cap = StrokeCap.Round)
        drawCircle(color, radius = size.width * 0.44f, style = stroke)
        drawLine(
            color,
            start = Offset(size.width / 2f, size.height / 2f),
            end = Offset(size.width / 2f, size.height * 0.22f),
            strokeWidth = size.width * 0.12f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            start = Offset(size.width / 2f, size.height / 2f),
            end = Offset(size.width * 0.72f, size.height / 2f),
            strokeWidth = size.width * 0.12f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun HistorySearchIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(tint, radius = 6.5f, center = Offset(10.5f, 10.5f), style = stroke)
            drawLine(tint, Offset(15.5f, 15.5f), Offset(21f, 21f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val clean = hex.removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return null
    return if (clean.length == 6) Color(0xFF000000 or value) else Color(value)
}

