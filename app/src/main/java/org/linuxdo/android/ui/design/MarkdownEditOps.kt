package org.linuxdo.android.ui.design

/**
 * 纯函数 Markdown 文本编辑运算结果。
 * 统一服务于原生 EditText（RichMarkdownEditorView）与单测，保证选中包裹、占位高亮、
 * 行前缀与块级插入的光标位置完全对齐交互原型。
 */
internal data class MarkdownEditResult(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)

internal object MarkdownEditOps {

    /**
     * 行内包裹（如 **粗体**、*斜体*、~~删除线~~、`行内代码`、[spoiler]...[/spoiler]、<small>...</small>）。
     * 有选区时包裹选中文本并选中正文；无选区时插入占位词并高亮占位词便于直接覆盖输入。
     */
    fun applyWrap(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefix: String,
        suffix: String,
        placeholder: String = "",
    ): MarkdownEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val selected = text.substring(start, end)
        val body = selected.ifEmpty { placeholder }
        val updated = buildString(text.length + prefix.length + suffix.length + body.length) {
            append(text, 0, start)
            append(prefix)
            append(body)
            append(suffix)
            append(text, end, text.length)
        }
        val newStart = start + prefix.length
        return MarkdownEditResult(
            text = updated,
            selectionStart = newStart,
            selectionEnd = newStart + body.length,
        )
    }

    /**
     * 行首前缀（如 # 标题、> 引用、- 无序列表、1. 有序列表、- [ ] 待办）。
     * - 有选区：按选区逐行加前缀；
     * - 无选区且当前行光标前为空：在行首插入前缀+占位词并选中占位词；
     * - 无选区且当前行已有内容：在行首补前缀并将光标移到原行内容后移后的对应位置。
     */
    fun applyLinePrefix(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefix: String,
        placeholder: String = "",
    ): MarkdownEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        if (end > start) {
            val selected = text.substring(start, end)
            val transformed = selected.split('\n').joinToString("\n") { prefix + it }
            val updated = text.substring(0, start) + transformed + text.substring(end)
            return MarkdownEditResult(
                text = updated,
                selectionStart = start,
                selectionEnd = start + transformed.length,
            )
        }
        val lineStart = if (start == 0) 0 else text.lastIndexOf('\n', start - 1) + 1
        val lineBeforeCaret = text.substring(lineStart, start)
        return if (lineBeforeCaret.isEmpty()) {
            val insertText = prefix + placeholder
            val updated = text.substring(0, lineStart) + insertText + text.substring(lineStart)
            val selStart = lineStart + prefix.length
            MarkdownEditResult(
                text = updated,
                selectionStart = selStart,
                selectionEnd = selStart + placeholder.length,
            )
        } else {
            val updated = text.substring(0, lineStart) + prefix + text.substring(lineStart)
            val newCaret = start + prefix.length
            MarkdownEditResult(
                text = updated,
                selectionStart = newCaret,
                selectionEnd = newCaret,
            )
        }
    }

    /**
     * 块级插入（如代码块、折叠详情、表格、分割线）。
     * 光标不在行首时自动补前置换行，末尾补换行；若有占位词或选区，自动选中正文区域。
     */
    fun applyBlock(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefix: String,
        suffix: String = "",
        placeholder: String = "",
    ): MarkdownEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val selected = text.substring(start, end)
        val needLead = if (start > 0 && text[start - 1] != '\n') "\n" else ""
        val body = selected.ifEmpty { placeholder }
        val insert = needLead + prefix + body + suffix + "\n"
        val updated = text.substring(0, start) + insert + text.substring(end)
        val selStart = start + needLead.length + prefix.length
        return MarkdownEditResult(
            text = updated,
            selectionStart = selStart,
            selectionEnd = selStart + body.length,
        )
    }

    /**
     * 插入标准 Markdown 链接。URL 为空时使用默认 https://linux.do，未填文字时复用 URL。
     */
    fun applyLink(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        labelInput: String,
        urlInput: String,
    ): MarkdownEditResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val url = urlInput.trim().ifEmpty { "https://linux.do" }
        val label = labelInput.trim().ifEmpty { url }
        val markdown = "[$label]($url)"
        val updated = text.substring(0, start) + markdown + text.substring(end)
        val caret = start + markdown.length
        return MarkdownEditResult(
            text = updated,
            selectionStart = caret,
            selectionEnd = caret,
        )
    }

    /**
     * 插入标准 3 列表格模板，并选中第一个数据单元格便于快速改写。
     */
    fun applyTable(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): MarkdownEditResult {
        val template = "| 列 1 | 列 2 | 列 3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |"
        val result = applyBlock(text, selectionStart, selectionEnd, prefix = template)
        val caret = result.selectionEnd + 1
        return result.copy(
            selectionStart = caret.coerceAtMost(result.text.length),
            selectionEnd = caret.coerceAtMost(result.text.length),
        )
    }

    /**
     * 插入分割线。
     */
    fun applyDivider(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): MarkdownEditResult {
        val result = applyBlock(text, selectionStart, selectionEnd, prefix = "---")
        val caret = (result.selectionEnd + 1).coerceAtMost(result.text.length)
        return result.copy(selectionStart = caret, selectionEnd = caret)
    }

    /**
     * 插入脚注（自动计算下一个可用编号，避免重复 [^1]）。
     */
    fun applyFootnote(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): MarkdownEditResult {
        val existing = Regex("""\[\^(\d+)]""").findAll(text)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toSet()
        var nextIndex = 1
        while (nextIndex in existing) nextIndex++
        return applyWrap(
            text = text,
            selectionStart = selectionStart,
            selectionEnd = selectionEnd,
            prefix = "[^$nextIndex]\n\n[^$nextIndex]: ",
            suffix = "",
            placeholder = "脚注说明",
        )
    }
}
