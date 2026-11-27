package org.linuxdo.android.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Topic and profile bells share the same surface, selected row, typography and checkmark. */
@Composable
internal fun NotificationMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = if (IosTheme.colors.isDark) IosTheme.colors.card else Color.White
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = background,
        tonalElevation = 0.dp, modifier = Modifier.background(background), content = content)
}

@Composable
internal fun NotificationMenuItem(
    title: String,
    description: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    DropdownMenuItem(
        modifier = if (selected) Modifier.background(colors.accent.copy(alpha = .12f)) else Modifier,
        enabled = enabled,
        text = {
            Column(Modifier.padding(vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    icon()
                    Text(title, style = IosTheme.type.subheadline,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) colors.accent else colors.label)
                    if (selected) Text("✓", style = IosTheme.type.caption, color = colors.accent, fontWeight = FontWeight.Bold)
                }
                Text(description, style = IosTheme.type.caption,
                    color = if (colors.isDark) Color(0xFFB0B0B5) else colors.secondaryLabel, maxLines = 2)
            }
        },
        onClick = onClick,
    )
}
