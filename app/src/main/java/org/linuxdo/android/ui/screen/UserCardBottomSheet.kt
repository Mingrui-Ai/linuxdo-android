package org.linuxdo.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.linuxdo.android.App
import org.linuxdo.android.data.AvatarCache
import org.linuxdo.android.data.UserCardData
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.CdkScoreUiState
import org.linuxdo.android.ui.UserCardUiState
import org.linuxdo.android.ui.rememberDrawerBadges
import org.linuxdo.android.ui.design.*

/** 头像弹窗只展示简要资料；用户操作集中在详细主页菜单。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCardBottomSheet(
    state: UserCardUiState,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenBadges: (String) -> Unit,
    communityScore: CdkScoreUiState? = null,
    onOpenCommunityScore: (() -> Unit)? = null,
) {
    val accountId = (LocalContext.current.applicationContext as App).container.sessionStore.currentUserId()
    val user = state.user
    val badges = rememberDrawerBadges(user?.username, accountId, user != null)
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IosTheme.colors.card) {
        Box(Modifier.fillMaxWidth()) {
            user?.cardBackgroundUrl?.let { background ->
                val request = rememberAnimatedImageRequest(background)
                AsyncImage(model = request ?: background, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                    IosTheme.colors.card.copy(alpha = .7f), IosTheme.colors.card.copy(alpha = .96f)))))
            }
            when {
                user != null -> Column(Modifier.fillMaxWidth().sheetScrollable()
                    .padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompactUserIdentityHeader(user, onOpenProfile = { onOpenProfile(user.username) })
                    ProfileDetails(user, badges.items, badges.loading,
                        onOpenBadges = { onOpenBadges(user.username) },
                        communityScore = communityScore?.score,
                        communityScoreLoading = communityScore?.loading == true,
                        needsAuthorization = communityScore?.needsAuthorization == true,
                        onOpenCommunityScore = onOpenCommunityScore, loading = state.loading)
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

/** 简评弹层也沿用相同的头像、昵称/等级、用户名顺序。 */
@Composable
internal fun CompactUserIdentityHeader(
    user: UserCardData,
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
) {
    ProfileIdentity(user.username, user.cleanDisplayName, user.trustLevel,
        avatarUrlOf(user.avatarTemplate, user.animatedAvatar ?: AvatarCache.get(user.username), 160, user.username),
        avatarUrlOf(user.avatarTemplate, size = 160), modifier = modifier, onOpenProfile = onOpenProfile)
}
