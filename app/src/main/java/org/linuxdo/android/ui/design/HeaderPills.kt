package org.linuxdo.android.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 顶栏右侧轻量统计/状态胶囊，与主页及徽章页风格统一。
 */
@Composable
internal fun HeaderCountPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = IosTheme.colors
    Text(
        text = text,
        style = IosTheme.type.footnote.copy(
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = colors.secondaryLabel,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (colors.isDark) Color.White.copy(alpha = 0.08f)
                else Color(0xFF767680).copy(alpha = 0.11f),
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/**
 * 顶栏右侧轻量操作胶囊（如“新建”“管理”“完成”），浅深色自适应、视觉克制。
 */
@Composable
internal fun HeaderActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = true,
) {
    val colors = IosTheme.colors
    val isDark = colors.isDark
    val shape = RoundedCornerShape(99.dp)
    val bgColor = if (emphasized) {
        colors.accent.copy(alpha = if (isDark) 0.18f else 0.11f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF767680).copy(alpha = 0.11f)
    }
    val borderColor = if (emphasized) {
        colors.accent.copy(alpha = if (isDark) 0.42f else 0.28f)
    } else {
        Color.Transparent
    }
    val textColor = if (emphasized) colors.accent else colors.label

    Row(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(0.8.dp, borderColor, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = IosTheme.type.footnote.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = textColor,
            maxLines = 1,
        )
    }
}

/**
 * 与主页筛选栏及徽章页一致的轻量单选胶囊。
 */
@Composable
internal fun HeaderFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = IosTheme.colors
    val isDark = colors.isDark
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (selected) {
        colors.accent.copy(alpha = if (isDark) 0.18f else 0.11f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF767680).copy(alpha = 0.11f)
    }
    val borderColor = if (selected) {
        colors.accent.copy(alpha = if (isDark) 0.42f else 0.30f)
    } else {
        Color.Transparent
    }
    val textColor = if (selected) colors.accent else colors.label
    val countColor = if (selected) colors.accent.copy(alpha = 0.85f) else colors.secondaryLabel

    Row(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(0.9.dp, borderColor, shape) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            style = IosTheme.type.footnote.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = textColor,
            maxLines = 1,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = IosTheme.type.caption.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = countColor,
                maxLines = 1,
            )
        }
    }
}

/**
 * 通用的横向滚动筛选胶囊行（底色与页面背景融为一体，底部带单条通栏分隔线）。
 */
@Composable
internal fun HeaderFilterPillRow(
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
    if (showDivider) IosFullDivider()
}
