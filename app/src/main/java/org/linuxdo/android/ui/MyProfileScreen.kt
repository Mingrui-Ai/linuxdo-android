package org.linuxdo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.screen.*

/** 自己与其他用户共用详细主页；个人工具统一从首页侧栏进入。 */
@Composable
fun ProfileScreen(
    username: String?,
    active: Boolean,
    accountState: AccountUiState,
    onAddAccount: () -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenBadges: (String) -> Unit,
    onOpenNetwork: (String, Boolean) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onOpenConnect: () -> Unit,
    onEditProfile: () -> Unit,
    cdkScore: Int?,
    cdkScoreLoading: Boolean,
    cdkScoreError: String?,
    cdkScoreNeedsAuthorization: Boolean,
    onLoadCdkScore: () -> Unit,
    onReloadCdkScore: () -> Unit,
    onOpenCdkWebsite: () -> Unit,
    scrollToTopRequest: Int = 0,
    onBack: (() -> Unit)? = null,
) {
    if (username.isNullOrBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onAddAccount) { Text("登录后查看个人主页") }
        }
        return
    }
    key(username.lowercase(), accountState.activeUserId) {
        var showCommunityScore by remember { mutableStateOf(false) }
        val account = accountState.accounts.firstOrNull { it.userId == accountState.activeUserId }
        LaunchedEffect(active, username) {
            if (active) onLoadCdkScore()
        }
        UserProfileScreen(
            username = username,
            initialAvatarUrl = avatarUrlOf(account?.avatarTemplate, account?.animatedAvatar, 160, username),
            initialStaticAvatarUrl = avatarUrlOf(account?.avatarTemplate, size = 160),
            onOpenTopic = onOpenTopic,
            onOpenUser = onOpenUser,
            onOpenBadges = { onOpenBadges(username) },
            onOpenNetwork = { onOpenNetwork(username, it) }, onOpenWebsite = onOpenWebsite,
            onBack = onBack,
            onEditProfile = onEditProfile,
            communityScore = cdkScore,
            communityScoreLoading = cdkScoreLoading,
            communityScoreNeedsAuthorization = cdkScoreNeedsAuthorization,
            onOpenCommunityScore = { showCommunityScore = true; onReloadCdkScore() },
            onOpenLevel = onOpenConnect,
            scrollToTopRequest = scrollToTopRequest,
        )
        if (showCommunityScore) {
            CommunityScoreDialog(cdkScore, onReloadCdkScore, cdkScoreLoading, cdkScoreError,
                onOpenWebsite = { showCommunityScore = false; onOpenCdkWebsite() },
            ) { showCommunityScore = false }
        }
    }
}
