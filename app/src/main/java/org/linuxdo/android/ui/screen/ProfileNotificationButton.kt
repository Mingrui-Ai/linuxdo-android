package org.linuxdo.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            else TopicNotificationIcon(
                if (level == UserNotificationLevel.Mute || level == UserNotificationLevel.Ignore)
                    TopicNotificationLevel.Muted else TopicNotificationLevel.Regular,
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
                        icon = { TopicNotificationIcon(if (option == UserNotificationLevel.Normal)
                            TopicNotificationLevel.Regular else TopicNotificationLevel.Muted) },
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
