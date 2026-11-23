package org.linuxdo.android.ui.design

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Stable
internal class CollapsingHeaderState(initialOffset: Float = 0f) {
    var offset by mutableFloatStateOf(initialOffset)
        private set
    var limit by mutableFloatStateOf(0f)
        private set

    fun updateLimit(value: Float) {
        if (value == limit) return
        val wasCollapsed = limit > 0f && offset >= limit
        limit = value.coerceAtLeast(0f)
        offset = if (wasCollapsed) limit else offset.coerceIn(0f, limit)
    }

    fun consume(delta: Float): Float {
        val previous = offset
        offset = (offset - delta).coerceIn(0f, limit)
        return previous - offset
    }

    fun canConsume(delta: Float) = if (delta < 0f) offset < limit else delta > 0f && offset > 0f
    fun expand() { offset = 0f }

    companion object {
        val Saver = Saver<CollapsingHeaderState, Float>(save = { it.offset }, restore = { CollapsingHeaderState(it) })
    }
}

@Composable
internal fun rememberCollapsingHeaderState() = rememberSaveable(saver = CollapsingHeaderState.Saver) {
    CollapsingHeaderState()
}

/** 头部先消费实际纵向位移，剩余位移/惯性继续交给列表；分类不参与收起。 */
@Composable
internal fun CollapsingHeaderLayout(
    state: CollapsingHeaderState,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    statusBarColor: Color = IosTheme.colors.groupedBackground,
    header: @Composable () -> Unit,
    pinnedHeader: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val decay = rememberSplineBasedDecay<Float>()
    val connection = remember(state, decay) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource) =
                Offset(0f, state.consume(available.y))

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!state.canConsume(available.y)) return Velocity.Zero
                var last = 0f
                var remaining = available.y
                AnimationState(initialValue = 0f, initialVelocity = available.y).animateDecay(decay) {
                    val delta = value - last
                    last = value
                    val consumed = state.consume(delta)
                    remaining = velocity
                    if (abs(consumed - delta) > 0.5f || !state.canConsume(velocity)) cancelAnimation()
                }
                // 在边界处保留剩余速度，让同一次甩动自然接到内容列表。
                return Velocity(0f, available.y - remaining)
            }
        }
    }
    // 空列表、加载态及头部空白也能纵向拖动；子列表优先接收手势。
    val emptyScroll = rememberScrollableState { 0f }
    Box(modifier.fillMaxSize().clipToBounds().nestedScroll(connection)
        .scrollable(emptyScroll, Orientation.Vertical)) {
        Column(Modifier.fillMaxSize()) {
            Layout(content = { Box { header() } }, modifier = Modifier.fillMaxWidth().clipToBounds()) { measurables, constraints ->
                val placeable = measurables.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                state.updateLimit((placeable.height - topInset.roundToPx()).coerceAtLeast(0).toFloat())
                val hidden = state.offset.roundToInt()
                layout(placeable.width, placeable.height - hidden) {
                    placeable.placeRelative(0, -hidden)
                }
            }
            pinnedHeader()
            Box(Modifier.fillMaxWidth().weight(1f)) { content() }
        }
        if (topInset > 0.dp) {
            Spacer(Modifier.fillMaxWidth().height(topInset).graphicsLayer {
                alpha = if (state.limit > 0f) (state.offset / state.limit * 4f).coerceIn(0f, 1f) else 0f
            }.background(statusBarColor))
        }
    }
}
