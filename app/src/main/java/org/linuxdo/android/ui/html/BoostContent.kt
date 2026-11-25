package org.linuxdo.android.ui.html

private val BOOST_EMOJI_SHORTCODE = Regex(":([A-Za-z0-9_+-]+(?::t[2-6])?):")

/** 简评可能混用未转换的 shortcode 和表情图片，显示时补齐已知表情。 */
internal fun boostInlineText(cooked: String, emojiUrls: Map<String, String>): List<InlineText> {
    val paragraphs = CookedParser.parseCached(cooked).filterIsInstance<CookedBlock.Paragraph>()
    val runs = paragraphs.flatMapIndexed { index, paragraph ->
        if (index == 0) paragraph.runs else listOf(InlineText("\n")) + paragraph.runs
    }
    // 已有图片也能提供映射；表情索引暂不可用时，连续相同表情仍能完整显示。
    val cookedEmojis = runs.mapNotNull { run ->
        val name = BOOST_EMOJI_SHORTCODE.matchEntire(run.text)?.groupValues?.get(1)
        if (name != null && run.emojiUrl != null) name to run.emojiUrl else null
    }.toMap()
    return runs.flatMap { run ->
        if (run.emojiUrl != null || run.code || run.href != null) return@flatMap listOf(run)
        buildList {
            var offset = 0
            BOOST_EMOJI_SHORTCODE.findAll(run.text).forEach { match ->
                val name = match.groupValues[1]
                val url = cookedEmojis[name] ?: emojiUrls[name]?.takeIf { it.isNotBlank() }
                    ?: return@forEach
                if (match.range.first > offset) add(run.copy(text = run.text.substring(offset, match.range.first)))
                add(run.copy(text = match.value, emojiUrl = url))
                offset = match.range.last + 1
            }
            if (offset < run.text.length) add(run.copy(text = run.text.substring(offset)))
        }
    }
}
