package org.linuxdo.android.ui.screen

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import org.linuxdo.android.App
import org.linuxdo.android.ui.FollowUiState

/** 与“我的关注”管理状态隔离，查看他人关系和粉丝时不提供取消关注操作。 */
@Composable
internal fun ProfileNetworkScreen(
    username: String,
    followers: Boolean,
    accountId: Long?,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val repo = (LocalContext.current.applicationContext as App).container.topicRepository
    var retry by remember(username, followers, accountId) { mutableIntStateOf(0) }
    var state by remember(username, followers, accountId) { mutableStateOf(FollowUiState(username, loading = true)) }
    LaunchedEffect(username, followers, accountId, retry) {
        state = FollowUiState(username, loading = true)
        try {
            state = FollowUiState(username, users = repo.fetchProfileNetwork(username, followers))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state = FollowUiState(username, error = e.message ?: "暂时无法加载")
        }
    }
    FollowListScreen(state, onBack, onRetry = { retry++ }, onOpenUser = onOpenUser,
        title = "@$username 的${if (followers) "粉丝" else "关注"}", canManage = false,
        emptyText = if (followers) "还没有粉丝" else "还没有关注的人")
}
