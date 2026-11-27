package org.linuxdo.android.ui.html

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.html.UrlSanitizer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.linuxdo.android.data.AttachedMedia
import org.linuxdo.android.ui.PollBuilderSpec

/**
 * 回帖编辑器本地 Markdown + Discourse 语法实时预览解析器。
 * - 不发网络请求，不提交草稿；
 * - 复用 CommonMark + CookedParser，确保与帖子正文渲染视觉一致；
 * - 支持 Discourse 常用扩展语法：~~删除线~~、<small>小字</small>、GFM 表格、待办清单、
 *   [details="..."]...[/details]、[spoiler]...[/spoiler]、[poll]...[/poll]、表情短码及已上传附件预览。
 */
internal fun replyMarkdownPreview(
    raw: String,
    emojiUrls: Map<String, String> = emptyMap(),
    attachments: List<AttachedMedia> = emptyList(),
): List<CookedBlock> {
    if (raw.isBlank()) return emptyList()

    val mediaByShortUrl = attachments
        .filter { it.uploaded.shortUrl.isNotBlank() }
        .associateBy { it.uploaded.shortUrl }

    val parser = Parser.builder().build()
    val htmlRenderer = HtmlRenderer.builder()
        .escapeHtml(true)
        .sanitizeUrls(true)
        .urlSanitizer(object : UrlSanitizer {
            override fun sanitizeLinkUrl(url: String): String =
                mediaByShortUrl[url]?.previewUrl ?: contentUrl(url).orEmpty()

            override fun sanitizeImageUrl(url: String): String =
                mediaByShortUrl[url]?.previewUrl ?: contentUrl(url).orEmpty()
        })
        .softbreak("<br />\n")
        .build()

    // 1. 预处理 Discourse 块级语法（[details]、[poll]）以及 GFM 表格（CommonMark 核心不含表格扩展）
    val tokenBlocks = mutableMapOf<String, String>()
    var tokenCounter = 0
    fun registerBlockHtml(html: String): String {
        val token = "LDCOMPOSERBLOCKTOKEN${tokenCounter++}X"
        tokenBlocks[token] = html
        return "\n\n$token\n\n"
    }

    // 先保护围栏代码块，避免内部示例语法被当作 Discourse 标签或表格解析
    val fencedCodes = mutableMapOf<String, String>()
    var codeCounter = 0
    val fenceRegex = Regex("""(?ms)^(`{3,}|~{3,})([^\n]*)\n(.*?)^\1[ \t]*$""")
    var preprocessed = fenceRegex.replace(raw) { match ->
        val token = "LDCOMPOSERCODETOKEN${codeCounter++}X"
        val body = escapeHtmlText(match.groupValues[3].trimEnd())
        fencedCodes[token] = "<pre><code>$body</code></pre>"
        "\n\n$token\n\n"
    }

    // 展开 [details="标题"]...[/details]（支持单双引号或无引号）
    val detailsRegex = Regex("""(?is)\[details(?:=(?:"([^"\]]*)"|'([^'\]]*)'|([^\]]*)))?](.*?)\[/details]""")
    fun renderInlineFragment(md: String): String {
        val innerHtml = renderCoreMarkdown(md, parser, htmlRenderer, emojiUrls, mediaByShortUrl)
        return innerHtml
    }
    preprocessed = detailsRegex.replace(preprocessed) { match ->
        val summary = match.groupValues[1].ifBlank {
            match.groupValues[2].ifBlank { match.groupValues[3] }
        }.ifBlank { "查看详情" }
        val innerMd = match.groupValues[4].trim()
        val innerHtml = if (innerMd.isBlank()) "" else renderInlineFragment(innerMd)
        registerBlockHtml(
            "<details open><summary>${escapeHtmlText(summary)}</summary>$innerHtml</details>",
        )
    }

    // 展开 [poll ...]...[/poll] 为 CookedParser 可识别的 poll HTML 结构，直接复用原生 PollCard 展示
    val pollRegex = Regex("""(?is)\[poll(?:\s+[^\]]*)?](.*?)\[/poll]""")
    preprocessed = pollRegex.replace(preprocessed) { match ->
        val spec = PollBuilderSpec.fromBBCode(match.value)
        if (spec == null) {
            match.value
        } else {
            val pollName = escapeHtmlAttr(spec.pollName ?: "poll")
            val pollTitle = escapeHtmlText(spec.title.ifBlank { "投票" })
            val pollType = escapeHtmlAttr(spec.type)
            val pollResults = escapeHtmlAttr(spec.results)
            val optionsHtml = spec.options.mapIndexed { idx, opt ->
                """<li data-poll-option-id="opt-$idx" data-votes="0">${escapeHtmlText(opt)}</li>"""
            }.joinToString("")
            registerBlockHtml(
                """<div class="poll" data-poll-name="$pollName" data-poll-type="$pollType" data-poll-status="open" data-poll-results="$pollResults"><div class="poll-title">$pollTitle</div><ul>$optionsHtml</ul></div>""",
            )
        }
    }

    // 展开标准 GFM Markdown 表格
    preprocessed = extractMarkdownTables(preprocessed) { headers, rows ->
        val thead = headers.joinToString("") { "<th>${renderInlineOnly(it, parser, htmlRenderer, emojiUrls, mediaByShortUrl)}</th>" }
        val tbody = rows.joinToString("") { row ->
            "<tr>" + headers.indices.joinToString("") { colIdx ->
                val cell = row.getOrElse(colIdx) { "" }
                "<td>${renderInlineOnly(cell, parser, htmlRenderer, emojiUrls, mediaByShortUrl)}</td>"
            } + "</tr>"
        }
        registerBlockHtml("<table><thead><tr>$thead</tr></thead><tbody>$tbody</tbody></table>")
    }

    // 2. 用 CommonMark 渲染主干并应用行内扩展
    var html = renderCoreMarkdown(preprocessed, parser, htmlRenderer, emojiUrls, mediaByShortUrl)

    // 3. 回填代码块与预处理块级 HTML
    fencedCodes.forEach { (token, codeHtml) ->
        html = html.replace("<p>$token</p>", codeHtml).replace(token, codeHtml)
    }
    tokenBlocks.forEach { (token, blockHtml) ->
        html = html.replace("<p>$token</p>", blockHtml).replace(token, blockHtml)
    }

    return CookedParser().parse(html)
}

private fun renderInlineOnly(
    md: String,
    parser: Parser,
    htmlRenderer: HtmlRenderer,
    emojiUrls: Map<String, String>,
    mediaByShortUrl: Map<String, AttachedMedia>,
): String {
    val html = renderCoreMarkdown(md.trim(), parser, htmlRenderer, emojiUrls, mediaByShortUrl).trim()
    val doc = Jsoup.parseBodyFragment(html).body()
    val children = doc.children()
    return if (children.size == 1 && children[0].normalName() == "p") children[0].html() else doc.html()
}

private fun renderCoreMarkdown(
    md: String,
    parser: Parser,
    htmlRenderer: HtmlRenderer,
    emojiUrls: Map<String, String>,
    mediaByShortUrl: Map<String, AttachedMedia>,
): String {
    // 兼容 Discourse 图片尺寸标记 ![alt|690x388](url) -> 提取干净 URL 并保留 alt
    val normalized = normalizeDiscourseImages(md, mediaByShortUrl)
    val rendered = htmlRenderer.render(parser.parse(normalized))
    val body = Jsoup.parseBodyFragment(rendered).body()
    transformInlineExtensions(body, emojiUrls)
    transformTaskListItems(body)
    return body.html()
}

private val DISCOURSE_IMAGE_REGEX = Regex("""!\[([^\]|]*)\|(\d+)x(\d+)(?:,\s*\d+%)?]\(([^)]+)\)""")

private fun normalizeDiscourseImages(
    md: String,
    mediaByShortUrl: Map<String, AttachedMedia>,
): String = DISCOURSE_IMAGE_REGEX.replace(md) { match ->
    val alt = match.groupValues[1].ifBlank { "图片" }
    val rawUrl = match.groupValues[4].trim()
    val resolved = mediaByShortUrl[rawUrl]?.previewUrl ?: rawUrl
    "![$alt]($resolved)"
}

private val SHORTCODE_REGEX = Regex(""":([A-Za-z0-9_+-]+(?::t[2-6])?):""")
private val INLINE_EXTENSION_REGEX = Regex(
    """(?s)(\[spoiler](.*?)\[/spoiler])|(~~([^~\n]+?)~~)|(&lt;small&gt;(.*?)&lt;/small&gt;)|(<small>(.*?)</small>)""",
)

private fun transformInlineExtensions(root: Element, emojiUrls: Map<String, String>) {
    fun walk(node: Node) {
        if (node is Element && node.normalName() in setOf("code", "pre")) return
        if (node is TextNode) {
            val text = node.wholeText
            if (text.isEmpty()) return
            val hasExt = INLINE_EXTENSION_REGEX.containsMatchIn(text)
            val hasEmoji = emojiUrls.isNotEmpty() && SHORTCODE_REGEX.containsMatchIn(text)
            if (!hasExt && !hasEmoji) return

            val fragments = buildInlineNodes(text, emojiUrls)
            if (fragments.isNotEmpty()) {
                fragments.forEach { node.before(it) }
                node.remove()
            }
        } else {
            node.childNodes().toList().forEach(::walk)
        }
    }
    walk(root)
}

private fun buildInlineNodes(text: String, emojiUrls: Map<String, String>): List<Node> {
    val result = mutableListOf<Node>()
    var offset = 0
    for (match in INLINE_EXTENSION_REGEX.findAll(text)) {
        if (match.range.first > offset) {
            result += splitEmojiNodes(text.substring(offset, match.range.first), emojiUrls)
        }
        when {
            match.groups[1] != null -> {
                val inner = match.groupValues[2]
                val span = Element("span").addClass("spoiler")
                splitEmojiNodes(inner, emojiUrls).forEach { span.appendChild(it) }
                result += span
            }
            match.groups[3] != null -> {
                val inner = match.groupValues[4]
                val del = Element("del")
                splitEmojiNodes(inner, emojiUrls).forEach { del.appendChild(it) }
                result += del
            }
            match.groups[5] != null || match.groups[7] != null -> {
                val inner = match.groups[6]?.value ?: match.groups[8]?.value.orEmpty()
                val small = Element("small")
                splitEmojiNodes(inner, emojiUrls).forEach { small.appendChild(it) }
                result += small
            }
        }
        offset = match.range.last + 1
    }
    if (offset < text.length) {
        result += splitEmojiNodes(text.substring(offset), emojiUrls)
    }
    return result
}

private fun splitEmojiNodes(text: String, emojiUrls: Map<String, String>): List<Node> {
    if (text.isEmpty() || emojiUrls.isEmpty()) return if (text.isEmpty()) emptyList() else listOf(TextNode(text))
    val nodes = mutableListOf<Node>()
    var offset = 0
    for (match in SHORTCODE_REGEX.findAll(text)) {
        val url = emojiUrls[match.groupValues[1]]?.let(::contentUrl) ?: continue
        if (match.range.first > offset) {
            nodes += TextNode(text.substring(offset, match.range.first))
        }
        nodes += Element("img").addClass("emoji").attr("src", url).attr("alt", match.value)
        offset = match.range.last + 1
    }
    if (offset < text.length) {
        nodes += TextNode(text.substring(offset))
    }
    return nodes
}

private fun transformTaskListItems(root: Element) {
    val taskPrefixRegex = Regex("""^\[([ xX])]\s+""")
    root.select("li").forEach { li ->
        val firstText = findFirstTextNode(li) ?: return@forEach
        val match = taskPrefixRegex.find(firstText.wholeText) ?: return@forEach
        val checked = match.groupValues[1].equals("x", ignoreCase = true)
        val replacement = (if (checked) "☑ " else "☐ ") + firstText.wholeText.substring(match.range.last + 1)
        firstText.text(replacement)
    }
}

private fun findFirstTextNode(node: Node): TextNode? {
    if (node is TextNode) return node.takeIf { it.wholeText.isNotEmpty() }
    for (child in node.childNodes()) {
        if (child is Element && child.normalName() in setOf("ul", "ol")) continue
        val found = findFirstTextNode(child)
        if (found != null) return found
    }
    return null
}

private val TABLE_DIVIDER_REGEX = Regex("""^\|?[\s:]*-{3,}[\s:]*(\|[\s:]*-{3,}[\s:]*)+\|?$""")

private fun extractMarkdownTables(
    text: String,
    onTable: (headers: List<String>, rows: List<List<String>>) -> String,
): String {
    val lines = text.split('\n')
    val out = StringBuilder()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (i + 1 < lines.size &&
            line.contains('|') &&
            TABLE_DIVIDER_REGEX.matches(lines[i + 1].trim())
        ) {
            val headers = parseMarkdownTableRow(line)
            if (headers.size >= 2) {
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows += parseMarkdownTableRow(lines[i])
                    i++
                }
                out.append(onTable(headers, rows))
                continue
            }
        }
        out.append(line)
        if (i < lines.lastIndex) out.append('\n')
        i++
    }
    return out.toString()
}

private fun parseMarkdownTableRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

private fun escapeHtmlText(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun escapeHtmlAttr(text: String): String = escapeHtmlText(text).replace("\"", "&quot;")
