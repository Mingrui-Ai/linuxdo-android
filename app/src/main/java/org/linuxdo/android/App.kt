package org.linuxdo.android

import android.app.Application
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File
import org.linuxdo.android.data.Diagnostics
import org.linuxdo.android.data.AccountManager
import org.linuxdo.android.data.DraftStore
import org.linuxdo.android.data.HistoryStore
import org.linuxdo.android.data.ReadingStore
import org.linuxdo.android.data.SessionStore
import org.linuxdo.android.net.BrowserSession
import org.linuxdo.android.net.NetworkPath
import org.linuxdo.android.net.OkHttpTransport
import org.linuxdo.android.net.Transport
import org.linuxdo.android.net.UserApiKeyAuthService
import org.linuxdo.android.net.WebViewFetchTransport
import org.linuxdo.android.net.AdaptiveDohDns
import org.linuxdo.android.net.DohFailoverCoordinator
import org.linuxdo.android.net.DohPresets
import org.linuxdo.android.net.DohTransport
import org.linuxdo.android.net.anycast.AnycastPool
import org.linuxdo.android.net.proxy.LocalProxyServer
import org.linuxdo.android.net.NetworkStateMonitor
import org.linuxdo.android.net.NetworkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import org.linuxdo.android.net.buildBootstrapClient
import org.linuxdo.android.net.buildSharedHttpClient

/** 实现 ImageLoaderFactory,Coil 的 AsyncImage 就会自动用上带凭据的共享客户端。 */
class App : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private fun getProcessNameCompat(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        return try {
            val cmdline = File("/proc/self/cmdline").readText().trim { it <= ' ' || it == '\u0000' }
            if (cmdline.isNotEmpty()) cmdline else packageName
        } catch (_: Exception) {
            packageName
        }
    }

    override fun onCreate() {
        super.onCreate()
        val currentProcess = getProcessNameCompat()
        if (currentProcess != packageName) {
            // 是 GeckoView 的沙盒子进程（如 :gpu, :tab 等），设置专属目录后缀并跳过主进程业务容器初始化
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val suffix = currentProcess.substringAfter(":", "child")
                try {
                    WebView.setDataDirectorySuffix(suffix)
                } catch (_: Exception) {}
            }
            return
        }

        container = AppContainer(this)

        org.linuxdo.android.push.LinuxDoPushSdk.init(
            org.linuxdo.android.push.PushConfig(
                targetActivityClass = MainActivity::class.java,
                smallIconResId = R.drawable.ic_launcher_enhanced_monochrome,
                baseUrl = Config.BASE_URL,
                httpClientProvider = { container.httpClient },
                sessionProvider = { ctx ->
                    val store = (ctx.applicationContext as? App)?.container?.sessionStore ?: container.sessionStore
                    object : org.linuxdo.android.push.PushSessionProvider {
                        override fun hasSession(): Boolean = store.hasSession()
                        override fun isBackgroundNotificationEnabled(): Boolean = store.isBackgroundNotificationEnabled()
                        override fun backgroundRequestDelayMs(): Long = maxOf(
                            org.linuxdo.android.data.ApiQuotaPool.automaticReadDelayMs(),
                            org.linuxdo.android.data.TopicRepository.mainSiteGateWaitMs(),
                            org.linuxdo.android.data.ApiQuotaPool.UNREAD_POLL.penaltyRemainingMs(),
                            if (org.linuxdo.android.data.TopicRepository.isSilentRevalidating() ||
                                store.isClearanceRecoveryInProgress() ||
                                store.sessionChanging ||
                                store.shouldPauseBackgroundForChallengedClearance()
                            ) 1_000L else 0L,
                        )
                        override suspend fun tryAcquireBackgroundPoll(includeUnreadQuota: Boolean): Boolean {
                            if (backgroundRequestDelayMs() > 0L) return false
                            val force = !store.getPendingTestReplayTag().isNullOrBlank()
                            if (includeUnreadQuota && !org.linuxdo.android.data.ApiQuotaPool.UNREAD_POLL.tryAcquire(force = force)) {
                                return false
                            }
                            return org.linuxdo.android.data.TopicRepository.tryAcquireBackgroundGetSlot(store)
                        }
                        override fun recordBackgroundPollResponse(
                            path: String,
                            status: Int,
                            cfMitigated: String?,
                            retryAfterSeconds: Long?,
                            sentClearanceFp: String,
                        ): Long {
                            val repo = (ctx.applicationContext as? App)?.container?.topicRepository ?: container.topicRepository
                            return repo.recordBackgroundPollHttpResult(path, status, cfMitigated, retryAfterSeconds, sentClearanceFp)
                        }
                        override fun getCookie(): String = store.storedCookies().entries.joinToString("; ") { "${it.key}=${it.value}" }
                        override fun getUserAgent(): String = container.userAgent
                        override fun getUserId(): Long = store.currentUserId()
                        override fun getSessionGeneration(): Long = store.sessionGeneration
                        override fun saveUserId(id: Long) = store.saveCurrentUserId(id)
                        override fun getLastSeenNotificationId(): Long = store.getLastSeenNotificationId()
                        override fun setLastSeenNotificationId(id: Long) = store.setLastSeenNotificationId(id)
                        override fun getLastBadgeCount(): Int = store.getLastBadgeCount()
                        override fun setLastBadgeCount(count: Int, force: Boolean) = store.setLastBadgeCount(count, force)
                        override fun incrementLastBadgeCount(): Int = store.incrementLastBadgeCount()
                        override fun isBadgeTestHolding(): Boolean = store.isBadgeTestHolding()
                        override fun setBadgeTestHoldUntil(timestamp: Long) = store.setBadgeTestHoldUntil(timestamp)
                        override fun getPendingTestReplayTag(): String? = store.getPendingTestReplayTag()
                        override fun setPendingTestReplayTag(tag: String?) = store.setPendingTestReplayTag(tag)
                        override fun isTestWaitScreenOnArmed(): Boolean = store.isTestWaitScreenOnArmed()
                        override fun setTestWaitScreenOnArmed(armed: Boolean) = store.setTestWaitScreenOnArmed(armed)
                        override fun isAppInForeground(): Boolean = store.isAppInForeground()
                        override fun setAppInForeground(inForeground: Boolean) = store.setAppInForeground(inForeground)
                        override fun getNotificationScope(): org.linuxdo.android.push.PushNotificationScope =
                            org.linuxdo.android.push.PushNotificationScope.fromRaw(store.getNotificationScope().code)
                        override fun getCustomRealtimeSeconds(): Int = store.getCustomRealtimeSeconds()
                        override fun setNotificationTransportHealthyAtMillis(timestamp: Long) {}
                    }
                },
                logger = { key, message, force ->
                    container.diagnostics.logThrottled(key, message, force)
                }
            )
        )
    }

    override fun newImageLoader(): ImageLoader = container.imageLoader
}

class AppContainer(context: Context) {
    val chatRealtime by lazy {
        org.linuxdo.android.data.ChatRealtime(topicRepository, sessionStore) { channel, message ->
            val key = "chat_notified_${sessionStore.currentUserId()}_${channel.id}"
            if (message.id > preferences.getLong(key, 0L)) {
                val shown = org.linuxdo.android.service.NotificationHelper.showNotification(
                    context = context,
                    notificationId = message.id,
                    title = "直接消息 · ${channel.displayTitle}",
                    content = message.message.ifBlank { "收到一条新消息" },
                    chatChannelId = channel.id,
                    chatUserId = sessionStore.currentUserId(),
                )
                if (shown) preferences.edit().putLong(key, message.id).apply()
            }
        }
    }

    val preferences = context.getSharedPreferences("display_preferences", Context.MODE_PRIVATE)
    val profileDisplayStore = org.linuxdo.android.data.ProfileDisplayStore(preferences)
    val diagnostics = Diagnostics(File(context.filesDir, "diagnostics.log")).also {
        org.linuxdo.android.net.NetDiagnostics.sink = it
    }

    init {
        installCrashHandler(context, diagnostics)
        org.linuxdo.android.data.ApiQuotaPool.restoreAutomaticBackoff(preferences)
    }
    private val appContext = context.applicationContext
    val sessionStore = SessionStore(context)
    private val _networkMode = MutableStateFlow(sessionStore.getNetworkMode())
    val networkMode = _networkMode.asStateFlow()
    private val networkModeMutex = Mutex()
    val reading = ReadingStore(context)
    val accountManager = AccountManager(context).also { manager ->
        sessionStore.accountCookieFallback = { uid ->
            val account = manager.account(uid)?.takeIf { it.userId == uid }
            account?.token?.takeIf { it.isNotBlank() }?.let { token ->
                buildMap {
                    put(Config.SESSION_COOKIE, token)
                    account.forumSession?.takeIf { it.isNotBlank() }?.let { put("_forum_session", it) }
                }
            }
        }
    }
    val draftStore = DraftStore(context)
    val historyStore = HistoryStore(context)
    private val initialUserAgent: String = resolveUserAgent(context, sessionStore)
    val userAgent: String get() = try {
        sessionStore.userAgentOrNull() ?: initialUserAgent
    } catch (_: org.linuxdo.android.data.EncryptedStorageUnavailableException) {
        initialUserAgent
    }
    val browser = BrowserSession(
        appContext = context,
        userAgent = userAgent,
        store = sessionStore,
        diagnostics = diagnostics,
        httpClientProvider = { dohCoordinator.currentClient },
        isProxyOrVpnActiveProvider = { sessionStore.getNetworkMode() == NetworkMode.SYSTEM },
    )

    val networkMonitor = NetworkStateMonitor(context) {
        // 环境变化只回收连接，不更改用户选定的联网方式。
        dohCoordinator.refresh()
        // 三个网络栈继续遵循同一个已保存的模式。
        updateWebViewProxyState()
        org.linuxdo.android.net.gecko.GeckoManager.refreshNetworkSettings(context)
        dohCoordinator.startSmartRouteMonitoring()
    }

    /**
     * 内置 Cloudflare Anycast IP 池 ——「直连优先」的寻址基座。
     *
     * 进程级单例：健康探测结果必须跨 transport 代次复用。若随代次重建，每次 VPN 切换或
     * 故障轮换都会把已测得的节点健康状态清空，重新交一遍探测学费。
     */
    val anycastPool: AnycastPool = AnycastPool()

    /**
     * 本地 HTTP CONNECT 代理与 TLS Record 级分片服务（路径 B）。
     * 为系统原生 WebView 访问 *.linux.do 提供免 DNS、Anycast 直连与 ClientHello Record 分片抗审查能力。
     */
    val localProxyServer: LocalProxyServer = LocalProxyServer(
        targetSuffix = Config.HOST,
        anycastPool = anycastPool,
    )

    /**
     * 原生 DoH + ECH 自动故障转移协调器。
     * 维护代次（Generation）、在途请求宽限期退役、单飞轮换与 Call.Factory 动态分发。
     */
    val dohCoordinator: DohFailoverCoordinator = DohFailoverCoordinator(
        context = context,
        store = sessionStore,
        userAgent = userAgent,
        networkMonitor = networkMonitor,
        anycastPool = anycastPool,
        clientFactory = { dns, transport, coordinator, routeMode ->
            buildSharedHttpClient(
                store = sessionStore,
                userAgent = userAgent,
                dns = dns,
                dohTransport = transport,
                failoverCoordinator = coordinator,
                anycastPool = anycastPool,
                routeMode = routeMode,
            )
        },
    )

    /** 动态分发的全局 Call.Factory：新发起的请求永远自动获取当前最新代次的 Client */
    val dynamicCallFactory: okhttp3.Call.Factory = dohCoordinator

    /** 保持向后兼容：全局共享的当前活跃 Client */
    val httpClient: okhttp3.OkHttpClient
        get() = dohCoordinator.currentClient

    val userApiKeyAuthService: UserApiKeyAuthService = UserApiKeyAuthService(
        sessionStore,
        dohCoordinator.currentClient,
        diagnostics,
        // 网页授权兑换发生在 App 内的原生网络栈，外部浏览器里的 cf_clearance 带不过来。
        // 撞盾时复用共享 WebView 静默过一次，比直接抛"获取 CSRF 失败"可靠得多。
        silentRevalidate = { reason -> browser.clearChallenge(reason) },
    )

    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .callFactory(org.linuxdo.android.net.ImageCallFactory { dohCoordinator.currentClient })
        .components {
            add(org.linuxdo.android.net.SiteImageCacheInterceptor())
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory(enforceMinimumFrameDelay = true))
            } else {
                add(GifDecoder.Factory(enforceMinimumFrameDelay = true))
            }
            add(SvgDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(context.cacheDir, "image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()

    private val okHttpTransport: OkHttpTransport = OkHttpTransport(
        sessionStore,
        browser,
        dohCoordinator.currentClient,
        diagnostics,
        epochLabel(dohCoordinator.currentEpoch.get()),
    )
    private val webViewTransport: WebViewFetchTransport = WebViewFetchTransport(browser, diagnostics)

    /** 最近一次退到后台的时间（elapsedRealtime）；0 表示当前在前台或尚未退过后台。 */
    @Volatile private var backgroundSince = 0L

    init {
        dohCoordinator.onEpochChanged = { epoch ->
            val label = epochLabel(epoch)
            okHttpTransport.updateClient(epoch.client, label)
            userApiKeyAuthService.updateClient(epoch.client)
            diagnostics.log("网络链路: 切换到 $label")
        }
        networkMonitor.onDefaultNetworkChanged = {
            diagnostics.log("网络链路: 默认网络已切换，回收连接")
            dohCoordinator.recycleConnections("默认网络切换")
            updateWebViewProxyState()
        }
        updateWebViewProxyState()
        dohCoordinator.warmUpAnycastIfNeeded()
        dohCoordinator.startSmartRouteMonitoring()
    }

    /** WebView 的目标站点使用内置 CONNECT，其余域名直连；系统模式清除覆盖规则。 */
    fun updateWebViewProxyState(onApplied: (() -> Unit)? = null) {
        if (sessionStore.getNetworkMode() == NetworkMode.SYSTEM) {
            LocalProxyServer.clearWebViewProxyOverride(onApplied)
        } else {
            LocalProxyServer.applyWebViewProxyOverride(localProxyServer.port, onApplied = onApplied)
        }
    }

    private suspend fun awaitWebViewNetworkSettings() = withTimeout(8_000L) {
        if (sessionStore.getNetworkMode() == NetworkMode.BUILT_IN) {
            check(localProxyServer.port > 0 && androidx.webkit.WebViewFeature.isFeatureSupported(
                androidx.webkit.WebViewFeature.PROXY_OVERRIDE,
            )) { "此设备的 WebView 暂不支持内置网络" }
        }
        suspendCancellableCoroutine<Unit> { continuation ->
            updateWebViewProxyState { if (continuation.isActive) continuation.resume(Unit) }
        }
    }

    /** 完成浏览器和业务客户端同步后才更新 UI；失败恢复原偏好，保留账号及草稿。 */
    suspend fun setNetworkMode(mode: NetworkMode) = withContext(Dispatchers.Main.immediate) {
        networkModeMutex.withLock {
            val previous = sessionStore.getNetworkMode()
            if (previous == mode) return@withLock
            sessionStore.setNetworkMode(mode)
            try {
                check(org.linuxdo.android.net.gecko.GeckoManager.applyNetworkSettings(appContext)) {
                    "验证浏览器网络设置未完成，请重试"
                }
                awaitWebViewNetworkSettings()
                dohCoordinator.refreshAndAwait()
                // cf_clearance 与出口绑定，仅清除这项验证凭据，不清除登录会话。
                sessionStore.clearCookie(Config.CF_CLEARANCE_COOKIE)
                sessionStore.clearChallengedClearances()
                _networkMode.value = mode
                diagnostics.log("联网方式已切换: ${previous.label} -> ${mode.label}")
                dohCoordinator.warmUpAnycastIfNeeded()
            } catch (error: Exception) {
                withContext(NonCancellable) {
                    sessionStore.setNetworkMode(previous)
                    runCatching { org.linuxdo.android.net.gecko.GeckoManager.applyNetworkSettings(appContext) }
                    runCatching { awaitWebViewNetworkSettings() }
                    dohCoordinator.refreshAndAwait()
                }
                diagnostics.log("联网方式切换未完成，已恢复偏好: ${previous.label} (${error.javaClass.simpleName})")
                throw error
            }
        }
    }

    fun onAppBackground() {
        backgroundSince = android.os.SystemClock.elapsedRealtime()
        dohCoordinator.setAppForeground(false)
    }

    /**
     * 后台久置后回前台：期间 NAT 表项可能已过期，池里的旧连接复用会一路超时，先整体回收。
     * 必须在回前台的首批请求发出之前调用。
     */
    fun onAppForeground() {
        dohCoordinator.setAppForeground(true)
        val since = backgroundSince
        backgroundSince = 0L
        if (since == 0L) return
        val idleMs = android.os.SystemClock.elapsedRealtime() - since
        if (idleMs < STALE_CONNECTION_IDLE_MS) return
        diagnostics.log("网络链路: 后台 ${idleMs / 60_000L} 分钟后回前台，回收连接")
        dohCoordinator.recycleConnections("后台久置回前台")
    }

    fun transport(path: NetworkPath): Transport = when (path) {
        NetworkPath.OKHTTP -> okHttpTransport
        NetworkPath.WEBVIEW -> webViewTransport
    }

    val topicRepository: org.linuxdo.android.data.TopicRepository by lazy {
        org.linuxdo.android.data.TopicRepository(
            transportProvider = { transport(NetworkPath.OKHTTP) },
            diagnostics = diagnostics,
            reading = reading,
            sessionStore = sessionStore,
            fallbackTransportProvider = { transport(NetworkPath.WEBVIEW) },
            httpClientProvider = { httpClient },
        )
    }
    /**
     * 重建或刷新 OkHttp 客户端（DoH 偏好设置变更或手动切换后调用）。
     */
    fun rebuildHttpClient() {
        dohCoordinator.refresh()
        if (sessionStore.getNetworkMode() == NetworkMode.BUILT_IN &&
            sessionStore.isDohEnabled() && sessionStore.isDohAutoOptimizeEnabled()) {
            dohCoordinator.triggerSmartRouteEvaluation()
        }
    }

    companion object {
        /** 与共享连接池的 keep-alive 对齐：后台超过这么久，池里的连接就不再可信。 */
        private const val STALE_CONNECTION_IDLE_MS = 5 * 60_000L

        private fun epochLabel(epoch: org.linuxdo.android.net.TransportEpoch): String =
            "gen=${epoch.generation} " + when {
                epoch.routeMode == org.linuxdo.android.net.NetworkRouteMode.SYSTEM -> "跟随系统"
                epoch.transport != null -> "${epoch.routeMode.label}(${epoch.server.name})"
                else -> "内置网络 · 平台 TLS"
            }

        /**
         * 全应用共用同一个 UA。首次运行时由系统 WebView 的默认 UA 规整成 Chrome 移动端的形态
         * (去掉 "; wv" 与 "Version/4.0"),并持久化——WebView 升级也不改变它,
         * 因为 cf_clearance 与 UA 绑定,UA 漂移会触发重新挑战。
         */
        fun resolveUserAgent(context: Context, store: SessionStore): String {
            var storageReadable = true
            val stored = try { store.userAgentOrNull() } catch (_: org.linuxdo.android.data.EncryptedStorageUnavailableException) {
                storageReadable = false
                null
            }
            // 旧版本把设备段强行改成固定的 Android 10; K。它会与 WebView 自动
            // 发送的 Client Hints 不一致，CF 可能将同一会话视为指纹漂移。
            if (!stored.isNullOrBlank() && !stored.contains("Android 10; K")) return stored
            val raw = try {
                WebSettings.getDefaultUserAgent(context)
            } catch (_: Throwable) {
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            val normalized = normalize(raw)
            try { if (storageReadable) store.saveUserAgent(normalized) } catch (_: org.linuxdo.android.data.EncryptedStorageUnavailableException) {
                // 临时展示默认 UA，不把不可读的旧配置覆盖掉。
            }
            return normalized
        }

        fun normalize(raw: String): String = raw
            .replace(Regex("[;\\s]*\\bwv\\b[;\\s]*(?=\\))"), "")
            .replace(Regex("Version/[^ ]+ *"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }
}

private fun installCrashHandler(context: Context, diagnostics: Diagnostics) {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            diagnostics.recordCrash(thread, throwable, context)
        } catch (_: Throwable) {
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

