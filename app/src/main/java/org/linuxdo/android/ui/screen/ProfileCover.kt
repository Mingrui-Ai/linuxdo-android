package org.linuxdo.android.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.delay
import org.linuxdo.android.data.UserCardData

internal fun profileCoverHeight(width: Dp): Dp = (width / 1.8f).coerceIn(210.dp, 300.dp)

/** 弹窗预加载与主页使用相同尺寸及缓存键，避免进入主页后重新解码封面。 */
@Composable
internal fun rememberProfileCoverRequest(url: String?, width: Dp, retryToken: Int = 0): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    val top = WindowInsets.systemBars.union(WindowInsets.displayCutout).getTop(density)
    val widthPx = with(density) { width.roundToPx() }.coerceAtLeast(1)
    val heightPx = with(density) { profileCoverHeight(width).roundToPx() } + top
    return remember(context, url, widthPx, heightPx, retryToken) {
        url?.let {
            ImageRequest.Builder(context).data(it)
                .size(widthPx, heightPx).scale(Scale.FILL).precision(Precision.INEXACT)
                .memoryCacheKey("profile-cover:$it:$widthPx:$heightPx")
                .crossfade(false)
                .setParameter("coil#repeat_count", -1, memoryCacheKey = null)
                .setParameter("coil#retry_token", retryToken, memoryCacheKey = null)
                .listener(object : ImageRequest.Listener {
                    private var started = 0L
                    override fun onStart(request: ImageRequest) {
                        started = android.os.SystemClock.elapsedRealtime()
                    }
                    override fun onSuccess(request: ImageRequest, result: coil.request.SuccessResult) {
                        if (org.linuxdo.android.BuildConfig.DEBUG) android.util.Log.d("ProfileImage",
                            "cover ${result.dataSource} ${android.os.SystemClock.elapsedRealtime() - started}ms ${widthPx}x$heightPx")
                    }
                    override fun onError(request: ImageRequest, result: coil.request.ErrorResult) {
                        if (org.linuxdo.android.BuildConfig.DEBUG) android.util.Log.d("ProfileImage",
                            "cover error ${result.throwable.javaClass.simpleName}: ${result.throwable.message?.substringBefore('?')}")
                    }
                })
                .build()
        }
    }
}

/** 仅为当前打开的资料卡准备一张封面；快速关闭或换人时取消。 */
@Composable
internal fun PrefetchProfileCover(card: UserCardData?) {
    val context = LocalContext.current
    val request = rememberProfileCoverRequest(card?.profileBackgroundUrl ?: card?.cardBackgroundUrl,
        LocalConfiguration.current.screenWidthDp.dp)
    LaunchedEffect(request) {
        if (request != null) {
            delay(200)
            context.imageLoader.execute(request)
        }
    }
}
