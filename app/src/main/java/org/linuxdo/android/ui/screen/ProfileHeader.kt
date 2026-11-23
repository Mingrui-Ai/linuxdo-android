package org.linuxdo.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.linuxdo.android.Config
import org.linuxdo.android.data.*
import org.linuxdo.android.ui.design.*

data class ProfileMenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** 自己与他人的主页共用资料头部；账号工具只在自己的页面出现。 */
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
) {
    val colors = IosTheme.colors
    val context = LocalContext.current
    var showMenu by remember(username) { mutableStateOf(false) }
    var showBio by remember(username) { mutableStateOf(false) }
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
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        ProfileHeaderButton(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack)
                    } else {
                        Row(
                            Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0x99101C2A))
                                .then(if (onSwitchAccount != null) Modifier.clickable(onClick = onSwitchAccount) else Modifier)
                                .padding(horizontal = 14.dp, vertical = 10.dp).weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(username, color = Color.White, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                            if (onSwitchAccount != null) Icon(Icons.Default.KeyboardArrowDown, "切换账号", tint = Color.White,
                                modifier = Modifier.padding(start = 6.dp).size(18.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    ProfileHeaderButton(Icons.Default.Share, "分享个人主页") {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${Config.BASE_URL}/u/${android.net.Uri.encode(username)}")
                        }, "分享个人主页"))
                    }
                    Spacer(Modifier.width(8.dp))
                    ProfileHeaderButton(Icons.Default.Menu, "个人主页菜单") { showMenu = true }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    UserAvatar(avatarUrl = avatar, fallbackUrl = staticAvatar, username = username,
                        modifier = Modifier.size(78.dp).border(3.dp, surface, CircleShape), cornerRadius = 39.dp)
                    Spacer(Modifier.weight(1f))
                    if (onEditProfile != null) {
                        OutlinedButton(onClick = onEditProfile, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp), border = androidx.compose.foundation.BorderStroke(1.dp, colors.separator)) {
                            Text("编辑资料", color = colors.label, fontSize = 12.sp)
                        }
                    } else if (onPrivateMessage != null) {
                        OutlinedButton(onClick = onPrivateMessage, modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.separator)) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(15.dp), tint = colors.label)
                            Spacer(Modifier.width(6.dp))
                            Text("私信", color = colors.label, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card?.cleanDisplayName ?: username, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                card?.trustLevel?.let { LevelBadge(it) }
            }
            Text("@$username", color = colors.secondaryLabel, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
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
    if (showMenu) {
        ProfileMenuSheet(username, menuActions + listOfNotNull(
            onChat?.let { ProfileMenuAction("发起聊天", Icons.Default.Send, it) },
            ProfileMenuAction("刷新个人主页", Icons.Default.Refresh, onRefresh),
        )) { showMenu = false }
    }
}

@Composable
private fun ProfileMetric(value: Int?, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value?.let(::formatCount) ?: "—", color = IosTheme.colors.label, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(label, color = IosTheme.colors.secondaryLabel, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileHeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(40.dp).background(Color(0x99101C2A), CircleShape)) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileMenuSheet(username: String, actions: List<ProfileMenuAction>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IosTheme.colors.card) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(username, color = IosTheme.colors.label, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
            actions.forEach { action ->
                Row(Modifier.fillMaxWidth().clickable { onDismiss(); action.onClick() }
                    .padding(horizontal = 24.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(action.icon, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(21.dp))
                    Text(action.label, color = IosTheme.colors.label, modifier = Modifier.padding(start = 18.dp).weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = IosTheme.colors.tertiaryLabel, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
