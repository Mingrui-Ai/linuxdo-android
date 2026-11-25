package org.linuxdo.android.ui.design

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** 由当前导航栈提供，错误组件无需持有 Activity 或重新打开首页。 */
val LocalNetworkSettingsAction = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun NetworkSettingsLink(modifier: Modifier = Modifier) {
    val openSettings = LocalNetworkSettingsAction.current ?: return
    TextButton(onClick = openSettings, modifier = modifier) {
        Text("代理设置", style = IosTheme.type.subheadline, color = IosTheme.colors.accent)
    }
}
