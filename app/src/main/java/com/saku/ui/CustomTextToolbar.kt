package com.saku.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Custom TextToolbar implementation to intercept Compose's text selection toolbar
 * and provide a floating text-only OLED dark menu with [Copy | Translate].
 */
class CustomTextToolbar(
    private val onShow: (Rect, (() -> Unit)?, (() -> Unit)?) -> Unit,
    private val onHide: () -> Unit
) : TextToolbar {

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        status = TextToolbarStatus.Shown
        onShow(rect, onCopyRequested, onSelectAllRequested)
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        onHide()
    }
}

/**
 * Intercepting clipboard manager that catches the exact selected text in-memory
 * when Compose's SelectionManager invokes copy, avoiding IPC delays or Android 10+
 * background clipboard read restrictions.
 */
class InterceptingClipboardManager(
    private val delegate: ClipboardManager
) : ClipboardManager {
    var capturedText: String = ""
    /** When true, setText only captures in-memory without touching the system clipboard. */
    var captureOnly: Boolean = false

    override fun getText(): AnnotatedString? {
        return delegate.getText()
    }

    override fun setText(annotatedString: AnnotatedString) {
        capturedText = annotatedString.text
        if (!captureOnly) {
            delegate.setText(annotatedString)
        }
    }

    override fun hasText(): Boolean {
        return delegate.hasText()
    }
}

private class SelectionMenuPositionProvider(
    private val contentRect: Rect,
    private val density: Density
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (contentRect.left + (contentRect.width - popupContentSize.width) / 2f)
            .toInt()
            .coerceIn(16, (windowSize.width - popupContentSize.width - 16).coerceAtLeast(16))

        val margin = with(density) { 10.dp.roundToPx() }
        var y = (contentRect.top - popupContentSize.height - margin).toInt()
        if (y < 40) {
            y = (contentRect.bottom + margin).toInt()
        }
        y = y.coerceIn(16, (windowSize.height - popupContentSize.height - 16).coerceAtLeast(16))

        return IntOffset(x, y)
    }
}

/**
 * Composable wrapper providing the custom OLED dark selection toolbar to its content.
 */
@Composable
fun CustomSelectionContainer(
    onTranslate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var menuRect by remember { mutableStateOf<Rect?>(null) }
    var onCopyAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val customToolbar = remember {
        CustomTextToolbar(
            onShow = { rect, onCopy, _ ->
                menuRect = rect
                onCopyAction = onCopy
            },
            onHide = {
                menuRect = null
                onCopyAction = null
            }
        )
    }

    val context = LocalContext.current
    val systemClipboard = LocalClipboardManager.current
    val interceptingClipboard = remember(systemClipboard) {
        InterceptingClipboardManager(systemClipboard)
    }
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalTextToolbar provides customToolbar,
        LocalClipboardManager provides interceptingClipboard
    ) {
        Box {
            content()

            menuRect?.let { rect ->
                Popup(
                    popupPositionProvider = SelectionMenuPositionProvider(rect, density),
                    onDismissRequest = { customToolbar.hide() },
                    properties = PopupProperties(focusable = false)
                ) {
                    OledSelectionPill(
                        onCopy = {
                            onCopyAction?.invoke()
                            customToolbar.hide()
                        },
                        onTranslate = {
                            interceptingClipboard.capturedText = ""
                            interceptingClipboard.captureOnly = true
                            onCopyAction?.invoke()
                            interceptingClipboard.captureOnly = false
                            val selected = interceptingClipboard.capturedText.ifBlank {
                                systemClipboard.getText()?.text ?: ""
                            }
                            customToolbar.hide()
                            if (selected.isNotBlank()) {
                                onTranslate(selected)
                            } else {
                                Toast.makeText(context, "No text selected", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OledSelectionPill(
    onCopy: () -> Unit,
    onTranslate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141416),
        border = BorderStroke(1.dp, Color(0xFF2C2C32)),
        shadowElevation = 10.dp,
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            TextToolbarButton(
                label = "Copy",
                onClick = onCopy
            )

            ToolbarDivider()

            TextToolbarButton(
                label = "Translate",
                onClick = onTranslate
            )
        }
    }
}

@Composable
private fun TextToolbarButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.25f), bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEEEEEE),
            maxLines = 1
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(Color(0xFF282830))
    )
}
