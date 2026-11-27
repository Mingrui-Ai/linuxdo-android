package org.linuxdo.android.data

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull
import org.linuxdo.android.net.RateLimitException

/** 一次前台翻页共享一次重试机会，包括由该页并发发出的 GET。离开页面即收回。 */
internal class PageReadRetry private constructor(private val pageExit: Deferred<Unit>) :
    AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<PageReadRetry> {
        fun forPage(pageExit: Deferred<Unit>?): CoroutineContext =
            if (pageExit == null) EmptyCoroutineContext else PageReadRetry(pageExit)
    }

    private val used = AtomicBoolean(false)

    suspend fun waitForRetry(error: RateLimitException) {
        if (!error.canRetryPageRead || error.retryAfterMs !in 1L..(TopicRepository.AUTO_RETRY_MAX_WAIT_SECONDS * 1_000L) ||
            pageExit.isCompleted || !used.compareAndSet(false, true)) throw error

        val deadline = SystemClock.elapsedRealtime() + error.retryAfterMs
        withTimeoutOrNull(error.retryAfterMs) { pageExit.await() }
        if (pageExit.isCompleted) throw RateLimitException((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
    }

    fun checkBeforeSend() {
        if (pageExit.isCompleted) throw RateLimitException(0L)
    }
}
