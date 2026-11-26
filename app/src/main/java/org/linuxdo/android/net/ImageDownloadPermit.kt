package org.linuxdo.android.net

import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Call
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

/** 取消离屏图片时及时离开等待队列；下载名额保持到响应体读完或关闭。 */
internal class ImageDownloadPermit private constructor(private val semaphore: Semaphore) {
    private val released = AtomicBoolean(false)

    fun release() {
        if (released.compareAndSet(false, true)) semaphore.release()
    }

    fun attachTo(response: Response): Response {
        val body = response.body
        if (body == null || !response.isSuccessful || body.contentLength() == 0L) {
            release()
            return response
        }
        val source = object : ForwardingSource(body.source()) {
            override fun read(sink: Buffer, byteCount: Long): Long = try {
                super.read(sink, byteCount).also { if (it == -1L) release() }
            } catch (error: Exception) {
                release()
                throw error
            }

            override fun close() {
                try { super.close() } finally { release() }
            }
        }.buffer()
        return response.newBuilder().body(object : ResponseBody() {
            override fun contentType() = body.contentType()
            override fun contentLength() = body.contentLength()
            override fun source() = source
        }).build()
    }

    companion object {
        fun acquire(semaphore: Semaphore, call: Call, timeoutMs: Long): ImageDownloadPermit? {
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            while (true) {
                if (call.isCanceled()) throw IOException("Canceled")
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0L) return null
                val acquired = try {
                    semaphore.tryAcquire(minOf(remaining, TimeUnit.MILLISECONDS.toNanos(100)), TimeUnit.NANOSECONDS)
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException("Image request interrupted").apply { initCause(error) }
                }
                if (acquired) return ImageDownloadPermit(semaphore)
            }
        }
    }
}
