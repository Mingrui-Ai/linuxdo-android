package org.linuxdo.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.linuxdo.android.data.*
import org.linuxdo.android.ui.UserNotificationController
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosTheme
import org.linuxdo.android.ui.design.NotificationMenu
import org.linuxdo.android.ui.design.NotificationMenuItem

@Composable
internal fun ProfileNotificationButton(controller: UserNotificationController) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    var expanded by remember(controller) { mutableStateOf(false) }
    var choosingDuration by remember(controller) { mutableStateOf(false) }
    var duration by remember(controller) { mutableStateOf<UserIgnoreDuration?>(null) }
    val busy = state.saving || (state.loading && !state.fresh)
    val colors = IosTheme.colors
    val level = state.settings?.level
    val activeTint = if (level == UserNotificationLevel.Mute || level == UserNotificationLevel.Ignore)
        androidx.compose.ui.graphics.Color(0xFFFF3B30) else colors.label
    Box {
        IconButton(onClick = {
            expanded = true
            scope.launch { controller.load() }
        }, enabled = !state.saving, modifier = Modifier.size(44.dp)) {
            if (state.saving || (state.loading && state.settings == null)) IosActivityIndicator(diameter = 20.dp, color = activeTint)
            else UserNotificationIcon(
                level = level,
                modifier = Modifier.size(24.dp).semantics { contentDescription = "用户通知：${level?.label ?: "点击加载"}" },
                tint = activeTint,
            )
        }
        NotificationMenu(expanded = expanded, onDismiss = { expanded = false }) {
            if (state.loading && state.settings == null) DropdownMenuItem(text = { Text("正在同步通知设置…") }, onClick = {}, enabled = false)
            state.error?.let { message ->
                DropdownMenuItem(text = { Text("$message，点击重试", color = colors.secondaryLabel) },
                    onClick = { scope.launch { controller.load(force = true) } }, enabled = !busy)
            }
            UserNotificationLevel.entries.filter { it != UserNotificationLevel.Ignore || state.settings?.canIgnore == true || level == it }
                .forEach { option ->
                    NotificationMenuItem(
                        title = option.label,
                        description = option.description,
                        selected = option == level,
                        icon = { UserNotificationIcon(option) },
                        enabled = !busy && state.fresh && state.settings?.allows(option) == true,
                        onClick = {
                            if (option == UserNotificationLevel.Ignore) {
                                expanded = false
                                duration = null
                                choosingDuration = true
                            } else {
                                expanded = false
                                scope.launch { if (!controller.save(option)) expanded = true }
                            }
                        },
                    )
                }
        }
    }
    if (choosingDuration) AlertDialog(
        onDismissRequest = { if (!state.saving) choosingDuration = false },
        title = { Text("忽略此用户") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("选择忽略时限，到期自动恢复常规。此设置与网页版同步。", color = colors.secondaryLabel)
                UserIgnoreDuration.entries.forEach { option ->
                    Surface(onClick = { duration = option }, enabled = !busy, color = androidx.compose.ui.graphics.Color.Transparent) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = duration == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    }
                }
                state.error?.let { Text(it, color = colors.secondaryLabel) }
                if (!state.fresh && !busy) TextButton(onClick = { scope.launch { controller.load(force = true) } }) { Text("重新同步") }
            }
        },
        confirmButton = {
            TextButton(enabled = duration != null && state.fresh && !busy, onClick = {
                val selected = duration ?: return@TextButton
                scope.launch { if (controller.save(UserNotificationLevel.Ignore, selected.expiresAt())) choosingDuration = false }
            }) { Text(if (state.saving) "保存中…" else "忽略") }
        },
        dismissButton = { TextButton(enabled = !state.saving, onClick = { choosingDuration = false }) { Text("取消") } },
        containerColor = colors.card,
    )
}

@Composable
private fun UserNotificationIcon(
    level: UserNotificationLevel?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    if (level != UserNotificationLevel.Ignore) {
        TopicNotificationIcon(
            if (level == UserNotificationLevel.Mute) TopicNotificationLevel.Muted else TopicNotificationLevel.Regular,
            modifier = modifier, tint = tint,
        )
        return
    }
    val color = tint ?: Color(0xFFFF3B30)
    val eye = remember {
        Path().apply {
            // Broken contours leave the diagonal slash readable on either menu background.
            moveTo(10f, 5.2f)
            cubicTo(10.7f, 5.1f, 11.3f, 5f, 12f, 5f)
            cubicTo(16.5f, 5f, 20f, 7.8f, 22f, 12f)
            cubicTo(21.4f, 13.3f, 20.6f, 14.5f, 19.7f, 15.5f)
            moveTo(6.5f, 6.5f)
            cubicTo(4.5f, 7.8f, 3f, 9.6f, 2f, 12f)
            cubicTo(4f, 16.2f, 7.5f, 19f, 12f, 19f)
            cubicTo(14f, 19f, 15.8f, 18.5f, 17.5f, 17.5f)
            moveTo(9.9f, 9.9f)
            cubicTo(8.7f, 11.1f, 8.7f, 12.9f, 9.9f, 14.1f)
            cubicTo(11.1f, 15.3f, 12.9f, 15.3f, 14.1f, 14.1f)
        }
    }
    Canvas(modifier.size(18.dp)) {
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            drawPath(eye, color, style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(color, Offset(3f, 3f), Offset(21f, 21f), 1.7f, StrokeCap.Round)
        }
    }
}
