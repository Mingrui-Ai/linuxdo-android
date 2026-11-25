package org.linuxdo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.linuxdo.android.net.NetworkMode
import org.linuxdo.android.net.NetworkRouteMode
import org.linuxdo.android.net.NetworkSecurityState
import org.linuxdo.android.ui.design.IosActivityIndicator
import org.linuxdo.android.ui.design.IosDivider
import org.linuxdo.android.ui.design.IosGroupCard
import org.linuxdo.android.ui.design.IosTheme

@Composable
internal fun NetworkModeSettings(
    mode: NetworkMode,
    route: NetworkRouteMode,
    environment: NetworkSecurityState,
    switching: Boolean,
    error: String?,
    onSelect: (NetworkMode) -> Unit,
) {
    IosGroupCard {
        Column(Modifier.selectableGroup()) {
            NetworkMode.entries.forEachIndexed { index, option ->
                if (index > 0) IosDivider()
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(
                            selected = option == mode,
                            enabled = !switching,
                            role = Role.RadioButton,
                            onClick = { onSelect(option) },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(option.label, style = IosTheme.type.body, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (option) {
                                NetworkMode.BUILT_IN -> "使用 App 内置连接方式，不使用系统 HTTP 代理"
                                NetworkMode.SYSTEM -> "使用手机当前网络、系统代理或 VPN（如 FlClash）"
                            },
                            style = IosTheme.type.footnote,
                            color = IosTheme.colors.secondaryLabel,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    RadioButton(
                        selected = option == mode,
                        onClick = null,
                        enabled = !switching,
                        colors = RadioButtonDefaults.colors(selectedColor = IosTheme.colors.accent),
                    )
                }
            }
        }
        IosDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (switching) {
                    IosActivityIndicator(diameter = 14.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (switching) "正在切换联网方式…" else when (route) {
                        NetworkRouteMode.SYSTEM -> "当前：跟随系统 · " + when {
                            environment.isProxy -> "系统 HTTP 代理"
                            environment.isVpn -> "系统 VPN"
                            else -> "系统默认网络"
                        }
                        NetworkRouteMode.DIRECT -> "当前：内置网络 · Anycast"
                        NetworkRouteMode.DOH -> "当前：内置网络 · DoH"
                    },
                    style = IosTheme.type.footnote,
                    color = IosTheme.colors.accent,
                )
            }
            Text(environment.summary, style = IosTheme.type.footnote, color = IosTheme.colors.secondaryLabel)
            if (environment.isVpn && mode == NetworkMode.BUILT_IN) {
                Text(
                    "Android 仍可能把内置连接交给 VPN。若要完全绕过 FlClash，请在 FlClash 中将本 App 排除。",
                    style = IosTheme.type.footnote,
                    color = IosTheme.colors.secondaryLabel,
                )
            }
            Text(
                "选择会自动保存。切换完成后，返回原页面重试即可。",
                style = IosTheme.type.footnote,
                color = IosTheme.colors.secondaryLabel,
            )
            error?.let { Text(it, style = IosTheme.type.footnote, color = IosTheme.colors.destructive) }
        }
    }
}
