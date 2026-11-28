package org.linuxdo.android.ui.design

import android.content.Context
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext

private val LocalContinuousSelection = compositionLocalOf { false }
private data object QuoteSelectionKey

/** One selection registrar for the entire rendered body, including nested blocks. */
@Composable
internal fun ContinuousTextSelection(
    onCopied: (String) -> Unit,
    onQuote: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalContinuousSelection.current) {
        content()
        return
    }
    val context = LocalContext.current
    val delegate = LocalClipboard.current
    val currentOnCopied by rememberUpdatedState(onCopied)
    val clipboard = remember(delegate, context) {
        SelectionClipboard(delegate, context) { currentOnCopied(it) }
    }
    val currentOnQuote by rememberUpdatedState(onQuote)
    val toolbar = LocalTextContextMenuToolbarProvider.current
    val dropdown = LocalTextContextMenuDropdownProvider.current
    val toolbarProvider = remember(toolbar, clipboard) {
        toolbar?.let { SelectionMenuProvider(it, clipboard) { currentOnQuote } }
    }
    val dropdownProvider = remember(dropdown, clipboard) {
        dropdown?.let { SelectionMenuProvider(it, clipboard) { currentOnQuote } }
    }
    CompositionLocalProvider(
        LocalContinuousSelection provides true,
        LocalClipboard provides clipboard,
        LocalTextContextMenuToolbarProvider provides toolbarProvider,
        LocalTextContextMenuDropdownProvider provides dropdownProvider,
    ) {
        SelectionContainer(modifier = modifier, content = content)
    }
}

/** Compose 1.10 copies through LocalClipboard, not the deprecated ClipboardManager. */
private class SelectionClipboard(
    private val delegate: Clipboard,
    private val context: Context,
    private val onCopied: (String) -> Unit,
) : Clipboard by delegate {
    var capture: ((String) -> Unit)? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val clip = clipEntry?.clipData
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        val receiver = capture
        if (receiver != null) {
            text?.takeIf { it.isNotBlank() }?.let(receiver)
            return
        }
        delegate.setClipEntry(clipEntry)
        text?.takeIf { it.isNotBlank() }?.let(onCopied)
    }
}

private class SelectionMenuProvider(
    private val delegate: TextContextMenuProvider,
    private val clipboard: SelectionClipboard,
    private val onQuote: () -> ((String) -> Unit)?,
) : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        delegate.showTextContextMenu(object : TextContextMenuDataProvider by dataProvider {
            override fun data(): TextContextMenuData {
                val data = dataProvider.data()
                val quote = onQuote() ?: return data
                return TextContextMenuData(data.components.flatMap { component ->
                    if (component is TextContextMenuItem && component.key == TextContextMenuKeys.CopyKey) {
                        listOf(component, TextContextMenuItem(QuoteSelectionKey, "引用回复") {
                            // SelectionContainer starts its copy coroutine UNDISPATCHED. Capture
                            // before the clipboard write suspends; quoting leaves the clipboard intact.
                            clipboard.capture = quote
                            try {
                                component.onClick(this)
                            } finally {
                                clipboard.capture = null
                            }
                        })
                    } else listOf(component)
                })
            }
        })
    }
}
