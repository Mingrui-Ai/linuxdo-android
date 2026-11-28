package org.linuxdo.android.net

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 统一的用户友好错误文案解析与格式化工具：
 * 1. 从 Discourse 服务端非 2xx JSON 响应中提取清晰的人类可读提示（屏蔽原始 JSON/HTML 片段、error_type 字段与生硬 HTTP 前缀）；
 * 2. 将各类底层网络、TLS、序列化、限流与 HTTP 状态异常映射为自然中文提示，不暴露英文堆栈或类名；
 * 3. 技术排查细节保留在 HttpStatusException.diagnosticMessage 中，仅供 diagnostics 日志记录。
 */
object UserErrorMessage {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val htmlTagPattern = Regex("<\\s*(?:!doctype\\b|/?[a-z][a-z0-9:-]*\\b[^>]*>)", RegexOption.IGNORE_CASE)
    private val embeddedJsonPattern = Regex("[{\\[]\\s*\"")
    private val rawHttpPrefixPattern = Regex("^HTTP\\s+\\d{3}\\s*:?\\s*", RegexOption.IGNORE_CASE)
    private val jsonEnvelopePattern = Regex("^\\s*[{\\[].*[}\\]]\\s*$", setOf(RegexOption.DOT_MATCHES_ALL))
    private val chineseCharPattern = Regex("[\\u4e00-\\u9fff]")

    /**
     * 从 Discourse / 子站 HTTP 响应体中提取适合面向用户展示的错误提示。
     * 若响应体为 HTML 错误页、空串或无语义的英文技术字段，则返回按状态码兜底的中文提示。
     */
    fun fromHttpBody(status: Int, body: String?, fallback: String? = null): String {
        val raw = body?.trim().orEmpty()
        if (raw.isNotEmpty() && !looksLikeHtml(raw)) {
            parseDiscourseError(raw)?.let { return it }
            // 纯文本但非 JSON/HTML 且包含中文（部分接口会返回纯文本中文原因）
            if (!looksLikeRawJson(raw) && raw.length <= 200) {
                cleanUserText(raw)?.let { return it }
            }
        }
        return fallback?.takeIf { it.isNotBlank() } ?: defaultForHttpStatus(status)
    }

    /** 登录响应可能成功且不含 user；仅明确的 error/errors 才表示业务拒绝。 */
    fun serverError(body: String?, fallback: String = "登录失败，请稍后重试"): String? {
        val obj = runCatching { json.parseToJsonElement(body.orEmpty()) as? JsonObject }.getOrNull() ?: return null
        fun hasError(value: kotlinx.serialization.json.JsonElement?): Boolean = when (value) {
            is JsonArray -> value.any(::hasError)
            is JsonObject -> value.isNotEmpty()
            is JsonPrimitive -> value.contentOrNull?.trim()?.let {
                it.isNotEmpty() && it != "null" && it != "[]" && it != "false"
            } == true
            else -> false
        }
        if (!hasError(obj["error"]) && !hasError(obj["errors"])) return null
        return parseDiscourseError(body.orEmpty()) ?: fallback
    }

    /**
     * 将任意异常转为面向界面的干净中文提示，避免直接暴露原始 JSON、HTML、英文异常消息或类名。
     */
    fun format(error: Throwable?, fallback: String = "操作失败，请稍后重试"): String {
        if (error == null) return fallback
        return when (error) {
            is RateLimitException -> {
                val sec = ((error.retryAfterMs + 999L) / 1_000L).coerceAtLeast(1L)
                "请求过于频繁，请在 ${sec} 秒后重试"
            }
            is NetworkTimeoutException -> error.message ?: fallback
            is NeedsInteractiveVerification -> "安全验证已过期，请完成人机验证"
            is LoginRejectedException -> cleanUserText(error.message) ?: "登录失败，请检查账号信息后重试"
            is SecondFactorRequiredException -> cleanUserText(error.message) ?: "该账号已开启两步验证，请输入验证码"
            is RiskControlException -> "当前请求触发安全防护，请稍后重试"
            is HttpStatusException -> {
                val clean = cleanHttpDetail(error.detail, error.status)
                clean ?: defaultForHttpStatus(error.status)
            }
            is SocketTimeoutException -> "连接超时，请检查网络后重试"
            is UnknownHostException -> "无法连接到服务器，请检查网络或 DNS 设置"
            is ConnectException -> "无法连接到服务器，请检查网络后重试"
            is SSLException -> if (error.message?.contains("ech_config_missing", ignoreCase = true) == true) {
                "未能获取 ECH 配置，安全连接建立失败，请稍后重试"
            } else "安全连接建立失败，请检查网络或切换网络模式"
            is SocketException -> "网络连接已中断，请稍后重试"
            is SerializationException -> "服务器返回的数据格式异常，请稍后重试"
            is IOException -> formatIoMessage(error.message, fallback)
            else -> cleanUserText(error.message) ?: fallback
        }
    }

    /**
     * 清理已有字符串错误（如 WebView / 回调传入的文案），剥离原始 `HTTP 403 {...}` / HTML / 纯英文技术错误。
     */
    fun sanitize(raw: String?, fallback: String = "操作失败，请稍后重试"): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return fallback
        // 识别形如 "HTTP 403 {...}" 或 "HTTP 502: <html>..." 的旧格式文案
        val httpMatch = Regex("^HTTP\\s+(\\d{3})\\s*:?\\s*(.*)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .matchEntire(text)
        if (httpMatch != null) {
            val status = httpMatch.groupValues[1].toIntOrNull() ?: 0
            val rest = httpMatch.groupValues[2].trim()
            return fromHttpBody(status, rest)
        }
        if (looksLikeRawJson(text)) {
            return parseDiscourseError(text) ?: fallback
        }
        if (looksLikeHtml(text)) {
            return fallback
        }
        return cleanUserText(text) ?: fallback
    }

    fun defaultForHttpStatus(status: Int): String = when (status) {
        0 -> "连接超时，请检查网络"
        400 -> "请求参数有误，请检查后重试"
        401 -> "登录状态已失效，请重新登录"
        403 -> "当前账号无权访问或执行此操作"
        404 -> "请求的内容不存在或已被删除"
        409 -> "内容状态已变化，请刷新后重试"
        410 -> "该内容已被删除"
        413 -> "提交的内容或文件过大，请缩小后重试"
        422 -> "提交的内容不符合要求，请检查后重试"
        429 -> "请求过于频繁，请稍后再试"
        in 500..599 -> "站点服务暂时繁忙，请稍后重试"
        else -> "请求失败，请稍后重试"
    }

    private fun parseDiscourseError(rawJson: String): String? {
        val obj = runCatching { json.parseToJsonElement(rawJson) as? JsonObject }.getOrNull() ?: return null
        val errorType = (obj["error_type"] as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase()

        // 1. 优先提取服务端明确给出的 errors 数组 / error / message 文案
        val rawMessages = mutableListOf<String>()
        when (val errorsElem = obj["errors"]) {
            is JsonArray -> errorsElem.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
                .filter { it.isNotEmpty() }
                .forEach(rawMessages::add)
            is JsonPrimitive -> errorsElem.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let(rawMessages::add)
            else -> Unit
        }
        if (rawMessages.isEmpty()) {
            (obj["error"] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let(rawMessages::add)
        }
        if (rawMessages.isEmpty()) {
            (obj["message"] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let(rawMessages::add)
        }

        val joined = rawMessages.joinToString("\n").take(400).trim()
        if (joined.isNotEmpty()) {
            translateKnownServerPhrase(joined, errorType)?.let { return it }
            if (chineseCharPattern.containsMatchIn(joined) && !looksLikeHtml(joined) && !looksLikeRawJson(joined)) {
                return sanitizePlainText(joined)
            }
        }

        // 2. 根据 Discourse 标准 error_type 映射为自然中文
        return mapErrorType(errorType)
    }

    private fun mapErrorType(errorType: String?): String? = when (errorType) {
        "not_logged_in" -> "登录状态已失效，请重新登录"
        "invalid_access" -> "当前账号无权查看或执行此操作"
        "not_found" -> "请求的内容不存在或已被删除"
        "rate_limit" -> "请求过于频繁，请稍后再试"
        "read_only" -> "站点当前处于只读维护状态，请稍后重试"
        "invalid_parameters" -> "请求参数有误，请检查后重试"
        "unauthenticated" -> "请先登录后再继续操作"
        "failed_to_login" -> "账号或密码不正确，请检查后重试"
        else -> null
    }

    private fun translateKnownServerPhrase(text: String, errorType: String?): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("not permitted to view") || lower.contains("invalid_access") ||
                lower.contains("not allowed to") -> "当前账号无权查看或执行此操作"
            lower.contains("requested resource could not be found") ||
                lower.contains("page doesn't exist") ||
                lower.contains("topic not found") -> "请求的内容不存在或已被删除"
            lower.contains("you need to be logged in") || lower.contains("not logged in") ->
                "请先登录后再继续操作"
            lower.contains("rate limit") || lower.contains("too many requests") ||
                lower.contains("performed this action too many times") ->
                "操作过于频繁，请稍后再试"
            lower.contains("bad csrf") || lower.contains("csrf") ->
                "安全校验已过期，请重试"
            lower.contains("body is too short") ->
                "回复内容字数不足，请补充后再发送"
            lower.contains("title is too short") ->
                "标题字数不足，请补充后再发布"
            lower.contains("body seems unclear") || lower.contains("body is invalid") ->
                "内容格式不符合要求，请调整后再试"
            lower.contains("post can't be empty") || lower.contains("can't be blank") ->
                "内容不能为空"
            lower.contains("similar to what you recently posted") ->
                "内容与你刚发过的帖子过于相似，请稍作修改"
            errorType != null && !chineseCharPattern.containsMatchIn(text) -> mapErrorType(errorType)
            else -> null
        }
    }

    private fun cleanHttpDetail(detail: String, status: Int): String? {
        val trimmed = detail.trim()
        if (trimmed.isEmpty()) return null
        val stripped = rawHttpPrefixPattern.replaceFirst(trimmed, "").trim()
        if (stripped.isEmpty()) return null
        if (looksLikeHtml(stripped)) return defaultForHttpStatus(status)
        if (looksLikeRawJson(stripped)) {
            return parseDiscourseError(stripped) ?: defaultForHttpStatus(status)
        }
        translateKnownServerPhrase(stripped, null)?.let { return it }
        return cleanUserText(stripped) ?: defaultForHttpStatus(status)
    }

    private fun formatIoMessage(raw: String?, fallback: String): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return fallback
        val lower = text.lowercase()
        return when {
            lower == "canceled" || lower == "cancelled" || lower.contains("socket closed") ->
                "请求已取消，请重试"
            lower.contains("timeout") || lower.contains("timed out") ->
                "连接超时，请检查网络后重试"
            lower.contains("unable to resolve host") || lower.contains("nodename") || lower.contains("dns") ->
                "无法连接到服务器，请检查网络或 DNS 设置"
            lower.contains("connection_failed") || lower.contains("failed to connect") || lower.contains("connection refused") ->
                "无法连接到服务器，请检查网络或代理设置"
            lower.contains("connection reset") || lower.contains("broken pipe") || lower.contains("unexpected end of stream") ->
                "网络连接中断，请稍后重试"
            lower.contains("ssl") || lower.contains("handshake") || lower.contains("cert") ->
                "安全连接建立失败，请检查网络或切换网络模式"
            lower.startsWith("http ") -> sanitize(text, fallback)
            chineseCharPattern.containsMatchIn(text) -> cleanUserText(text) ?: fallback
            else -> fallback
        }
    }

    private fun cleanUserText(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (looksLikeHtml(text)) return null
        if (looksLikeRawJson(text)) return parseDiscourseError(text)
        if (rawHttpPrefixPattern.containsMatchIn(text)) {
            val status = Regex("^HTTP\\s+(\\d{3})", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val rest = rawHttpPrefixPattern.replaceFirst(text, "").trim()
            return fromHttpBody(status, rest)
        }
        embeddedJsonPattern.find(text)?.let { match ->
            return parseDiscourseError(text.substring(match.range.first))
        }
        translateKnownServerPhrase(text, null)?.let { return it }
        // 含中文且不是明显的技术拼接（如 "响应: {..."）则直接返回干净文案
        if (chineseCharPattern.containsMatchIn(text)) {
            val cleaned = text.replace(Regex("[,，]\\s*响应\\s*[:：].*$"), "").trim()
            return sanitizePlainText(cleaned).takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun looksLikeHtml(text: String): Boolean = htmlTagPattern.containsMatchIn(text)

    private fun looksLikeRawJson(text: String): Boolean =
        (text.startsWith("{") || text.startsWith("[")) &&
            (jsonEnvelopePattern.matches(text) || text.contains("\"errors\"") || text.contains("\"error\"") || text.contains("\"error_type\""))

    private fun sanitizePlainText(text: String): String = text
        .replace(rawHttpPrefixPattern, "")
        .replace(Regex("\\s*\\(HTTP\\s+\\d{3}\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("[:：]\\s*HTTP\\s+\\d{3}$", RegexOption.IGNORE_CASE), "")
        .trim()
}
