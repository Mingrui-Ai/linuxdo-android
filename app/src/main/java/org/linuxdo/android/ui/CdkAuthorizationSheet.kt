package org.linuxdo.android.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.doOnLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.linuxdo.android.App
import org.linuxdo.android.BuildConfig
import org.linuxdo.android.data.CdkAuthorization
import org.linuxdo.android.net.RateLimitException
import org.linuxdo.android.ui.design.*

/** 网页完成 Connect 验证/授权，原生兑换受校验的回调后取分；不把过盾等同于登录成功。 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun CdkAuthorizationSheet(
    username: String,
    onAuthorized: (generation: Long, score: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as App).container
    val scope = rememberCoroutineScope()
    val currentAuthorized by rememberUpdatedState(onAuthorized)
    val currentDismiss by rememberUpdatedState(onDismiss)
    val generation = remember { container.sessionStore.sessionGeneration }
    var attempt by remember { mutableStateOf<CdkAuthorization?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var exchanging by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryAfter by remember { mutableLongStateOf(0L) }
    var disposed by remember { mutableStateOf(false) }

    fun sessionCurrent() = !disposed && !container.sessionStore.sessionChanging &&
        container.sessionStore.sessionGeneration == generation

    fun showFailure(failure: Exception) {
        if (failure is RateLimitException) {
            retryAfter = android.os.SystemClock.elapsedRealtime() + failure.retryAfterMs
            error = "社区分服务繁忙，请稍后重试"
        } else {
            error = if (failure is IllegalStateException || failure is IllegalArgumentException) failure.message
                else "社区分授权未完成，请重试"
        }
    }

    fun finish(callback: String) {
        val activeAttempt = attempt ?: return
        if (exchanging || !sessionCurrent()) return
        // 先清掉 attempt，重复导航回调不会重复兑换一次性 code。
        attempt = null
        exchanging = true
        loading = false
        error = null
        scope.launch {
            try {
                val score = container.topicRepository.completeCdkAuthorization(activeAttempt, callback)
                if (sessionCurrent()) {
                    CookieManager.getInstance().flush()
                    currentAuthorized(generation, score)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (sessionCurrent()) showFailure(failure)
            } finally {
                exchanging = false
            }
        }
    }

    val webView = remember(context) {
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = false
                allowContentAccess = false
                userAgentString = container.userAgent
            }
            val cookies = CookieManager.getInstance()
            cookies.setAcceptCookie(true)
            cookies.setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    if (!sessionCurrent()) { currentDismiss(); return true }
                    val target = request.url
                    val allowed = target.scheme == "https" && target.port in setOf(-1, 443) && target.userInfo == null &&
                        target.host in setOf("cdk.linux.do", "connect.linux.do", "linux.do")
                    if (!allowed) { error = "此窗口仅用于社区分授权"; return true }
                    if (target.host == "cdk.linux.do" && target.path == "/callback") {
                        finish(target.toString())
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false
                    CookieManager.getInstance().flush()
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                    if (request.isForMainFrame) {
                        loading = false
                        error = "网页暂时无法加载，请检查网络后重试"
                    }
                }
            }
        }
    }

    fun startAuthorization() {
        if (preparing || exchanging || !sessionCurrent()) return
        if (android.os.SystemClock.elapsedRealtime() < retryAfter) {
            error = "社区分服务繁忙，请稍后重试"
            return
        }
        preparing = true
        attempt = null
        webView.stopLoading()
        error = null
        scope.launch {
            try {
                val next = container.topicRepository.beginCdkAuthorization(username)
                if (sessionCurrent()) {
                    attempt = next
                    webView.doOnLayout { if (sessionCurrent() && attempt === next) webView.loadUrl(next.url) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (sessionCurrent()) { loading = false; showFailure(failure) }
            } finally {
                preparing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // 等当前联网方式应用到 WebView 后再导航，避免与原生请求走不同代理。
        container.updateWebViewProxyState { if (sessionCurrent()) startAuthorization() }
    }
    DisposableEffect(webView) {
        onDispose {
            disposed = true
            CookieManager.getInstance().flush()
            webView.stopLoading()
            webView.destroy()
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { if (!exchanging && webView.canGoBack()) webView.goBack() else onDismiss() }
        Column(Modifier.fillMaxSize().background(IosTheme.colors.groupedBackground).systemBarsPadding().imePadding()) {
            IosNavBar(title = "社区分授权", onBack = onDismiss, trailing = {
                TextButton(onClick = { startAuthorization() }, enabled = !preparing && !exchanging) { Text("重新授权") }
            })
            Text(when {
                error != null -> error!!
                exchanging -> "授权已返回，正在获取社区分…"
                preparing -> "正在准备授权…"
                else -> "请以 @$username 完成网页验证和授权，成功后会自动返回。"
            }, style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            if (loading && !preparing && !exchanging) Text("正在加载…", style = IosTheme.type.footnote,
                color = IosTheme.colors.secondaryLabel, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}
