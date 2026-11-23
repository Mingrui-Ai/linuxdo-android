package org.linuxdo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.screen.*

/** “我的”只装配账号工具和权限，内容直接复用所有用户的详细主页。 */
@Composable
fun ProfileScreen(
    username: String?,
    active: Boolean,
    accountState: AccountUiState,
    onRefreshAccounts: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onRemoveAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: (String) -> Unit,
    onBookmarks: () -> Unit,
    onOpenDrafts: () -> Unit,
    onSettings: () -> Unit,
    onOpenConnect: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenAppsPlaza: () -> Unit,
    onOpenBlacklist: () -> Unit,
    onOpenLdcStore: () -> Unit,
    onOpenLdcMail: () -> Unit,
    onEditProfile: () -> Unit,
    cdkScore: Int?,
    onLoadCdkScore: () -> Unit,
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
        var showAccounts by remember { mutableStateOf(false) }
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
            onBack = onBack,
            onSwitchAccount = { onRefreshAccounts(); showAccounts = true },
            onEditProfile = onEditProfile,
            communityScore = cdkScore,
            onOpenCommunityScore = { showCommunityScore = true; onLoadCdkScore() },
            onOpenLevel = onOpenConnect,
            scrollToTopRequest = scrollToTopRequest,
            menuActions = listOf(
                ProfileMenuAction("我的收藏", Icons.Default.Star, onBookmarks),
                ProfileMenuAction("浏览历史", Icons.Default.DateRange, onOpenHistory),
                ProfileMenuAction("我的关注", Icons.Default.Person, onOpenFollowing),
                ProfileMenuAction("草稿箱", Icons.Default.Edit, onOpenDrafts),
                ProfileMenuAction("应用广场", Icons.Default.List, onOpenAppsPlaza),
                ProfileMenuAction("LDCstore", Icons.Default.ShoppingCart, onOpenLdcStore),
                ProfileMenuAction("专属邮箱", Icons.Default.MailOutline, onOpenLdcMail),
                ProfileMenuAction("本地黑名单", Icons.Default.Lock, onOpenBlacklist),
                ProfileMenuAction("设置", Icons.Default.Settings, onSettings),
            ),
        )
        if (showAccounts) AccountSwitchBottomSheet(accountState,
            onDismiss = { showAccounts = false },
            onSwitch = { showAccounts = false; onSwitchAccount(it) },
            onRemoveAccount = onRemoveAccount,
            onAddAccount = { showAccounts = false; onAddAccount() })
        if (showCommunityScore) {
            AlertDialog(
                onDismissRequest = { showCommunityScore = false },
                title = { Text("社区分") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(cdkScore?.toString() ?: "暂未获取", style = IosTheme.type.largeTitle,
                            color = IosTheme.colors.accent)
                        Text("社区分根据获得的徽章结算。")
                    }
                },
                confirmButton = { TextButton(onClick = { showCommunityScore = false }) { Text("完成") } },
                dismissButton = if (cdkScore == null) ({
                    TextButton(onClick = onLoadCdkScore) { Text("重新获取") }
                }) else null,
                containerColor = IosTheme.colors.card,
            )
        }
    }
}
