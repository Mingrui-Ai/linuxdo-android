package org.linuxdo.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linuxdo.android.App
import org.linuxdo.android.data.UserBadgeItem
import org.linuxdo.android.ui.BadgeDetailDialog
import org.linuxdo.android.ui.DiscourseBadgeIcon
import org.linuxdo.android.ui.UserBadgesSectionCard
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.IosLargeTitleScaffold

@Composable
internal fun ProfileBadgePreview(badges: List<UserBadgeItem>?, loading: Boolean, onClick: () -> Unit) {
    val earned = remember(badges) { badges?.filter { it.isEarned } }
    val preview = earned.orEmpty().take(3)
    Row(Modifier.heightIn(min = 32.dp).clickable(onClickLabel = "查看徽章", onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (preview.isNotEmpty()) {
            Box(Modifier.width((22 + (preview.size - 1) * 14).dp).height(22.dp)) {
                preview.forEachIndexed { index, badge ->
                    val color = when (badge.badgeTypeId) {
                        1 -> Color(0xFFE6B84B)
                        2 -> Color(0xFFB0BEC5)
                        else -> Color(0xFFD57C46)
                    }
                    Box(Modifier.offset(x = (index * 14).dp).size(22.dp).clip(CircleShape)
                        .background(color).border(1.dp, IosTheme.colors.separator, CircleShape),
                        contentAlignment = Alignment.Center) {
                        DiscourseBadgeIcon(badge.icon, badge.imageUrl, true, badge.badgeTypeId, Modifier.size(14.dp))
                    }
                }
            }
        } else {
            Icon(Icons.Default.Star, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(18.dp))
        }
        Text(when {
            earned != null -> "${earned.size} 枚徽章"
            loading -> "徽章加载中"
            else -> "徽章"
        }, color = IosTheme.colors.secondaryLabel, fontSize = 12.sp)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(13.dp))
    }
}

@Composable
fun ProfileBadgesScreen(
    username: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as? App)?.container?.topicRepository
    var badges by remember(username) { mutableStateOf(repo?.getCachedUserBadges(username)) }
    var loading by remember(username) { mutableStateOf(true) }
    var error by remember(username) { mutableStateOf<String?>(null) }
    var refreshGeneration by remember(username) { mutableIntStateOf(0) }
    var selectedBadge by remember(username) { mutableStateOf<UserBadgeItem?>(null) }

    LaunchedEffect(username, refreshGeneration) {
        loading = true
        error = null
        try {
            checkNotNull(repo) { "暂时无法加载徽章" }
            badges = repo.fetchUserBadges(username, forceRefresh = refreshGeneration > 0)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "徽章加载失败"
        } finally {
            loading = false
        }
    }

    IosLargeTitleScaffold(
        title = "徽章展示",
        showLargeTitle = false,
        onBack = onBack,
        trailing = {
            TextButton(onClick = { refreshGeneration++ }, enabled = !loading) {
                Text(if (loading) "加载中" else "刷新")
            }
        },
    ) {
        item("user") {
            Text("@$username", color = IosTheme.colors.secondaryLabel,
                style = IosTheme.type.footnote, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
        if (error != null) {
            item("error") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(error.orEmpty(), color = IosTheme.colors.secondaryLabel)
                    TextButton(onClick = { refreshGeneration++ }, enabled = !loading) { Text("重新加载") }
                }
            }
        }
        item("badges") {
            UserBadgesSectionCard(
                badges = badges,
                isLoading = loading,
                onRefresh = { refreshGeneration++; Unit }.takeUnless { loading },
                onBadgeClick = { selectedBadge = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
    selectedBadge?.let { BadgeDetailDialog(it) { selectedBadge = null } }
}
