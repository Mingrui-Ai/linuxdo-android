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
import org.linuxdo.android.App
import org.linuxdo.android.ui.design.*

/** CDK 登录使用原生网络读取所共享的 CookieManager，不改动隔离小程序的会话规则。 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun CdkAuthorizationSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as App).container
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val webView = remember(context) {
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
                    val target = request.url
                    val allowed = target.scheme == "https" && target.port in setOf(-1, 443) && target.userInfo == null &&
                        target.host in setOf("cdk.linux.do", "connect.linux.do", "linux.do")
                    if (!allowed) error = "此窗口仅用于社区分授权"
                    return !allowed
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                    error = null
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
    val close = {
        CookieManager.getInstance().flush()
        onDismiss()
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler {
            if (webView.canGoBack()) webView.goBack() else close()
        }
        Column(Modifier.fillMaxSize().background(IosTheme.colors.groupedBackground).systemBarsPadding().imePadding()) {
            IosNavBar(title = "社区分验证", onBack = close, trailing = {
                Row {
                    TextButton(onClick = { webView.reload() }, enabled = !loading) { Text("刷新") }
                    TextButton(onClick = close) { Text("完成") }
                }
            })
            if (loading) Text("正在加载…", style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            error?.let { Text(it, style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            AndroidView(factory = {
                webView.also { view -> view.doOnLayout { view.loadUrl("https://cdk.linux.do/") } }
            }, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}
