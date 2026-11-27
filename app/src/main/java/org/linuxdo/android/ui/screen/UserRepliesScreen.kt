package org.linuxdo.android.ui.screen

import org.linuxdo.android.ui.design.ForumCategoryIcon
import org.linuxdo.android.data.ProfileDisplayField.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.linuxdo.android.ui.format.RelativeTime
import org.linuxdo.android.data.UserRepliesUiState
import org.linuxdo.android.data.UserReplyItem
import org.linuxdo.android.data.AvatarCache
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.data.isAnimatedAvatarPath
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.style.TextAlign
import org.linuxdo.android.data.UserSummaryData
import org.linuxdo.android.data.SummaryTopicItem
import org.linuxdo.android.data.SummaryReplyItem
import org.linuxdo.android.data.SummaryUserItem
import org.linuxdo.android.data.SummaryCategoryItem
import org.linuxdo.android.data.SummaryBadgeItem
import org.linuxdo.android.data.SummaryLinkItem
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import kotlinx.coroutines.launch
import org.linuxdo.android.App
import org.linuxdo.android.data.UserCardData
import org.linuxdo.android.data.avatarUrlOf
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosLoadingBox
import org.linuxdo.android.ui.design.scrollListBars
import org.linuxdo.android.ui.design.IosLargeTitleScaffold
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.UserBioText
import org.linuxdo.android.ui.design.CollapsingHeaderLayout
import org.linuxdo.android.ui.design.rememberCollapsingHeaderState

@Composable
fun UserRepliesScreen(
    title: String,
    state: UserRepliesUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: ((String) -> Unit)? = null,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= listState.layoutInfo.totalItemsCount - 4
        }
    }

    LaunchedEffect(nearEnd, state.hasMore, state.loading) {
        if (nearEnd && state.hasMore && !state.loading && state.items.isNotEmpty()) {
            onLoadMore()
        }
    }

    IosLargeTitleScaffold(
        title = title,
        showLargeTitle = false,
        listState = listState,
        onBack = onBack,
        backLabel = "返回",
    ) {
        when {
            state.initialLoading && state.items.isEmpty() -> item("loading") {
                IosLoadingBox()
            }
            state.error != null && state.items.isEmpty() -> item("error") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = state.error,
                        style = IosTheme.type.footnote,
                        color = IosTheme.colors.secondaryLabel,
                    )
                    org.linuxdo.android.ui.design.NetworkSettingsLink()
                    Text(
                        text = "点击重试",
                        style = IosTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = IosTheme.colors.accent,
                        modifier = Modifier.clickable(onClick = onRefresh),
                    )
                }
            }
            state.items.isEmpty() -> item("empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无回复记录",
                        style = IosTheme.type.footnote,
                        color = IosTheme.colors.secondaryLabel,
                    )
                }
            }
            else -> {
                items(state.items, key = { it.id }) { item ->
                    UserReplyCard(
                        item = item,
                        onClick = { onOpenTopic(item.topicId, item.title, item.postNumber) },
                        onOpenUser = onOpenUser,
                    )
                }
                if (state.loading) {
                    item("loading-more") {
                        IosLoadingBox()
                    }
                } else if (!state.hasMore && state.items.isNotEmpty()) {
                    item("footer-no-more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "已加载全部回复",
                                style = IosTheme.type.caption,
                                color = IosTheme.colors.tertiaryLabel,
                            )
                        }
                    }
                item("bottom-spacer") { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}

@Composable
private fun UserReplyCard(
    item: UserReplyItem,
    onClick: () -> Unit,
    onOpenUser: ((String) -> Unit)? = null,
    fallbackAvatarUrl: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 用户名占满剩余宽度，分类/回应与时间共同贴齐右侧。
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.avatarUrl?.takeIf { it.isNotBlank() } ?: fallbackAvatarUrl,
                contentDescription = item.username,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(IosTheme.colors.fieldBackground)
                    .clickable(enabled = onOpenUser != null) { onOpenUser?.invoke(item.username) },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.username,
                style = IosTheme.type.footnote.copy(fontWeight = FontWeight.Medium),
                color = IosTheme.colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            item.categoryName?.let { catName ->
                Row(
                    modifier = Modifier.widthIn(max = 200.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.reactionName != null) {
                        AsyncImage(
                            model = item.reactionUrl,
                            contentDescription = "回应：${item.reactionName}",
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    item.categoryColor?.let { hex ->
                        val dotColor = runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(IosTheme.colors.accent)
                        ForumCategoryIcon(null, 12.dp, dotColor, catName, item.categoryId?.toInt())
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = catName,
                        style = IosTheme.type.caption,
                        color = IosTheme.colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            item.createdAt?.let { time ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = RelativeTime.format(time),
                    style = IosTheme.type.caption,
                    color = IosTheme.colors.tertiaryLabel,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 主题标题
        Text(
            text = item.title,
            style = IosTheme.type.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = IosTheme.colors.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // 我的评论内容（一行多了省略号，参照 PC 样式）
        val cleaned = cleanExcerpt(item.commentExcerpt)
        if (!cleaned.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = cleaned,
                style = IosTheme.type.footnote.copy(fontSize = 14.sp),
                color = IosTheme.colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item.postNumber?.takeIf { it > 1 }?.let { number ->
            Spacer(Modifier.height(8.dp))
            Text("#$number", style = IosTheme.type.caption,
                color = IosTheme.colors.tertiaryLabel, maxLines = 1,
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
    IosDivider(startIndent = 16.dp)
}

enum class UserProfileTab(val label: String, val filter: String?) {
    Summary("总结", null),
    Topics("话题", "4"),
    Replies("回复", "5"),
    Activity("动态", null),
    Likes("赞", "1"),
    Reactions("回应", null),
    Boost("Boost", null),
    Votes("投票", null),
    Solutions("已解决", null);
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    username: String,
    initialAvatarUrl: String? = null,
    initialStaticAvatarUrl: String? = null,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenBadges: () -> Unit,
    onOpenNetwork: (Boolean) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onOpenUser: ((String) -> Unit)? = null,
    onChat: ((String) -> Unit)? = null,
    onComposePrivateMessage: ((String) -> Unit)? = null,
    onSetFollowing: (suspend (UserCardData, Boolean) -> Unit)? = null,
    onNotificationSettingsObserved: ((org.linuxdo.android.data.UserNotificationSettings) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSwitchAccount: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
    onOpenDisplaySettings: () -> Unit,
    menuActions: List<ProfileMenuAction> = emptyList(),
    communityScore: Int? = null,
    communityScoreLoading: Boolean = false,
    communityScoreNeedsAuthorization: Boolean = false,
    onOpenCommunityScore: (() -> Unit)? = null,
    onOpenLevel: (() -> Unit)? = null,
    scrollToTopRequest: Int = 0,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = (context.applicationContext as? App)?.container?.topicRepository
    val session = (context.applicationContext as? App)?.container?.sessionStore
    val accountId = session?.currentUserId() ?: 0L
    val accountGeneration = session?.sessionGeneration ?: 0L
    val observedCallback = androidx.compose.runtime.rememberUpdatedState(onNotificationSettingsObserved)
    val notificationController = remember(username, accountId, accountGeneration, repo, onSetFollowing != null) {
        if (repo != null && accountId > 0 && onSetFollowing != null) org.linuxdo.android.ui.UserNotificationController(
            read = { repo.fetchUserNotificationSettings(username, accountId, accountGeneration) },
            write = { settings, level, expiry -> repo.setUserNotificationLevel(settings, level, expiry, accountId, accountGeneration) },
            onObserved = { observedCallback.value?.invoke(it) },
        ) else null
    }
    var userCard by remember(username) { mutableStateOf<UserCardData?>(repo?.getCachedUserCard(username)) }
    val profileEditRevision by org.linuxdo.android.ui.ProfileEditorUpdates.revision.collectAsState()
    LaunchedEffect(username, profileEditRevision) {
        if (profileEditRevision > 0) repo?.getCachedUserCard(username)?.let { userCard = it }
    }
    // 回应、Boost 和已解决接口不带操作者头像，复用主页资料，随异步资料更新一起补齐。
    val profileAvatarUrl = avatarUrlOf(userCard?.avatarTemplate, userCard?.animatedAvatar,
        size = 64, username = username) ?: initialAvatarUrl ?: initialStaticAvatarUrl
    var selectedProfileTab by androidx.compose.runtime.key("profile-tab-by-name") {
        rememberSaveable(username) { mutableStateOf(UserProfileTab.Summary) }
    }
    val selectedTab = selectedProfileTab.ordinal
    var refreshGeneration by remember(username) { mutableIntStateOf(0) }
    var profileCompleted by remember(username) { mutableIntStateOf(-1) }
    var badgesCompleted by remember(username) { mutableIntStateOf(-1) }
    var summaryCompleted by remember(username) { mutableIntStateOf(-1) }
    var contentCompleted by remember(username, selectedTab) { mutableIntStateOf(-1) }
    var finishedRefresh by remember(username) { mutableIntStateOf(0) }
    var followUpdating by remember(username) { mutableStateOf(false) }
    var profileError by remember(username) { mutableStateOf<String?>(null) }
    val tabEntries = remember { UserProfileTab.entries }

    var summaryData by remember(username) { mutableStateOf<UserSummaryData?>(null) }
    var summaryLoading by remember(username) { mutableStateOf(false) }
    var summaryError by remember(username) { mutableStateOf<String?>(null) }
    var badges by remember(username) { mutableStateOf(repo?.getCachedUserBadges(username)) }
    var badgesLoading by remember(username) { mutableStateOf(false) }

    LaunchedEffect(username, refreshGeneration) {
        val generation = refreshGeneration
        if (repo == null) { badgesCompleted = generation; return@LaunchedEffect }
        badgesLoading = true
        try {
            badges = repo.fetchUserBadges(username, forceRefresh = refreshGeneration > 0)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // 头部保留缓存预览；详细错误与重试在独立徽章页显示。
        } finally {
            badgesLoading = false
            badgesCompleted = generation
        }
    }

    var beforeBoostId by remember(username, selectedTab) { mutableStateOf<Long?>(null) }
    var beforeReactionId by remember(username, selectedTab) { mutableStateOf<Long?>(null) }
    var retryTrigger by remember(username, selectedTab) { mutableIntStateOf(0) }
    var loadMoreJob by remember(username, selectedTab) { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var items by remember(username, selectedTab) { mutableStateOf<List<UserReplyItem>>(emptyList()) }
    var loading by remember(username, selectedTab) { mutableStateOf(false) }
    var error by remember(username, selectedTab) { mutableStateOf<String?>(null) }
    var cursor by remember(username, selectedTab) { mutableIntStateOf(0) }
    var hasMore by remember(username, selectedTab) { mutableStateOf(true) }

    LaunchedEffect(username, refreshGeneration) {
        val generation = refreshGeneration
        profileError = null
        if (repo == null) { profileCompleted = generation; return@LaunchedEffect }
        try {
            val card = repo.fetchUserProfile(username)
            userCard = card
            if (card.animatedAvatar.isNullOrBlank() || !isAnimatedAvatarPath(card.animatedAvatar)) {
                userCard = repo.enrichUserCardAvatar(username, card)
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (e: Exception) {
            profileError = e.message ?: "资料加载失败"
        } finally {
            profileCompleted = generation
        }
    }

    LaunchedEffect(username, refreshGeneration) {
        val generation = refreshGeneration
        if (repo == null) { summaryCompleted = generation; return@LaunchedEffect }
        summaryLoading = true
        summaryError = null
        try {
            summaryData = repo.fetchUserSummary(username)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            summaryError = e.message ?: "加载总结失败"
        } finally {
            summaryLoading = false
            summaryCompleted = generation
        }
    }

    androidx.compose.runtime.DisposableEffect(username, selectedTab) {
        onDispose { loadMoreJob?.cancel() }
    }

    LaunchedEffect(username, selectedTab, retryTrigger, refreshGeneration) {
        val generation = refreshGeneration
        val currentTab = tabEntries[selectedTab]
        if (repo == null) { contentCompleted = generation; return@LaunchedEffect }
        loadMoreJob?.cancel()
        if (currentTab == UserProfileTab.Summary) return@LaunchedEffect
        loading = true
        error = null
        try {
            when (currentTab) {
                UserProfileTab.Boost -> {
                    val (newItems, nextBeforeId) = repo.fetchUserBoosts(username, null)
                    items = newItems
                    beforeBoostId = nextBeforeId
                    hasMore = nextBeforeId != null
                }
                UserProfileTab.Votes -> {
                    val (newItems, more) = repo.fetchUserVotedTopics(username, 0)
                    items = newItems
                    hasMore = more
                    cursor = 1
                }
                UserProfileTab.Solutions -> {
                    val (newItems, more) = repo.fetchUserSolvedPosts(username, 0)
                    items = newItems
                    hasMore = more
                    cursor = 1
                }
                UserProfileTab.Reactions -> {
                    val (newItems, nextBeforeId) = repo.fetchUserReactions(username, null)
                    items = newItems
                    beforeReactionId = nextBeforeId
                    hasMore = nextBeforeId != null
                }
                else -> {
                    val (newItems, more) = repo.fetchUserActionItems(username, currentTab.filter, 0)
                    items = newItems
                    hasMore = more
                    cursor = 1
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "加载数据失败"
        } finally {
            loading = false
            contentCompleted = generation
        }
    }

    fun loadMore() {
        val currentTab = tabEntries[selectedTab]
        if (loading || !hasMore || repo == null || currentTab == UserProfileTab.Summary) return
        loadMoreJob?.cancel()
        loading = true
        loadMoreJob = coroutineScope.launch {
            try {
                when (currentTab) {
                    UserProfileTab.Boost -> {
                        val (newItems, nextBeforeId) = repo.fetchUserBoosts(username, beforeBoostId)
                        items = (items + newItems).distinctBy { it.id }
                        beforeBoostId = nextBeforeId
                        hasMore = nextBeforeId != null
                    }
                    UserProfileTab.Votes -> {
                        val (newItems, more) = repo.fetchUserVotedTopics(username, cursor)
                        items = (items + newItems).distinctBy { it.topicId }
                        hasMore = more
                        cursor += 1
                    }
                    UserProfileTab.Solutions -> {
                        val (newItems, more) = repo.fetchUserSolvedPosts(username, cursor * 30)
                        items = (items + newItems).distinctBy { it.id }
                        hasMore = more
                        cursor += 1
                    }
                    UserProfileTab.Reactions -> {
                        val (newItems, nextBeforeId) = repo.fetchUserReactions(username, beforeReactionId)
                        items = (items + newItems).distinctBy { it.id }
                        beforeReactionId = nextBeforeId
                        hasMore = nextBeforeId != null
                    }
                    else -> {
                        val (newItems, more) = repo.fetchUserActionItems(username, currentTab.filter, cursor)
                        items = (items + newItems).distinctBy { it.id }
                        hasMore = more
                        cursor += 1
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "加载更多失败"
            } finally {
                loading = false
            }
        }
    }

    val listState = androidx.compose.runtime.key(username, selectedTab) { rememberLazyListState() }
    val aboutScroll = androidx.compose.runtime.key(username) { rememberScrollState() }
    val headerState = androidx.compose.runtime.key(username) { rememberCollapsingHeaderState() }
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) {
            headerState.expand()
            listState.scrollToItem(0)
            aboutScroll.scrollTo(0)
        }
    }
    val nearEnd by remember(listState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(username, selectedTab, nearEnd, loading, hasMore) {
        if (nearEnd && items.isNotEmpty() && hasMore && !loading && error == null) {
            loadMore()
        }
    }

    val isDark = IosTheme.colors.isDark

    val refreshing = refreshGeneration > finishedRefresh
    val refreshComplete = minOf(profileCompleted, badgesCompleted, summaryCompleted,
        if (selectedProfileTab == UserProfileTab.Summary) refreshGeneration else contentCompleted)
    LaunchedEffect(refreshComplete) {
        if (refreshComplete >= refreshGeneration) finishedRefresh = refreshGeneration
    }
    val followAction = onSetFollowing?.takeIf { accountId > 0 }?.let { setFollowing ->
        val following = userCard?.following == true
        ProfileMenuAction(if (followUpdating) "更新中…" else if (following) "已关注" else "关注",
            if (following) Icons.Default.Check else Icons.Default.Person,
            enabled = profileCompleted == refreshGeneration && profileError == null && !followUpdating && userCard?.let { it.canFollow || following } == true,
            onClick = {
                val target = userCard
                if (target != null && !followUpdating) {
                    followUpdating = true
                    coroutineScope.launch {
                        try {
                            setFollowing(target, !following)
                            userCard = userCard?.copy(following = !following)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            Toast.makeText(context, failure.message ?: "关注操作失败", Toast.LENGTH_SHORT).show()
                        } finally { followUpdating = false }
                    }
                }
            })
    }
    val pullState = rememberPullToRefreshState()
    val topPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)
        .asPaddingValues().calculateTopPadding()
    PullToRefreshBox(isRefreshing = refreshing, state = pullState,
        onRefresh = {
            val atTop = if (selectedProfileTab == UserProfileTab.Summary) !aboutScroll.canScrollBackward
                else !listState.canScrollBackward
            if (!refreshing && !followUpdating && atTop && headerState.offset == 0f) refreshGeneration++
        }, modifier = Modifier.fillMaxSize(),
        indicator = {
            if (refreshing || pullState.distanceFraction > 0f) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = topPadding + 10.dp)) {
                    org.linuxdo.android.ui.design.IosActivityIndicator(diameter = 22.dp)
                }
            }
        }) {
    CollapsingHeaderLayout(
        state = headerState,
        topInset = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)
            .asPaddingValues().calculateTopPadding(),
        statusBarColor = if (isDark) Color(0xFF101113) else Color.White,
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F0F10) else Color(0xFFF7F8FA)),
        header = {
            Column {
                ProfileHeader(
                    username = username, card = userCard,
                    initialAvatarUrl = initialAvatarUrl, initialStaticAvatarUrl = initialStaticAvatarUrl,
                    onBack = onBack, onSwitchAccount = onSwitchAccount, onEditProfile = onEditProfile,
                    onOpenDisplaySettings = onOpenDisplaySettings,
                    onChat = onChat?.takeIf { accountId > 0 }?.let { callback -> { callback(username) } },
                    onPrivateMessage = onComposePrivateMessage?.let { callback -> { callback(username) } },
                    menuActions = menuActions,
                    followAction = followAction,
                    notificationController = notificationController,
                    communityScore = communityScore, onOpenCommunityScore = onOpenCommunityScore,
                    communityScoreLoading = communityScoreLoading, needsAuthorization = communityScoreNeedsAuthorization,
                    onOpenLevel = onOpenLevel,
                    badges = badges, badgesLoading = badgesLoading, onOpenBadges = onOpenBadges,
                    onOpenNetwork = onOpenNetwork, onOpenWebsite = onOpenWebsite, onOpenTopic = onOpenTopic,
                    onOpenUser = onOpenUser,
                )
                profileError?.let { message ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { refreshGeneration++ }, modifier = Modifier.weight(1f)) {
                            Text("$message · 重试", color = IosTheme.colors.secondaryLabel, maxLines = 2)
                        }
                        org.linuxdo.android.ui.design.NetworkSettingsLink()
                    }
                }
            }
        },
        pinnedHeader = {
            // ── 下半部分: 横向滑动 TabRow (总结、动态、话题、回复、赞、回应、Boost、投票、已解决) ──
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDark) Color(0xFF101113) else Color.White,
                contentColor = IosTheme.colors.accent,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = IosTheme.colors.accent,
                            height = 2.dp,
                        )
                    }
                },
                divider = { IosDivider() },
            ) {
                tabEntries.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    Tab(
                        selected = selected,
                        onClick = { selectedProfileTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 14.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) IosTheme.colors.accent else IosTheme.colors.secondaryLabel,
                            )
                        },
                    )
                }
            }
        },
    ) {
        // 列表区
        val currentTab = tabEntries[selectedTab]
        if (currentTab == UserProfileTab.Summary) {
            when {
                summaryLoading && summaryData == null -> {
                    IosLoadingBox(Modifier.fillMaxSize())
                }
                summaryError != null && summaryData == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(summaryError ?: "加载失败", color = IosTheme.colors.secondaryLabel)
                            org.linuxdo.android.ui.design.NetworkSettingsLink()
                            TextButton(onClick = { refreshGeneration++ }) {
                                Text("重试", color = IosTheme.colors.accent)
                            }
                        }
                    }
                }
                summaryData != null -> {
                    UserSummaryView(
                        summary = summaryData!!,
                        userCard = userCard,
                        username = username,
                        onOpenTopic = onOpenTopic,
                        onOpenUser = { u -> onOpenUser?.invoke(u) },
                        scrollState = aboutScroll,
                    )
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无总结记录", color = IosTheme.colors.secondaryLabel)
                    }
                }
            }
        } else {
            if (loading && items.isEmpty()) {
                IosLoadingBox(Modifier.fillMaxSize())
            } else if (error != null && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "加载失败", color = IosTheme.colors.secondaryLabel)
                        org.linuxdo.android.ui.design.NetworkSettingsLink()
                        TextButton(onClick = { retryTrigger++ }) { Text("重试") }
                    }
                }
            } else if (!loading && items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("暂无${currentTab.label}记录", color = IosTheme.colors.secondaryLabel, style = IosTheme.type.body)
                        if (currentTab == UserProfileTab.Votes) {
                            Text(
                                text = "注：此处记录仅统计功能建议等板块的话题投票支持（Topic Voting），帖子内参与的问卷调查（Poll）不归入此项。",
                                color = IosTheme.colors.tertiaryLabel,
                                style = IosTheme.type.footnote,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().scrollListBars(listState),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        UserReplyCard(
                            item = item,
                            onClick = { onOpenTopic(item.topicId, item.title, item.postNumber) },
                            onOpenUser = onOpenUser,
                            // 投票/动态可能显示其他作者，不能给他们套用主页用户的头像。
                            fallbackAvatarUrl = profileAvatarUrl.takeIf { item.username.equals(username, ignoreCase = true) },
                        )
                    }
                    if (error != null && items.isNotEmpty()) {
                        item("retry-more") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { error = null; loadMore() }) { Text("加载更多失败，点击重试") }
                                org.linuxdo.android.ui.design.NetworkSettingsLink()
                            }
                        }
                    }
                    if (loading && items.isNotEmpty()) {
                        item("loading-more") { IosLoadingBox() }
                    }
                }
            }
        }
    }
    }
}


private fun formatTimeRead(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}小时${minutes}分"
        minutes > 0 -> "${minutes}分钟"
        else -> "${seconds}秒"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserSummaryView(
    summary: UserSummaryData,
    userCard: UserCardData?,
    username: String,
    onOpenTopic: (Long, String, Int?) -> Unit,
    onOpenUser: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    val context = LocalContext.current
    val isDark = IosTheme.colors.isDark
    val summaryScroll = scrollState
    val display = org.linuxdo.android.ui.rememberProfileDisplayPreferences()
    val viewerId = (context.applicationContext as App).container.sessionStore.currentUserId()
    val ownProfile = viewerId > 0 && userCard?.id == viewerId
    val stats = buildList<Pair<String, String>> {
        fun metric(field: org.linuxdo.android.data.ProfileDisplayField, label: String, value: String?) {
            if (display[field] && value != null) add(label to value)
        }
        if (summary.canSeeSummaryStats) {
            metric(DaysVisited, "📅 访问天数", formatCount(summary.daysVisited))
            metric(TimeRead, "⏱️ 阅读时长", formatTimeRead(summary.timeRead))
            metric(PostsRead, "📖 已读帖子", formatCount(summary.postsReadCount))
            metric(TopicsEntered, "👀 浏览话题", formatCount(summary.topicsEntered))
            metric(TopicCount, "📝 建立话题", formatCount(summary.topicCount))
            metric(PostCount, "💬 发表回复", formatCount(summary.postCount))
            metric(LikesReceived, "❤️ 获得点赞", formatCount(summary.likesReceived))
            metric(LikesGiven, "👍 送出点赞", formatCount(summary.likesGiven))
            metric(RecentTimeRead, "⏳ 最近阅读", summary.recentTimeRead?.let(::formatTimeRead))
            metric(Solutions, "✅ 解决方案", summary.solvedCount?.let { formatCount(it) })
        }
        if (ownProfile) metric(Bookmarks, "🔖 书签", summary.bookmarkCount?.let { formatCount(it) })
    }
    val cardBg = if (isDark) Color(0xFF18181A) else Color.White
    val cardBorder = if (isDark) Color(0xFF262628) else Color(0xFFE5E7EB)
    val textPrimary = if (isDark) Color.White else Color(0xFF111827)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .scrollListBars(summaryScroll)
            .verticalScroll(summaryScroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 隐藏或无权限的统计不占位，原有 emoji 与四列布局保留。
        if (stats.isNotEmpty()) Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stats.chunked(4).forEachIndexed { index, row ->
                if (index > 0) IosDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for ((label, value) in row) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(label, fontSize = 11.sp, color = textSecondary, textAlign = TextAlign.Center)
                            BasicText(
                                text = value,
                                modifier = Modifier.fillMaxWidth(),
                                style = IosTheme.type.body.copy(fontSize = 14.sp, lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold, color = textPrimary, textAlign = TextAlign.Center),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // 2. 热门话题
        if (summary.topTopics.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "🔥 热门话题",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                summary.topTopics.take(5).forEachIndexed { idx, topic ->
                    if (idx > 0) IosDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTopic(topic.id, topic.title, null) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = topic.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (topic.likeCount > 0) {
                                    Text("❤️ ${topic.likeCount}", fontSize = 11.5.sp, color = Color(0xFFFF453A))
                                }
                                topic.createdAt?.let {
                                    Text(RelativeTime.format(it), fontSize = 11.5.sp, color = textSecondary)
                                }
                            }
                        }
                        Text("›", fontSize = 18.sp, color = textSecondary, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        // 3. 热门回复
        if (summary.topReplies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "💬 热门回复",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                summary.topReplies.take(5).forEachIndexed { idx, reply ->
                    if (idx > 0) IosDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTopic(reply.topicId, reply.title, reply.postNumber) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = reply.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("#${reply.postNumber} 楼", fontSize = 11.5.sp, color = IosTheme.colors.accent)
                                if (reply.likeCount > 0) {
                                    Text("❤️ ${reply.likeCount}", fontSize = 11.5.sp, color = Color(0xFFFF453A))
                                }
                                reply.createdAt?.let {
                                    Text(RelativeTime.format(it), fontSize = 11.5.sp, color = textSecondary)
                                }
                            }
                        }
                        Text("›", fontSize = 18.sp, color = textSecondary, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        // 4. 热门链接
        if (summary.topLinks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "🔗 热门链接",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                summary.topLinks.take(5).forEachIndexed { idx, link ->
                    if (idx > 0) IosDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (link.topicId != null) {
                                    onOpenTopic(link.topicId, link.topicTitle ?: link.title ?: "", link.postNumber)
                                } else if (shouldOpenInAppBrowser(link.url)) {
                                    MiniAppSessionPool.requestOpen(normalizeInAppTargetUrl(link.url) ?: link.url)
                                } else {
                                    openExternal(context, link.url)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = link.title?.takeIf { it.isNotBlank() } ?: link.url,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("${link.clicks} 次点击", fontSize = 11.5.sp, color = IosTheme.colors.accent)
                                link.topicTitle?.let {
                                    Text("来自: $it", fontSize = 11.5.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Text("›", fontSize = 18.sp, color = textSecondary, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        // 5. 用户互动
        val hasInteractions = summary.mostRepliedToUsers.isNotEmpty() ||
                summary.mostLikedByUsers.isNotEmpty() ||
                summary.mostLikedUsers.isNotEmpty()
        if (hasInteractions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "🤝 用户互动",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )

                if (summary.mostRepliedToUsers.isNotEmpty()) {
                    InteractionUserSection(
                        title = "最常回复的人",
                        users = summary.mostRepliedToUsers.take(6),
                        onOpenUser = onOpenUser,
                        isDark = isDark,
                        textSecondary = textSecondary,
                    )
                }

                if (summary.mostLikedByUsers.isNotEmpty()) {
                    InteractionUserSection(
                        title = "被谁点赞最多",
                        users = summary.mostLikedByUsers.take(6),
                        onOpenUser = onOpenUser,
                        isDark = isDark,
                        textSecondary = textSecondary,
                    )
                }

                if (summary.mostLikedUsers.isNotEmpty()) {
                    InteractionUserSection(
                        title = "给谁点赞最多",
                        users = summary.mostLikedUsers.take(6),
                        onOpenUser = onOpenUser,
                        isDark = isDark,
                        textSecondary = textSecondary,
                    )
                }
            }
        }

        // 6. 活跃类别
        if (summary.topCategories.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "📁 活跃类别",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                summary.topCategories.forEachIndexed { idx, cat ->
                    if (idx > 0) IosDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val catColor = cat.color?.let {
                                runCatching { Color(android.graphics.Color.parseColor("#$it")) }.getOrNull()
                            } ?: IosTheme.colors.accent
                            ForumCategoryIcon(null, 15.dp, catColor, name = cat.name)
                            Text(
                                text = cat.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                            )
                        }
                        Text(
                            text = "${cat.topicCount} 话题 · ${cat.postCount} 回复",
                            fontSize = 12.5.sp,
                            color = textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(96.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractionUserSection(
    title: String,
    users: List<SummaryUserItem>,
    onOpenUser: (String) -> Unit,
    isDark: Boolean,
    textSecondary: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 12.5.sp, color = textSecondary)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            users.forEach { user ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF222226) else Color(0xFFF3F4F6))
                        .clickable { onOpenUser(user.username) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val avatarUrl = user.avatarTemplate?.let { avatarUrlOf(it, 48) }
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = user.username,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                    )
                    Text(
                        text = "@${user.username}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else Color(0xFF111827),
                    )
                    if (user.count > 0) {
                        Text(
                            text = "${user.count}",
                            fontSize = 11.sp,
                            color = IosTheme.colors.accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
