package org.linuxdo.android.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.linuxdo.android.Config

internal data class ForumIcon(val name: String, val color: String?)
internal val forumIconName = Regex("[a-z0-9][a-z0-9-]{0,95}")

internal fun parseForumIcons(raw: String): Map<String, ForumIcon> = buildMap {
    for (entry in raw.split('|', '\n')) {
        val parts = entry.split(',').map(String::trim)
        if (parts.size < 2 || parts[0].isBlank() || !forumIconName.matches(parts[1])) continue
        put(parts[0].lowercase(), ForumIcon(parts[1], parts.getOrNull(2)?.takeIf(String::isNotBlank)))
    }
}

@Serializable
internal data class ForumIconSnapshot(
    val checkedAt: Long = 0,
    val sprite: String? = null,
    val symbols: Map<String, String> = emptyMap(),
    val tags: String? = null,
    val categories: String? = null,
)

internal fun tagIconsByName(config: Map<String, ForumIcon>, tags: List<DiscourseTag>): Map<String, ForumIcon> =
    config.toMutableMap().apply {
        for (tag in tags) {
            val name = tag.displayName.lowercase()
            // Chinese slugs commonly use Discourse's numeric fallback, e.g. 444-tag.
            val icon = config[tag.slug?.lowercase()] ?: config[tag.id + "-tag"] ?: config[name]
            if (icon != null) put(name, icon)
        }
    }

private fun JsonElement?.asObject(): JsonObject? = when (this) {
    is JsonObject -> this
    is JsonPrimitive -> contentOrNull?.let { Json.parseToJsonElement(it) as? JsonObject }
    else -> null
}

internal fun readForumTheme(html: String): ForumIconSnapshot {
    val doc = Jsoup.parse(html)
    val node = doc.getElementById("data-preloaded") ?: error("Missing Discourse bootstrap")
    val raw = if (node.tagName() == "script") node.data() else node.attr("data-preloaded")
    val themes = Json.parseToJsonElement(raw).jsonObject["activatedThemes"].asObject()
        ?: error("Missing activated themes")
    val settings = themes.values.mapNotNull { it.asObject()?.get("settings").asObject() }
    fun list(name: String) = settings.firstNotNullOfOrNull { (it[name] as? JsonPrimitive)?.contentOrNull }.orEmpty()
    val setup = doc.getElementById("data-discourse-setup")
    val path = setup?.attr("data-svg-sprite-path").orEmpty()
    val base = setup?.attr("data-cdn")?.toHttpUrlOrNull()?.takeIf {
        it.isHttps && it.host.endsWith(".ldstatic.com") && it.port == 443 && it.username.isEmpty() && it.password.isEmpty()
    } ?: Config.BASE_URL.toHttpUrl()
    val url = path.takeIf { it.isNotBlank() }?.let { base.resolve(it) }?.takeIf {
        it.isHttps && it.port == 443 && it.username.isEmpty() && it.password.isEmpty() &&
            (it.host == Config.HOST || it.host.endsWith(".ldstatic.com")) &&
            it.encodedPath.matches(Regex("""/svg-sprite/linux\.do/svg--?\d*-[a-fA-F0-9]{40}\.js"""))
    }
    return ForumIconSnapshot(sprite = url?.toString(), tags = list("tag_icon_list"), categories = list("category_icon_list"))
}

// The sprite response is a JavaScript string assignment, not an executable dependency.
internal fun readForumSprite(script: String): Map<String, String> {
    val source = script.trim()
    require(source.startsWith("window.__svg_sprite ="))
    val xml = Json.parseToJsonElement(source.substringAfter('=').trim().removeSuffix(";")).jsonPrimitive.content
    val doc = Jsoup.parse(xml, "", Parser.xmlParser())
    doc.outputSettings().prettyPrint(false).syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
    return buildMap {
        for (symbol in doc.select("symbol[id][viewBox]")) {
            val name = symbol.id()
            if (!forumIconName.matches(name)) continue
            symbol.tagName("svg").removeAttr("id").attr("xmlns", "http://www.w3.org/2000/svg")
            // Native badges need vector geometry only; exclude embedded documents and external images.
            symbol.select("script, foreignObject, image, use, style").remove()
            if (symbol.childrenSize() > 0) put(name, symbol.outerHtml())
        }
    }.also { require(it.isNotEmpty()) }
}
