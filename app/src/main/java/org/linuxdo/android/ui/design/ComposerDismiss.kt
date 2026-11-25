package org.linuxdo.android.ui.design

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** 只响应一次可见到隐藏；切换表情、打开选择器或退到后台不算用户关闭输入。 */
internal class ComposerImeDismissState {
    private var wasVisible = false

    fun update(visible: Boolean, allowed: Boolean): Boolean {
        val shouldDismiss = allowed && wasVisible && !visible
        wasVisible = allowed && visible
        return shouldDismiss
    }
}

/** 键盘、系统返回和遮罩点击共享同一关闭动作，调用方仍负责保存草稿/提交中的保护。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberComposerDismiss(
    enabled: Boolean = true,
    canDismiss: Boolean = true,
    ignoreKeyboardHide: Boolean = false,
    onDismiss: () -> Unit,
): () -> Unit {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val owner = LocalLifecycleOwner.current
    val lifecycleState by owner.lifecycle.currentStateFlow.collectAsState()
    val latestEnabled by rememberUpdatedState(enabled)
    val latestCanDismiss by rememberUpdatedState(canDismiss)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val imeState = remember { ComposerImeDismissState() }
    val dismiss = remember(view, focusManager, keyboard) {
        {
            if (latestEnabled && latestCanDismiss) {
                imeState.update(visible = false, allowed = false)
                latestOnDismiss()
                keyboard?.hide()
                view.findFocus()?.clearFocus()
                focusManager.clearFocus(force = true)
            }
        }
    }
    BackHandler(enabled = enabled, onBack = dismiss)
    // Android 13+ 的 IME 自己持有返回回调。输入弹层使用 overlay 优先级，在同一次返回里关闭。
    DisposableEffect(view, owner, enabled) {
        val release = if (enabled && Build.VERSION.SDK_INT >= 33) {
            registerComposerBack(view, owner.lifecycle, dismiss)
        } else ({})
        onDispose { release() }
    }
    // 兼容键盘自身的收起按钮；不因选择图片/表情、子弹窗或后台隐藏键盘而丢掉编辑现场。
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible, enabled, canDismiss, ignoreKeyboardHide, lifecycleState) {
        if (imeState.update(imeVisible, enabled && canDismiss && !ignoreKeyboardHide &&
                lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && view.hasWindowFocus())) {
            dismiss()
        }
    }
    return dismiss
}

@RequiresApi(33)
private fun registerComposerBack(view: View, lifecycle: Lifecycle, onBack: () -> Unit): () -> Unit {
    var dispatcher: OnBackInvokedDispatcher? = null
    val callback = OnBackInvokedCallback { onBack() }
    fun unregister() {
        dispatcher?.unregisterOnBackInvokedCallback(callback)
        dispatcher = null
    }
    fun sync() {
        if (!view.isAttachedToWindow || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            unregister()
        } else if (dispatcher == null) {
            dispatcher = view.findOnBackInvokedDispatcher()
            dispatcher?.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback)
        }
    }
    val observer = LifecycleEventObserver { _, _ -> sync() }
    val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = sync()
        override fun onViewDetachedFromWindow(v: View) = unregister()
    }
    lifecycle.addObserver(observer)
    view.addOnAttachStateChangeListener(listener)
    sync()
    return {
        unregister()
        view.removeOnAttachStateChangeListener(listener)
        lifecycle.removeObserver(observer)
    }
}

/** 旧版 Android/实体返回键在交给 IME 前消费；取消的手势不触发关闭。 */
internal fun handleComposerBackKey(event: KeyEvent, onDismiss: () -> Unit): Boolean {
    if (event.keyCode != KeyEvent.KEYCODE_BACK) return false
    if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onDismiss()
    return true
}

internal fun Modifier.composerBackKeys(onDismiss: () -> Unit): Modifier =
    onPreInterceptKeyBeforeSoftKeyboard { handleComposerBackKey(it.nativeKeyEvent, onDismiss) }
