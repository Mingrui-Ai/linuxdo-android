package org.linuxdo.android.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class UpdateNoteKind { Heading, Bullet, Paragraph }

internal data class UpdateNoteLine(val text: String, val kind: UpdateNoteKind)

/** 清单使用可被旧版直接阅读的纯文本；同时兼容 Release 回退内容中的标题与列表。 */
internal fun parseUpdateNoteLines(raw: String): List<UpdateNoteLine> {
    val heading = Regex("^#{1,6}\\s+(.+)$")
    val bullet = Regex("^(?:[-*•]\\s+|[0-9]+[.)、]\\s*)")
    return raw.lineSequence().mapNotNull { source ->
        val line = source.trim()
        when {
            line.isEmpty() -> null
            line.startsWith("【") && line.endsWith("】") && line.length > 2 ->
                UpdateNoteLine(line.substring(1, line.length - 1), UpdateNoteKind.Heading)
            heading.matches(line) ->
                UpdateNoteLine(heading.matchEntire(line)!!.groupValues[1], UpdateNoteKind.Heading)
            bullet.containsMatchIn(line) ->
                line.replaceFirst(bullet, "").takeIf { it.isNotBlank() }
                    ?.let { UpdateNoteLine(it, UpdateNoteKind.Bullet) }
            else -> UpdateNoteLine(line, UpdateNoteKind.Paragraph)
        }
    }.toList()
}

@Composable
internal fun UpdateReleaseNotes(raw: String) {
    val lines = remember(raw) { parseUpdateNoteLines(raw) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        lines.forEachIndexed { index, line ->
            if (line.kind == UpdateNoteKind.Heading && index > 0) Spacer(Modifier.height(5.dp))
            when (line.kind) {
                UpdateNoteKind.Heading -> Text(
                    text = line.text,
                    style = IosTheme.type.footnote.copy(fontSize = 13.sp, lineHeight = 20.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = IosTheme.colors.label,
                )
                UpdateNoteKind.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.width(10.dp),
                        style = IosTheme.type.footnote.copy(fontSize = 13.sp, lineHeight = 20.sp),
                        color = IosTheme.colors.secondaryLabel,
                    )
                    Text(
                        text = line.text,
                        modifier = Modifier.weight(1f),
                        style = IosTheme.type.footnote.copy(fontSize = 13.sp, lineHeight = 20.sp),
                        color = IosTheme.colors.label,
                    )
                }
                UpdateNoteKind.Paragraph -> Text(
                    text = line.text,
                    style = IosTheme.type.footnote.copy(fontSize = 13.sp, lineHeight = 20.sp),
                    color = IosTheme.colors.label,
                )
            }
        }
    }
}
