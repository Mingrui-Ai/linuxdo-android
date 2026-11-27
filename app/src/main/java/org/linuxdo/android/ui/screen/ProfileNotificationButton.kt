package org.linuxdo.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.linuxdo.android.data.*
import org.linuxdo.android.ui.UserNotificationController
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosTheme

@Composable
internal fun ProfileNotificationButton(controller: UserNotificationController) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    var expanded by remember(controller) { mutableStateOf(false) }
    var choosingDuration by remember(controller) { mutableStateOf(false) }
    var duration by remember(controller) { mutableStateOf<UserIgnoreDuration?>(null) }
    val busy = state.loading || state.saving
    val colors = IosTheme.colors
    val level = state.settings?.level
    LaunchedEffect(controller) { controller.load() }
    Box {
        IconButton(onClick = {
            expanded = true
            scope.launch { controller.load() }
        }, enabled = !state.saving, modifier = Modifier.size(44.dp)) {
            if (busy) IosActivityIndicator(diameter = 20.dp, color = colors.accent)
            else TopicNotificationIcon(
                if (level == UserNotificationLevel.Mute || level == UserNotificationLevel.Ignore)
                    TopicNotificationLevel.Muted else TopicNotificationLevel.Regular,
                modifier = Modifier.size(24.dp).semantics { contentDescription = "用户通知：${level?.label ?: "点击加载"}" },
                tint = colors.accent,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            containerColor = colors.card, tonalElevation = 0.dp, modifier = Modifier.widthIn(max = 300.dp)) {
            if (state.loading) DropdownMenuItem(text = { Text("正在同步通知设置…") }, onClick = {}, enabled = false)
            state.error?.let { message ->
                DropdownMenuItem(text = { Text("$message，点击重试", color = colors.secondaryLabel) },
                    onClick = { scope.launch { controller.load() } }, enabled = !busy)
            }
            UserNotificationLevel.entries.filter { it != UserNotificationLevel.Ignore || state.settings?.canIgnore == true || level == it }
                .forEach { option ->
                    DropdownMenuItem(
                        text = { Column {
                            Text(option.label, color = colors.label)
                            Text(option.description, fontSize = 12.sp, color = colors.secondaryLabel)
                        } },
                        trailingIcon = { if (option == level) Icon(Icons.Default.Check, "当前设置", tint = colors.accent) },
                        enabled = !busy && state.fresh && state.settings?.allows(option) == true,
                        onClick = {
                            if (option == UserNotificationLevel.Ignore) {
                                expanded = false
                                duration = null
                                choosingDuration = true
                            } else scope.launch {
                                if (controller.save(option)) expanded = false
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
                if (!state.fresh && !busy) TextButton(onClick = { scope.launch { controller.load() } }) { Text("重新同步") }
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
