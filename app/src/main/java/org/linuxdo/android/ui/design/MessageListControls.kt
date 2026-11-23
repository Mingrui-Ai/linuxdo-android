package org.linuxdo.android.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MessageSearchField(query: String, onQuery: (String) -> Unit, placeholder: String) {
    Box(Modifier.fillMaxWidth().background(IosTheme.colors.card).padding(horizontal = 14.dp, vertical = 8.dp)) {
        BasicTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            textStyle = IosTheme.type.body.copy(fontSize = 14.sp, color = IosTheme.colors.label),
            cursorBrush = SolidColor(IosTheme.colors.accent),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(IosTheme.colors.fieldBackground).padding(horizontal = 10.dp, vertical = 8.dp),
            decorationBox = { field ->
                if (query.isEmpty()) Text(placeholder, fontSize = 14.sp, color = IosTheme.colors.tertiaryLabel)
                field()
            },
        )
    }
}

/** 与首页相同的下拉刷新手势；空内容也由可滚动列表承接。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageListRefresh(
    refreshing: Boolean,
    active: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing, state = pullState,
        onRefresh = { if (active && !refreshing) onRefresh() },
        modifier = modifier,
        indicator = {
            if (refreshing || pullState.distanceFraction > 0f) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                    IosActivityIndicator(diameter = 22.dp)
                }
            }
        },
        content = content,
    )
}
