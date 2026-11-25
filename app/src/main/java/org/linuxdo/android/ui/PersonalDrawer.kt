package org.linuxdo.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.linuxdo.android.App
import org.linuxdo.android.Config
import org.linuxdo.android.data.AccountSession
import org.linuxdo.android.data.Category
import org.linuxdo.android.data.UserCardData
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.data.orderedCategoryGroups
import org.linuxdo.android.ui.design.*
import org.linuxdo.android.ui.screen.LevelBadge
import org.linuxdo.android.ui.screen.LINUXDO_APPS_PLAZA_URL
import org.linuxdo.android.ui.screen.ProfileWebApp

internal sealed interface PersonalDrawerAction {
    data object Profile : PersonalDrawerAction
    data object Bookmarks : PersonalDrawerAction
    data object History : PersonalDrawerAction
    data object Drafts : PersonalDrawerAction
    data object Following : PersonalDrawerAction
    data object Badges : PersonalDrawerAction
    data object Level : PersonalDrawerAction
    data object Categories : PersonalDrawerAction
    data object Tags : PersonalDrawerAction
    data object Blacklist : PersonalDrawerAction
    data object Settings : PersonalDrawerAction
    data object Share : PersonalDrawerAction
    data class CategoryTopics(val category: Category) : PersonalDrawerAction
    data class TagTopics(val tag: String) : PersonalDrawerAction
    data class Web(val url: String) : PersonalDrawerAction
}

@Composable
internal fun PersonalDrawerHost(
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val drawerWidth = minOf(maxWidth * .86f, 360.dp)
        ModalNavigationDrawer(drawerState = drawerState, gesturesEnabled = gesturesEnabled,
            scrimColor = Color.Black.copy(alpha = .37f), drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(drawerWidth).fillMaxHeight(),
                    drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
                    drawerContainerColor = IosTheme.colors.groupedBackground,
                    drawerTonalElevation = 0.dp, windowInsets = WindowInsets(0)) { drawerContent() }
            }, content = content)
    }
}

/** 独立于用户资料弹窗加载，避免打开侧栏触发 UserCardBottomSheet。 */
@Composable
internal fun rememberDrawerProfile(username: String?, accountId: Long, open: Boolean): UserCardData? {
    val context = LocalContext.current
    val repository = (context.applicationContext as? App)?.container?.topicRepository
    var card by remember(username, accountId) {
        mutableStateOf(username?.let { repository?.getCachedUserCard(it) })
    }
    LaunchedEffect(username, accountId, open) {
        if (!open || username.isNullOrBlank() || repository == null) return@LaunchedEffect
        try {
            val loaded = repository.fetchUserCard(username)
            currentCoroutineContext().ensureActive()
            if (loaded.username.equals(username, ignoreCase = true)) card = loaded
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 账号基础资料和缓存仍可用；详细页沿用既有错误/重试入口。
        }
    }
    return card?.takeIf { it.username.equals(username, ignoreCase = true) }
}

@Composable
internal fun PersonalDrawerAvatar(
    username: String?, account: AccountSession?, onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)
        .semantics { contentDescription = "打开个人侧栏" }) {
        if (username.isNullOrBlank()) {
            Icon(Icons.Default.AccountCircle, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(30.dp))
        } else {
            UserAvatar(avatarUrlOf(account?.avatarTemplate, account?.animatedAvatar, 64, username),
                avatarUrlOf(account?.avatarTemplate, size = 64), username, Modifier.size(30.dp))
        }
    }
}

@Composable
internal fun PersonalDrawerContent(
    username: String?,
    account: AccountSession?,
    card: UserCardData?,
    communityScore: Int?,
    categories: CategoriesUiState,
    tags: TagsUiState,
    onLoadCategories: () -> Unit,
    onLoadTags: () -> Unit,
    onAction: (PersonalDrawerAction) -> Unit,
    onAccounts: () -> Unit,
    onCommunityScore: () -> Unit,
    onToggleTheme: () -> Unit,
    onLogin: () -> Unit,
) {
    val colors = IosTheme.colors
    val loggedIn = !username.isNullOrBlank()
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().background(colors.groupedBackground)
        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))) {
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = if (loggedIn) "进入个人主页" else "登录",
                            onClick = { if (loggedIn) onAction(PersonalDrawerAction.Profile) else onLogin() })) {
                        if (loggedIn) UserAvatar(
                            avatarUrlOf(card?.avatarTemplate ?: account?.avatarTemplate,
                                card?.animatedAvatar ?: account?.animatedAvatar, 160, username),
                            avatarUrlOf(card?.avatarTemplate ?: account?.avatarTemplate, size = 160),
                            username, Modifier.fillMaxSize(),
                        ) else Icon(Icons.Default.AccountCircle, null, tint = colors.secondaryLabel, modifier = Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(card?.cleanDisplayName ?: account?.name?.takeIf { it.isNotBlank() } ?: username ?: "尚未登录",
                                style = IosTheme.type.largeTitle, color = colors.label,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, false).clickable(role = Role.Button) {
                                    if (loggedIn) onAction(PersonalDrawerAction.Profile) else onLogin()
                                })
                            if (loggedIn) (card?.trustLevel ?: account?.trustLevel)?.let { level ->
                                Box(Modifier.heightIn(min = 32.dp).clickable(role = Role.Button, onClickLabel = "等级与积分") {
                                    onAction(PersonalDrawerAction.Level)
                                }, contentAlignment = Alignment.Center) { LevelBadge(level) }
                            }
                        }
                        Text(if (loggedIn) "@$username" else "登录后使用个人功能", style = IosTheme.type.caption,
                            color = colors.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onAccounts, modifier = Modifier.size(44.dp).align(Alignment.Top)
                        .semantics { contentDescription = "切换账号" }) { AccountSwitchIcon(colors.accent) }
                }
                if (loggedIn) {
                    Spacer(Modifier.height(12.dp))
                    UserBioText(card?.bioExcerpt, "还没有填写个人简介", IosTheme.type.footnote, colors.label,
                        emptyColor = colors.secondaryLabel, maxLines = 2)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        DrawerCompactAction("社区分 ${communityScore ?: "—"}", Icons.Default.Star,
                            Modifier.weight(1f), onCommunityScore)
                        Box(Modifier.size(0.5.dp, 14.dp).background(colors.separator))
                        DrawerCompactAction("我的徽章", Icons.Default.Star, Modifier.weight(1f)) { onAction(PersonalDrawerAction.Badges) }
                    }
                } else {
                    TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("登录或添加账号", color = colors.accent)
                    }
                }
            }

            if (loggedIn) {
                DrawerGroupCard(Modifier.padding(top = 4.dp)) {
                    Row {
                        DrawerQuickAction("我的收藏", Icons.Default.Star, Modifier.weight(1f)) { onAction(PersonalDrawerAction.Bookmarks) }
                        DrawerQuickAction("浏览历史", Icons.Default.DateRange, Modifier.weight(1f)) { onAction(PersonalDrawerAction.History) }
                    }
                    IosDivider(startIndent = 12.dp)
                    Row {
                        DrawerQuickAction("草稿箱", Icons.Default.Edit, Modifier.weight(1f)) { onAction(PersonalDrawerAction.Drafts) }
                        DrawerQuickAction("我的关注", Icons.Default.Person, Modifier.weight(1f)) { onAction(PersonalDrawerAction.Following) }
                    }
                }
            }

            DrawerGroupCard(Modifier.padding(top = 12.dp)) {
                Text("浏览社区", color = colors.secondaryLabel, style = IosTheme.type.caption,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
                DrawerDisclosure("社区分类", Icons.Default.List, onExpand = onLoadCategories) {
                    val groups = remember(categories.items, categories.order) { orderedCategoryGroups(categories.items, categories.order) }
                    groups.take(3).forEach { (category, _) ->
                        DrawerSubRow(category.name, leading = {
                            val color = remember(category.color) {
                                runCatching { Color(android.graphics.Color.parseColor("#${category.color?.removePrefix("#")}")) }.getOrNull()
                            } ?: colors.accent
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(color))
                        }) { onAction(PersonalDrawerAction.CategoryTopics(category)) }
                    }
                    DrawerLoadState(categories.loading, categories.error, groups.isEmpty())
                    DrawerSubRow("全部分类", Icons.Default.List) { onAction(PersonalDrawerAction.Categories) }
                }
                IosDivider(startIndent = 45.dp)
                DrawerDisclosure("标签", Icons.Default.List, onExpand = onLoadTags) {
                    tags.tags.take(3).forEach { tag ->
                        DrawerSubRow(tag.displayName) { onAction(PersonalDrawerAction.TagTopics(tag.displayName)) }
                    }
                    DrawerLoadState(tags.loading, tags.error, tags.tags.isEmpty())
                    DrawerSubRow("全部标签", Icons.Default.List) { onAction(PersonalDrawerAction.Tags) }
                }
                IosDivider(startIndent = 45.dp)
                DrawerDisclosure("社区探索", Icons.Default.Search) {
                    DrawerWebRow("看板", Icons.Default.List, "${Config.BASE_URL}/boards", onAction)
                    DrawerWebRow("AI 机器人", Icons.Default.Face, "${Config.BASE_URL}/discourse-ai/ai-bot/conversations", onAction)
                    DrawerWebRow("近期活动", Icons.Default.DateRange, "${Config.BASE_URL}/upcoming-events", onAction)
                    DrawerWebRow("社区徽章", Icons.Default.Star, "${Config.BASE_URL}/badges", onAction)
                    DrawerWebRow("排行榜", Icons.Default.ThumbUp, "${Config.BASE_URL}/leaderboard", onAction)
                    DrawerDisclosure("更多社区入口", Icons.Default.MoreVert) {
                        DrawerWebRow("关于社区", Icons.Default.Info, "${Config.BASE_URL}/about", onAction)
                        DrawerWebRow("群组", Icons.Default.Person, "${Config.BASE_URL}/g", onAction)
                        DrawerWebRow("周年纪念日", Icons.Default.DateRange, "${Config.BASE_URL}/cakeday/anniversaries/today", onAction)
                        DrawerWebRow("生日", Icons.Default.DateRange, "${Config.BASE_URL}/cakeday/birthdays/today", onAction)
                    }
                }
            }

            DrawerGroupCard(Modifier.padding(top = 12.dp)) {
                DrawerDisclosure("应用与服务", Icons.Default.ShoppingCart) {
                    DrawerWebRow("应用广场", Icons.Default.List, LINUXDO_APPS_PLAZA_URL, onAction)
                    DrawerWebRow("LDCstore", Icons.Default.ShoppingCart, ProfileWebApp.Store.url, onAction)
                    DrawerWebRow("专属邮箱", Icons.Default.Email, ProfileWebApp.Mail.url, onAction)
                    if (loggedIn) DrawerSubRow("等级与积分", Icons.Default.Star) { onAction(PersonalDrawerAction.Level) }
                    DrawerWebRow("Channel", Icons.Default.Send, "https://t.me/linux_do_channel", onAction)
                    DrawerWebRow("IDC Flare", Icons.Default.Build, "https://idcflare.com", onAction)
                    DrawerWebRow("更多资源", Icons.Default.MoreVert, "https://go.linux.do/pub/resources", onAction)
                }
                if (loggedIn) {
                    IosDivider(startIndent = 45.dp)
                    DrawerDisclosure("账号工具", Icons.Default.Person) {
                        DrawerWebRow("编辑个人资料", Icons.Default.Edit, "${Config.BASE_URL}/my/preferences/profile", onAction)
                        DrawerSubRow("分享个人主页", Icons.Default.Share) { onAction(PersonalDrawerAction.Share) }
                        DrawerSubRow("本地黑名单", Icons.Default.Lock) { onAction(PersonalDrawerAction.Blacklist) }
                        DrawerSubRow("管理账号", Icons.Default.AccountCircle, onClick = onAccounts)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        IosFullDivider()
        Row(Modifier.fillMaxWidth().background(colors.card).padding(start = 32.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 44.dp).clickable(role = Role.Button) { onAction(PersonalDrawerAction.Settings) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Icon(Icons.Default.Settings, null, tint = colors.secondaryLabel, modifier = Modifier.size(18.dp))
                Text("设置", style = IosTheme.type.subheadline, color = colors.label)
            }
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(44.dp).semantics {
                contentDescription = if (colors.isDark) "切换为浅色" else "切换为深色"
            }) { AppearanceIcon(colors.isDark, colors.secondaryLabel) }
        }
    }
}

@Composable
private fun DrawerGroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // 复用 App 的分组容器、边距、按压反馈与主题色。
    IosGroupCard(modifier = modifier, content = content)
}

@Composable
private fun DrawerQuickAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.heightIn(min = IosMetrics.rowMinHeight).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(18.dp))
        Text(label, style = IosTheme.type.subheadline, color = IosTheme.colors.label,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DrawerCompactAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(16.dp))
        Text(label, style = IosTheme.type.caption, color = IosTheme.colors.secondaryLabel)
    }
}

@Composable
private fun DrawerDisclosure(label: String, icon: ImageVector, onExpand: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(expanded) { if (expanded) onExpand() }
    IosRow(onClick = { expanded = !expanded }, modifier = Modifier.semantics {
        stateDescription = if (expanded) "已展开" else "已收起"
    }) {
        Icon(icon, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(11.dp))
        Text(label, style = IosTheme.type.subheadline, color = IosTheme.colors.label, modifier = Modifier.weight(1f))
        IosChevron(Modifier.rotate(if (expanded) 90f else 0f))
    }
    AnimatedVisibility(expanded) { Column(Modifier.padding(start = 28.dp, end = 8.dp, bottom = 8.dp), content = content) }
}

@Composable
private fun DrawerSubRow(label: String, icon: ImageVector? = null, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(6.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (leading != null) Box(Modifier.size(17.dp), contentAlignment = Alignment.Center) { leading() }
        else if (icon != null) Icon(icon, null, tint = IosTheme.colors.secondaryLabel, modifier = Modifier.size(17.dp))
        Text(label, style = IosTheme.type.subheadline.copy(fontSize = 14.sp), color = IosTheme.colors.label,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DrawerWebRow(label: String, icon: ImageVector, url: String, onAction: (PersonalDrawerAction) -> Unit) {
    DrawerSubRow(label, icon, onClick = { onAction(PersonalDrawerAction.Web(url)) })
}

@Composable
private fun DrawerLoadState(loading: Boolean, error: String?, empty: Boolean) {
    if (!empty) return
    if (loading) Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) { IosActivityIndicator(diameter = 18.dp) }
    else Text(if (error == null) "暂无内容" else "暂时无法加载，请打开完整列表重试",
        style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel, modifier = Modifier.padding(8.dp))
}

@Composable
internal fun CommunityScoreDialog(score: Int?, onReload: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("社区分") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(score?.toString() ?: "暂未获取", style = IosTheme.type.largeTitle, color = IosTheme.colors.accent)
            Text("社区分根据获得的徽章结算。")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        dismissButton = if (score == null) ({ TextButton(onClick = onReload) { Text("重新获取") } }) else null,
        containerColor = IosTheme.colors.card)
}

@Composable
private fun AccountSwitchIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = 1.7.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color,
            Offset(size.width * x1, size.height * y1), Offset(size.width * x2, size.height * y2), stroke, StrokeCap.Round)
        line(.18f, .32f, .82f, .32f); line(.18f, .32f, .36f, .14f); line(.18f, .32f, .36f, .50f)
        line(.18f, .68f, .82f, .68f); line(.82f, .68f, .64f, .50f); line(.82f, .68f, .64f, .86f)
    }
}

@Composable
private fun AppearanceIcon(dark: Boolean, color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val radius = size.minDimension * .23f
        drawCircle(color, radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1.6.dp.toPx()))
        repeat(8) { index ->
            val angle = index * Math.PI / 4
            val start = if (dark) .36f else .35f
            drawLine(color, center + Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat()) * (size.minDimension * start),
                center + Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat()) * (size.minDimension * .47f),
                1.6.dp.toPx(), StrokeCap.Round)
        }
    }
}
