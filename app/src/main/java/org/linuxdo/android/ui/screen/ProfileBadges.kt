package org.linuxdo.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linuxdo.android.data.UserBadgeItem
import org.linuxdo.android.ui.DiscourseBadgeIcon
import org.linuxdo.android.ui.UserBadgesSectionCard
import org.linuxdo.android.ui.design.IosTheme

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileBadgesSheet(
    badges: List<UserBadgeItem>?, loading: Boolean, error: String?, onRefresh: () -> Unit,
    onBadgeClick: (UserBadgeItem) -> Unit, onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IosTheme.colors.groupedBackground) {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (error != null) {
                Text(error, color = IosTheme.colors.secondaryLabel)
                TextButton(onClick = onRefresh, enabled = !loading) { Text("重新加载") }
            }
            UserBadgesSectionCard(badges, loading, onRefresh.takeUnless { loading }, onBadgeClick)
        }
    }
}
