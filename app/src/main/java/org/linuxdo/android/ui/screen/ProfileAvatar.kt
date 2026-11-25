package org.linuxdo.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.Instant
import kotlinx.coroutines.delay
import org.linuxdo.android.App
import org.linuxdo.android.data.ProfileStatus
import org.linuxdo.android.data.normalizeProfileStatusEmoji
import org.linuxdo.android.data.profileWebsiteUrl
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.UserAvatar
import org.linuxdo.android.ui.design.rememberAnimatedImageRequest

/** 状态按钮与头像分别处理点击，查看状态不会跳转详细主页。 */
@Composable
internal fun ProfileAvatar(
    username: String,
    avatarUrl: String?,
    staticAvatarUrl: String?,
    status: ProfileStatus?,
    size: Dp,
    avatarModifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    avatarClickLabel: String = "查看 @$username 的详细主页",
    onImageLoaded: ((coil.memory.MemoryCache.Key?) -> Unit)? = null,
) {
    var now by remember(status) { mutableStateOf(Instant.now()) }
    LaunchedEffect(status) {
        while (status?.isVisible(Instant.now()) == true) {
            now = Instant.now()
            val remaining = status.endsAt?.let { runCatching {
                java.time.Duration.between(now, Instant.parse(it)).toMillis()
            }.getOrNull() } ?: 60_000L
            delay(remaining.coerceIn(1L, 60_000L))
        }
        now = Instant.now()
    }
    Box(Modifier.size(size)) {
        UserAvatar(avatarUrl, staticAvatarUrl, username, Modifier.fillMaxSize().then(avatarModifier)
            .then(if (onOpenProfile != null) Modifier.clickable(role = Role.Button,
                onClickLabel = avatarClickLabel, onClick = onOpenProfile) else Modifier),
            cornerRadius = size / 2, onImageLoaded = onImageLoaded)
        if (status?.isVisible(now) == true) {
            var expanded by remember(username, status) { mutableStateOf(false) }
            Box(Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-10).dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).semantics { contentDescription = "@$username 的状态" }
                    .clickable(role = Role.Button,
                    onClickLabel = "查看 @$username 的状态") { expanded = true },
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(28.dp).background(IosTheme.colors.card, CircleShape)
                        .border(1.dp, IosTheme.colors.separator, CircleShape), contentAlignment = Alignment.Center) {
                        ProfileStatusEmoji(status.emoji)
                    }
                }
                DropdownMenu(expanded, onDismissRequest = { expanded = false },
                    containerColor = IosTheme.colors.card, tonalElevation = 0.dp) {
                    Text(status.description, style = IosTheme.type.body, color = IosTheme.colors.label,
                        modifier = Modifier.widthIn(max = 260.dp).padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileStatusEmoji(raw: String) {
    val repo = (LocalContext.current.applicationContext as App).container.topicRepository
    val code = normalizeProfileStatusEmoji(raw)
    val isShortcode = code.matches(Regex("[a-zA-Z0-9_+\\-:]+"))
    val url by produceState<String?>(null, code) {
        if (isShortcode) value = profileWebsiteUrl(repo.emojiUrls()[code])
            ?: "${org.linuxdo.android.Config.BASE_URL}/images/emoji/twemoji/$code.png?v=15"
    }
    var failed by remember(url) { mutableStateOf(false) }
    val fallback = if (!isShortcode) raw else when (code) {
        "mag" -> "🔍"
        "mag_right" -> "🔎"
        else -> renderEmojiShortcodes(":$code:")?.takeIf { it.isNotBlank() } ?: "💬"
    }
    // 图片加载期间也保留可见表情，索引/图片失败不影响查看状态文字。
    Text(fallback, fontSize = 18.sp)
    if (url != null && !failed) {
        val request = rememberAnimatedImageRequest(url)
        AsyncImage(model = request ?: url, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.size(20.dp).background(IosTheme.colors.card), onError = { failed = true })
    }
}
