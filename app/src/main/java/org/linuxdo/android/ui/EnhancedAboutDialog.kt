package org.linuxdo.android.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.linuxdo.android.BuildConfig
import org.linuxdo.android.ui.design.IosTheme

@Composable
fun EnhancedAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var thirdParty by remember { mutableStateOf(false) }
    val paragraphs = remember(thirdParty) {
        val notice = if (thirdParty) context.assets.open("THIRD_PARTY_NOTICES").bufferedReader().use { it.readText() }
        else context.assets.open("NOTICE").bufferedReader().use { it.readText() } +
            "\n\n" + context.assets.open("LICENSE").bufferedReader().use { it.readText() }
        notice.split("\n\n")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosTheme.colors.card,
        titleContentColor = IosTheme.colors.label,
        textContentColor = IosTheme.colors.label,
        title = { Text(if (thirdParty) "第三方许可证" else "${context.getString(org.linuxdo.android.R.string.app_name)} ${BuildConfig.VERSION_NAME}") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                items(paragraphs) { paragraph -> SelectionContainer { Text(paragraph + "\n") } }
            }
        },
        dismissButton = { TextButton(onClick = { thirdParty = !thirdParty }) { Text(if (thirdParty) "返回关于" else "第三方许可证") } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
