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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.linuxdo.android.Config
import org.linuxdo.android.data.*
import org.linuxdo.android.data.ProfileDisplayField.*
import org.linuxdo.android.ui.rememberProfileDisplayPreferences
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
    onOpenDisplaySettings: () -> Unit,
    onChat: (() -> Unit)?,
    followAction: ProfileMenuAction?,
    notificationController: org.linuxdo.android.ui.UserNotificationController?,
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
    onOpenUser: ((String) -> Unit)?,
) {
    val display = rememberProfileDisplayPreferences()
    val colors = IosTheme.colors
    val context = LocalContext.current
    var showMenu by remember(username) { mutableStateOf(false) }
    var previewImage by remember(username) { mutableStateOf<ImageViewerItem?>(null) }
    val surface = if (colors.isDark) Color(0xFF101113) else Color.White
    val animated = card?.animatedAvatar ?: AvatarCache.get(username)
        ?: initialAvatarUrl?.takeIf(::isAnimatedAvatarPath)
    val avatar = avatarUrlOf(card?.avatarTemplate, animated, 160, username)
        ?: initialAvatarUrl ?: initialStaticAvatarUrl
    val staticAvatar = avatarUrlOf(card?.avatarTemplate, size = 160) ?: initialStaticAvatarUrl
    var avatarPreviewKey by remember(avatar, staticAvatar) { mutableStateOf<coil.memory.MemoryCache.Key?>(null) }
    val fullAvatar = avatarUrlOf(card?.avatarTemplate, animated, 720, username) ?: avatar ?: staticAvatar

    Column(Modifier.fillMaxWidth().background(surface)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val background = (card?.profileBackgroundUrl ?: card?.cardBackgroundUrl).takeIf { display[Background] }
            val coverHeight = profileCoverHeight(maxWidth)
            val topInset = with(LocalDensity.current) {
                WindowInsets.systemBars.union(WindowInsets.displayCutout).getTop(this).toDp()
            }
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                if (colors.isDark) listOf(Color(0xFF254E78), Color(0xFF12273E), surface)
                else listOf(Color(0xFF9CC5E8), Color(0xFFDCEBFA), surface)
            )))
            if (background != null) {
                var retryToken by remember(background) { mutableIntStateOf(0) }
                var retryPending by remember(background) { mutableStateOf(false) }
                LaunchedEffect(background, retryPending) {
                    if (retryPending) {
                        org.linuxdo.android.net.awaitImageRetrySlot(retryToken)
                        retryPending = false
                        retryToken++
                    }
                }
                val request = rememberProfileCoverRequest(background, maxWidth, retryToken)
                var coverPreviewKey by remember(background, request) { mutableStateOf<coil.memory.MemoryCache.Key?>(null) }
                Box(Modifier.fillMaxWidth().height(coverHeight + topInset)
                    .clickable(onClickLabel = "查看背景大图") {
                        previewImage = ImageViewerItem(background, description = "@$username 的背景图",
                            previewMemoryCacheKey = coverPreviewKey)
                    }) {
                    AsyncImage(model = request, contentDescription = "个人主页背景图",
                        contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(),
                        onSuccess = { coverPreviewKey = it.result.memoryCacheKey },
                        onError = {
                            if (retryToken < org.linuxdo.android.net.IMAGE_AUTO_RETRY_LIMIT &&
                                org.linuxdo.android.net.isTransientImageError(it.result.throwable)) retryPending = true
                        })
                    // 渐变仅用于顶栏可读性与封面底沿过渡，保留中部完整画面。
                    Box(Modifier.fillMaxWidth().height(topInset + IosMetrics.navBarHeight + 16.dp)
                        .background(Brush.verticalGradient(listOf(surface.copy(alpha = .85f), Color.Transparent))))
                    Box(Modifier.fillMaxWidth().height(44.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, surface))))
                }
            }
            Column(Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Top))) {
                IosNavBar(
                    title = "", onBack = onBack, background = Color.Transparent, showDivider = false,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${Config.BASE_URL}/u/${android.net.Uri.encode(username)}")
                                }, "分享个人主页"))
                            }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Default.Share, "分享个人主页", tint = colors.accent)
                            }
                            notificationController?.let { ProfileNotificationButton(it) }
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
                                        ProfileMenuAction("主页显示设置", Icons.Default.Settings, onOpenDisplaySettings),
                                        onPrivateMessage?.let { ProfileMenuAction("私信", Icons.Default.Email, it) },
                                    ),
                                    onDismiss = { showMenu = false },
                                )
                            }
                        }
                    },
                )
                Spacer(Modifier.height(if (background != null) coverHeight - IosMetrics.navBarHeight - 39.dp else 24.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                    ProfileAvatar(username, avatar, staticAvatar, card?.status, 78.dp,
                        avatarModifier = Modifier.border(3.dp, surface, CircleShape),
                        onOpenProfile = fullAvatar?.let { url -> {
                            previewImage = ImageViewerItem(url, description = "@$username 的头像",
                                previewMemoryCacheKey = avatarPreviewKey)
                        } },
                        avatarClickLabel = "查看 @$username 的头像大图",
                        onImageLoaded = { avatarPreviewKey = it })
                    Spacer(Modifier.weight(1f))
                    if (followAction != null || onChat != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                            followAction?.let { action ->
                                ProfileQuickAction(action.label, action.icon, action.enabled, action.onClick)
                            }
                            onChat?.let { ProfileQuickAction("聊天", Icons.Default.Send, true, it) }
                        }
                    }
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
                card?.trustLevel?.takeIf { display[Level] }?.let { level ->
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
                if (display[Badges] || (display[CommunityScore] && onOpenCommunityScore != null)) Box(Modifier.align(Alignment.CenterVertically)) {
                    ProfileAchievements(badges, badgesLoading, onOpenBadges, communityScore,
                        communityScoreLoading, needsAuthorization, onOpenCommunityScore)
                }
            }
            profileDisplayTitle(card?.title, card?.trustLevel)?.takeIf { display[Title] }?.let {
                Text(it, style = IosTheme.type.caption, color = colors.secondaryLabel)
            }
            ProfileDetails(card, badges, badgesLoading, onOpenBadges,
                loading = card == null, showAchievements = false, onOpenNetwork = onOpenNetwork,
                modifier = Modifier.padding(top = 8.dp), onOpenUser = onOpenUser)
            ProfilePublicDetails(card, onOpenWebsite, onOpenTopic)
            Spacer(Modifier.height(12.dp))
        }
    }
    previewImage?.let { image ->
        ImageViewer(images = listOf(image)) { previewImage = null }
    }
}

@Composable
private fun ProfileQuickAction(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val colors = IosTheme.colors
    Surface(onClick = onClick, enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colors.card.copy(alpha = .94f), contentColor = if (enabled) colors.accent else colors.secondaryLabel,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = .16f))) {
        Row(Modifier.widthIn(min = 100.dp).heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 13.sp, maxLines = 1)
        }
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
