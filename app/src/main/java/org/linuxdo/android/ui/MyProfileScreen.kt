package org.linuxdo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.linuxdo.android.data.UserBadgeItem
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
    userBadges: List<UserBadgeItem>?,
    isUserBadgesLoading: Boolean,
    onRefreshUserBadges: () -> Unit,
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
        var selectedBadge by remember { mutableStateOf<UserBadgeItem?>(null) }
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
            scrollToTopRequest = scrollToTopRequest,
            menuActions = listOf(
                ProfileMenuAction("我的收藏", Icons.Default.Star, onBookmarks),
                ProfileMenuAction("浏览历史", Icons.Default.DateRange, onOpenHistory),
                ProfileMenuAction("我的关注", Icons.Default.Person, onOpenFollowing),
                ProfileMenuAction("草稿箱", Icons.Default.Edit, onOpenDrafts),
                ProfileMenuAction("等级与积分", Icons.Default.Info, onOpenConnect),
                ProfileMenuAction("应用广场", Icons.Default.List, onOpenAppsPlaza),
                ProfileMenuAction("LDCstore", Icons.Default.ShoppingCart, onOpenLdcStore),
                ProfileMenuAction("专属邮箱", Icons.Default.MailOutline, onOpenLdcMail),
                ProfileMenuAction("本地黑名单", Icons.Default.Lock, onOpenBlacklist),
                ProfileMenuAction("设置", Icons.Default.Settings, onSettings),
            ),
            aboutContent = {
                LaunchedEffect(username) { onRefreshUserBadges() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("社区分 ${cdkScore ?: "—"}", color = IosTheme.colors.label)
                    TextButton(onClick = onOpenConnect) { Text("等级与积分") }
                }
                UserBadgesSectionCard(userBadges, isUserBadgesLoading, onRefreshUserBadges,
                    onBadgeClick = { selectedBadge = it })
            },
        )
        if (showAccounts) AccountSwitchBottomSheet(accountState,
            onDismiss = { showAccounts = false },
            onSwitch = { showAccounts = false; onSwitchAccount(it) },
            onRemoveAccount = onRemoveAccount,
            onAddAccount = { showAccounts = false; onAddAccount() })
        selectedBadge?.let { BadgeDetailDialog(it) { selectedBadge = null } }
    }
}
