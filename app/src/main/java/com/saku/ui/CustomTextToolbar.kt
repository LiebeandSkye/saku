package com.saku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
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
 * and provide a floating OLED dark menu with Copy, Select All, and Translate.
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
    var onSelectAllAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val customToolbar = remember {
        CustomTextToolbar(
            onShow = { rect, onCopy, onSelectAll ->
                menuRect = rect
                onCopyAction = onCopy
                onSelectAllAction = onSelectAll
            },
            onHide = {
                menuRect = null
                onCopyAction = null
                onSelectAllAction = null
            }
        )
    }

    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current

    CompositionLocalProvider(LocalTextToolbar provides customToolbar) {
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
                        onSelectAll = {
                            onSelectAllAction?.invoke()
                        },
                        onTranslate = {
                            onCopyAction?.invoke()
                            val selected = clipboardManager.getText()?.text ?: ""
                            customToolbar.hide()
                            if (selected.isNotBlank()) {
                                onTranslate(selected)
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
    onSelectAll: () -> Unit,
    onTranslate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF141416),
        border = BorderStroke(1.dp, Color(0xFF2C2C32)),
        shadowElevation = 10.dp,
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            ToolbarButton(
                icon = Icons.Default.ContentCopy,
                label = "Copy",
                onClick = onCopy
            )

            ToolbarDivider()

            ToolbarButton(
                icon = Icons.Default.SelectAll,
                label = "Select All",
                onClick = onSelectAll
            )

            ToolbarDivider()

            ToolbarButton(
                icon = Icons.Default.Translate,
                label = "Translate",
                accentColor = Color(0xFF90D695),
                onClick = onTranslate
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    accentColor: Color = Color(0xFFEEEEEE),
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor.copy(alpha = 0.3f), bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
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
