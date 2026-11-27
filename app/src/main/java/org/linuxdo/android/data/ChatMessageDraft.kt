package org.linuxdo.android.data

internal data class ChatMessageDraft(val message: String, val uploadIds: List<Long>)

/** 所有发送入口使用点击时的正文快照，附件已在正文中时不重复追加。 */
internal fun prepareChatMessage(raw: String, attachments: List<AttachedMedia>): ChatMessageDraft? {
    val text = raw.trim()
    val missing = attachments.filter { media ->
        val urls = listOfNotNull(media.uploaded.shortUrl, media.uploaded.url).filter { it.isNotBlank() }
        urls.none(text::contains)
    }
    val message = listOf(text, missing.joinToString("\n") { it.toMarkdown() })
        .filter { it.isNotBlank() }.joinToString("\n\n")
    return message.takeIf { it.isNotBlank() }?.let {
        ChatMessageDraft(it, attachments.mapNotNull { media -> media.uploadId }.distinct())
    }
}
