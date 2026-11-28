package localqa

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import java.io.File
import java.lang.reflect.Proxy
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.linuxdo.android.data.ApiQuotaPool
import org.linuxdo.android.data.Diagnostics
import org.linuxdo.android.data.SessionStore
import org.linuxdo.android.data.TopicRepository
import org.linuxdo.android.net.BrowserSession
import org.linuxdo.android.net.HttpStatusException
import org.linuxdo.android.net.OkHttpTransport
import org.linuxdo.android.net.UserErrorMessage
import org.linuxdo.android.push.LinuxDoPushSdk
import org.linuxdo.android.push.PushConfig
import org.linuxdo.android.push.PushNotificationScope
import org.linuxdo.android.push.PushSessionProvider
import org.linuxdo.android.service.NotificationWorker

/** Each instrumentation process starts with empty rate-limit timers and intercepts every test request. */
class PollChecks : Instrumentation() {
    private lateinit var group: String
    private var scenario = ""
    private var requests = 0
    private var parsed = false
    private val waits = mutableListOf<Long?>()
    private val quotaChecks = mutableListOf<Boolean>()

    override fun callApplicationOnCreate(app: Application) = Unit // Do not start normal application services.

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        group = arguments?.getString("group") ?: "poll"
        start()
    }

    override fun onStart() {
        val out = Bundle()
        val result = runCatching { runBlocking { checkGroup(out) } }
        result.exceptionOrNull()?.let { out.putString("failure", it.stackTraceToString().take(1500)) }
        finish(if (result.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED, out)
    }

    private suspend fun checkGroup(out: Bundle) {
        val isolated = object : ContextWrapper(targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                baseContext.getSharedPreferences("qa_network_poll_$name", mode)
        }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            val code = when (scenario) {
                "tls" -> throw SSLHandshakeException("DoH: ech_config_missing")
                "timeout" -> throw SocketTimeoutException("fixture")
                "body", "reset" -> 429
                "busy", "unavailable" -> 503
                "denied" -> 403
                "expired" -> 401
                else -> 200
            }
            val body = if (scenario == "body") """{"wait_seconds":180}""" else """{"notifications":[]}"""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .apply {
                    if (scenario == "busy") header("Retry-After", "180")
                    if (scenario == "reset") header("RateLimit-Reset", "90")
                }
                .body(body.toResponseBody()).build()
        }.build()
        if (group == "transport") {
            checkTransport(isolated, client, out)
            return
        }
        val session = fakeSession()
        LinuxDoPushSdk.init(PushConfig(Activity::class.java, 0, "https://fixture.invalid", { client }, { session }))
        val cases = when (group) {
            "poll" -> listOf("quota", "denied", "unavailable", "expired", "tls", "success")
            "busy", "body", "reset" -> listOf(group)
            else -> error("Unknown group: $group")
        }
        for (name in cases) {
            scenario = name
            requests = 0
            parsed = false
            waits.clear()
            quotaChecks.clear()
            NotificationWorker.pollAfterWake(isolated, session)
            if (name == "busy") NotificationWorker.pollAfterWake(isolated, session)
            val expectedWait = when (name) { "busy", "body" -> 180L; "reset" -> 90L; else -> null }
            check(requests == (if (name == "quota") 0 else 1) && quotaChecks == listOf(true) &&
                (expectedWait == null || waits == listOf(expectedWait)) && parsed == (name == "success")) {
                "$name: calls=$requests hints=$waits quota=$quotaChecks parsed=$parsed"
            }
            out.putString(name, "PASS calls=$requests hints=$waits")
        }
    }

    private fun fakeSession(): PushSessionProvider = Proxy.newProxyInstance(
        PushSessionProvider::class.java.classLoader, arrayOf(PushSessionProvider::class.java),
    ) { _, method, args ->
        when (method.name) {
            "getUserId", "getSessionGeneration" -> 1L
            "hasSession", "isBackgroundNotificationEnabled", "isBadgeTestHolding" -> true
            "tryAcquireBackgroundPoll" -> { quotaChecks.add(args[0] as Boolean); scenario != "quota" }
            "recordBackgroundPollResponse" -> { waits.add(args[3] as Long?); 0L }
            "getNotificationScope" -> { parsed = true; PushNotificationScope.ALL }
            else -> when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Long.TYPE -> 0L
                java.lang.Integer.TYPE -> 0
                else -> null
            }
        }
    } as PushSessionProvider

    private suspend fun checkTransport(ctx: Context, client: OkHttpClient, out: Bundle) {
        val prefs = ctx.getSharedPreferences("timers", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        ApiQuotaPool.restoreAutomaticBackoff(prefs)
        val diag = Diagnostics(File(targetContext.cacheDir, "qa_network_poll.log"))
        val store = SessionStore(ctx)
        val transport = OkHttpTransport(store, BrowserSession(ctx, "fixture", store, diag), client, diag)
        val repo = TopicRepository({ transport }, diag)
        for (name in listOf("tls", "timeout", "busy")) {
            scenario = name
            requests = 0
            val err = runCatching { repo.fetchLatest(0, awaitCategories = false) }.exceptionOrNull()
            val msg = UserErrorMessage.format(err)
            val matched = when (name) {
                "tls" -> err?.cause is SSLHandshakeException && "ECH" in msg
                "timeout" -> err?.cause is SocketTimeoutException && "超时" in msg
                else -> err is HttpStatusException && err.status == 503 && err.retryAfterSeconds == 180L &&
                    ApiQuotaPool.automaticBackoffMs() in 175_000L..180_000L
            }
            check(requests == 1 && matched) { "$name: calls=$requests error=${err?.javaClass?.simpleName} message=$msg" }
            out.putString(name, "PASS calls=$requests message=$msg")
        }
        prefs.edit().clear().commit()
    }
}
