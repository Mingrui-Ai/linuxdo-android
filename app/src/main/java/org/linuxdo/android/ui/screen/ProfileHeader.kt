package org.linuxdo.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import org.linuxdo.android.ui.design.*

data class ProfileMenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit, val enabled: Boolean = true)

/** 自己与他人的主页共用资料头部；账号工具只在自己的页面出现。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileHeader(
    username: String,
    card: UserCardData?,
    initialAvatarUrl: String?,
    initialStaticAvatarUrl: String?,
    onBack: (() -> Unit)?,
    onSwitchAccount: (() -> Unit)?,
    onEditProfile: (() -> Unit)?,
    onChat: (() -> Unit)?,
    onPrivateMessage: (() -> Unit)?,
    menuActions: List<ProfileMenuAction>,
    communityScore: Int?,
    communityScoreLoading: Boolean = false,
    needsAuthorization: Boolean = false,
    onOpenCommunityScore: (() -> Unit)?,
    onOpenLevel: (() -> Unit)?,
    badges: List<UserBadgeItem>?,
    badgesLoading: Boolean,
    onOpenBadges: () -> Unit,
    onOpenNetwork: (Boolean) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
) {
    val colors = IosTheme.colors
    val context = LocalContext.current
    var showMenu by remember(username) { mutableStateOf(false) }
    var previewingBackgroundUrl by remember(username) { mutableStateOf<String?>(null) }
    val surface = if (colors.isDark) Color(0xFF101113) else Color.White
    val animated = card?.animatedAvatar ?: AvatarCache.get(username)
        ?: initialAvatarUrl?.takeIf(::isAnimatedAvatarPath)
    val avatar = avatarUrlOf(card?.avatarTemplate, animated, 160, username)
        ?: initialAvatarUrl ?: initialStaticAvatarUrl
    val staticAvatar = avatarUrlOf(card?.avatarTemplate, size = 160) ?: initialStaticAvatarUrl

    Column(Modifier.fillMaxWidth().background(surface)) {
        Box(Modifier.fillMaxWidth()) {
            val background = card?.profileBackgroundUrl ?: card?.cardBackgroundUrl
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                if (colors.isDark) listOf(Color(0xFF254E78), Color(0xFF12273E), surface)
                else listOf(Color(0xFF9CC5E8), Color(0xFFDCEBFA), surface)
            )))
            if (background != null) {
                val request = rememberAnimatedImageRequest(background)
                AsyncImage(
                    model = request ?: background,
                    contentDescription = "个人主页背景图",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clickable(onClickLabel = "查看大图") {
                        previewingBackgroundUrl = background
                    },
                )
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
                                actions = menuActions + listOfNotNull(
                                    onSwitchAccount?.let { ProfileMenuAction("切换账号", Icons.Default.Person, it) },
                                    onEditProfile?.let { ProfileMenuAction("编辑个人资料", Icons.Default.Edit, it) },
                                    onPrivateMessage?.let { ProfileMenuAction("私信", Icons.Default.Email, it) },
                                    onChat?.let { ProfileMenuAction("私聊", Icons.Default.Send, it) },
                                    ProfileMenuAction("分享个人主页", Icons.Default.Share, onClick = {
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${Config.BASE_URL}/u/${android.net.Uri.encode(username)}")
                                        }, "分享个人主页"))
                                    }),
                                ),
                                onDismiss = { showMenu = false },
                            )
                        }
                    },
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                    ProfileAvatar(username, avatar, staticAvatar, card?.status, 78.dp,
                        avatarModifier = Modifier.border(3.dp, surface, CircleShape))
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card?.cleanDisplayName ?: username, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, false))
                card?.trustLevel?.let { level ->
                    Box(Modifier.heightIn(min = 32.dp).then(if (onOpenLevel != null)
                        Modifier.clickable(onClickLabel = "等级与积分", onClick = onOpenLevel) else Modifier),
                        contentAlignment = Alignment.Center) { LevelBadge(level) }
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.heightIn(min = 32.dp).align(Alignment.CenterVertically),
                    contentAlignment = Alignment.CenterStart) {
                    Text("@$username", color = colors.secondaryLabel, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.align(Alignment.CenterVertically)) {
                    ProfileAchievements(badges, badgesLoading, onOpenBadges, communityScore,
                        communityScoreLoading, needsAuthorization, onOpenCommunityScore)
                }
            }
            profileDisplayTitle(card?.title, card?.trustLevel)?.let {
                Text(it, style = IosTheme.type.caption, color = colors.secondaryLabel)
            }
            Spacer(Modifier.height(8.dp))
            ProfileDetails(card, badges, badgesLoading, onOpenBadges,
                loading = card == null, showAchievements = false)
            ProfilePublicDetails(card, onOpenNetwork, onOpenWebsite, onOpenTopic)
            Spacer(Modifier.height(12.dp))
        }
    }
    previewingBackgroundUrl?.let { url ->
        ImageViewer(url = url) { previewingBackgroundUrl = null }
    }
}

@Composable
internal fun ProfileCommunityScore(score: Int?, onClick: () -> Unit, loading: Boolean = false, needsAuthorization: Boolean = false) {
    Row(Modifier.heightIn(min = 32.dp).clickable(onClickLabel = "查看社区分", onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        Text("社区分 ${score ?: if (loading) "…" else if (needsAuthorization) "待验证" else "—"}",
            color = IosTheme.colors.secondaryLabel, fontSize = 12.sp)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(13.dp))
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
                enabled = action.enabled,
                onClick = { onDismiss(); action.onClick() },
            )
        }
    }
}
