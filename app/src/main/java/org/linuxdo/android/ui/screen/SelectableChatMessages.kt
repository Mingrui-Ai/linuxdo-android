package org.linuxdo.android.ui.screen

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import org.linuxdo.android.ui.design.ContinuousTextSelection

internal data class ChatMessageBounds(val id: Long, val top: Int, val bottom: Int)
internal data class ChatSelectionLayout(
    val ids: List<Long> = emptyList(),
    val bounds: List<ChatMessageBounds> = emptyList(),
)

internal data class ChatSelectionScrollRequest(
    val followEnd: Boolean,
    val anchorId: Long? = null,
    val anchorOffset: Int = 0,
    val fallbackOffset: Int = 0,
) {
    fun resolve(bounds: List<ChatMessageBounds>, maxScroll: Int, currentOffset: Int = fallbackOffset): Int {
        if (followEnd) return maxScroll.coerceAtLeast(0)
        val anchor = bounds.firstOrNull { it.id == anchorId }
        // Preserve any finger movement since this request was captured.
        val delta = anchor?.let { it.top + anchorOffset - fallbackOffset } ?: 0
        return (currentOffset + delta).coerceIn(0, maxScroll.coerceAtLeast(0))
    }
}

internal fun chatSelectionScrollRequest(
    previous: ChatSelectionLayout,
    nextIds: List<Long>,
    offset: Int,
    viewportHeight: Int,
    selecting: Boolean,
    followThreshold: Int,
    forceFollowEnd: Boolean = false,
): ChatSelectionScrollRequest {
    if (forceFollowEnd || previous.ids.isEmpty()) return ChatSelectionScrollRequest(followEnd = true)
    val nextSet = nextIds.toSet()
    val retained = previous.bounds.filter { it.id in nextSet }
    val anchor = retained.firstOrNull { it.bottom > offset } ?: retained.lastOrNull()
    val prepended = previous.ids.first() in nextSet && nextIds.firstOrNull() != previous.ids.first()
    val appended = previous.ids.last() in nextSet && nextIds.lastOrNull() != previous.ids.last()
    val atEnd = (previous.bounds.lastOrNull()?.bottom ?: 0) - offset - viewportHeight <= followThreshold
    return ChatSelectionScrollRequest(
        followEnd = !selecting && !prepended && (appended || previous.ids == nextIds) && atEnd,
        anchorId = anchor?.id,
        anchorOffset = anchor?.let { offset - it.top } ?: 0,
        fallbackOffset = offset,
    )
}

internal fun visibleChatSelectionMessages(bounds: List<ChatMessageBounds>, offset: Int, height: Int): List<Long> =
    if (height <= 0) emptyList() else bounds.filter { it.bottom > offset && it.top < offset + height }.map { it.id }

internal data class ChatContentPlacement(val height: Int, val positions: List<Int>, val fitsViewport: Boolean)

internal fun chatContentPlacement(childHeights: List<Int>, messageCount: Int, viewportHeight: Int, gap: Int): ChatContentPlacement {
    var contentHeight = 0
    val positions = childHeights.mapIndexed { index, height ->
        contentHeight.also {
            contentHeight += height
            if (index in 1..messageCount) contentHeight += gap
        }
    }
    val height = maxOf(contentHeight, viewportHeight)
    val leadingSpace = height - contentHeight
    return ChatContentPlacement(height, positions.map { it + leadingSpace }, contentHeight <= viewportHeight)
}

internal fun chatShortDragOffset(current: Float, delta: Float, limit: Float): Float =
    (current + delta * 0.35f).coerceIn(-limit.coerceAtLeast(0f), limit.coerceAtLeast(0f))

/**
 * Keep the loaded conversation in one selection tree. A SelectionContainer around LazyColumn
 * silently omits recycled messages when copying a range or using Select All.
 * Pagination still fetches history on demand; only actual viewport intersections mark it read.
 */
@Composable
internal fun SelectableChatMessages(
    messageIds: List<Long>,
    scrollToBottomRequest: Int,
    active: Boolean,
    loadingPast: Boolean,
    canLoadPast: Boolean,
    onLoadPast: () -> Unit,
    onMessagesVisible: (Long) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    messageContent: @Composable (Int) -> Unit,
) {
    val scroll = rememberScrollState()
    var measured by remember { mutableStateOf(ChatSelectionLayout()) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    var appliedIds by remember { mutableStateOf<List<Long>?>(null) }
    var appliedBottomRequest by remember { mutableIntStateOf(0) }
    var geometryCorrection by remember { mutableStateOf<Pair<ChatSelectionLayout, ChatSelectionScrollRequest>?>(null) }
    var fitsViewport by remember { mutableStateOf(false) }
    var shortDragOffset by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 72.dp.roundToPx() }
    val dragLimit = with(LocalDensity.current) { 72.dp.toPx() }
    val platformOverscroll = rememberOverscrollEffect()
    val canDragShortContent by rememberUpdatedState(active && fitsViewport && messageIds.isNotEmpty() && !selecting)
    val shortDragConnection = remember(dragLimit) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!canDragShortContent || source != NestedScrollSource.UserInput) return Offset.Zero
                shortDragOffset = chatShortDragOffset(shortDragOffset, available.y, dragLimit)
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (shortDragOffset == 0f) return Velocity.Zero
                AnimationState(shortDragOffset).animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) {
                    shortDragOffset = value
                }
                return Velocity(0f, available.y)
            }
        }
    }
    LaunchedEffect(canDragShortContent, scrollToBottomRequest) {
        if (!canDragShortContent || scrollToBottomRequest != appliedBottomRequest) shortDragOffset = 0f
    }
    val currentOnVisible by rememberUpdatedState(onMessagesVisible)
    val currentOnLoadPast by rememberUpdatedState(onLoadPast)
    // Capture the old geometry before the new keyed messages are measured.
    val forceFollowEnd = remember(messageIds, scrollToBottomRequest) {
        measured.ids.isEmpty() || scrollToBottomRequest != appliedBottomRequest
    }
    val request = remember(messageIds, scrollToBottomRequest) {
        chatSelectionScrollRequest(
            measured, messageIds, scroll.value, viewportHeight, selecting, threshold, forceFollowEnd,
        )
    }

    LaunchedEffect(messageIds, scrollToBottomRequest) {
        appliedIds = null
        geometryCorrection = null
        snapshotFlow { measured.ids == messageIds && viewportHeight > 0 }.first { it }
        val target = request.resolve(measured.bounds, scroll.maxValue, scroll.value)
        if (request.followEnd) {
            if (forceFollowEnd || (!scroll.isScrollInProgress && !selecting)) scroll.scrollTo(target)
        } else {
            // Geometry compensation must not cancel an ongoing drag/fling.
            scroll.dispatchRawDelta((target - scroll.value).toFloat())
        }
        appliedBottomRequest = scrollToBottomRequest
        appliedIds = messageIds
    }
    LaunchedEffect(geometryCorrection) {
        val (layout, correction) = geometryCorrection ?: return@LaunchedEffect
        if (appliedIds == messageIds && measured == layout) {
            val target = correction.resolve(layout.bounds, scroll.maxValue, scroll.value)
            scroll.dispatchRawDelta((target - scroll.value).toFloat())
        }
        geometryCorrection = null
    }
    LaunchedEffect(active, messageIds) {
        if (!active) return@LaunchedEffect
        snapshotFlow {
            if (appliedIds != messageIds || geometryCorrection != null || shortDragOffset != 0f) null
            else visibleChatSelectionMessages(measured.bounds, scroll.value, viewportHeight).maxOrNull()
        }.distinctUntilChanged().collectLatest { lastVisible ->
            if (lastVisible != null) {
                delay(400)
                currentOnVisible(lastVisible)
            }
        }
    }
    LaunchedEffect(active, messageIds, loadingPast, canLoadPast) {
        if (!active || loadingPast || !canLoadPast || messageIds.isEmpty()) return@LaunchedEffect
        snapshotFlow { appliedIds == messageIds && geometryCorrection == null && scroll.value <= threshold }
            .distinctUntilChanged().collectLatest { nearTop ->
                if (nearTop) currentOnLoadPast()
            }
    }

    ContinuousTextSelection(
        // Copy clears the range but can retain native selection focus. Keep following paused
        // until focus leaves the conversation, including a second long-press after copying.
        onCopied = {},
        onQuote = null,
        modifier = modifier
            .onSizeChanged { viewportHeight = it.height }
            .onFocusChanged { selecting = it.hasFocus },
    ) {
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .nestedScroll(shortDragConnection)
                .verticalScroll(scroll, overscrollEffect = if (fitsViewport) null else platformOverscroll)
                .graphicsLayer { translationY = shortDragOffset }
                .padding(horizontal = 4.dp),
            content = {
                DisableSelection {
                    Box(Modifier.fillMaxWidth().padding(top = 10.dp)) { header() }
                }
                messageIds.forEachIndexed { index, id ->
                    key(id) { Box(Modifier.fillMaxWidth()) { messageContent(index) } }
                }
                Spacer(Modifier.height(10.dp))
            },
        ) { measurables, constraints ->
            val children = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val gap = 2.dp.roundToPx()
            // verticalScroll preserves the viewport minimum. Returning a smaller height lets
            // Compose center the coerced child, which stranded short conversations mid-screen.
            val placement = chatContentPlacement(children.map { it.height }, messageIds.size, constraints.minHeight, gap)
            val positions = placement.positions
            fitsViewport = placement.fitsViewport
            val next = ChatSelectionLayout(messageIds, messageIds.mapIndexed { index, id ->
                ChatMessageBounds(id, positions[index + 1], positions[index + 1] + children[index + 1].height)
            })
            if (measured != next) {
                // Spinner removal, an image resolving, or expanded Markdown can change heights
                // without changing IDs. Keep the same visible message at the same pixel offset.
                if (measured.ids == messageIds && appliedIds == messageIds) {
                    geometryCorrection = next to chatSelectionScrollRequest(
                        measured, messageIds, scroll.value, viewportHeight,
                        selecting || scroll.isScrollInProgress, threshold,
                    )
                }
                measured = next
            }
            layout(constraints.maxWidth, placement.height) {
                children.forEachIndexed { index, child -> child.placeRelative(0, positions[index]) }
            }
        }
    }
}
