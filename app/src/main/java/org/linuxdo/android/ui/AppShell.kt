package org.linuxdo.android.ui

import android.app.Dialog
import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.roundToInt
import org.linuxdo.android.ui.screen.AttachmentDownloadManager
import org.linuxdo.android.ui.screen.AttachmentDownloadStage
import org.linuxdo.android.ui.screen.AttachmentDownloadUiState
import org.linuxdo.android.data.InternalTopicLink
import org.linuxdo.android.data.TopicRepository
import org.linuxdo.android.data.parseTopicLinkFromText
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView as AndroidTextView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import org.linuxdo.android.data.TopicListSort
import org.linuxdo.android.data.TopicListKey
import org.linuxdo.android.data.TopicSearchScope
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.linuxdo.android.ui.screen.PostAction
import org.linuxdo.android.ui.screen.PostActionIcon
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.linuxdo.android.ui.screen.openExternal
import org.linuxdo.android.net.BrowserSession
import org.linuxdo.android.net.showVerificationSheet
import org.linuxdo.android.Config
import org.linuxdo.android.data.HomeFeedType
import org.linuxdo.android.data.NotificationPollInterval
import org.linuxdo.android.data.compactCategoryGroups
import org.linuxdo.android.ui.design.boundedTabSwipe
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosMetrics
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.nav.IosNavHost
import org.linuxdo.android.ui.nav.NavStack
import org.linuxdo.android.ui.nav.Route
import org.linuxdo.android.ui.screen.TopicListScreen
import org.linuxdo.android.ui.screen.TopicDetailScreen
import org.linuxdo.android.ui.screen.CategoryListScreen
import org.linuxdo.android.ui.screen.SearchScreen
import org.linuxdo.android.ui.screen.TopicListUiState
import org.linuxdo.android.ui.screen.LoginScreen
import org.linuxdo.android.ui.screen.categoryLevelOf
import org.linuxdo.android.ui.screen.HistoryScreen
import org.linuxdo.android.ui.screen.ConnectWebSheet
import org.linuxdo.android.ui.screen.InAppBrowserSheet
import org.linuxdo.android.ui.screen.openExternal
import org.linuxdo.android.ui.screen.ChatRoomScreen
import org.linuxdo.android.ui.screen.FollowListScreen
import org.linuxdo.android.ui.screen.UserCardBottomSheet
import org.linuxdo.android.ui.screen.UserProfileScreen
import org.linuxdo.android.ui.screen.ProfileBadgesScreen
import org.linuxdo.android.ui.screen.AccountSwitchBottomSheet
import org.linuxdo.android.ui.screen.PrivateMessageComposeSheet
import org.linuxdo.android.ui.nav.MainTab
import org.linuxdo.android.ui.nav.TabBar

/**
 * 应用外壳。
 *
 * 分层顺序是硬约束:全局单例 WebView 必须留在导航之外的最外层,靠 zIndex 切换显隐。
 * Login 屏使用原生 Compose，安全验证只在独立对话框中显示轻量 WebView。
 */
@Composable
fun AppShell(viewModel: MainViewModel, browser: BrowserSession) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val visibleTags = remember(tags.tags, tags.searchedTags) { tags.tags + tags.searchedTags }
    org.linuxdo.android.ui.design.ProvideForumIcons(state.username, state.screen == Screen.List, categories.items, visibleTags) {
        AppShellContent(viewModel, browser, state)
    }
}

@Composable
private fun AppShellContent(viewModel: MainViewModel, browser: BrowserSession, state: UiState) {
    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    // 全局 WebView 在 CF 验证时显示；登录人机验证使用独立对话框。
    val showWeb = state.screen == Screen.Verify
    BackHandler(enabled = showWeb) { viewModel.cancelVerification() }
    val showWebLoginModal = state.screen == Screen.Login && loginUiState.showWebLoginDialog
    val accountsState by viewModel.accounts.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val notificationPollInterval by viewModel.notificationPollInterval.collectAsStateWithLifecycle()
    val customRealtimeSeconds by viewModel.customRealtimeSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycle, state.screen, state.latest.initialLoading, notificationPollInterval, customRealtimeSeconds) {
        if (state.screen == Screen.List && !state.latest.initialLoading) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshUnread()
            // 前台未读角标与后台通知设置解耦：可见时始终按 10 秒基线轮询，
            // 只有“实时”且自定义间隔更短时才加快，下限 5 秒避免触发限频。
            val pollDelayMs = when (notificationPollInterval) {
                NotificationPollInterval.REALTIME ->
                    (customRealtimeSeconds * 1000L).coerceIn(5_000L, Config.UNREAD_POLL_INTERVAL_MS)
                else -> Config.UNREAD_POLL_INTERVAL_MS
            }
            while (true) {
                // 限流冷却期间直接睡到解封，不再每 10 秒空转一次
                delay(viewModel.nextUnreadPollDelayMs(pollDelayMs))
                viewModel.refreshUnread()
            }
        }
    }

    var verifyPageState by remember(showWeb) { mutableStateOf(BrowserSession.VerifyPageState.LOADING) }
    var verifyRetryAllowed by remember(showWeb) { mutableStateOf(true) }
    var verifyRetryRemainingSec by remember(showWeb) { mutableStateOf(0L) }
    var verifyStallReason by remember(showWeb) { mutableStateOf<String?>(null) }
    val verificationProbeFeedback by viewModel.verificationProbeFeedback.collectAsStateWithLifecycle()
    LaunchedEffect(showWeb) {
        if (showWeb) {
            verifyPageState = BrowserSession.VerifyPageState.LOADING
            launch {
                // 验证页未确认需要人工交互前用原生加载层遮住 WebView；主框架报错（DoH 直连下连不上）
                // 或数秒仍未加载完则换成异常提示。
                while (showWeb) {
                    verifyPageState = viewModel.verifyPageState()
                    verifyRetryAllowed = viewModel.canRetryVerification()
                    verifyRetryRemainingSec = viewModel.verificationRetryRemainingSeconds()
                    verifyStallReason = viewModel.verifyStallReason()
                    delay(150)
                }
            }
            while (true) {
                delay(300)
                viewModel.pollVerification()
                verifyPageState = viewModel.verifyPageState()
            }
        }
    }
    val readSyncVerifyBanner by viewModel.readSyncVerifyBanner.collectAsStateWithLifecycle()
    val attachmentDownload by AttachmentDownloadManager.state.collectAsStateWithLifecycle()

    val skinPreset by viewModel.skinPreset.collectAsStateWithLifecycle()
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    IosTheme(dark = dark, skinPreset = skinPreset, fontScale = fontScale) {
        Box(
            Modifier
                .fillMaxSize()
                .background(IosTheme.colors.groupedBackground)
                // 顶部间距由每个导航页面处理，个人主页可将背景画到状态栏后方。
                // 底部保留给各输入弹层自身按 IME 高度平移避让，避免键盘动画每帧触发整棵主内容树 relayout。
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .clipToBounds(),
        ) {
            // 验证期间保留导航的组合身份，返回后恢复原页面与滚动位置。
            if (state.username != null || state.screen == Screen.List) {
                key(state.username) { MainNavigation(state, viewModel, lifecycle) }
            }

            if (state.screen == Screen.Boot) BootScreen()

            if (readSyncVerifyBanner && state.screen == Screen.List) {
                ReadSyncVerifyBanner(
                    onVerify = viewModel::openReadSyncVerification,
                    onDismiss = viewModel::dismissReadSyncVerifyBanner,
                    dark = dark,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                        .padding(top = IosMetrics.navBarHeight + 4.dp, start = 12.dp, end = 12.dp)
                        .zIndex(1f),
                )
            }

            // 原生登录页面（背景延展至状态栏后方，顶部安全区由 LoginScreen 内部处理）。
            if (state.screen == Screen.Login) {
                Box(Modifier.fillMaxSize()) {
                    LoginScreen(
                        state = loginUiState,
                        onPasswordLogin = viewModel::loginWithPassword,
                        onSendEmailCode = viewModel::sendLoginEmail,
                        onEmailCodeLogin = viewModel::loginWithEmailCode,
                        onDismissError = viewModel::dismissLoginError,
                        onResetSecondFactor = viewModel::resetSecondFactor,
                        onOpenWebLogin = viewModel::openWebLoginModal,
                        onStartWebAuth = viewModel::startWebAuth,
                        onDismissRiskControl = viewModel::dismissRiskControlDialog,
                        onCancelWebAuth = viewModel::cancelWebAuth,
                        onCancel = if (viewModel.canCancelAddAccount()) viewModel::cancelAddAccount else null,
                    )
                }
            }

            // CF 的全屏验证和后台 WebView 使用全局页面内 AndroidView；登录人机验证在弹框中。
            if (!showWebLoginModal) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                        .zIndex(if (showWeb) 2f else -1f)
                        .then(if (showWeb) Modifier.background(IosTheme.colors.groupedBackground) else Modifier),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = if (showWeb) {
                            Modifier
                                .fillMaxSize()
                                .padding(top = IosMetrics.navBarHeight)
                        } else {
                            Modifier.fillMaxSize()
                        },
                    ) {
                        val context = LocalContext.current
                        if (showWeb) {
                            WebTopBar(
                                screen = state.screen,
                                onCancel = viewModel::cancelVerification,
                                onRetry = viewModel::retryVerification,
                                retryEnabled = verifyRetryAllowed,
                                retryRemainingSeconds = verifyRetryRemainingSec,
                            )
                        }
                        val webReady = !showWeb || verifyPageState == BrowserSession.VerifyPageState.READY
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clipToBounds(),
                        ) {
                            AndroidView(
                                factory = { browser.attach(context) },
                                onRelease = { browser.detach() },
                                // 保持 View.VISIBLE 让 Chromium/Turnstile 正常跑帧，未就绪或已回源 404 时用 alpha=0f + 原生遮罩层挡住画面
                                update = { view ->
                                    if (view.visibility != android.view.View.VISIBLE) {
                                        view.visibility = android.view.View.VISIBLE
                                    }
                                    val targetAlpha = if (webReady) 1f else 0f
                                    if (view.alpha != targetAlpha) view.alpha = targetAlpha
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (!webReady) {
                                VerifyLoadingLayer(
                                    state = verifyPageState,
                                    feedback = verificationProbeFeedback,
                                    stallReason = verifyStallReason,
                                    retryEnabled = verifyRetryAllowed,
                                    retryRemainingSeconds = verifyRetryRemainingSec,
                                    onRetry = viewModel::retryVerification,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(IosTheme.colors.groupedBackground)
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitPointerEvent()
                                                }
                                            }
                                        },
                                )
                            }
                        }
                    }
                }
            }
            else {
                NativeWebLoginDialog(browser, viewModel::closeWebLoginModal, dark)
            }

            val downloadContext = LocalContext.current
            var lastShownDownload by remember { mutableStateOf<AttachmentDownloadUiState?>(null) }
            if (attachmentDownload != null) {
                lastShownDownload = attachmentDownload
            }
            AnimatedVisibility(
                visible = attachmentDownload != null,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    initialOffsetY = { -it - 32 },
                ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it - 32 },
                ) + fadeOut(animationSpec = tween(durationMillis = 180)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                    .padding(top = 10.dp, start = 14.dp, end = 14.dp)
                    .widthIn(max = 420.dp)
                    .zIndex(8f),
            ) {
                val cardState = attachmentDownload ?: lastShownDownload
                if (cardState != null) {
                    AttachmentDownloadOverlayCard(
                        state = cardState,
                        dark = dark,
                        onCancel = { AttachmentDownloadManager.cancelCurrent(downloadContext) },
                        onRetry = { AttachmentDownloadManager.retryLast(downloadContext) },
                        onOpen = { uri, mime -> openDownloadedAttachment(downloadContext, uri, mime) },
                        onDismiss = { AttachmentDownloadManager.dismiss() },
                    )
                }
            }

            val updateInfo = updateState.updateInfo
            if (updateState.showDialog && updateInfo != null) {
                UpdateDialog(
                    currentVersion = updateState.currentVersion,
                    info = updateInfo,
                    stage = updateState.apkStage,
                    downloadedBytes = updateState.apkDownloadedBytes,
                    totalBytes = updateState.apkTotalBytes,
                    errorMessage = updateState.apkError,
                    pendingConfirmIntent = updateState.pendingInstallConfirmIntent,
                    onConfirmIntentLaunched = viewModel::consumeInstallConfirmIntent,
                    onConfirmIntentLaunchFailed = viewModel::onInstallConfirmLaunchFailed,
                    onStartDownload = viewModel::startApkUpdate,
                    onRetryInstall = viewModel::retryApkInstallOrDownload,
                    onRetryAfterPermission = viewModel::retryApkUpdateAfterPermission,
                    onCancelDownload = viewModel::cancelApkUpdate,
                    onDismiss = viewModel::dismissUpdateDialog,
                )
            }
        }
    }
}

/** 阅读同步被 CF 风控时的提示条：点击进入验证页，关闭则 10 分钟内不再提示。 */
@Composable
private fun ReadSyncVerifyBanner(
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val warnBg = if (dark) Color(0xFF3A2E12) else Color(0xFFFEF3C7)
    val warnText = if (dark) Color(0xFFFDE68A) else Color(0xFF92400E)
    val warnBtn = if (dark) Color(0xFFF59E0B) else Color(0xFFB45309)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(warnBg)
            .clickable(onClickLabel = "进行安全验证", onClick = onVerify)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "阅读进度同步触发风控，需要完成 Cloudflare 安全验证",
            color = warnText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onVerify) {
            Text("去验证", fontSize = 13.sp, color = warnBtn, fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = onDismiss) {
            Text("忽略", fontSize = 13.sp, color = warnText)
        }
    }
}

/** 验证页加载层：加载中显示转圈；验证页打不开，或验证通过但 App 请求仍被拦截时提供重试。 */
@Composable
private fun VerifyLoadingLayer(
    state: BrowserSession.VerifyPageState,
    feedback: TopicRepository.VerificationProbeResult?,
    stallReason: String?,
    retryEnabled: Boolean,
    retryRemainingSeconds: Long,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        val failure = when (state) {
            BrowserSession.VerifyPageState.STALLED -> "验证未完成" to (stallReason ?: "验证页面或组件尚未准备好，请检查网络后重试")
            BrowserSession.VerifyPageState.MISMATCH -> when (feedback?.outcome) {
                TopicRepository.VerificationProbeOutcome.CHALLENGED ->
                    "App 请求仍被安全验证拦截" to
                        "网页验证已通过（${feedback.detail ?: "复核被拦截"}），但 App 数据通道仍被 Cloudflare 拦截。\n" +
                        "原因多为验证页与 App 通道的网络出口、代理分流或 DoH/直连节点不一致。\n" +
                        "请点击「重试」；若持续出现，请切换网络、关闭冲突代理或调整 DoH 设置。"
                TopicRepository.VerificationProbeOutcome.RATE_LIMITED ->
                    "触发请求速率限制" to buildString {
                        append(feedback.detail ?: "站点暂时限流")
                        if (retryRemainingSeconds > 0L) {
                            append("，请等待约 ")
                            append(retryRemainingSeconds)
                            append(" 秒冷却结束后重试，避免连续点击加重风控。")
                        } else {
                            append("，冷却已结束，现在可点击「重试」。")
                        }
                    }
                TopicRepository.VerificationProbeOutcome.INCONCLUSIVE -> if ((feedback.status ?: 0) >= 500) {
                    "站点服务暂不可用 (HTTP ${feedback.status})" to
                        "网页验证已通过，但复核时${feedback.detail ?: "收到服务端异常"}。\n" +
                        "已保留当前验证凭据，请稍后点击「重试」直接重新确认连接。"
                } else {
                    "连接尚未确认恢复" to
                        "网页验证已通过，但复核时${feedback.detail ?: "请求超时或网络中断"}。\n" +
                        "已保留当前验证凭据，点击「重试」将直接重新检查连接（无需重过验证码）。"
                }
                else -> "App 连接尚未恢复" to "尚未确认 App 连接恢复（${feedback?.detail ?: "请稍后重试"}）。"
            }
            else -> null
        }
        if (failure != null) {
            Text(
                text = failure.first,
                style = IosTheme.type.navTitle,
                color = IosTheme.colors.label,
            )
            Text(
                text = failure.second,
                color = IosTheme.colors.secondaryLabel,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = onRetry, enabled = retryEnabled, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    if (retryEnabled) "重试" else if (retryRemainingSeconds > 0L) "等待后重试 (${retryRemainingSeconds}s)" else "等待后重试",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            IosActivityIndicator(diameter = 28.dp)
            Text(
                text = if (state == BrowserSession.VerifyPageState.CONFIRMING) {
                    when (feedback?.outcome) {
                        TopicRepository.VerificationProbeOutcome.RATE_LIMITED ->
                            if (retryRemainingSeconds > 0L) "触发请求速率限制，约 ${retryRemainingSeconds} 秒后自动重探连接。"
                            else "站点暂时限流，正在等待后再检查连接。"
                        TopicRepository.VerificationProbeOutcome.INCONCLUSIVE ->
                            "连接复核暂未成功（${feedback.detail ?: "网络波动或超时"}），稍后会自动再试。"
                        TopicRepository.VerificationProbeOutcome.CHALLENGED ->
                            "App 请求仍被拦截（${feedback.detail ?: "需安全验证"}），正在换连重试确认。"
                        else -> "正在确认 App 连接。\n遇到限流时会等待后复核。"
                    }
                } else "正在连接安全验证服务…",
                color = IosTheme.colors.secondaryLabel,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun NativeWebLoginDialog(
    browser: BrowserSession,
    onClose: () -> Unit,
    dark: Boolean,
) {
    val context = LocalContext.current
    DisposableEffect(browser, context, dark) {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val surfaceColor = if (dark) AndroidColor.rgb(28, 28, 30) else AndroidColor.WHITE
        val labelColor = if (dark) AndroidColor.rgb(245, 245, 247) else AndroidColor.rgb(28, 28, 30)
        val secondaryColor = if (dark) AndroidColor.rgb(142, 142, 147) else AndroidColor.rgb(142, 142, 147)
        val dividerColor = if (dark) AndroidColor.rgb(56, 56, 58) else AndroidColor.rgb(229, 229, 234)
        val dialog = Dialog(context)
        var dismissedByComposition = false

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(surfaceColor)
                cornerRadii = floatArrayOf(
                    dp(20).toFloat(), dp(20).toFloat(),
                    dp(20).toFloat(), dp(20).toFloat(),
                    0f, 0f, 0f, 0f,
                )
            }
        }
        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        val icon = AndroidTextView(context).apply {
            text = "🛡️"
            gravity = Gravity.CENTER_VERTICAL
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }
        val title = AndroidTextView(context).apply {
            text = "安全验证"
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(labelColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(8), 0, 0, 0)
        }
        val close = AndroidTextView(context).apply {
            text = "✕"
            gravity = Gravity.CENTER
            setTextColor(secondaryColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (dark) AndroidColor.rgb(44, 44, 46) else AndroidColor.rgb(242, 242, 247))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.MATCH_PARENT))
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(30), dp(30)))
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        root.addView(View(context).apply { setBackgroundColor(dividerColor) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))

        val webView = browser.attachAuth(context)
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            if (!dismissedByComposition) onClose()
        }
        dialog.showVerificationSheet(root, dark)
        webView.requestFocus()

        onDispose {
            dismissedByComposition = true
            dialog.setOnDismissListener(null)
            root.removeView(webView)
            dialog.dismiss()
            browser.detachAuth(webView)
        }
    }
}

@Composable
private fun MainNavigation(state: UiState, viewModel: MainViewModel, lifecycle: Lifecycle) {
    val stacks = MainTab.primaryTabs.associateWith { tab ->
        key(tab) { rememberSaveable(saver = NavStack.Saver) { NavStack(tab.root) } }
    }
    val scrollRequests = MainTab.primaryTabs.associateWith { tab ->
        key(tab) { rememberSaveable { mutableIntStateOf(0) } }
    }
    var selected by rememberSaveable { mutableStateOf(MainTab.Latest) }
    // 老版本停留在“我的”时回到首页，避免恢复已删除的主导航栈。
    if (selected !in MainTab.primaryTabs) selected = MainTab.Latest
    val currentSelected by rememberUpdatedState(selected)
    val holder = rememberSaveableStateHolder()
    val stack = stacks.getValue(selected)
    val scrollToTopRequest = scrollRequests.getValue(selected).intValue
    val feedStates by viewModel.feedStates.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categorySubTab by viewModel.categorySubTab.collectAsStateWithLifecycle()
    val tagsState by viewModel.tags.collectAsStateWithLifecycle()
    val tagSearchQuery by viewModel.tagSearchQuery.collectAsStateWithLifecycle()
    val tagTopics by viewModel.tagTopics.collectAsStateWithLifecycle()
    val categoryTopics by viewModel.categoryTopics.collectAsStateWithLifecycle()
    val userTopicStates by viewModel.userTopicStates.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val topicSearch by viewModel.topicSearch.collectAsStateWithLifecycle()
    val metadataEditor by viewModel.metadataEditor.collectAsStateWithLifecycle()
    val metadataUpdated by viewModel.metadataUpdated.collectAsStateWithLifecycle()
    LaunchedEffect(metadataUpdated) {
        metadataUpdated?.let { value -> stacks.values.forEach { it.updateTopicTitle(value.topicId, value.title) } }
    }
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val notificationFilter by viewModel.notificationFilter.collectAsStateWithLifecycle()
    val composer by viewModel.composer.collectAsStateWithLifecycle()
    val topicCreator by viewModel.topicCreator.collectAsStateWithLifecycle()
    val draftConflict by viewModel.draftConflict.collectAsStateWithLifecycle()
    draftConflict?.let { conflict ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::dismissDraftConflict,
            title = { Text("两端草稿内容不同") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("手机和网页都有修改，可选择继续编辑的版本。也可稍后处理，直接发送手机内容，网页的不同版本会保留。")
                    Text("手机：${conflict.local.content.raw.ifBlank { conflict.local.content.title }.take(180)}")
                    Text("网页：${conflict.remote.content?.let { it.raw.ifBlank { it.title }.take(180) } ?: "草稿已删除"}")
                    TextButton(onClick = viewModel::dismissDraftConflict) { Text("稍后处理，两端保留") }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.resolveDraftConflict(true) }) { Text("使用手机版本") } },
            dismissButton = { TextButton(onClick = { viewModel.resolveDraftConflict(false) }) { Text("使用网页版本") } },
        )
    }
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    val accountState by viewModel.accounts.collectAsStateWithLifecycle()
    val profileEditRevision by ProfileEditorUpdates.revision.collectAsStateWithLifecycle()
    LaunchedEffect(profileEditRevision) { if (profileEditRevision > 0) viewModel.refreshAccounts() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val drawerVisible = drawerState.currentValue != DrawerValue.Closed || drawerState.targetValue != DrawerValue.Closed
    var showDrawerAccounts by remember { mutableStateOf(false) }
    var showDrawerScore by remember { mutableStateOf(false) }
    var drawerReturnEntry by rememberSaveable { mutableStateOf<Long?>(null) }
    var drawerReturnTab by rememberSaveable { mutableStateOf<String?>(null) }
    var drawerReturnReady by rememberSaveable { mutableStateOf(false) }
    var drawerNavigating by remember { mutableStateOf(false) }
    val drawerAccount = accountState.accounts.firstOrNull { it.userId == accountState.activeUserId }
        ?.takeIf { it.username.equals(state.username, ignoreCase = true) }
    val drawerProfile = key(profileEditRevision) { rememberDrawerProfile(state.username, accountState.activeUserId, drawerVisible) }
    val drawerBadges = rememberDrawerBadges(state.username, accountState.activeUserId, drawerVisible)
    val history by viewModel.history.collectAsStateWithLifecycle()
    val userCard by viewModel.userCard.collectAsStateWithLifecycle()
    val followList by viewModel.followList.collectAsStateWithLifecycle()
    val trustReport by viewModel.trustReport.collectAsStateWithLifecycle()
    val ldcState by viewModel.ldcState.collectAsStateWithLifecycle()
    val cdkScoreState by viewModel.cdkScore.collectAsStateWithLifecycle()
    val chat by viewModel.chat.collectAsStateWithLifecycle()
    val pendingChatChannel by viewModel.pendingChatChannel.collectAsStateWithLifecycle()
    val chatUnread = chat.channels.filter { it.membership?.muted != true }.sumOf { it.unreadCount }
    var messageSection by rememberSaveable { mutableStateOf(MessageSection.Notifications) }
    val openChat = {
        messageSection = MessageSection.DirectMessages
        selected = MainTab.Messages
        stacks.getValue(MainTab.Messages).popToRoot()
    }
    LaunchedEffect(pendingChatChannel) {
        pendingChatChannel?.let { channelId ->
            openChat()
            val messagesStack = stacks.getValue(MainTab.Messages)
            messagesStack.push(Route.ChatRoom(channelId, "直接消息"))
            viewModel.clearChatNotification()
        }
    }
    val skinPreset by viewModel.skinPreset.collectAsStateWithLifecycle()
    val userRepliesStates by viewModel.userRepliesStates.collectAsStateWithLifecycle()
    val bookmarkSearchQueries by viewModel.bookmarkSearchQueries.collectAsStateWithLifecycle()
    val homeFilter by viewModel.homeFilter.collectAsStateWithLifecycle()
    val topicListSorts by viewModel.topicListSorts.collectAsStateWithLifecycle()
    val floatingBallEnabled by viewModel.floatingBallEnabled.collectAsStateWithLifecycle()
    val floatingBallActions by viewModel.floatingBallActions.collectAsStateWithLifecycle()
    val floatingBallXRatio by viewModel.floatingBallXRatio.collectAsStateWithLifecycle()
    val floatingBallYRatio by viewModel.floatingBallYRatio.collectAsStateWithLifecycle()
    val floatingBallOnRight by viewModel.floatingBallOnRight.collectAsStateWithLifecycle()
    val treeView by viewModel.treeView.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val showTopicScrollbar by viewModel.showTopicScrollbar.collectAsStateWithLifecycle()
    val syncReadProgress by viewModel.syncReadProgress.collectAsStateWithLifecycle()
    val autoTrackTopic by viewModel.autoTrackTopic.collectAsStateWithLifecycle()
    val autoTrackTopicSeconds by viewModel.autoTrackTopicSeconds.collectAsStateWithLifecycle()
    val showUnreadDot by viewModel.showUnreadDot.collectAsStateWithLifecycle()
    val unreadDotPosition by viewModel.unreadDotPosition.collectAsStateWithLifecycle()
    val seeSignatures by viewModel.seeSignatures.collectAsStateWithLifecycle()
    val signatureSyncBusy by viewModel.signatureSyncBusy.collectAsStateWithLifecycle()
    val signatureSyncNotice by viewModel.signatureSyncNotice.collectAsStateWithLifecycle()
    val showTopicUnreadDot by viewModel.showTopicUnreadDot.collectAsStateWithLifecycle()
    val showDirectMessageQuickBar by viewModel.showDirectMessageQuickBar.collectAsStateWithLifecycle()
    val topicUnreadDotPosition by viewModel.topicUnreadDotPosition.collectAsStateWithLifecycle()
    val listBars = remember(selected, stack.entries.last().id) { mutableStateOf(true) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val autoRefreshIncoming by viewModel.autoRefreshIncoming.collectAsStateWithLifecycle()
    val autoRefreshInterval by viewModel.autoRefreshInterval.collectAsStateWithLifecycle()
    val autoReadClipboardEnabled by viewModel.autoReadClipboardEnabled.collectAsStateWithLifecycle()
    val detectedClipboardTarget by viewModel.detectedClipboardTarget.collectAsStateWithLifecycle()
    val backgroundNotification by viewModel.backgroundNotification.collectAsStateWithLifecycle()
    val notificationPollInterval by viewModel.notificationPollInterval.collectAsStateWithLifecycle()
    val notificationScope by viewModel.notificationScope.collectAsStateWithLifecycle()
    val customRealtimeSeconds by viewModel.customRealtimeSeconds.collectAsStateWithLifecycle()
    val keepAliveService by viewModel.keepAliveService.collectAsStateWithLifecycle()
    val pendingNotificationTarget by viewModel.pendingNotificationTarget.collectAsStateWithLifecycle()
    // ── DoH 状态 ─────────────────────────────────────────────────────────────────
    val dohEnabled by viewModel.dohEnabled.collectAsStateWithLifecycle()
    val dohServers by viewModel.dohServers.collectAsStateWithLifecycle()
    val selectedDohServerUrl by viewModel.selectedDohServerUrl.collectAsStateWithLifecycle()
    val dohAutoFailover by viewModel.dohAutoFailover.collectAsStateWithLifecycle()
    val dohAutoOptimize by viewModel.dohAutoOptimize.collectAsStateWithLifecycle()
    val networkMode by viewModel.networkMode.collectAsStateWithLifecycle()
    val networkEnvironment by viewModel.networkEnvironment.collectAsStateWithLifecycle()
    val activeNetworkRoute by viewModel.activeNetworkRoute.collectAsStateWithLifecycle()
    val networkModeSwitching by viewModel.networkModeSwitching.collectAsStateWithLifecycle()
    val networkModeError by viewModel.networkModeError.collectAsStateWithLifecycle()
    val dohLatencies by viewModel.dohLatencies.collectAsStateWithLifecycle()
    val dohSummaries by viewModel.dohSummaries.collectAsStateWithLifecycle()
    val isTestingAllDoh by viewModel.isTestingAllDoh.collectAsStateWithLifecycle()
    val smartRouteMode by viewModel.smartRouteMode.collectAsStateWithLifecycle()
    val smartDirectLatency by viewModel.smartDirectLatency.collectAsStateWithLifecycle()
    val smartDohLatency by viewModel.smartDohLatency.collectAsStateWithLifecycle()
    val smartRouteSummary by viewModel.smartRouteSummary.collectAsStateWithLifecycle()
    val isSmartEvaluating by viewModel.isSmartEvaluating.collectAsStateWithLifecycle()
    val isSelfHealing by viewModel.isSelfHealing.collectAsStateWithLifecycle()
    val feedGraphicMode by viewModel.feedGraphicMode.collectAsStateWithLifecycle()
    val feedDisplayMode by viewModel.feedDisplayMode.collectAsStateWithLifecycle()

    LaunchedEffect(pendingNotificationTarget) {
        val target = pendingNotificationTarget ?: return@LaunchedEffect
        selected = MainTab.Latest
        val stack = stacks.getValue(MainTab.Latest)
        val currentRoute = stack.current
        if (currentRoute is Route.TopicDetail && currentRoute.topicId == target.topicId) {
            viewModel.loadTopic(
                target.topicId,
                postNumber = target.postNumber,
                resumePostNumber = viewModel.resolveResumePostNumber(target.topicId, target.postNumber),
                fromNotification = true,
            )
        } else {
            stack.push(Route.TopicDetail(target.topicId, "", target.postNumber))
        }
        viewModel.clearPendingNotificationTarget()
    }

    val context = LocalContext.current

    LaunchedEffect(autoReadClipboardEnabled) {
        if (autoReadClipboardEnabled) {
            viewModel.checkClipboard(context)
        }
    }

    DisposableEffect(lifecycle) {
        viewModel.setAccountSyncForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.setAccountSyncForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.setAccountSyncForeground(false)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.setAccountSyncForeground(false) }
    }

    DisposableEffect(lifecycle, autoReadClipboardEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && autoReadClipboardEnabled) {
                viewModel.checkClipboard(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    val browsing = state.screen == Screen.List
    LaunchedEffect(browsing, accountState.switching, accountState.activeUserId) {
        if (!browsing || accountState.switching) {
            showDrawerAccounts = false
            showDrawerScore = false
            drawerReturnEntry = null
            drawerReturnTab = null
            drawerReturnReady = false
            drawerState.snapTo(DrawerValue.Closed)
        }
    }
    var detailOverlay by rememberSaveable { mutableStateOf(false) }
    var showConnectSheet by rememberSaveable { mutableStateOf(false) }
    var privateMessageRecipient by rememberSaveable { mutableStateOf<String?>(null) }
    var globalBrowserUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val miniAppHomePullEnabled = rememberMiniAppHomePullEnabled()
    var browserAccountId by remember { mutableStateOf(accountState.activeUserId) }
    LaunchedEffect(accountState.activeUserId, accountState.switching) {
        if (browserAccountId != accountState.activeUserId || accountState.switching) {
            globalBrowserUrl = null
            org.linuxdo.android.ui.screen.MiniAppSessionPool.destroyCurrent()
        }
        browserAccountId = accountState.activeUserId
    }
    val contentObscured = topicCreator.open || composer.open || detailOverlay || topicSearch.open || metadataEditor.open ||
        showConnectSheet || globalBrowserUrl != null || privateMessageRecipient != null || accountState.switching ||
        userCard.loading || userCard.user != null || userCard.error != null || drawerVisible || showDrawerAccounts || showDrawerScore
    val visibleRoute = stack.current
    // 底栏仅属于当前主板块根页；所有带返回栈的页面统一隐藏，新增路由无需逐个排除。
    val isMainTabRoot = !stack.canPop && visibleRoute == selected.root
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardOpen by remember(density) {
        androidx.compose.runtime.derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val isDetailRoute = visibleRoute is Route.TopicDetail || visibleRoute == Route.Chat ||
        visibleRoute is Route.ChatRoom || visibleRoute is Route.Search || visibleRoute == Route.Profile || visibleRoute is Route.UserProfile
    val navigationLifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val listForeground = navigationLifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !contentObscured
    DisposableEffect(lifecycle, browsing, accountState.activeUserId) {
        fun updateChatConnection() {
            viewModel.setChatForeground(browsing && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        val observer = LifecycleEventObserver { _, _ -> updateChatConnection() }
        lifecycle.addObserver(observer)
        updateChatConnection()
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.setChatForeground(false)
        }
    }
    DisposableEffect(lifecycle, browsing, visibleRoute, contentObscured) {
        fun updateChatVisibility() {
            val foreground = browsing && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            viewModel.setVisibleChatChannel((visibleRoute as? Route.ChatRoom)?.channelId?.takeIf {
                foreground && !contentObscured && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            })
        }
        val observer = LifecycleEventObserver { _, _ -> updateChatVisibility() }
        lifecycle.addObserver(observer)
        updateChatVisibility()
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.setVisibleChatChannel(null)
        }
    }
    DisposableEffect(lifecycle, visibleRoute, browsing, contentObscured, state.homeFeed) {
        fun updateProbeVisibility() {
            val category = visibleRoute as? Route.CategoryTopics
            val visible = browsing && !contentObscured &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                (visibleRoute == Route.TopicList || category != null)
            viewModel.setAutoProbeVisible(visible, category?.categoryId, category?.slug)
            val previewVisible = browsing && !contentObscured &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                (visibleRoute == Route.TopicList || category != null || visibleRoute is Route.TagTopics)
            viewModel.setPreviewListVisible(previewVisible)
        }
        val observer = LifecycleEventObserver { _, _ -> updateProbeVisibility() }
        lifecycle.addObserver(observer)
        updateProbeVisibility()
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.setAutoProbeVisible(false)
            viewModel.setPreviewListVisible(false)
        }
    }
    fun openDrafts() {
        viewModel.openDrafts {
            if (stack.current != Route.Drafts) stack.push(Route.Drafts)
        }
    }

    fun selectBoard(tab: MainTab) {
        if (tab != selected) {
            // 发帖入口的草稿箱是临时页面，切换板块后恢复原浏览页。
            // 也清理目标栈中旧版本留下的草稿箱，不重置底层页面及滚动状态。
            listOf(stacks.getValue(selected), stacks.getValue(tab)).forEach { tabStack ->
                if (tabStack.current == Route.Drafts) tabStack.pop()
            }
        }
        selected = tab
    }
    fun openScopedSearch(scope: TopicSearchScope?) {
        val target = Route.Search(scope)
        viewModel.prepareSearch(scope)
        if (stack.current != target) stack.push(target)
    }
    fun openSearch() = openScopedSearch(null)
    fun openCategories() {
        if (stack.current != Route.CategoryList) stack.push(Route.CategoryList)
    }
    fun openPersonalDrawer() {
        if (!browsing || contentObscured) return
        viewModel.refreshAccounts()
        drawerScope.launch { drawerState.open() }
    }
    fun drawerNavigate(action: () -> Unit) {
        if (drawerNavigating || drawerState.targetValue == DrawerValue.Closed || accountState.switching) return
        drawerNavigating = true
        val accountId = accountState.activeUserId
        val sourceTab = selected
        val sourceEntry = stack.entries.last().id
        drawerReturnEntry = stack.entries.last().id
        drawerReturnTab = selected.name
        drawerReturnReady = false
        drawerScope.launch {
            try {
                drawerState.close()
                if (viewModel.state.value.screen == Screen.List && !viewModel.accounts.value.switching &&
                    viewModel.accounts.value.activeUserId == accountId && currentSelected == sourceTab && stack.entries.last().id == sourceEntry) {
                    action()
                    drawerReturnReady = true
                }
            } finally {
                drawerNavigating = false
            }
        }
    }
    val drawerScore = cdkScoreState.takeIf { it.username.equals(state.username, ignoreCase = true) } ?: CdkScoreUiState()
    val drawerDark = IosTheme.colors.isDark
    val onDrawerAction: (PersonalDrawerAction) -> Unit = { action ->
        drawerNavigate {
            val username = state.username
            when (action) {
                PersonalDrawerAction.Profile -> if (username != null) stack.push(Route.Profile)
                PersonalDrawerAction.Bookmarks -> if (username != null) stack.push(Route.UserTopics(username, "我的收藏", true))
                PersonalDrawerAction.History -> stack.push(Route.BrowsingHistory)
                PersonalDrawerAction.Drafts -> stack.push(Route.Drafts)
                PersonalDrawerAction.Following -> if (username != null) stack.push(Route.FollowList(username))
                PersonalDrawerAction.Badges -> if (username != null) stack.push(Route.UserBadges(username))
                is PersonalDrawerAction.Network -> if (username != null) stack.push(Route.ProfileNetwork(username, action.followers))
                PersonalDrawerAction.Level -> showConnectSheet = true
                PersonalDrawerAction.Categories -> { viewModel.setCategorySubTab(CategorySectionTab.CATEGORIES); openCategories() }
                PersonalDrawerAction.Tags -> { viewModel.setCategorySubTab(CategorySectionTab.TAGS); openCategories() }
                PersonalDrawerAction.Settings -> stack.push(Route.Settings)
                PersonalDrawerAction.Share -> if (username != null) context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${Config.BASE_URL}/u/${Uri.encode(username)}")
                }, "分享个人主页"))
                is PersonalDrawerAction.CategoryTopics -> stack.push(Route.CategoryTopics(action.category.id, action.category.slug, action.category.name))
                is PersonalDrawerAction.TagTopics -> stack.push(Route.TagTopics(action.tag))
                is PersonalDrawerAction.Web -> {
                    globalBrowserUrl = action.url
                }
            }
        }
    }
    LaunchedEffect(drawerReturnEntry, drawerReturnTab, drawerReturnReady, selected, stack.entries.last().id,
        globalBrowserUrl, showConnectSheet, browsing, accountState.switching, topicCreator.open) {
        if (drawerReturnReady && drawerReturnEntry == stack.entries.last().id && drawerReturnTab == selected.name &&
            globalBrowserUrl == null && !showConnectSheet && !topicCreator.open && browsing && !accountState.switching) {
            drawerReturnReady = false
            drawerReturnEntry = null
            drawerReturnTab = null
            // 清除回程标记会重启本 effect，动画由稳定的界面作用域完成。
            drawerScope.launch { drawerState.open() }
        }
    }
    BackHandler(enabled = drawerVisible && !showDrawerAccounts && !showDrawerScore) {
        drawerScope.launch { drawerState.close() }
    }
    val boardSwipe = Modifier.boundedTabSwipe(MainTab.primaryTabs.indexOf(selected), MainTab.primaryTabs.size,
        enabled = browsing && isMainTabRoot && !contentObscured) { selectBoard(MainTab.primaryTabs[it]) }
    PersonalDrawerHost(
        drawerState = drawerState,
        gesturesEnabled = drawerVisible && !showDrawerAccounts && !showDrawerScore && browsing && !accountState.switching,
        drawerContent = {
            key(accountState.activeUserId, state.username) {
                PersonalDrawerContent(state.username, drawerAccount, drawerProfile, drawerBadges, drawerVisible, categories, tagsState,
                    onLoadCategories = { viewModel.loadCategories() }, onLoadTags = { viewModel.loadTags() },
                    onAction = onDrawerAction,
                    onAccounts = { viewModel.refreshAccounts(); showDrawerAccounts = true },
                    onToggleTheme = { viewModel.setThemeMode(if (drawerDark) ThemeMode.Light else ThemeMode.Dark) },
                    onLogin = { drawerScope.launch { drawerState.close(); viewModel.addAccount() } })
            }
        },
    ) {
    Box(Modifier.fillMaxSize().then(if (drawerVisible) Modifier.clearAndSetSemantics {} else Modifier)) {
        Box(Modifier.fillMaxSize()) {
            // 保持首页与消息两大主 Tab 各自常驻组合树：切 Tab 时仅改 zIndex/alpha 与指针拦截，
            // 消除切换时整页卸载重建（LazyColumn + 图片 + 分页器重构）带来的卡顿与跳闪。
            MainTab.primaryTabs.forEach { tab ->
                val isTabSelected = tab == selected
                val tabStack = stacks.getValue(tab)
                val tabScrollToTopRequest = scrollRequests.getValue(tab).intValue
                val tabVisibleRoute = tabStack.current
                val tabIsDetailRoute = tabVisibleRoute is Route.TopicDetail || tabVisibleRoute == Route.Chat ||
                    tabVisibleRoute is Route.ChatRoom || tabVisibleRoute is Route.Search ||
                    tabVisibleRoute == Route.Profile || tabVisibleRoute is Route.UserProfile
                key(tab) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isTabSelected) 1f else 0f)
                            .graphicsLayer { alpha = if (isTabSelected) 1f else 0f }
                            .then(
                                if (!isTabSelected) {
                                    Modifier.clearAndSetSemantics {}
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        holder.SaveableStateProvider(tab.name) {
                            androidx.compose.runtime.CompositionLocalProvider(
                                org.linuxdo.android.ui.design.LocalNetworkSettingsAction provides {
                                    val destination = Route.SettingsDetail(org.linuxdo.android.ui.nav.SettingsPage.Network)
                                    if (tabStack.current != destination) tabStack.push(destination)
                                },
                            ) {
                                IosNavHost(
                                    tabStack,
                                    interactive = isTabSelected && browsing && !topicCreator.open && !topicSearch.open && !metadataEditor.open &&
                                        !drawerVisible && !showDrawerAccounts && !showDrawerScore,
                                    listBars = listBars.takeIf { isTabSelected && browsing && listForeground && !tabIsDetailRoute && !isKeyboardOpen },
                                    contentWindowInsets = { route ->
                                        if (route == Route.Profile || route is Route.UserProfile) WindowInsets(0)
                                        else WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)
                                    },
                                ) { route ->
                                    val stack = tabStack
                                    val scrollToTopRequest = tabScrollToTopRequest
                                    val active = isTabSelected && browsing && stack.current == route
                        // 仅回写标题时保持加载身份，避免保存后重新按阅读游标跳楼。
                        val routeLoadIdentity = if (route is Route.TopicDetail) route.copy(title = "") else route
                        // 首次详情响应可补齐历史位置，之后固定；0 代表新话题，不能被阅读回执改写。
                        var entryResumeFloor by rememberSaveable(routeLoadIdentity) {
                            mutableIntStateOf((route as? Route.TopicDetail)?.let {
                                viewModel.resolveResumePostNumber(it.topicId, it.postNumber)
                            } ?: -1)
                        }
                        var topicEntryLoaded by rememberSaveable(routeLoadIdentity) { mutableStateOf(false) }
                        val entryDetail = (route as? Route.TopicDetail)?.let { details[it.topicId] }
                        if (entryResumeFloor == -1 && topicEntryLoaded && entryDetail?.topic != null && !entryDetail.loading) {
                            SideEffect { entryResumeFloor = entryDetail.topic.lastReadPostNumber?.takeIf { it > 1 } ?: 0 }
                        }
                        val resumePostNumber = entryResumeFloor.takeIf { it > 0 }
                        LaunchedEffect(route, active) {
                            if (active) when (route) {
                                is Route.TopicDetail -> {
                                    if (!topicEntryLoaded || viewModel.details.value[route.topicId]?.topic == null) {
                                        topicEntryLoaded = true
                                        viewModel.loadTopic(route.topicId, postNumber = route.postNumber,
                                            resumePostNumber = resumePostNumber,
                                            restoredPosition = route.postNumber?.let {
                                                org.linuxdo.android.data.TopicReadingPosition(postNumber = it, atTop = it == 1,
                                                    descending = details[route.topicId]?.descending ?: false)
                                            })
                                    }
                                }
                                Route.CategoryList -> viewModel.loadCategories()
                                Route.TopicList -> if (pendingNotificationTarget == null) {
                                    if (state.latest.needsRefresh) viewModel.refreshLatest()
                                    else if (state.latest.initialLoading) viewModel.retryLatest()
                                }
                                is Route.Search -> {
                                    viewModel.prepareSearch(route.scope)
                                    if (search.scope == route.scope && search.results.needsRefresh) viewModel.updateSearch(search.query)
                                }
                                is Route.TagTopics -> viewModel.loadTagTopics(route.tag)
                                is Route.CategoryTopics -> {
                                    if (!categories.loaded && !categories.loading) viewModel.loadCategories()
                                    viewModel.loadCategory(route.categoryId, route.slug)
                                }
                                is Route.UserTopics -> viewModel.loadUserTopics(route.username, route.isBookmark)
                                is Route.UserReplies -> viewModel.loadUserReplies(route.username, filter = route.filter)
                                Route.BrowsingHistory -> viewModel.loadHistory()
                                Route.Drafts -> {
                                    if (!categories.loaded && !categories.loading) viewModel.loadCategories()
                                    viewModel.loadDrafts(refresh = true)
                                }
                                is Route.FollowList -> viewModel.loadFollowing(route.username)
                                else -> Unit
                            }
                        }
                        when (route) {
                            Route.TopicList -> HomeTopicListRoute(
                                state = state,
                                viewModel = viewModel,
                                stack = stack,
                                selected = selected,
                                active = active,
                                browsing = browsing,
                                listForeground = listForeground,
                                isKeyboardOpen = isKeyboardOpen,
                                scrollToTopRequest = scrollToTopRequest,
                                drawerAccount = drawerAccount,
                                categories = categories,
                                tagsState = tagsState,
                                tagSearchQuery = tagSearchQuery,
                                homeFilter = homeFilter,
                                feedStates = feedStates,
                                topicListSorts = topicListSorts,
                                feedGraphicMode = feedGraphicMode,
                                feedDisplayMode = feedDisplayMode,
                                showTopicUnreadDot = showTopicUnreadDot,
                                topicUnreadDotPosition = topicUnreadDotPosition,
                                miniAppHomePullEnabled = miniAppHomePullEnabled,
                                floatingBallEnabled = floatingBallEnabled,
                                floatingBallActions = floatingBallActions,
                                floatingBallXRatio = floatingBallXRatio,
                                floatingBallYRatio = floatingBallYRatio,
                                floatingBallOnRight = floatingBallOnRight,
                                onOpenPersonalDrawer = ::openPersonalDrawer,
                                onOpenDrafts = ::openDrafts,
                                onOpenScopedSearch = ::openScopedSearch,
                                onOpenCategories = ::openCategories,
                                onOpenChat = openChat,
                                onOpenConnect = { showConnectSheet = true },
                                onOpenBrowserUrl = { globalBrowserUrl = it },
                            )

                            is Route.TopicDetail -> {
                                val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
                                val isForeground = active && !contentObscured && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
                                DisposableEffect(lifecycle, route.topicId, active, contentObscured) {
                                    fun updateReadingActivity() {
                                        viewModel.setTopicActive(route.topicId, active && !contentObscured &&
                                            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                                    }
                                    val observer = LifecycleEventObserver { _, _ -> updateReadingActivity() }
                                    lifecycle.addObserver(observer)
                                    updateReadingActivity()
                                    onDispose {
                                        lifecycle.removeObserver(observer)
                                        viewModel.setTopicActive(route.topicId, false)
                                    }
                                }
                                // 离开这篇帖子时结算一次阅读进度(仅在用户开启同步后才会真正发请求)。
                                DisposableEffect(route.topicId) {
                                    onDispose {
                                        viewModel.setTopicActive(route.topicId, false)
                                        viewModel.leaveTopic(route.topicId)
                                    }
                                }
                                val detailState = details[route.topicId] ?: DetailUiState(loading = true)
                                TopicDetailScreen(
                                    title = route.title,
                                    state = detailState,
                                    onBack = { stack.pop() },
                                    onOpenCategory = { category ->
                                        stack.push(Route.CategoryTopics(category.id, category.slug, category.name))
                                    },
                                    onRetry = { viewModel.retryTopic(route.topicId, resumePostNumber) },
                                    onRefresh = { viewModel.refreshTopic(route.topicId) },
                                    onLoadMore = { viewModel.loadTopic(route.topicId, more = true) },
                                    onLoadPrevious = { viewModel.loadTopic(route.topicId, previous = true) },
                                    initialPostNumber = resumePostNumber,
                                    onUnknownTag = { viewModel.diagnostics.log("未知 cooked 标签: $it") },
                                    treeView = treeView,
                                    onTreeView = viewModel::setTreeView,
                                    showTopicScrollbar = showTopicScrollbar,
                                    showUnreadDot = showUnreadDot,
                                    unreadDotPosition = unreadDotPosition,
                                    showSignatures = seeSignatures,
                                    onLoadReplies = { viewModel.loadReplies(route.topicId, it) },
                                    onReact = { postId, reaction -> viewModel.react(route.topicId, postId, reaction) },
                                    onBoost = { postId, raw -> viewModel.createBoost(route.topicId, postId, raw) },
                                    onDeleteBoost = { postId, boostId -> viewModel.deleteBoost(route.topicId, postId, boostId) },
                                    onFlagBoost = { postId, boostId, reasonId, message ->
                                        viewModel.flagBoost(route.topicId, postId, boostId, reasonId, message)
                                    },
                                    onOpenUser = viewModel::openUserCard,
                                    onOpenUserProfile = { username -> stack.push(Route.UserProfile(username)) },
                                    onRefreshPost = { viewModel.refreshPost(route.topicId, it) },
                                    onSolution = { viewModel.setSolution(route.topicId, it) },
                                    composer = composer,
                                    onOpenComposer = viewModel::openComposer,
                                    onDraftChange = viewModel::updateDraft,
                                    onSubmitComposer = viewModel::submitComposer,
                                    onCloseComposer = viewModel::closeComposer,
                                    onDeletePost = { viewModel.deletePost(route.topicId, it) },
                                    onReloadFromStart = { viewModel.reloadFromStart(route.topicId) },
                                    onCreatedPostShown = { viewModel.clearCreatedPost(route.topicId) },
                                    onAttach = viewModel::attachToComposer,
                                    onRemoveComposerAttachment = viewModel::removeComposerAttachment,
                                    topicId = route.topicId,
                                    onOpenTopic = { id, title, postNumber ->
                                        stack.push(Route.TopicDetail(id, title, postNumber))
                                    },
                                    onJumpToFloor = { viewModel.jumpToFloor(route.topicId, it) },
                                    onOpenTag = { tag -> stack.push(Route.TagTopics(tag)) },
                                    active = isForeground,
                                    trailing = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        viewModel.openTopicSearch(
                                                            route.topicId,
                                                            detailState.topic?.title ?: route.title,
                                                        )
                                                    }
                                                    .semantics { contentDescription = "搜索" },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                PostActionIcon(
                                                    action = PostAction.Search,
                                                    selected = false,
                                                    tint = IosTheme.colors.label,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                            if (detailState.topic?.details?.canEdit == true) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            viewModel.openTopicMetadataEditor(route.topicId)
                                                        }
                                                        .semantics { contentDescription = "编辑" },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    PostActionIcon(
                                                        action = PostAction.Edit,
                                                        selected = false,
                                                        tint = IosTheme.colors.label,
                                                        modifier = Modifier.size(19.dp),
                                                    )
                                                }
                                            }
                                            org.linuxdo.android.ui.screen.TopicNotificationHeaderButton(
                                                currentLevel = detailState.notificationLevel,
                                                onSelectLevel = { viewModel.setTopicNotificationLevel(route.topicId, it) },
                                                enabled = state.username != null &&
                                                    detailState.topic != null &&
                                                    !detailState.notificationLevelUpdating,
                                            )
                                        }
                                    },
                                    onSelectOrder = { viewModel.setCommentOrder(route.topicId, it) },
                                    onPostsVisible = { viewModel.onPostsVisible(route.topicId, it) },
                                    onLiveViewport = { atEnd, scrolling -> viewModel.onTopicViewport(route.topicId, atEnd, scrolling) },
                                    hasViewedFloor = { viewModel.hasViewedFloor(accountState.activeUserId, route.topicId, it) },
                                    onFloorViewed = { viewModel.markViewedFloor(accountState.activeUserId, route.topicId, it) },
                                    onTopicViewed = { viewModel.onTopicViewed(accountState.activeUserId, route.topicId) },
                                    onScrolled = { viewModel.onTopicScrolled(route.topicId) },
                                    onOverlayActive = { detailOverlay = it },
                                    onSetFilterUser = { viewModel.setTopicAuthorFilter(route.topicId, it) },
                                    onVotePoll = { postId, pollName, optionIds -> viewModel.votePoll(route.topicId, postId, pollName, optionIds) },
                                    signedIn = state.username != null,
                                    onToggleBookmark = { viewModel.toggleBookmark(route.topicId, it) },
                                    onOpenFlagDialog = { viewModel.openFlagDialog(route.topicId, it) },
                                    onCloseFlagDialog = { viewModel.closeFlagDialog(route.topicId) },
                                    onSubmitFlag = { postId, reasonId, message ->
                                        viewModel.submitFlag(route.topicId, postId, reasonId, message)
                                    },
                                )
                            }
                            Route.CategoryList -> {
                                val customSection by viewModel.customSection.collectAsStateWithLifecycle()
                                val context = LocalContext.current
                                CategoryListScreen(
                                    state = categories,
                                    onBack = { stack.pop() },
                                    customSection = customSection,
                                    onRetry = { viewModel.loadCategories(retry = true) },
                                    onOpen = { stack.push(Route.CategoryTopics(it.id, it.slug, it.name)) },
                                    onOpenTopic = { topicId, title, postNumber ->
                                        stack.push(Route.TopicDetail(topicId, title, postNumber))
                                    },
                                    onOpenWebUrl = { url ->
                                        globalBrowserUrl = url
                                    },
                                    onSaveCustomSection = viewModel::saveCustomSection,
                                    onDeleteCustomSection = viewModel::deleteCustomSection,
                                    scrollToTopRequest = scrollToTopRequest,
                                    onSaveOrder = viewModel::saveCategoryOrder,
                                    onResetOrder = viewModel::resetCategoryOrder,
                                    currentTab = categorySubTab,
                                    onTabChange = viewModel::setCategorySubTab,
                                    tagsState = tagsState,
                                    tagSearchQuery = tagSearchQuery,
                                    onTagSearchQueryChange = viewModel::updateTagSearchQuery,
                                    onRetryTags = { viewModel.loadTags(retry = true) },
                                    onOpenTag = { tag -> stack.push(Route.TagTopics(tag)) },
                                    trailing = {},
                                )
                            }

                            is Route.TagTopics -> {
                                val topicState = tagTopics[route.tag] ?: TopicListUiState()
                                LaunchedEffect(route.tag) {
                                    viewModel.loadTagTopics(route.tag)
                                }
                                TopicListScreen(
                                    title = "# ${route.tag}",
                                    tagTitle = route.tag,
                                    showHeaderActions = true,
                                    state = topicState,
                                    feedGraphicMode = feedGraphicMode,
                                    feedDisplayMode = feedDisplayMode,
                                    showTopicUnreadDot = showTopicUnreadDot,
                                    topicUnreadDotPosition = topicUnreadDotPosition,
                                    onRefresh = { viewModel.loadTagTopics(route.tag, refresh = true) },
                                    onLoadMore = { viewModel.loadTagTopics(route.tag, more = true) },
                                    onOpenTopic = {
                                        viewModel.recordTopicListOpen(it.id, it.postsCount)
                                        stack.push(Route.TopicDetail(it.id, it.title))
                                    },
                                    onOpenUser = { username, primaryAvatarUrl, staticAvatarUrl, displayName ->
                                        viewModel.openUserCard(username, primaryAvatarUrl, staticAvatarUrl, displayName)
                                    },
                                    onPrefetchExcerpt = viewModel::prefetchTopicExcerpt,
                                    onScrollBusyChanged = viewModel::setTopicListScrollBusy,
                                    onBack = { stack.pop() },
                                    active = active && listForeground,
                                    trailing = {},
                                    onApplyIncoming = {},
                                    onSetNotificationLevel = { topic, level ->
                                        viewModel.setTopicNotificationLevel(topic.id, level)
                                    },
                                    sortOrder = (topicListSorts[TopicListKey.tag(route.tag)] ?: TopicListSort()).order,
                                    sortAscending = (topicListSorts[TopicListKey.tag(route.tag)] ?: TopicListSort()).ascending,
                                    onSortChange = { viewModel.setTopicSort(it, currentTag = route.tag) },
                                    floatingBallEnabled = floatingBallEnabled,
                                    floatingBallActions = floatingBallActions,
                                    floatingBallXRatio = floatingBallXRatio,
                                    floatingBallYRatio = floatingBallYRatio,
                                    floatingBallOnRight = floatingBallOnRight,
                                    onFloatingBallPositionChange = viewModel::updateFloatingBallPosition,
                                    onNavigateToDrafts = { stack.push(Route.Drafts) },
                                    onNavigateToSearch = { openScopedSearch(TopicSearchScope(tag = route.tag)) },
                                    onNavigateToCategories = ::openCategories,
                                    onNavigateToHistory = { stack.push(Route.BrowsingHistory) },
                                    onNavigateToConnect = { showConnectSheet = true },
                                    onNavigateToChat = openChat,
                                onNavigateToLdcStore = { globalBrowserUrl = org.linuxdo.android.ui.screen.ProfileWebApp.Store.url },
                                onNavigateToLdcMail = { globalBrowserUrl = org.linuxdo.android.ui.screen.ProfileWebApp.Mail.url },
                                    onNavigateToBookmarks = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserTopics(username, "我的收藏", isBookmark = true))
                                        }
                                    },
                                    onNavigateToFollowing = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.FollowList(username))
                                        }
                                    },
                                    onNavigateToLikes = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserReplies(username, "我的点赞", filter = "1"))
                                        }
                                    },
                                )
                            }

                            is Route.CategoryTopics -> {
                                val variants = remember(categories.items, route.categoryId) {
                                    val group = compactCategoryGroups(categories.items).firstOrNull { (parent, levels) ->
                                        levels.isNotEmpty() && (parent.id == route.categoryId || levels.any { it.id == route.categoryId })
                                    }
                                    if (group != null) {
                                        (listOf(group.first) + group.second.sortedBy { categoryLevelOf(it) })
                                            .filter { categoryLevelOf(it) in 0..3 }
                                    } else {
                                        emptyList()
                                    }
                                }
                                TopicListScreen(
                                    title = route.name,
                                    state = categoryTopics[route.categoryId] ?: TopicListUiState(),
                                    showHeaderActions = true,
                                    feedGraphicMode = feedGraphicMode,
                                    feedDisplayMode = feedDisplayMode,
                                    showTopicUnreadDot = showTopicUnreadDot,
                                    topicUnreadDotPosition = topicUnreadDotPosition,
                                    onRefresh = { viewModel.loadCategory(route.categoryId, route.slug, refresh = true) },
                                    onLoadMore = { viewModel.loadCategory(route.categoryId, route.slug, more = true) },
                                    onOpenTopic = {
                                        viewModel.recordTopicListOpen(it.id, it.postsCount)
                                        stack.push(Route.TopicDetail(it.id, it.title))
                                    },
                                    onOpenUser = { username, primaryAvatarUrl, staticAvatarUrl, displayName ->
                                        viewModel.openUserCard(username, primaryAvatarUrl, staticAvatarUrl, displayName)
                                    },
                                    onPrefetchExcerpt = viewModel::prefetchTopicExcerpt,
                                    onScrollBusyChanged = viewModel::setTopicListScrollBusy,
                                    onBack = { stack.pop() },
                                    active = active && listForeground,
                                    trailing = {},
                                    onApplyIncoming = { viewModel.applyIncomingTopics(state.homeFeed, categoryId = route.categoryId) },
                                    categoryVariants = variants,
                                    currentCategoryId = route.categoryId,
                                    onSelectCategoryVariant = { target ->
                                        if (target.id != route.categoryId) {
                                            stack.replaceCurrent(Route.CategoryTopics(target.id, target.slug, target.name))
                                            viewModel.loadCategory(target.id, target.slug)
                                        }
                                    },
                                    onSetNotificationLevel = { topic, level ->
                                        viewModel.setTopicNotificationLevel(topic.id, level)
                                    },
                                    onCreateTopic = if (state.username != null) ({ openDrafts() }) else null,
                                    onBookmarkTopic = { topic, onResult ->
                                        viewModel.bookmarkTopicFromList(topic.id, onResult)
                                    },
                                    onFlagTopic = { topic, reasonId, message, onSuccess, onError ->
                                        viewModel.submitTopicFlagFromList(topic.id, reasonId, message, onSuccess, onError)
                                    },
                                    sortOrder = (topicListSorts[TopicListKey.category(route.categoryId)] ?: TopicListSort()).order,
                                    sortAscending = (topicListSorts[TopicListKey.category(route.categoryId)] ?: TopicListSort()).ascending,
                                    onSortChange = { viewModel.setTopicSort(it, currentCategoryId = route.categoryId, currentCategorySlug = route.slug) },
                                    floatingBallEnabled = floatingBallEnabled,
                                    floatingBallActions = floatingBallActions,
                                    floatingBallXRatio = floatingBallXRatio,
                                    floatingBallYRatio = floatingBallYRatio,
                                    floatingBallOnRight = floatingBallOnRight,
                                    onFloatingBallPositionChange = viewModel::updateFloatingBallPosition,
                                    onNavigateToDrafts = { stack.push(Route.Drafts) },
                                    onNavigateToSearch = { openScopedSearch(TopicSearchScope(categoryId = route.categoryId, categoryName = route.name)) },
                                    onNavigateToCategories = ::openCategories,
                                    onNavigateToHistory = { stack.push(Route.BrowsingHistory) },
                                    onNavigateToConnect = { showConnectSheet = true },
                                    onNavigateToChat = openChat,
                                onNavigateToLdcStore = { globalBrowserUrl = org.linuxdo.android.ui.screen.ProfileWebApp.Store.url },
                                onNavigateToLdcMail = { globalBrowserUrl = org.linuxdo.android.ui.screen.ProfileWebApp.Mail.url },
                                    onNavigateToBookmarks = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserTopics(username, "我的收藏", isBookmark = true))
                                        }
                                    },
                                    onNavigateToFollowing = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.FollowList(username))
                                        }
                                    },
                                    onNavigateToLikes = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserReplies(username, "我的点赞", filter = "1"))
                                        }
                                    },
                                    onNavigateToReplies = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserReplies(username, "我的回复", filter = "5"))
                                        }
                                    },
                                )
                            }

                            is Route.Search -> SearchScreen(
                                state = if (search.scope == route.scope) search else SearchUiState(scope = route.scope),
                                onQuery = viewModel::updateSearch,
                                onLoadMore = viewModel::loadMoreSearch,
                                onOpenTopic = {
                                    viewModel.recordTopicListOpen(it.topic.id, it.topic.postsCount)
                                    stack.push(Route.TopicDetail(it.topic.id, it.topic.title, it.targetPostNumber))
                                },
                                onOrder = viewModel::setSearchOrder,
                                onBack = { stack.pop() },
                                active = active,
                                scrollToTopRequest = scrollToTopRequest,
                                trailing = {},
                                searchHistory = searchHistory,
                                onSelectHistory = { keyword ->
                                    viewModel.updateSearch(keyword)
                                    viewModel.addSearchHistory(keyword)
                                },
                                onClearHistory = viewModel::clearSearchHistory,
                            )

                            Route.Profile, is Route.UserProfile -> {
                                val profileRoute = route as? Route.UserProfile
                                val profileUsername = profileRoute?.username ?: state.username
                                val ownProfile = profileRoute == null || profileUsername.equals(state.username, ignoreCase = true)
                                if (ownProfile) {
                                    ProfileScreen(
                                        username = profileUsername,
                                        active = active,
                                        accountState = accountState,
                                        onAddAccount = viewModel::addAccount,
                                        onOpenTopic = { id, title, post -> stack.push(Route.TopicDetail(id, title, post)) },
                                        onOpenUser = { stack.push(Route.UserProfile(it)) },
                                        onOpenBadges = { stack.push(Route.UserBadges(it)) },
                                        onOpenNetwork = { name, followers -> stack.push(Route.ProfileNetwork(name, followers)) },
                                        onOpenWebsite = { globalBrowserUrl = it },
                                        onOpenConnect = { showConnectSheet = true },
                                        onEditProfile = { globalBrowserUrl = "${Config.BASE_URL}/my/preferences/profile" },
                                        onOpenDisplaySettings = { stack.push(Route.SettingsDetail(org.linuxdo.android.ui.nav.SettingsPage.ProfileDisplay)) },
                                        cdkScore = cdkScoreState.score.takeIf { cdkScoreState.username.equals(profileUsername, ignoreCase = true) },
                                        cdkScoreLoading = drawerScore.loading,
                                        cdkScoreError = drawerScore.error,
                                        cdkScoreNeedsAuthorization = drawerScore.needsAuthorization,
                                        onLoadCdkScore = { viewModel.loadCdkScore(userInitiated = true) },
                                        onReloadCdkScore = { viewModel.loadCdkScore(userInitiated = true) },
                                        onOpenCdkWebsite = { globalBrowserUrl = "https://cdk.linux.do/" },
                                        scrollToTopRequest = scrollToTopRequest,
                                        onBack = if (stack.canPop) ({ stack.pop(); Unit }) else null,
                                    )
                                } else if (profileRoute != null) {
                                    UserProfileScreen(
                                        username = profileRoute.username,
                                        initialAvatarUrl = profileRoute.initialAvatarUrl,
                                        initialStaticAvatarUrl = profileRoute.initialStaticAvatarUrl,
                                        onOpenTopic = { id, title, post -> stack.push(Route.TopicDetail(id, title, post)) },
                                        onOpenUser = { stack.push(Route.UserProfile(it)) },
                                        onOpenBadges = { stack.push(Route.UserBadges(profileRoute.username)) },
                                        onOpenNetwork = { stack.push(Route.ProfileNetwork(profileRoute.username, it)) },
                                        onOpenWebsite = { globalBrowserUrl = it },
                                        onChat = { target -> viewModel.openChatWithUser(target) { stack.push(it) } },
                                        onComposePrivateMessage = { privateMessageRecipient = it },
                                        onSetFollowing = viewModel::setProfileFollowing,
                                        onNotificationSettingsObserved = viewModel::onUserNotificationSettingsObserved,
                                        onOpenDisplaySettings = { stack.push(Route.SettingsDetail(org.linuxdo.android.ui.nav.SettingsPage.ProfileDisplay)) },
                                        onBack = { stack.pop() },
                                    )
                                }
                            }

                            is Route.UserBadges -> ProfileBadgesScreen(
                                username = route.username,
                                onBack = { stack.pop() },
                            )

                            Route.Drafts -> DraftsScreen(
                                state = drafts,
                                categories = categories.items,
                                onBack = { stack.pop() },
                                onRefresh = { viewModel.loadDrafts(refresh = true) },
                                onCreateTopic = {
                                    val category = (stack.entries.dropLast(1).lastOrNull()?.route as? Route.CategoryTopics)?.categoryId
                                    viewModel.openTopicCreator(category, newDraft = true)
                                },
                                onEditDraft = { draft ->
                                    if (draft.action == "createPrivateMessage" || (draft.topicId == null && draft.archetype != "regular")) {
                                        globalBrowserUrl = "${Config.BASE_URL}/my/activity/drafts"
                                    } else if (draft.topicId != null && draft.topicId > 0L) {
                                        stack.push(Route.TopicDetail(draft.topicId, draft.title, draft.replyToPostNumber))
                                        viewModel.openReplyWithDraft(draft)
                                    } else {
                                        viewModel.openDraftInCreator(draft)
                                    }
                                },
                                onDeleteDraft = viewModel::deleteDraft,
                            )

                            Route.BrowsingHistory -> HistoryScreen(
                                entries = history.entries,
                                query = history.query,
                                loading = history.loading,
                                onQueryChange = viewModel::setHistoryQuery,
                                onRefresh = { viewModel.loadHistory() },
                                onLoadMore = { viewModel.loadHistory(refresh = false) },
                                hasMore = history.hasMore,
                                error = history.error,
                                onOpenTopic = { id, title, postNumber ->
                                    stack.push(Route.TopicDetail(id, title, postNumber))
                                },
                                onBack = { stack.pop() },
                            )

                            // 恢复旧版保存的聊天路由时转到消息页。
                            Route.Chat -> LaunchedEffect(active) {
                                if (active) {
                                    stack.pop()
                                    openChat()
                                }
                            }

                            is Route.ChatRoom -> {
                                val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
                                val roomActive = active && !contentObscured && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
                                val channel = chat.channels.firstOrNull { it.id == route.channelId }
                                    ?: org.linuxdo.android.data.ChatChannelSummary(id = route.channelId, title = route.title)
                                val currentUsername = accountState.accounts.firstOrNull { it.userId == accountState.activeUserId }?.username
                                val chatEmojiUrls by viewModel.chatEmojiUrls.collectAsStateWithLifecycle()
                                ChatRoomScreen(
                                    channel = channel,
                                    state = chat,
                                    active = roomActive,
                                    currentUsername = currentUsername,
                                    emojiUrls = chatEmojiUrls,
                                    onLoad = viewModel::loadChatRoom,
                                    onLoadPast = viewModel::loadMoreChatHistory,
                                    onSetChannelMuted = viewModel::setChatChannelMuted,
                                    onMessagesVisible = viewModel::markChatMessagesVisible,
                                    onSend = viewModel::sendChatMessage,
                                    onUploadAttachment = viewModel::uploadChatAttachment,
                                    onOpenUser = viewModel::openUserCard,
                                    onOpenTopic = { topicId, title, postNumber ->
                                        stack.push(Route.TopicDetail(topicId, title, postNumber))
                                    },
                                    onOpenExternalUrl = { url -> globalBrowserUrl = url },
                                    onBack = { stack.pop() },
                                )
                            }

                            is Route.ProfileNetwork -> org.linuxdo.android.ui.screen.ProfileNetworkScreen(
                                username = route.username, followers = route.followers,
                                accountId = accountState.activeUserId,
                                onBack = { stack.pop() }, onOpenUser = viewModel::openUserCard,
                            )

                            is Route.FollowList -> FollowListScreen(
                                state = followList,
                                onBack = { stack.pop() },
                                onRetry = { viewModel.loadFollowing(route.username, refresh = true) },
                                onOpenUser = viewModel::openUserCard,
                                onUnfollow = viewModel::unfollowUser,
                                onUnfollowBatch = viewModel::unfollowBatch,
                            )

                            is Route.UserTopics -> {
                                val key = "${if (route.isBookmark) "bookmark" else "created"}:${route.username}"
                                TopicListScreen(
                                    title = route.title,
                                    state = userTopicStates[key] ?: TopicListUiState(),
                                    feedGraphicMode = feedGraphicMode,
                                    feedDisplayMode = feedDisplayMode,
                                    showTopicUnreadDot = showTopicUnreadDot,
                                    topicUnreadDotPosition = topicUnreadDotPosition,
                                    onRefresh = { viewModel.loadUserTopics(route.username, route.isBookmark, refresh = true) },
                                    onLoadMore = { viewModel.loadUserTopics(route.username, route.isBookmark, more = true) },
                                    onOpenTopic = {
                                        viewModel.recordTopicListOpen(it.id, it.postsCount)
                                        stack.push(Route.TopicDetail(it.id, it.title))
                                    },
                                    onOpenItem = { item ->
                                        viewModel.recordTopicListOpen(item.topic.id, item.topic.postsCount)
                                        stack.push(Route.TopicDetail(item.topic.id, item.topic.title, item.targetPostNumber))
                                    },
                                    onRemoveBookmark = if (route.isBookmark && route.username.equals(state.username, ignoreCase = true)) viewModel::removeBookmarkFromList else null,
                                    onScrollBusyChanged = viewModel::setTopicListScrollBusy,
                                    onOpenUser = { username, primaryAvatarUrl, staticAvatarUrl, displayName ->
                                        viewModel.openUserCard(username, primaryAvatarUrl, staticAvatarUrl, displayName)
                                    },
                                    onBack = { stack.pop() },
                                    showLargeTitle = false,
                                    unifiedSecondaryHeader = true,
                                    inlineSearchQuery = if (route.isBookmark) bookmarkSearchQueries[route.username].orEmpty() else null,
                                    inlineSearchPlaceholder = "搜索收藏的标题或内容",
                                    onInlineSearchQueryChange = if (route.isBookmark) ({ query ->
                                        viewModel.updateBookmarkSearchQuery(route.username, query)
                                    }) else null,
                                    active = active,
                                    trailing = {},
                                    onSetNotificationLevel = { topic, level ->
                                        viewModel.setTopicNotificationLevel(topic.id, level)
                                    },
                                    onBookmarkTopic = if (route.isBookmark) null else { topic, onResult ->
                                        viewModel.bookmarkTopicFromList(topic.id, onResult)
                                    },
                                    onFlagTopic = { topic, reasonId, message, onSuccess, onError ->
                                        viewModel.submitTopicFlagFromList(topic.id, reasonId, message, onSuccess, onError)
                                    },
                                    sortEnabled = false,
                                    floatingBallEnabled = floatingBallEnabled,
                                    floatingBallActions = floatingBallActions,
                                    floatingBallXRatio = floatingBallXRatio,
                                    floatingBallYRatio = floatingBallYRatio,
                                    floatingBallOnRight = floatingBallOnRight,
                                    onFloatingBallPositionChange = viewModel::updateFloatingBallPosition,
                                    onNavigateToDrafts = { stack.push(Route.Drafts) },
                                    onNavigateToSearch = ::openSearch,
                                    onNavigateToCategories = ::openCategories,
                                    onNavigateToHistory = { stack.push(Route.BrowsingHistory) },
                                    onNavigateToConnect = { showConnectSheet = true },
                                    onNavigateToChat = openChat,
                                onNavigateToLdcStore = { globalBrowserUrl = org.linuxdo.android.ui.screen.ProfileWebApp.Store.url },
                                onNavigateToLdcMail = { globalBrowserUrl = org.linuxdo.android.ui.screen.ProfileWebApp.Mail.url },
                                    onNavigateToBookmarks = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserTopics(username, "我的收藏", isBookmark = true))
                                        }
                                    },
                                    onNavigateToFollowing = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.FollowList(username))
                                        }
                                    },
                                    onNavigateToLikes = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserReplies(username, "我的点赞", filter = "1"))
                                        }
                                    },
                                    onNavigateToReplies = {
                                        val username = state.username
                                        if (!username.isNullOrBlank()) {
                                            stack.push(Route.UserReplies(username, "我的回复", filter = "5"))
                                        }
                                    },
                                )
                            }

                            is Route.UserReplies -> {
                                val stateKey = if (route.filter == "5") route.username else "${route.username}:${route.filter}"
                                val repliesState = userRepliesStates[stateKey] ?: org.linuxdo.android.data.UserRepliesUiState()
                                org.linuxdo.android.ui.screen.UserRepliesScreen(
                                    title = route.title,
                                    state = repliesState,
                                    onRefresh = { viewModel.loadUserReplies(route.username, refresh = true, filter = route.filter) },
                                    onLoadMore = { viewModel.loadUserReplies(route.username, more = true, filter = route.filter) },
                                    onOpenTopic = { id, title, postNumber ->
                                        stack.push(Route.TopicDetail(id, title, postNumber))
                                    },
                                    onOpenUser = viewModel::openUserCard,
                                    onBack = { stack.pop() },
                                )
                            }

                            Route.Settings, is Route.SettingsDetail -> SettingsScreen(
                                page = (route as? Route.SettingsDetail)?.page,
                                onOpenPage = { stack.push(Route.SettingsDetail(it)) },
                                treeView = treeView,
                                onTreeView = viewModel::setTreeView,
                                showUnreadDot = showUnreadDot,
                                onShowUnreadDot = viewModel::setShowUnreadDot,
                                unreadDotPosition = unreadDotPosition,
                                onUnreadDotPosition = viewModel::setUnreadDotPosition,
                                showTopicUnreadDot = showTopicUnreadDot,
                                onShowTopicUnreadDot = viewModel::setShowTopicUnreadDot,
                                topicUnreadDotPosition = topicUnreadDotPosition,
                                onTopicUnreadDotPosition = viewModel::setTopicUnreadDotPosition,
                                seeSignatures = seeSignatures,
                                signatureSyncBusy = signatureSyncBusy,
                                signatureSyncNotice = signatureSyncNotice,
                                onSyncSeeSignatures = { viewModel.syncSignaturePreference(fromWebPreferences = false) },
                                onOpenWebSignaturePreferences = {
                                    viewModel.clearSignatureSyncNotice()
                                    globalBrowserUrl = "${org.linuxdo.android.Config.BASE_URL}/my/preferences/profile"
                                },
                                onBack = { stack.pop() },
                                themeMode = themeMode,
                                onThemeMode = viewModel::setThemeMode,
                                fontScale = fontScale,
                                onFontScale = viewModel::setFontScale,
                                showTopicScrollbar = showTopicScrollbar,
                                onShowTopicScrollbar = viewModel::setShowTopicScrollbar,
                                floatingBallEnabled = floatingBallEnabled,
                                onFloatingBallEnabled = viewModel::setFloatingBallEnabled,
                                floatingBallActions = floatingBallActions,
                                onFloatingBallActions = viewModel::setFloatingBallActions,
                                updateState = updateState,
                                onCheckUpdate = viewModel::onVersionItemClick,
                                syncReadProgress = syncReadProgress,
                                onSyncReadProgress = viewModel::setSyncReadProgress,
                                autoTrackTopic = autoTrackTopic,
                                onAutoTrackTopic = viewModel::setAutoTrackTopic,
                                autoTrackTopicSeconds = autoTrackTopicSeconds,
                                onAutoTrackTopicSeconds = viewModel::setAutoTrackTopicSeconds,
                                onDiagnostics = { stack.push(Route.Diagnostics) },
                                onLogout = viewModel::logout,
                                autoRefreshIncoming = autoRefreshIncoming,
                                onAutoRefreshIncoming = viewModel::setAutoRefreshIncoming,
                                autoRefreshInterval = autoRefreshInterval,
                                onAutoRefreshInterval = viewModel::setAutoRefreshInterval,
                                autoReadClipboard = autoReadClipboardEnabled,
                                onAutoReadClipboard = viewModel::setAutoReadClipboardEnabled,
                                backgroundNotification = backgroundNotification,
                                onBackgroundNotification = viewModel::setBackgroundNotification,
                                notificationPollInterval = notificationPollInterval,
                                onNotificationPollInterval = viewModel::setNotificationPollInterval,
                                customRealtimeSeconds = customRealtimeSeconds,
                                onCustomRealtimeSeconds = viewModel::setCustomRealtimeSeconds,
                                notificationScope = notificationScope,
                                onNotificationScope = viewModel::setNotificationScope,
                                keepAliveService = keepAliveService,
                                onKeepAliveService = viewModel::setKeepAliveService,
                                onTestNotification = viewModel::testPushNotification,
                                // ── DoH ──────────────────────────────────────────
                                dohEnabled = dohEnabled,
                                onDohEnabled = viewModel::setDohEnabled,
                                dohServers = dohServers,
                                selectedDohServerUrl = selectedDohServerUrl,
                                onDohServerSelected = viewModel::setDohServerSelected,
                                dohAutoFailover = dohAutoFailover,
                                onDohAutoFailover = viewModel::setDohAutoFailover,
                                dohAutoOptimize = dohAutoOptimize,
                                onDohAutoOptimize = viewModel::setDohAutoOptimize,
                                networkMode = networkMode,
                                onNetworkMode = viewModel::setNetworkMode,
                                networkEnvironment = networkEnvironment,
                                activeNetworkRoute = activeNetworkRoute,
                                networkModeSwitching = networkModeSwitching,
                                networkModeError = networkModeError,
                                dohLatencies = dohLatencies,
                                dohSummaries = dohSummaries,
                                isTestingAllDoh = isTestingAllDoh,
                                onTestDohServer = viewModel::testDohServer,
                                onTestAllDohServers = viewModel::testAllDohServers,
                                smartRouteMode = smartRouteMode,
                                smartDirectLatency = smartDirectLatency,
                                smartDohLatency = smartDohLatency,
                                smartRouteSummary = smartRouteSummary,
                                isSmartEvaluating = isSmartEvaluating,
                                onEvaluateSmartRoute = viewModel::evaluateSmartRoute,
                                onAddCustomDohServer = viewModel::addCustomDohServer,
                                onUpdateDohServer = viewModel::updateDohServer,
                                onDeleteDohServer = viewModel::deleteDohServer,
                                onResetDohServers = viewModel::resetDohServersToDefault,
                                feedGraphicMode = feedGraphicMode,
                                onFeedGraphicMode = viewModel::setFeedGraphicMode,
                                feedDisplayMode = feedDisplayMode,
                                onFeedDisplayMode = viewModel::setFeedDisplayMode,
                                skinPreset = skinPreset,
                                onSkinPreset = viewModel::setSkinPreset,
                                isSelfHealing = isSelfHealing,
                                onSelfHealNetwork = { viewModel.selfHealNetwork(context) },
                                showDirectMessageQuickBar = showDirectMessageQuickBar,
                                onShowDirectMessageQuickBar = viewModel::setShowDirectMessageQuickBar,
                            )

                            Route.Notifications -> MessagesScreen(
                                feedDisplayMode = feedDisplayMode,
                                section = messageSection,
                                onSection = { messageSection = it },
                                leading = { PersonalDrawerAvatar(state.username, drawerAccount, ::openPersonalDrawer) },
                                showDirectMessageQuickBar = showDirectMessageQuickBar,
                                currentUsername = accountState.accounts.firstOrNull { it.userId == accountState.activeUserId }?.username,
                                notifications = profile[org.linuxdo.android.data.ProfileSection.Notifications] ?: ProfileUiState(),
                                notificationPages = viewModel.notificationPages.collectAsStateWithLifecycle().value,
                                chat = chat,
                                filter = notificationFilter,
                                unreadFilters = state.unreadFilters,
                                unreadNotifications = state.unreadCount,
                                active = active && listForeground,
                                scrollToTopRequest = scrollToTopRequest,
                                onFilter = viewModel::setNotificationFilter,
                                onRefreshNotifications = { viewModel.loadProfile(org.linuxdo.android.data.ProfileSection.Notifications, refresh = true) },
                                onMore = { viewModel.loadProfile(org.linuxdo.android.data.ProfileSection.Notifications, more = true) },
                                onMarkAllRead = viewModel::markAllNotificationsRead,
                                onMarkAllChatRead = viewModel::markAllChatRead,
                                onRead = viewModel::readNotification,
                                onRefreshChat = { viewModel.loadChatChannels(refresh = true) },
                                onOpenChannel = { stack.push(Route.ChatRoom(it.id, it.displayTitle)) },
                                onStartDirectMessage = { username ->
                                    viewModel.openChatWithUser(username) { targetRoute -> stack.push(targetRoute) }
                                },
                                onSearchUsers = viewModel::searchUsersForDirectMessage,
                                onOpenTopic = { id, title, postNumber ->
                                    viewModel.markNotificationTopicPending(id, postNumber)
                                    val currentRoute = stack.current
                                    if (currentRoute is Route.TopicDetail && currentRoute.topicId == id) {
                                        viewModel.loadTopic(
                                            id,
                                            postNumber = postNumber,
                                            resumePostNumber = viewModel.resolveResumePostNumber(id, postNumber),
                                            fromNotification = true,
                                        )
                                    } else {
                                        stack.push(Route.TopicDetail(id, title, postNumber))
                                    }
                                },
                                onOpenUser = viewModel::openUserCard,
                            )

                                    Route.Diagnostics -> DiagnosticsScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { stack.pop() },
                                    )

                                }
                            }
                        }
                    }
                }
            }
            }

            if (topicCreator.open) {
                TopicCreatorSheet(
                    state = topicCreator,
                    categories = categories.items,
                    onTitleChange = viewModel::updateTopicCreatorTitle,
                    onRawChange = viewModel::updateTopicCreatorRaw,
                    onCategoryChange = viewModel::updateTopicCreatorCategory,
                    onTagsChange = viewModel::updateTopicCreatorTags,
                    onSearchTags = viewModel::searchTopicCreatorTags,
                    onAttach = viewModel::attachToTopicCreator,
                    onRemoveAttachment = viewModel::removeTopicCreatorAttachment,
                    onSaveDraft = viewModel::saveTopicDraft,
                    onSubmit = {
                        viewModel.submitTopicCreator { topicId, title, postNumber ->
                            stack.push(Route.TopicDetail(topicId, title, postNumber))
                        }
                    },
                    onClose = viewModel::closeTopicCreator,
                    onClearNotice = viewModel::clearTopicCreatorNotice,
                )
            }
        }
        val shouldShowTabBar = browsing && isMainTabRoot && listBars.value && !contentObscured && !isKeyboardOpen
        if (browsing && topicSearch.open) {
            TopicSearchSheet(topicSearch, { viewModel.updateTopicSearch(it) },
                { viewModel.updateTopicSearch(topicSearch.query, it) }, viewModel::loadMoreTopicSearch,
                viewModel::openTopicSearchHit, viewModel::closeTopicSearch)
        }
        if (browsing && metadataEditor.open) {
            TopicMetadataEditorSheet(metadataEditor, viewModel::updateMetadataTitle, viewModel::updateMetadataCategory,
                viewModel::updateMetadataTags, viewModel::searchMetadataTags, viewModel::submitTopicMetadata,
                { viewModel.openTopicMetadataEditor(metadataEditor.topicId, preserveInput = true) }, viewModel::closeTopicMetadataEditor)
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = shouldShowTabBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(220)) { it } +
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
            exit = androidx.compose.animation.slideOutVertically(animationSpec = androidx.compose.animation.core.tween(220)) { it } +
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(180)),
        ) {
            TabBar(
                selected,
                modifier = boardSwipe,
                onCreateTopic = {
                    if (browsing && isMainTabRoot && !contentObscured) {
                        openDrafts()
                    }
                },
                unreadCount = state.unreadCount + chatUnread,
            ) { tab ->
                if (browsing && isMainTabRoot && !contentObscured) {
                    if (tab == selected) {
                        stacks.getValue(tab).popToRoot()
                        scrollRequests.getValue(tab).intValue++
                    } else {
                        selectBoard(tab)
                    }
                }
            }
        }

        val requestedMiniAppUrl = org.linuxdo.android.ui.screen.MiniAppSessionPool.requestedMiniAppUrl
        LaunchedEffect(requestedMiniAppUrl, browsing) {
            if (browsing && !requestedMiniAppUrl.isNullOrBlank()) {
                org.linuxdo.android.ui.screen.MiniAppSessionPool.clearRequest()
                globalBrowserUrl = requestedMiniAppUrl
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = detectedClipboardTarget != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (shouldShowTabBar) 64.dp else 16.dp, start = 16.dp, end = 16.dp)
                .zIndex(10f),
            enter = androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(240)) { it / 2 } +
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.slideOutVertically(animationSpec = androidx.compose.animation.core.tween(200)) { it / 2 } +
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(160)),
        ) {
            val target = detectedClipboardTarget
            if (target != null) {
                ClipboardLinkDetectionCard(
                    rawUrl = target.first,
                    link = target.second,
                    onView = {
                        val consumed = viewModel.consumeClipboardTarget(context, target.first)
                        val linkToOpen = consumed ?: target.second
                        if (linkToOpen.isMiniApp) {
                            linkToOpen.miniAppUrl?.let { globalBrowserUrl = it }
                        } else {
                            selected = MainTab.Latest
                            val latestStack = stacks.getValue(MainTab.Latest)
                            val currentRoute = latestStack.current
                            if (currentRoute is Route.TopicDetail && currentRoute.topicId == linkToOpen.topicId) {
                                viewModel.loadTopic(
                                    linkToOpen.topicId,
                                    postNumber = linkToOpen.postNumber,
                                    resumePostNumber = viewModel.resolveResumePostNumber(linkToOpen.topicId, linkToOpen.postNumber),
                                    fromNotification = true,
                                )
                            } else {
                                latestStack.push(Route.TopicDetail(linkToOpen.topicId, "", linkToOpen.postNumber))
                            }
                        }
                    },
                    onDismiss = {
                        viewModel.dismissClipboardTarget(context, target.first)
                    },
                )
            }
        }

        org.linuxdo.android.ui.screen.MiniAppFloatingPill(
            onRestore = { url -> globalBrowserUrl = url },
            modifier = Modifier.zIndex(18f),
        )
    }
    }
    if (showDrawerAccounts) {
        AccountSwitchBottomSheet(accountState,
            onDismiss = { showDrawerAccounts = false },
            onSwitch = { id ->
                showDrawerAccounts = false
                drawerReturnEntry = null
                drawerReturnReady = false
                drawerScope.launch { drawerState.snapTo(DrawerValue.Closed); viewModel.switchAccount(id) }
            },
            onRemoveAccount = viewModel::removeAccount,
            onAddAccount = {
                showDrawerAccounts = false
                drawerScope.launch { drawerState.close(); viewModel.addAccount() }
            })
    }
    if (showDrawerScore) CommunityScoreDialog(drawerScore.score, { viewModel.loadCdkScore(userInitiated = true) },
        drawerScore.loading, drawerScore.error,
        onOpenWebsite = {
            showDrawerScore = false
            onDrawerAction(PersonalDrawerAction.Web("https://cdk.linux.do/"))
        },
    ) { showDrawerScore = false }
    if (showConnectSheet) {
        ConnectWebSheet(
            state = trustReport,
            ldcState = ldcState,
            onLoad = { viewModel.loadTrustReport() },
            onRefresh = { viewModel.loadTrustReport(refresh = true) },
            onLoadLdc = { viewModel.loadLdcUserInfo() },
            onRefreshLdc = { viewModel.loadLdcUserInfo(refresh = true) },
            onAuthorizeLdc = { viewModel.authorizeLdc() },
            onOpenWebUrl = { globalBrowserUrl = it },
            onDismiss = { showConnectSheet = false },
        )
    }
    globalBrowserUrl?.let { url ->
        val dismissBrowser = {
                val closedUrl = globalBrowserUrl
                globalBrowserUrl = null
                if (closedUrl?.contains("/my/preferences") == true) {
                    viewModel.syncSignaturePreference(fromWebPreferences = true)
                }
        }
        if (Uri.parse(url).host == Config.HOST && Uri.parse(url).path == "/my/preferences/profile" && state.username != null) {
            ProfileEditorDialog(state.username, accountState.activeUserId, onDismiss = dismissBrowser)
        } else if (Uri.parse(url).host == "cdk.linux.do") {
            val cdkUsername = state.username.orEmpty()
            DisposableEffect(cdkUsername) {
                viewModel.setCdkAuthorizationOpen(true)
                onDispose { viewModel.setCdkAuthorizationOpen(false) }
            }
            key(cdkUsername) {
                CdkAuthorizationSheet(username = cdkUsername, onAuthorized = { generation, score ->
                    viewModel.onCdkAuthorized(cdkUsername, generation, score)
                    dismissBrowser()
                }, onDismiss = dismissBrowser)
            }
        } else {
            InAppBrowserSheet(url = url, onDismiss = dismissBrowser)
        }
    }
    if (userCard.loading || userCard.user != null || userCard.error != null) {
        UserCardBottomSheet(
            state = userCard,
            onDismiss = viewModel::closeUserCard,
            signedIn = !state.username.isNullOrBlank(),
            onSetFollowing = viewModel::setProfileFollowing,
            onChat = { username -> viewModel.openChatWithUser(username) { stack.push(it) } },
            onOpenProfile = { username ->
                val card = userCard.user?.takeIf { it.username.equals(username, ignoreCase = true) }
                val staticAvatar = org.linuxdo.android.data.avatarUrlOf(card?.avatarTemplate, size = 160)
                val avatar = org.linuxdo.android.data.avatarUrlOf(card?.avatarTemplate,
                    card?.animatedAvatar, size = 160, username = username)
                viewModel.closeUserCard()
                stack.push(Route.UserProfile(username, initialAvatarUrl = avatar,
                    initialStaticAvatarUrl = staticAvatar))
            },
            onOpenBadges = { username ->
                viewModel.closeUserCard()
                stack.push(Route.UserBadges(username))
            },
            communityScore = drawerScore.takeIf { userCard.isSelf },
            onOpenNetwork = { username, followers ->
                viewModel.closeUserCard()
                stack.push(Route.ProfileNetwork(username, followers))
            },
            onOpenCommunityScore = if (userCard.isSelf) ({
                viewModel.closeUserCard()
                viewModel.loadCdkScore(userInitiated = true)
                showDrawerScore = true
            }) else null,
        )
    }
    val privateMessageDrafts = remember(accountState.activeUserId) { mutableMapOf<String, org.linuxdo.android.ui.screen.PrivateMessageInputDraft>() }
    privateMessageRecipient?.let { recipient ->
        key(accountState.activeUserId, recipient) {
            PrivateMessageComposeSheet(
                initialRecipient = recipient,
                initialDraft = privateMessageDrafts[recipient],
                onKeepDraft = { draft ->
                    if (draft == null) privateMessageDrafts.remove(recipient) else privateMessageDrafts[recipient] = draft
                },
                onDismiss = { privateMessageRecipient = null },
                onSend = viewModel::createPrivateMessage,
                onUploadAttachment = viewModel::uploadAttachmentDirect,
                onCreated = { topicId, title ->
                    viewModel.loadProfile(org.linuxdo.android.data.ProfileSection.Messages, refresh = true)
                    stack.push(Route.TopicDetail(topicId, title))
                },
            )
        }
    }
}

@Composable
private fun BootScreen() {
    Box(
        Modifier.fillMaxSize().background(IosTheme.colors.groupedBackground),
        contentAlignment = Alignment.Center,
    ) {
        IosActivityIndicator(diameter = 28.dp)
    }
}

@Composable
private fun ClipboardLinkDetectionCard(
    rawUrl: String,
    link: InternalTopicLink,
    onView: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = IosTheme.colors.accent
    val miniAppName = link.miniAppUrl?.let { org.linuxdo.android.ui.screen.rememberMiniAppName(it) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(IosTheme.colors.card)
            .border(0.5.dp, IosTheme.colors.separator.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                        val stroke = Stroke(2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val l1 = Path().apply {
                            moveTo(10f, 13f); lineTo(8f, 15f); cubicTo(6.5f, 16.5f, 6.5f, 19f, 8f, 20.5f); cubicTo(9.5f, 22f, 12f, 22f, 13.5f, 20.5f); lineTo(15.5f, 18.5f)
                        }
                        val l2 = Path().apply {
                            moveTo(14f, 11f); lineTo(16f, 9f); cubicTo(17.5f, 7.5f, 17.5f, 5f, 16f, 3.5f); cubicTo(14.5f, 2f, 12f, 2f, 10.5f, 3.5f); lineTo(8.5f, 5.5f)
                        }
                        drawPath(l1, accentColor, style = stroke)
                        drawPath(l2, accentColor, style = stroke)
                        drawLine(accentColor, Offset(9.5f, 14.5f), Offset(14.5f, 9.5f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (link.isMiniApp) "检测到小程序链接" else "检测到剪贴板帖子链接",
                    style = IosTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                    color = IosTheme.colors.label,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                val floorText = if (link.postNumber != null) " · #${link.postNumber}楼" else ""
                Text(
                    text = if (link.isMiniApp) miniAppName ?: rawUrl else "话题 #${link.topicId}$floorText",
                    style = IosTheme.type.caption.copy(fontSize = 12.sp),
                    color = IosTheme.colors.secondaryLabel,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosTheme.colors.accent)
                    .clickable(onClick = onView)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "打开",
                    color = Color.White,
                    style = IosTheme.type.footnote.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                )
            }

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    color = IosTheme.colors.secondaryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatDownloadBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        return if (kb < 10.0) String.format(java.util.Locale.US, "%.1f KB", kb) else "${kb.roundToInt()} KB"
    }
    val mb = kb / 1024.0
    return if (mb < 1024.0) {
        String.format(java.util.Locale.US, "%.2f MB", mb)
    } else {
        String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
    }
}

private fun openDownloadedAttachment(context: Context, uri: Uri, mimeType: String?) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType?.takeIf { it.isNotBlank() } ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val opened = runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
    if (!opened) {
        Toast.makeText(context, "未找到可直接打开此文件的应用，已保存至「下载/LinuxDo」", Toast.LENGTH_LONG).show()
    }
}

/**
 * 全局悬浮附件下载进度卡片（iOS 灵动胶囊风格）：
 * 挂载在 AppShell 顶层，切换出话题或切 Tab 后仍持续在后台下载并实时推进进度。
 */
@Composable
private fun AttachmentDownloadOverlayCard(
    state: AttachmentDownloadUiState,
    dark: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (Uri, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = IosTheme.colors.accent
    val successColor = if (dark) Color(0xFF30D158) else Color(0xFF28B446)
    val errorColor = if (dark) Color(0xFFFF453A) else Color(0xFFE53935)
    val tintColor = when (state.stage) {
        AttachmentDownloadStage.COMPLETED -> successColor
        AttachmentDownloadStage.FAILED -> errorColor
        else -> accent
    }
    val borderColor = if (dark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        IosTheme.colors.separator.copy(alpha = 0.48f)
    }
    val rawProgress = state.progressFraction
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress ?: 0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "attachmentDownloadProgress",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "attachmentDownloadShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "attachmentShimmerOffset",
    )
    val spinnerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "attachmentSpinnerAngle",
    )

    val subtitleText = when (state.stage) {
        AttachmentDownloadStage.DOWNLOADING -> {
            val sizePart = if (state.totalBytes > 0L) {
                "${formatDownloadBytes(state.downloadedBytes)} / ${formatDownloadBytes(state.totalBytes)}"
            } else if (state.downloadedBytes > 0L) {
                "已接收 ${formatDownloadBytes(state.downloadedBytes)}"
            } else {
                "正在接收数据…"
            }
            val speedPart = if (state.speedBytesPerSec > 0L) " · ${formatDownloadBytes(state.speedBytesPerSec)}/s" else ""
            sizePart + speedPart
        }
        AttachmentDownloadStage.COMPLETED -> {
            val sizePart = if (state.downloadedBytes > 0L) " · ${formatDownloadBytes(state.downloadedBytes)}" else ""
            "下载完成$sizePart · 下载/LinuxDo"
        }
        AttachmentDownloadStage.FAILED -> state.statusText
        else -> state.statusText
    }

    val percentText = if (state.stage == AttachmentDownloadStage.DOWNLOADING && rawProgress != null) {
        "${(animatedProgress * 100f).roundToInt().coerceIn(0, 100)}%"
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (dark) 18.dp else 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = if (dark) 0.45f else 0.16f),
                spotColor = Color.Black.copy(alpha = if (dark) 0.45f else 0.16f),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(IosTheme.colors.card)
            .border(0.6.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧状态徽章（进行中带实时环形进度/旋转光弧，完成为勾选，失败为叹号）
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tintColor.copy(alpha = if (dark) 0.18f else 0.11f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(26.dp)) {
                        val strokeW = 2.2.dp.toPx()
                        val pad = strokeW / 2f
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)
                        when (state.stage) {
                            AttachmentDownloadStage.COMPLETED -> {
                                drawCircle(
                                    color = tintColor.copy(alpha = 0.2f),
                                    radius = size.minDimension / 2f - pad,
                                    style = Stroke(width = strokeW),
                                )
                                val check = Path().apply {
                                    moveTo(size.width * 0.27f, size.height * 0.52f)
                                    lineTo(size.width * 0.44f, size.height * 0.69f)
                                    lineTo(size.width * 0.74f, size.height * 0.34f)
                                }
                                drawPath(
                                    path = check,
                                    color = tintColor,
                                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                            AttachmentDownloadStage.FAILED -> {
                                drawCircle(
                                    color = tintColor.copy(alpha = 0.25f),
                                    radius = size.minDimension / 2f - pad,
                                    style = Stroke(width = strokeW),
                                )
                                drawLine(
                                    color = tintColor,
                                    start = Offset(size.width * 0.5f, size.height * 0.28f),
                                    end = Offset(size.width * 0.5f, size.height * 0.57f),
                                    strokeWidth = 2.3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                                drawCircle(
                                    color = tintColor,
                                    radius = 1.4.dp.toPx(),
                                    center = Offset(size.width * 0.5f, size.height * 0.71f),
                                )
                            }
                            else -> {
                                drawArc(
                                    color = tintColor.copy(alpha = 0.18f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = strokeW),
                                )
                                if (rawProgress != null && state.stage == AttachmentDownloadStage.DOWNLOADING) {
                                    drawArc(
                                        color = tintColor,
                                        startAngle = -90f,
                                        sweepAngle = (animatedProgress * 360f).coerceAtLeast(10f),
                                        useCenter = false,
                                        topLeft = Offset(pad, pad),
                                        size = arcSize,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                                    )
                                } else {
                                    drawArc(
                                        color = tintColor,
                                        startAngle = spinnerAngle - 90f,
                                        sweepAngle = 105f,
                                        useCenter = false,
                                        topLeft = Offset(pad, pad),
                                        size = arcSize,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                                    )
                                }
                                // 居中精巧向下下载箭头
                                val arrowStroke = 1.8.dp.toPx()
                                drawLine(
                                    color = tintColor,
                                    start = Offset(size.width * 0.5f, size.height * 0.30f),
                                    end = Offset(size.width * 0.5f, size.height * 0.66f),
                                    strokeWidth = arrowStroke,
                                    cap = StrokeCap.Round,
                                )
                                val arrowHead = Path().apply {
                                    moveTo(size.width * 0.36f, size.height * 0.53f)
                                    lineTo(size.width * 0.5f, size.height * 0.68f)
                                    lineTo(size.width * 0.64f, size.height * 0.53f)
                                }
                                drawPath(
                                    path = arrowHead,
                                    color = tintColor,
                                    style = Stroke(width = arrowStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = state.fileName,
                            style = IosTheme.type.footnote.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                            color = IosTheme.colors.label,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (state.pendingCount > 0) {
                            Text(
                                text = "+${state.pendingCount} 排队",
                                style = IosTheme.type.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
                                color = accent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = if (dark) 0.2f else 0.11f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = subtitleText,
                            style = IosTheme.type.caption.copy(fontSize = 11.8.sp),
                            color = when (state.stage) {
                                AttachmentDownloadStage.COMPLETED -> successColor
                                AttachmentDownloadStage.FAILED -> errorColor
                                else -> IosTheme.colors.secondaryLabel
                            },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (percentText != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = percentText,
                                style = IosTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = accent,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                when (state.stage) {
                    AttachmentDownloadStage.COMPLETED -> {
                        val savedUri = state.savedUri
                        if (savedUri != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(successColor)
                                    .clickable {
                                        onOpen(savedUri, state.savedMimeType)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "打开",
                                    color = Color.White,
                                    style = IosTheme.type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "✕",
                                color = IosTheme.colors.secondaryLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    AttachmentDownloadStage.FAILED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(accent)
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "重试",
                                color = Color.White,
                                style = IosTheme.type.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "✕",
                                color = IosTheme.colors.secondaryLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(IosTheme.colors.fieldBackground)
                                .clickable(onClick = onCancel)
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "取消",
                                color = IosTheme.colors.secondaryLabel,
                                style = IosTheme.type.caption.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                            )
                        }
                    }
                }
            }

            // 底部圆角丝滑进度条（进行中显示，有总大小时平滑梯度推进，未知大小时流光扫描）
            if (state.isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(IosTheme.colors.fieldBackground),
                ) {
                    if (rawProgress != null && state.stage == AttachmentDownloadStage.DOWNLOADING) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(accent.copy(alpha = 0.85f), accent),
                                    ),
                                ),
                        )
                    } else {
                        Canvas(Modifier.fillMaxSize()) {
                            val barWidth = size.width * 0.36f
                            val startX = shimmerOffset * size.width
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.15f),
                                        accent,
                                        accent.copy(alpha = 0.15f),
                                    ),
                                    startX = startX,
                                    endX = startX + barWidth,
                                ),
                                topLeft = Offset(startX, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 独立拆出首页路由渲染函数，避免 MainNavigation 的单方法字节码超出 JVM 64KB 限制。 */
@Composable
private fun HomeTopicListRoute(
    state: UiState,
    viewModel: MainViewModel,
    stack: NavStack,
    selected: MainTab,
    active: Boolean,
    browsing: Boolean,
    listForeground: Boolean,
    isKeyboardOpen: Boolean,
    scrollToTopRequest: Int,
    drawerAccount: org.linuxdo.android.data.AccountSession?,
    categories: CategoriesUiState,
    tagsState: TagsUiState,
    tagSearchQuery: String,
    homeFilter: org.linuxdo.android.data.HomeFilterState,
    feedStates: Map<HomeFeedType, TopicListUiState>,
    topicListSorts: Map<String, TopicListSort>,
    feedGraphicMode: Boolean,
    feedDisplayMode: String,
    showTopicUnreadDot: Boolean,
    topicUnreadDotPosition: TopicUnreadDotPosition,
    miniAppHomePullEnabled: Boolean,
    floatingBallEnabled: Boolean,
    floatingBallActions: List<org.linuxdo.android.data.FloatingAction>,
    floatingBallXRatio: Float,
    floatingBallYRatio: Float,
    floatingBallOnRight: Boolean,
    onOpenPersonalDrawer: () -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenScopedSearch: (TopicSearchScope?) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenBrowserUrl: (String) -> Unit,
) {
    val homeSort = topicListSorts[homeFilter.sortKey()] ?: TopicListSort()
    TopicListScreen(
        title = "LINUX DO",
        state = state.latest,
        feedGraphicMode = feedGraphicMode,
        feedDisplayMode = feedDisplayMode,
        showTopicUnreadDot = showTopicUnreadDot,
        topicUnreadDotPosition = topicUnreadDotPosition,
        onRefresh = viewModel::refreshLatest,
        onLoadMore = viewModel::loadMoreLatest,
        onOpenTopic = { topic ->
            viewModel.recordTopicListOpen(topic.id, topic.postsCount)
            stack.push(Route.TopicDetail(topic.id, topic.title))
        },
        onOpenUser = { username, primaryAvatarUrl, staticAvatarUrl, displayName ->
            viewModel.openUserCard(username, primaryAvatarUrl, staticAvatarUrl, displayName)
        },
        onPrefetchExcerpt = viewModel::prefetchTopicExcerpt,
        onScrollBusyChanged = viewModel::setTopicListScrollBusy,
        active = active && listForeground,
        scrollToTopRequest = scrollToTopRequest,
        homeLeading = { PersonalDrawerAvatar(state.username, drawerAccount, onOpenPersonalDrawer) },
        homeFilter = homeFilter,
        homeSignedIn = state.username != null,
        homeTrustLevel = drawerAccount?.trustLevel,
        homeVisibleCategoryIds = categories.visibleCategoryIds,
        homeCategories = categories.items,
        homeCategoriesLoading = categories.loading,
        homeCategoryOrder = categories.order,
        homePinnedCategoryIds = categories.pinnedIds,
        homeTags = tagsState.tags,
        homeTagsLoading = tagsState.loading,
        homeSearchedTags = tagsState.searchedTags,
        homePinnedTags = tagsState.pinnedTags,
        homeTagOrder = tagsState.order,
        homeTagSearchQuery = tagSearchQuery,
        onSelectHomeCategory = viewModel::selectHomeCategory,
        onSelectHomeLevel = viewModel::selectHomeLevel,
        onSelectHomeTag = viewModel::selectHomeTag,
        onClearHomeFilters = viewModel::clearHomeFilters,
        onTogglePinCategory = viewModel::togglePinCategory,
        onMoveCategoryOrder = viewModel::moveCategoryOrder,
        onSaveCategoryOrder = viewModel::saveCategoryOrder,
        onResetCategoryOrder = viewModel::resetCategoryOrder,
        onTogglePinTag = viewModel::togglePinTag,
        onMoveTagOrder = viewModel::moveTagOrder,
        onSaveTagOrder = viewModel::saveTagOrder,
        onResetTagOrder = viewModel::resetTagOrder,
        onEnsureHomeCategories = { if (!categories.loaded && !categories.loading) viewModel.loadCategories() },
        onEnsureHomeTags = { if (!tagsState.loaded && !tagsState.loading) viewModel.loadTags() },
        onHomeTagSearchQueryChange = viewModel::updateTagSearchQuery,
        showHeaderActions = true,
        currentFeed = state.homeFeed,
        miniAppHomePullEnabled = miniAppHomePullEnabled && !isKeyboardOpen,
        onOpenMiniApps = {
            if (browsing && listForeground && !isKeyboardOpen &&
                selected == MainTab.Latest && stack.current == Route.TopicList) {
                onOpenBrowserUrl(org.linuxdo.android.ui.screen.LINUXDO_APPS_PLAZA_URL)
            }
        },
        feedStates = feedStates,
        onFeedSelect = viewModel::switchHomeFeed,
        onPreloadFeed = viewModel::preloadHomeFeed,
        hasUnread = state.unreadCount > 0,
        onApplyIncoming = { viewModel.applyIncomingTopics(state.homeFeed) },
        onSetNotificationLevel = { topic, level ->
            viewModel.setTopicNotificationLevel(topic.id, level)
        },
        onCreateTopic = if (state.username != null) onOpenDrafts else null,
        onBookmarkTopic = { topic, onResult ->
            viewModel.bookmarkTopicFromList(topic.id, onResult)
        },
        onFlagTopic = { topic, reasonId, message, onSuccess, onError ->
            viewModel.submitTopicFlagFromList(topic.id, reasonId, message, onSuccess, onError)
        },
        sortOrder = homeSort.order,
        sortAscending = homeSort.ascending,
        onSortChange = { viewModel.setTopicSort(it) },
        floatingBallEnabled = floatingBallEnabled,
        floatingBallActions = floatingBallActions,
        floatingBallXRatio = floatingBallXRatio,
        floatingBallYRatio = floatingBallYRatio,
        floatingBallOnRight = floatingBallOnRight,
        onFloatingBallPositionChange = viewModel::updateFloatingBallPosition,
        onNavigateToDrafts = { stack.push(Route.Drafts) },
        onNavigateToSearch = {
            val scope = org.linuxdo.android.data.HomeFilterResolver.toSearchScope(homeFilter, categories.items)
            onOpenScopedSearch(scope)
        },
        onNavigateToCategories = onOpenCategories,
        onNavigateToHistory = { stack.push(Route.BrowsingHistory) },
        onNavigateToConnect = onOpenConnect,
        onNavigateToChat = onOpenChat,
        onNavigateToLdcStore = { onOpenBrowserUrl(org.linuxdo.android.ui.screen.ProfileWebApp.Store.url) },
        onNavigateToLdcMail = { onOpenBrowserUrl(org.linuxdo.android.ui.screen.ProfileWebApp.Mail.url) },
        onNavigateToBookmarks = {
            val username = state.username
            if (!username.isNullOrBlank()) {
                stack.push(Route.UserTopics(username, "我的收藏", isBookmark = true))
            }
        },
        onNavigateToFollowing = {
            val username = state.username
            if (!username.isNullOrBlank()) {
                stack.push(Route.FollowList(username))
            }
        },
        onNavigateToLikes = {
            val username = state.username
            if (!username.isNullOrBlank()) {
                stack.push(Route.UserReplies(username, "我的点赞", filter = "1"))
            }
        },
        onNavigateToReplies = {
            val username = state.username
            if (!username.isNullOrBlank()) {
                stack.push(Route.UserReplies(username, "我的回复", filter = "5"))
            }
        },
    )
}
