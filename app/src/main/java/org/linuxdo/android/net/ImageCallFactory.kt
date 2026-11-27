package org.linuxdo.android.net

import coil.intercept.Interceptor
import coil.request.ImageResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 图片不占用业务 API 的调度队列，头像、表情也不排在大图后面。
 * 派生客户端保留当前网络的连接池、DNS、代理、TLS 和凭据拦截器；换线后新请求使用新客户端。
 */
internal class ImageCallFactory(private val currentClient: () -> OkHttpClient) : Call.Factory {
    private val dispatchers = ImageRequestLane.entries.associateWith { lane ->
        Dispatcher().apply {
            maxRequests = lane.concurrency
            maxRequestsPerHost = lane.concurrency
        }
    }
    private var base: OkHttpClient? = null
    private var clients: Map<ImageRequestLane, OkHttpClient> = emptyMap()

    @Synchronized
    override fun newCall(request: Request): Call {
        val active = currentClient()
        if (base !== active) {
            clients = dispatchers.mapValues { (_, dispatcher) ->
                active.newBuilder().dispatcher(dispatcher).build()
            }
            base = active
        }
        val lane = siteImageLane(request.url) ?: ImageRequestLane.MEDIA
        return clients.getValue(lane).newCall(request)
    }
}

/** 相同站内图片只让一项先填充 Coil 缓存，后续请求仍按自己的尺寸、缓存策略解码。 */
internal class SiteImageCacheInterceptor : Interceptor {
    private class Entry(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val active = mutableMapOf<String, Entry>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = (request.data as? String)?.toHttpUrlOrNull()
        if (url == null || siteImageLane(url) == null || !request.diskCachePolicy.readEnabled ||
            !request.diskCachePolicy.writeEnabled) return chain.proceed(request)
        val key = request.diskCacheKey ?: url.toString()
        val entry = synchronized(active) { active.getOrPut(key) { Entry() }.also { it.users++ } }
        try {
            return entry.mutex.withLock { chain.proceed(request) }
        } finally {
            synchronized(active) {
                if (--entry.users == 0) active.remove(key)
            }
        }
    }
}
