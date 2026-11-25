package org.linuxdo.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linuxdo.android.data.UserBadgeItem
import org.linuxdo.android.data.UserCardData
import org.linuxdo.android.ui.design.*
import org.linuxdo.android.ui.format.RelativeTime

/** 侧栏、头像弹窗和详细主页共用身份顺序与排版。 */
@Composable
internal fun ProfileIdentity(
    username: String,
    displayName: String,
    level: Int?,
    avatarUrl: String?,
    staticAvatarUrl: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 56.dp,
    onOpenProfile: (() -> Unit)? = null,
    onOpenLevel: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = IosTheme.colors
    val openProfile = if (onOpenProfile != null) Modifier.clickable(role = Role.Button,
        onClickLabel = "查看 @$username 的详细主页", onClick = onOpenProfile) else Modifier
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        UserAvatar(avatarUrl, staticAvatarUrl, username, Modifier.size(avatarSize).then(openProfile),
            cornerRadius = avatarSize / 2)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(displayName, style = IosTheme.type.largeTitle, color = colors.label,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, false).then(openProfile))
                level?.let {
                    Box(Modifier.heightIn(min = 32.dp).then(if (onOpenLevel != null)
                        Modifier.clickable(role = Role.Button, onClickLabel = "等级与积分", onClick = onOpenLevel)
                        else Modifier), contentAlignment = Alignment.Center) { LevelBadge(it) }
                }
            }
            Text("@$username", style = IosTheme.type.caption, color = colors.secondaryLabel,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

/** 三处固定为：简介 → 加入/活跃 → 社区分/徽章。未知数据不冒充真实数值。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileDetails(
    card: UserCardData?,
    badges: List<UserBadgeItem>?,
    badgesLoading: Boolean,
    onOpenBadges: () -> Unit,
    communityScore: Int? = null,
    communityScoreLoading: Boolean = false,
    needsAuthorization: Boolean = false,
    onOpenCommunityScore: (() -> Unit)? = null,
    loading: Boolean = false,
    active: Boolean = true,
) {
    val colors = IosTheme.colors
    var showBio by remember(card?.username) { mutableStateOf(false) }
    var now by remember { mutableStateOf(java.time.Instant.now()) }
    LaunchedEffect(active) {
        while (active) {
            now = java.time.Instant.now()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        UserBioText(card?.bioExcerpt, if (loading) "正在加载资料…" else "还没有填写个人简介",
            IosTheme.type.body.copy(fontSize = 13.sp), colors.label, maxLines = 2,
            emptyColor = colors.secondaryLabel, loadImages = active,
            modifier = Modifier.fillMaxWidth().then(if (!card?.bioExcerpt.isNullOrBlank())
                Modifier.clickable(onClickLabel = "查看完整简介") { showBio = true } else Modifier))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("加入时间 ${profileJoinDate(card?.createdAt)}", style = IosTheme.type.caption, color = colors.secondaryLabel)
            Text("最近活跃 ${if (card?.lastSeenAt.isNullOrBlank()) if (loading) "…" else "未公开" else RelativeTime.format(card?.lastSeenAt, now)}",
                style = IosTheme.type.caption, color = colors.secondaryLabel)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onOpenCommunityScore != null) {
                ProfileCommunityScore(communityScore, onOpenCommunityScore, communityScoreLoading, needsAuthorization)
            } else {
                Box(Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.CenterStart) {
                    Text("社区分 未公开", color = colors.secondaryLabel, fontSize = 12.sp)
                }
            }
            ProfileBadgePreview(badges, badgesLoading, onOpenBadges)
        }
    }
    if (showBio) AlertDialog(onDismissRequest = { showBio = false }, title = { Text("个人简介") },
        text = { UserBioText(card?.bioExcerpt, "还没有填写个人简介", IosTheme.type.body, colors.label,
            maxLines = Int.MAX_VALUE, modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { showBio = false }) { Text("完成") } }, containerColor = colors.card)
}

internal fun profileJoinDate(raw: String?): String = runCatching {
    java.time.OffsetDateTime.parse(raw).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate().toString()
}.getOrDefault("—")
