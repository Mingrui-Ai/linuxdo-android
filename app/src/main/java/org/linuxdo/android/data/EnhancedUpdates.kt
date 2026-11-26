package org.linuxdo.android.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.linuxdo.android.Config

data class EnhancedUpdate(
    val latestVersion: String,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val mandatory: Boolean = false,
    val apkSha256: String? = null,
    val versionCode: Int = 0,
)

/** Only published enhanced releases and the APK matching this installation are eligible. */
object EnhancedUpdates {
    private val versionPattern = Regex("[0-9]+(?:\\.[0-9]+){1,2}")
    private val sha256Pattern = Regex("[0-9a-fA-F]{64}")

    fun validVersion(version: String): Boolean = versionPattern.matches(version) &&
        version.split('.').all { it.toIntOrNull() != null }

    fun compareVersions(first: String, second: String): Int {
        require(validVersion(first) && validVersion(second))
        val a = first.split('.').map(String::toInt)
        val b = second.split('.').map(String::toInt)
        for (index in 0 until maxOf(a.size, b.size)) {
            val result = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
            if (result != 0) return result
        }
        return 0
    }

    fun apkName(version: String, applicationId: String): String {
        require(validVersion(version))
        val variant = when (applicationId) {
            "org.linuxdo.android.enhanced" -> ""
            "org.linuxdo.android.debug" -> "-debug"
            else -> error("Unsupported update package")
        }
        return "linuxdo-enhanced-$version$variant-arm64-v8a.apk"
    }

    fun releaseUrl(version: String) = "${Config.UPDATE_REPOSITORY_URL}/releases/tag/enhanced-v$version"

    fun trustedDownload(version: String, url: String, sha256: String?, applicationId: String): Boolean =
        validVersion(version) && sha256 != null && sha256Pattern.matches(sha256) &&
            url == "${Config.UPDATE_REPOSITORY_URL}/releases/download/enhanced-v$version/${apkName(version, applicationId)}"

    fun manifest(raw: String, applicationId: String, currentVersion: String, currentCode: Int): EnhancedUpdate? {
        val root = Json.parseToJsonElement(raw).jsonObject
        val version = root.string("versionName")
        require(validVersion(version)) { "Invalid enhanced version" }
        if (root.string("updatesEnabled") == "false") return null
        val code = root.string("versionCode").toIntOrNull() ?: error("Missing version code")
        if (code <= currentCode || compareVersions(version, currentVersion) < 0) return null
        val packages = root["packages"] as? JsonObject
        val artifact = if (packages == null) root else
            packages[applicationId] as? JsonObject ?: error("No update for this package")
        require(artifact.string("applicationId") == applicationId) { "Update package mismatch" }
        val url = artifact.string("apkUrl")
        val sha256 = artifact.string("apkSha256").lowercase()
        require(trustedDownload(version, url, sha256, applicationId)) { "Invalid enhanced APK or checksum" }
        require(root.string("releasePageUrl") == releaseUrl(version)) { "Release source mismatch" }
        val changelog = when (val value = root["changelog"]) {
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.joinToString("\n")
            is JsonPrimitive -> value.contentOrNull.orEmpty()
            else -> ""
        }
        return EnhancedUpdate(
            latestVersion = version,
            title = root.string("title").ifBlank { "LinuxDo 增强版 $version" },
            changelog = changelog.ifBlank { "优化使用体验并修复已知问题。" },
            downloadUrl = url,
            releasePageUrl = releaseUrl(version),
            mandatory = root.string("forceUpdate") == "true" ||
                (root.string("minVersionCode").toIntOrNull() ?: 0) > currentCode,
            apkSha256 = sha256,
            versionCode = code,
        )
    }

    fun release(raw: String, applicationId: String, currentVersion: String): EnhancedUpdate? {
        val root = Json.parseToJsonElement(raw).jsonObject
        require(root.string("draft") == "false" && root.string("prerelease") == "false") { "Unpublished release" }
        val tag = root.string("tag_name")
        require(tag.startsWith("enhanced-v")) { "Not an enhanced release" }
        val version = tag.removePrefix("enhanced-v")
        require(validVersion(version) && root.string("html_url") == releaseUrl(version)) { "Release source mismatch" }
        if (compareVersions(version, currentVersion) <= 0) return null
        val name = apkName(version, applicationId)
        val artifact = (root["assets"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            .singleOrNull { it.string("name") == name && it.string("state") == "uploaded" }
            ?: error("No completed APK for this package")
        val url = artifact.string("browser_download_url")
        val digest = artifact.string("digest")
        require(digest.startsWith("sha256:")) { "Missing APK checksum" }
        val sha256 = digest.removePrefix("sha256:").lowercase()
        require(trustedDownload(version, url, sha256, applicationId)) { "Invalid enhanced APK or checksum" }
        return EnhancedUpdate(
            latestVersion = version,
            title = root.string("name").ifBlank { "LinuxDo 增强版 $version" },
            changelog = root.string("body").ifBlank { "优化使用体验并修复已知问题。" },
            downloadUrl = url,
            releasePageUrl = releaseUrl(version),
            apkSha256 = sha256,
        )
    }

    private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
}
