package org.linuxdo.android.ui.screen

import kotlin.math.abs

/** 只识别从首页顶部开始、单指向下并正常松手的一次深拉，不接管列表滚动。 */
internal class MiniAppPullGesture {
    enum class Action { Open, Refresh, Ignore }

    private var eligible = false
    private var downward = false
    private var directionDecided = false
    private var released = false
    private var cancelled = true
    private var handled = false

    fun begin(canOpen: Boolean) {
        eligible = canOpen
        downward = false
        directionDecided = false
        released = false
        cancelled = false
        handled = false
    }

    fun move(dx: Float, dy: Float, touchSlop: Float) {
        if (cancelled || handled || released) return
        if (!directionDecided && dx * dx + dy * dy > touchSlop * touchSlop) {
            directionDecided = true
            downward = dy > abs(dx) * 1.35f
            if (!downward) eligible = false
        }
        // 横向占优或回拉到起点后，不能重新把同一手势解释为深拉。
        if (directionDecided && (dy <= 0f || abs(dx) > dy)) eligible = false
    }

    fun release() { if (!cancelled) released = true }
    fun cancel() { cancelled = true; eligible = false }

    fun ready(distanceFraction: Float): Boolean =
        eligible && downward && !cancelled && !handled && distanceFraction >= OPEN_FRACTION

    fun finish(distanceFraction: Float, canOpen: Boolean): Action {
        if (cancelled || !released || handled) return Action.Ignore
        val open = canOpen && ready(distanceFraction)
        handled = true
        return if (open) Action.Open else Action.Refresh
    }

    companion object {
        // 与普通刷新（1.0）留出明显距离；只在松手时使用最终距离，回拉可撤销。
        const val OPEN_FRACTION = 1.65f
    }
}
