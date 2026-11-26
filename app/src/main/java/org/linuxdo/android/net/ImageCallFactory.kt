package org.linuxdo.android.net

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
