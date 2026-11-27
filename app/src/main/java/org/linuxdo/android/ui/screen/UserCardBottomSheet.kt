package org.linuxdo.android.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.linuxdo.android.App
import org.linuxdo.android.data.AvatarCache
import org.linuxdo.android.data.ProfileDisplayField
import org.linuxdo.android.ui.rememberProfileDisplayPreferences
import org.linuxdo.android.data.UserCardData
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.CdkScoreUiState
import org.linuxdo.android.ui.UserCardUiState
import org.linuxdo.android.ui.rememberDrawerBadges
import org.linuxdo.android.ui.design.*

/** 头像弹窗展示简要资料，并提供关注与聊天快捷入口。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCardBottomSheet(
    state: UserCardUiState,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenBadges: (String) -> Unit,
    onOpenNetwork: (String, Boolean) -> Unit,
    signedIn: Boolean,
    onSetFollowing: suspend (UserCardData, Boolean) -> Unit,
    onChat: (String) -> Unit,
    communityScore: CdkScoreUiState? = null,
    onOpenCommunityScore: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val accountId = (context.applicationContext as App).container.sessionStore.currentUserId()
    val scope = rememberCoroutineScope()
    val display = rememberProfileDisplayPreferences()
    val user = state.user
    var followUpdating by remember(accountId, user?.username) { mutableStateOf(false) }
    if (display[ProfileDisplayField.Background]) PrefetchProfileCover(user)
    val badges = rememberDrawerBadges(user?.username, accountId, user != null)
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
        containerColor = IosTheme.colors.card) {
        Box(Modifier.fillMaxWidth()) {
            user?.cardBackgroundUrl?.takeIf { display[ProfileDisplayField.Background] }?.let { background ->
                val request = rememberAnimatedImageRequest(background)
                AsyncImage(model = request ?: background, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                    IosTheme.colors.card.copy(alpha = .7f), IosTheme.colors.card.copy(alpha = .96f)))))
            }
            Column(Modifier.fillMaxWidth().sheetScrollable().navigationBarsPadding()) {
                // 把拖动条放进背景层，避免 Material 默认拖动栏留下整条白边。
                Box(Modifier.fillMaxWidth().height(24.dp).semantics {
                    contentDescription = "用户资料拖动条"
                    dismiss { onDismiss(); true }
                }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp)
                        .background(IosTheme.colors.secondaryLabel.copy(alpha = .45f), CircleShape))
                }
                when {
                user != null -> Column(Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactUserIdentityHeader(user, onOpenProfile = { onOpenProfile(user.username) },
                        trailing = if (signedIn && !state.isSelf) ({
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UserCardQuickAction(
                                    label = if (state.following) "已关注" else "关注",
                                    description = if (state.following) "取消关注 @${user.username}" else "关注 @${user.username}",
                                    icon = if (state.following) Icons.Default.Check else Icons.Default.Person,
                                    enabled = !state.loading && !followUpdating && (user.canFollow || state.following),
                                    loading = followUpdating,
                                    onClick = {
                                        if (!followUpdating) {
                                            val following = !state.following
                                            followUpdating = true
                                            scope.launch {
                                                try {
                                                    onSetFollowing(user, following)
                                                } catch (cancelled: CancellationException) {
                                                    throw cancelled
                                                } catch (failure: Exception) {
                                                    Toast.makeText(context, failure.message ?: "关注操作失败", Toast.LENGTH_SHORT).show()
                                                } finally { followUpdating = false }
                                            }
                                        }
                                    },
                                )
                                UserCardQuickAction(label = "聊天", description = "与 @${user.username} 聊天",
                                    icon = Icons.AutoMirrored.Filled.Send, enabled = !followUpdating,
                                    onClick = { onChat(user.username) })
                            }
                        }) else null)
                    ProfileDetails(user, badges.items, badges.loading,
                        onOpenBadges = { onOpenBadges(user.username) },
                        communityScore = communityScore?.score,
                        communityScoreLoading = communityScore?.loading == true,
                        needsAuthorization = communityScore?.needsAuthorization == true,
                        onOpenCommunityScore = onOpenCommunityScore, loading = state.loading,
                        onOpenNetwork = { onOpenNetwork(user.username, it) }, onOpenUser = onOpenProfile)
                    state.error?.let { Text(it, style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel) }
                    Spacer(Modifier.height(4.dp))
                }
                state.loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    IosActivityIndicator(diameter = 24.dp, color = IosTheme.colors.accent)
                }
                else -> Text(state.error ?: "暂时无法加载资料", color = IosTheme.colors.secondaryLabel,
                    modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun UserCardQuickAction(
    label: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    val tint = if (enabled || loading) colors.accent else colors.secondaryLabel.copy(alpha = .5f)
    Surface(onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(12.dp), color = colors.card.copy(alpha = .9f), contentColor = tint,
        border = BorderStroke(1.dp, tint.copy(alpha = .18f)),
        modifier = Modifier.widthIn(min = 48.dp, max = 72.dp).heightIn(min = 48.dp)
            .semantics { contentDescription = description }) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
            if (loading) IosActivityIndicator(diameter = 20.dp, color = tint)
            else Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 简评弹层也沿用相同的头像、昵称/等级、用户名顺序。 */
@Composable
internal fun CompactUserIdentityHeader(
    user: UserCardData,
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ProfileIdentity(user.username, user.cleanDisplayName, user.trustLevel,
        avatarUrlOf(user.avatarTemplate, user.animatedAvatar ?: AvatarCache.get(user.username), 160, user.username),
        avatarUrlOf(user.avatarTemplate, size = 160), modifier = modifier, onOpenProfile = onOpenProfile,
        status = user.status, title = user.title, trailing = trailing)
}
