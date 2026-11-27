package org.linuxdo.android.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linuxdo.android.data.DiscourseUser
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.UserAvatar

/** 与网页 Presence 一致：真实回复者头像 + 提示 + 1.8 秒三点波浪；无人时不占空间。 */
@Composable
internal fun TopicReplyPresenceIndicator(users: List<DiscourseUser>, modifier: Modifier = Modifier) {
    if (users.isEmpty()) return
    BoxWithConstraints(modifier.clearAndSetSemantics {
        contentDescription = "${users.joinToString("、") { it.name?.takeIf(String::isNotBlank) ?: it.username }} 正在回复"
    }) {
        val avatarCount = when {
            maxWidth >= 136.dp -> 2
            maxWidth >= 108.dp -> 1
            else -> 0
        }
        val color = IosTheme.colors.secondaryLabel
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            users.take(avatarCount).forEach { user ->
                UserAvatar(avatarUrl = avatarUrlOf(user.avatarTemplate, user.animatedAvatar, username = user.username),
                    fallbackUrl = avatarUrlOf(user.avatarTemplate), username = user.username,
                    modifier = Modifier.size(18.dp), cornerRadius = 9.dp)
            }
            Text("正在回复", fontSize = 11.sp, color = color, maxLines = 1, softWrap = false,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            val transition = rememberInfiniteTransition(label = "reply-presence")
            repeat(3) { index ->
                val lift by transition.animateFloat(0f, 0f,
                    infiniteRepeatable(keyframes {
                        durationMillis = 1800
                        0f at 0
                        0f at index * 200
                        1f at 540 + index * 200
                        0f at 1080 + index * 200
                        0f at 1800
                    }, repeatMode = RepeatMode.Restart), label = "reply-dot-$index")
                Canvas(Modifier.size(width = 2.dp, height = 12.dp)) {
                    drawCircle(color, radius = 1.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.7f - lift * 2.dp.toPx()))
                }
            }
        }
    }
}
