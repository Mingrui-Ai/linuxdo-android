package org.linuxdo.android.ui.html

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.html.UrlSanitizer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** Local preview never submits the draft. Links and upload short URLs share the reader's URL rules. */
internal fun profileBioPreview(raw: String, emojiUrls: Map<String, String>): List<CookedBlock> {
    val renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true)
        .urlSanitizer(object : UrlSanitizer {
            override fun sanitizeLinkUrl(url: String) = contentUrl(url).orEmpty()
            override fun sanitizeImageUrl(url: String) = contentUrl(url).orEmpty()
        }).softbreak("<br />\n").build()
    val body = Jsoup.parseBodyFragment(renderer.render(Parser.builder().build().parse(raw))).body()
    val shortcode = Regex(":([A-Za-z0-9_+-]+(?::t[2-6])?):")
    fun replaceEmoji(node: org.jsoup.nodes.Node) {
        if (node is Element && node.normalName() in setOf("code", "pre")) return
        if (node is TextNode) {
            val text = node.wholeText
            var offset = 0
            shortcode.findAll(text).forEach { match ->
                val url = emojiUrls[match.groupValues[1]]?.let(::contentUrl) ?: return@forEach
                if (match.range.first > offset) node.before(TextNode(text.substring(offset, match.range.first)))
                node.before(Element("img").addClass("emoji").attr("src", url).attr("alt", match.value))
                offset = match.range.last + 1
            }
            if (offset > 0) { node.before(TextNode(text.substring(offset))); node.remove() }
        } else node.childNodes().toList().forEach(::replaceEmoji)
    }
    replaceEmoji(body)
    return CookedParser().parse(body.html())
}
