package org.linuxdo.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import org.linuxdo.android.App
import org.linuxdo.android.data.UserBadgeItem
import org.linuxdo.android.ui.BadgeDetailDialog
import org.linuxdo.android.ui.DiscourseBadgeIcon
import org.linuxdo.android.ui.design.HeaderCountPill
import org.linuxdo.android.ui.design.HeaderFilterPill
import org.linuxdo.android.ui.design.HeaderFilterPillRow
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosNavBar
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.boundedTabSwipe

private enum class BadgeFilter(val label: String) {
    All("全部"), Earned("已解锁"), Unearned("待解锁")
}

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
    var userRefreshing by remember(username) { mutableStateOf(false) }
    var selectedBadge by remember(username) { mutableStateOf<UserBadgeItem?>(null) }
    var filter by rememberSaveable(username) { mutableStateOf(BadgeFilter.All) }

    val totalCount = badges?.size ?: 0
    val earnedCount = remember(badges) { badges.orEmpty().count { it.isEarned } }
    val unearnedCount = remember(badges, earnedCount) { (badges?.size ?: 0) - earnedCount }
    val filteredBadges = remember(badges, filter) {
        badges.orEmpty().filter {
            when (filter) {
                BadgeFilter.All -> true
                BadgeFilter.Earned -> it.isEarned
                BadgeFilter.Unearned -> !it.isEarned
            }
        }
    }

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
            userRefreshing = false
        }
    }

    val colors = IosTheme.colors
    val pullState = rememberPullToRefreshState()

    Column(Modifier.fillMaxSize().background(colors.groupedBackground)) {
        // 通透单行导航栏：与首页 / 搜索 / 帖子详情统一 groupedBackground 底色，右侧直接呈现解锁进度
        IosNavBar(
            title = "徽章",
            onBack = onBack,
            backLabel = "",
            background = colors.groupedBackground,
            showDivider = false,
            trailingWidth = 76.dp,
            trailing = {
                if (badges != null) {
                    HeaderCountPill(
                        text = "$earnedCount/$totalCount",
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            },
        )

        // 与首页筛选栏一致的轻量胶囊筛选行（含各项计数），紧凑单行不割裂
        HeaderFilterPillRow {
            BadgeFilter.entries.forEach { option ->
                val count = if (badges == null) null else when (option) {
                    BadgeFilter.All -> totalCount
                    BadgeFilter.Earned -> earnedCount
                    BadgeFilter.Unearned -> unearnedCount
                }
                HeaderFilterPill(
                    label = option.label,
                    count = count,
                    selected = filter == option,
                    onClick = { filter = option },
                )
            }
        }

        if (error != null && !badges.isNullOrEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    error.orEmpty(),
                    color = colors.secondaryLabel,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { refreshGeneration++ }, enabled = !loading) {
                    Text("重试", color = colors.accent)
                }
                org.linuxdo.android.ui.design.NetworkSettingsLink()
            }
        }

        PullToRefreshBox(
            isRefreshing = userRefreshing && loading,
            onRefresh = {
                if (!loading) {
                    userRefreshing = true
                    refreshGeneration++
                }
            },
            state = pullState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .boundedTabSwipe(
                    selected = filter.ordinal,
                    count = BadgeFilter.entries.size,
                    enabled = selectedBadge == null,
                    onSelect = { filter = BadgeFilter.entries[it] },
                ),
            indicator = {
                val fraction = pullState.distanceFraction
                if ((userRefreshing && loading) || fraction > 0f) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .graphicsLayer {
                                alpha = if (userRefreshing && loading) 1f else min(1f, fraction * 1.4f)
                            },
                    ) {
                        IosActivityIndicator(diameter = 22.dp)
                    }
                }
            },
        ) {
            when {
                loading && badges == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    IosActivityIndicator(diameter = 24.dp)
                }
                error != null && badges.isNullOrEmpty() -> Box(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error.orEmpty(), color = colors.secondaryLabel, textAlign = TextAlign.Center)
                        org.linuxdo.android.ui.design.NetworkSettingsLink()
                        TextButton(onClick = { refreshGeneration++ }, enabled = !loading) {
                            Text("重新加载", color = colors.accent)
                        }
                    }
                }
                filteredBadges.isEmpty() -> Box(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when (filter) {
                            BadgeFilter.All -> "暂无徽章"
                            BadgeFilter.Earned -> "暂无已解锁的徽章"
                            BadgeFilter.Unearned -> "全部徽章已解锁"
                        },
                        color = colors.secondaryLabel,
                        style = IosTheme.type.footnote,
                    )
                }
                else -> key(username, filter) {
                    val gridState = rememberLazyGridState()
                    val minCellWidth = maxOf(100.dp, with(LocalDensity.current) { 100.sp.toDp() })
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minCellWidth),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredBadges, key = { it.badgeId }) { badge ->
                            ProfileBadgeTile(badge) { selectedBadge = badge }
                        }
                    }
                }
            }
        }
    }
    selectedBadge?.let { BadgeDetailDialog(it) { selectedBadge = null } }
}

@Composable
private fun ProfileBadgeTile(badge: UserBadgeItem, onClick: () -> Unit) {
    val colors = IosTheme.colors
    val isDark = colors.isDark
    val medalColors = when (badge.badgeTypeId) {
        1 -> listOf(Color(0xFFFFD66E), Color(0xFFC88B24))
        2 -> listOf(Color(0xFFE3EDF2), Color(0xFF97AAB5))
        else -> listOf(Color(0xFFFFB088), Color(0xFFCA7543))
    }
    val unearnedDiskColors = if (isDark) {
        listOf(Color(0xFF34373D), Color(0xFF23262B))
    } else {
        listOf(Color(0xFFE9ECF2), Color(0xFFD8DDE6))
    }
    val unearnedBorderColor = if (isDark) Color(0xFF4C5058) else Color(0xFFC9CFD9)
    val status = if (!badge.isEarned) "待解锁" else if (badge.count > 1) "已获得 ${badge.count} 次" else "已解锁"
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(
                0.5.dp,
                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClickLabel = "查看徽章详情", onClick = onClick)
            .semantics { stateDescription = status }
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            val diskColors = if (badge.isEarned) medalColors else unearnedDiskColors
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(diskColors))
                    .border(1.dp, if (badge.isEarned) medalColors.first() else unearnedBorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                DiscourseBadgeIcon(badge.icon, badge.imageUrl, badge.isEarned, badge.badgeTypeId, Modifier.size(28.dp))
            }
            if (!badge.isEarned) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(0.6.dp, colors.separator, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Lock, null, tint = colors.secondaryLabel, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            badge.name,
            color = if (badge.isEarned) colors.label else colors.secondaryLabel,
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            status,
            color = if (badge.isEarned) colors.secondaryLabel else colors.tertiaryLabel,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
