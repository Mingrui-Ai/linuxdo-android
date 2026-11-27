package org.linuxdo.android.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val colors = IosTheme.colors
    val fieldBg = if (colors.isDark) {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
    } else {
        androidx.compose.ui.graphics.Color(0xFF767680).copy(alpha = 0.11f)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.groupedBackground)
            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(fieldBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = IosTheme.type.body.copy(fontSize = 14.sp, color = colors.label),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(placeholder, fontSize = 14.sp, color = colors.tertiaryLabel, maxLines = 1)
                        }
                        field()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Text(
                    text = "清除",
                    style = IosTheme.type.caption.copy(fontSize = 12.sp),
                    color = colors.secondaryLabel,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onQuery("") }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
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
