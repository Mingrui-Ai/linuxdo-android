package org.linuxdo.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.linuxdo.android.App
import org.linuxdo.android.data.ProfileDisplayField
import org.linuxdo.android.data.ProfileDisplayField.*
import org.linuxdo.android.data.ProfileDisplayPreferences
import org.linuxdo.android.ui.design.*

@Composable
internal fun rememberProfileDisplayPreferences(): ProfileDisplayPreferences {
    val store = (LocalContext.current.applicationContext as App).container.profileDisplayStore
    return store.settings.collectAsState().value
}

@Composable
internal fun ProfileDisplaySettings() {
    val store = (LocalContext.current.applicationContext as App).container.profileDisplayStore
    val settings by store.settings.collectAsState()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("选择本机显示的资料字段，适用于自己和他人的详细主页、首页侧栏与头像弹窗。更改立即生效并自动保存，不改变网页资料的公开范围。",
            style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
            modifier = Modifier.padding(horizontal = 20.dp))
        Text("头像、昵称和 @用户名始终显示。各位置仅显示原本支持且已公开的字段。",
            style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
            modifier = Modifier.padding(horizontal = 20.dp))
        ProfileDisplayGroup("外观与身份", listOf(
            Background to "背景图片", Status to "状态表情", Title to "头衔", Level to "等级",
        ), settings, store::setVisible)
        ProfileDisplayGroup("个人资料", listOf(
            Bio to "个人简介", CommunityScore to "社区分", Badges to "徽章",
            JoinedAt to "加入时间", LastActive to "最近活跃", FollowCounts to "关注与粉丝",
        ), settings, store::setVisible)
        ProfileDisplayGroup("更多信息", listOf(
            Inviter to "邀请人", Email to "电子邮件", Points to "论坛点数",
            Website to "个人网站", FeaturedTopic to "精选话题",
        ), settings, store::setVisible)
        ProfileDisplayGroup("总结统计", listOf(
            DaysVisited to "访问天数", TimeRead to "阅读时长", PostsRead to "已读帖子",
            TopicsEntered to "浏览话题", TopicCount to "建立话题", PostCount to "发表回复",
            LikesReceived to "获得点赞", LikesGiven to "送出点赞",
            RecentTimeRead to "最近阅读时长", Solutions to "解决方案", Bookmarks to "书签",
        ), settings, store::setVisible)
        TextButton(onClick = store::restoreDefaults, enabled = settings.hiddenFields.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(
                contentColor = IosTheme.colors.accent,
                disabledContentColor = IosTheme.colors.tertiaryLabel,
            )) {
            Text("恢复默认显示")
        }
    }
}

@Composable
private fun ProfileDisplayGroup(
    title: String,
    fields: List<Pair<ProfileDisplayField, String>>,
    settings: ProfileDisplayPreferences,
    onChange: (ProfileDisplayField, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
            modifier = Modifier.padding(horizontal = 20.dp))
        IosGroupCard {
            fields.forEachIndexed { index, (field, label) ->
                if (index > 0) IosDivider()
                IosRow(onClick = { onChange(field, !settings[field]) }) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(label, style = IosTheme.type.body.copy(fontSize = 14.5.sp), color = IosTheme.colors.label)
                        val detail = when (field) {
                            Background -> "详细主页和头像弹窗的背景图"
                            Status -> "头像右上角的表情和状态文字"
                            CommunityScore -> "仅自己的已授权社区分"
                            Email -> "仅当前账号的邮箱"
                            Points -> "论坛点数，与 CDK 社区分分别显示"
                            RecentTimeRead -> "近期累计阅读时长，按网页统计口径"
                            Solutions -> "被采纳的解决方案数量"
                            Bookmarks -> "仅自己的书签数量"
                            FollowCounts -> "详细主页和头像弹窗中的公开数量"
                            Website, FeaturedTopic -> "仅详细主页"
                            else -> null
                        }
                        detail?.let { Text(it, style = IosTheme.type.footnote.copy(fontSize = 12.sp),
                            color = IosTheme.colors.secondaryLabel, modifier = Modifier.padding(top = 3.dp)) }
                    }
                    IosSwitch(checked = settings[field], onCheckedChange = { onChange(field, it) },
                        modifier = Modifier.semantics { contentDescription = label })
                }
            }
        }
    }
}
