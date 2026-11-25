package org.linuxdo.android.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** 单次网页授权绑定发起账号及 state；不把授权地址或凭据写入日志。 */
internal class CdkAuthorization(
    val url: String,
    val username: String,
    val userId: Long,
    val generation: Long,
) {
    private val state = checkedCdkAuthorizationUrl(url).queryParameter("state")!!
    private var callbackClaimed = false

    @Synchronized
    fun claimCallback(callback: String): Pair<String, String> {
        check(!callbackClaimed) { "此授权回调已处理，请重新授权" }
        val target = callback.toHttpUrlOrNull()
        require(target != null && target.isHttps && target.port == 443 &&
            target.username.isEmpty() && target.password.isEmpty() &&
            target.host == "cdk.linux.do" && target.encodedPath == "/callback" &&
            target.queryParameterValues("state") == listOf(state) &&
            target.queryParameterValues("code").size == 1 &&
            !target.queryParameter("code").isNullOrBlank()
        ) { "社区分授权回调无效，请重新授权" }
        callbackClaimed = true
        return target.queryParameter("code")!! to state
    }
}

internal fun checkedCdkAuthorizationUrl(url: String): HttpUrl {
    val target = url.toHttpUrlOrNull()
    require(target != null && target.isHttps && target.port == 443 &&
        target.username.isEmpty() && target.password.isEmpty() &&
        target.host == "connect.linux.do" && target.encodedPath == "/oauth2/authorize" &&
        target.queryParameterValues("state").size == 1 && !target.queryParameter("state").isNullOrBlank()
    ) { "社区分授权入口无效" }
    return target
}

/** 服务器返回的身份必须与发起账号一致，避免将浏览器里另一账号的分数存入当前账号。 */
internal fun parseCdkScore(raw: String, username: String): Int {
    val root = Json.parseToJsonElement(raw) as? JsonObject
    val data = root?.get("data") as? JsonObject ?: error("CDK 响应缺少 data 字段")
    val actualUsername = (data["username"] as? JsonPrimitive)?.contentOrNull
    check(username.isNotBlank() && username.equals(actualUsername, ignoreCase = true)) {
        "社区分网站登录账号与 App 不一致，请使用 @$username 重新授权"
    }
    val score = (data["score"] as? JsonPrimitive)?.intOrNull ?: error("CDK score 格式不正确")
    check(score in -100..100) { "CDK score 超出范围" }
    return score
}
