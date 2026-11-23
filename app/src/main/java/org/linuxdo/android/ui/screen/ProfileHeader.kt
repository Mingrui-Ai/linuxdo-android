package org.linuxdo.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.linuxdo.android.Config
import org.linuxdo.android.data.*
import org.linuxdo.android.ui.BadgeDetailDialog
import org.linuxdo.android.ui.design.*

data class ProfileMenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** 自己与他人的主页共用资料头部；账号工具只在自己的页面出现。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileHeader(
    username: String,
    card: UserCardData?,
    summary: UserSummaryData?,
    initialAvatarUrl: String?,
    initialStaticAvatarUrl: String?,
    onBack: (() -> Unit)?,
    onSwitchAccount: (() -> Unit)?,
    onEditProfile: (() -> Unit)?,
    onChat: (() -> Unit)?,
    onPrivateMessage: (() -> Unit)?,
    onAbout: () -> Unit,
    onTopics: () -> Unit,
    onReplies: () -> Unit,
    onRefresh: () -> Unit,
    menuActions: List<ProfileMenuAction>,
    communityScore: Int?,
    onOpenCommunityScore: (() -> Unit)?,
    onOpenLevel: (() -> Unit)?,
    badges: List<UserBadgeItem>?,
    badgesLoading: Boolean,
    badgesError: String?,
    onRefreshBadges: () -> Unit,
) {
    val colors = IosTheme.colors
    val context = LocalContext.current
    var showMenu by remember(username) { mutableStateOf(false) }
    var showBio by remember(username) { mutableStateOf(false) }
    var showBadges by remember(username) { mutableStateOf(false) }
    var selectedBadge by remember(username) { mutableStateOf<UserBadgeItem?>(null) }
    val surface = if (colors.isDark) Color(0xFF101113) else Color.White
    val animated = card?.animatedAvatar ?: AvatarCache.get(username)
        ?: initialAvatarUrl?.takeIf(::isAnimatedAvatarPath)
    val avatar = avatarUrlOf(card?.avatarTemplate, animated, 160, username)
        ?: initialAvatarUrl ?: initialStaticAvatarUrl
    val staticAvatar = avatarUrlOf(card?.avatarTemplate, size = 160) ?: initialStaticAvatarUrl

    Column(Modifier.fillMaxWidth().background(surface)) {
        Box(Modifier.fillMaxWidth()) {
            // 背景只属于主页，不回退到用户资料卡的图片。
            val background = card?.profileBackgroundUrl
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                if (colors.isDark) listOf(Color(0xFF254E78), Color(0xFF12273E), surface)
                else listOf(Color(0xFF9CC5E8), Color(0xFFDCEBFA), surface)
            )))
            if (background != null) {
                val request = rememberAnimatedImageRequest(background)
                AsyncImage(request ?: background, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = .3f), Color.Transparent, surface))))
            }
            Column(Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Top))) {
                IosNavBar(
                    title = "", onBack = onBack, background = Color.Transparent, showDivider = false,
                    trailing = {
                        Box {
                            TextButton(onClick = { showMenu = true },
                                modifier = Modifier.width(44.dp).semantics { contentDescription = "个人主页菜单" }) {
                                Text("⋯", color = colors.accent, fontSize = 22.sp)
                            }
                            ProfileMenu(
                                expanded = showMenu,
                                actions = listOfNotNull(
                                    onSwitchAccount?.let { ProfileMenuAction("切换账号", Icons.Default.Person, it) },
                                    onEditProfile?.let { ProfileMenuAction("编辑个人资料", Icons.Default.Edit, it) },
                                    onPrivateMessage?.let { ProfileMenuAction("私信", Icons.Default.Email, it) },
                                    ProfileMenuAction("分享个人主页", Icons.Default.Share) {
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${Config.BASE_URL}/u/${android.net.Uri.encode(username)}")
                                        }, "分享个人主页"))
                                    },
                                ) + menuActions + listOfNotNull(
                                    onChat?.let { ProfileMenuAction("发起聊天", Icons.Default.Send, it) },
                                    ProfileMenuAction("刷新个人主页", Icons.Default.Refresh, onRefresh),
                                ),
                                onDismiss = { showMenu = false },
                            )
                        }
                    },
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                    UserAvatar(avatarUrl = avatar, fallbackUrl = staticAvatar, username = username,
                        modifier = Modifier.size(78.dp).border(3.dp, surface, CircleShape), cornerRadius = 39.dp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card?.cleanDisplayName ?: username, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                card?.trustLevel?.let { level ->
                    Box(Modifier.heightIn(min = 32.dp)
                        .then(if (onOpenLevel != null) Modifier.clickable(onClickLabel = "等级与积分", onClick = onOpenLevel) else Modifier),
                        contentAlignment = Alignment.Center) {
                        LevelBadge(level)
                    }
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.CenterStart) {
                    Text("@$username", color = colors.secondaryLabel, fontSize = 12.sp)
                }
                if (onOpenCommunityScore != null) {
                    Row(Modifier.heightIn(min = 32.dp).clickable(onClickLabel = "查看社区分", onClick = onOpenCommunityScore),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("社区分 ${communityScore ?: "—"}", color = colors.secondaryLabel, fontSize = 12.sp)
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = colors.secondaryLabel, modifier = Modifier.size(13.dp))
                    }
                }
                ProfileBadgePreview(badges, badgesLoading) { showBadges = true }
            }
            UserBioText(card?.bioExcerpt, "还没有填写个人简介", IosTheme.type.body.copy(fontSize = 13.sp), colors.label,
                maxLines = 2, emptyColor = colors.secondaryLabel,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showBio = true })
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileMetric(summary?.likesReceived ?: card?.likesReceived, "获赞", Modifier.weight(1f), onAbout)
                ProfileMetric(summary?.topicCount ?: card?.topicCount, "话题", Modifier.weight(1f), onTopics)
                ProfileMetric(summary?.postCount ?: card?.postCount, "回复", Modifier.weight(1f), onReplies)
                ProfileMetric(summary?.daysVisited, "访问天数", Modifier.weight(1f), onAbout)
            }
        }
    }
    if (showBio) {
        AlertDialog(onDismissRequest = { showBio = false }, title = { Text("个人简介") },
            text = { UserBioText(card?.bioExcerpt, "还没有填写个人简介", IosTheme.type.body, colors.label,
                maxLines = Int.MAX_VALUE, modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { showBio = false }) { Text("完成") } }, containerColor = colors.card)
    }
    if (showBadges) {
        ProfileBadgesSheet(badges, badgesLoading, badgesError, onRefreshBadges,
            onBadgeClick = { selectedBadge = it }, onDismiss = { showBadges = false })
    }
    selectedBadge?.let { BadgeDetailDialog(it) { selectedBadge = null } }
}

@Composable
private fun ProfileMetric(value: Int?, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value?.let(::formatCount) ?: "—", color = IosTheme.colors.label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(label, color = IosTheme.colors.secondaryLabel, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileMenu(expanded: Boolean, actions: List<ProfileMenuAction>, onDismiss: () -> Unit) {
    val background = if (IosTheme.colors.isDark) IosTheme.colors.card else Color.White
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = background,
        tonalElevation = 0.dp, modifier = Modifier.background(background)) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(action.icon, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(action.label, color = IosTheme.colors.label)
                    }
                },
                onClick = { onDismiss(); action.onClick() },
            )
        }
    }
}
