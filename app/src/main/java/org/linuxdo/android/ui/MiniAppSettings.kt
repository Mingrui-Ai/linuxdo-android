package org.linuxdo.android.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.linuxdo.android.App
import org.linuxdo.android.data.MiniAppOpenMode
import org.linuxdo.android.data.MiniAppPreferences
import org.linuxdo.android.ui.design.*

@Composable
internal fun rememberMiniAppOpenMode(): MiniAppOpenMode {
    val preferences = (LocalContext.current.applicationContext as App).container.preferences
    val store = remember(preferences) { MiniAppPreferences(preferences) }
    var mode by remember(store) { mutableStateOf(store.openMode()) }
    DisposableEffect(preferences, store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == MiniAppPreferences.KEY) mode = store.openMode()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        mode = store.openMode()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return mode
}

@Composable
internal fun MiniAppSettings() {
    val mode = rememberMiniAppOpenMode()
    val preferences = (LocalContext.current.applicationContext as App).container.preferences
    val store = remember(preferences) { MiniAppPreferences(preferences) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IosGroupCard {
            Column(Modifier.selectableGroup()) {
                MiniAppOpenMode.entries.forEachIndexed { index, option ->
                    if (index > 0) IosDivider()
                    Row(Modifier.fillMaxWidth().selectable(
                        selected = mode == option, role = Role.RadioButton,
                        onClick = { store.setOpenMode(option) },
                    ).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(option.label, style = IosTheme.type.body, color = IosTheme.colors.label)
                            Text(when (option) {
                                MiniAppOpenMode.Internal -> "在 App 内打开小程序，关闭后回到原页面"
                                MiniAppOpenMode.External -> "交给系统浏览器或对应应用打开"
                            }, style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel)
                        }
                        Spacer(Modifier.width(12.dp))
                        RadioButton(selected = mode == option, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = IosTheme.colors.accent))
                    }
                }
            }
        }
        Text("应用广场始终先显示本机的最近使用和我的小程序，点击小程序后按此设置打开。选择会自动保存，外部浏览器使用自己的登录状态。",
            style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel,
            modifier = Modifier.padding(horizontal = 20.dp))
    }
}
