package com.example.seteasecloudmusic.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/** Hosts modal content wholly in one display segment; its scrim can still cover the full window. */
@Composable
fun AdaptiveOverlayPane(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val window = LocalAdaptiveWindow.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Layout(modifier = modifier.onGloballyPositioned { origin = it.positionInWindow() },
        content = { Box(Modifier.fillMaxSize(), content = content) }) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val fold = window.separatingFold
        var x = 0
        var y = 0
        var paneWidth = width
        var paneHeight = height
        if (fold != null) {
            if (window.horizontalFold) {
                val top = (fold.top - origin.y).roundToInt().coerceIn(0, height)
                val bottom = (fold.bottom - origin.y).roundToInt().coerceIn(0, height)
                if (bottom in 1 until height) { y = bottom; paneHeight = height - bottom }
                else if (top > 0) paneHeight = top
            } else {
                val left = (fold.left - origin.x).roundToInt().coerceIn(0, width)
                val right = (fold.right - origin.x).roundToInt().coerceIn(0, width)
                if (right in 1 until width) { x = right; paneWidth = width - right }
                else if (left > 0) paneWidth = left
            }
        }
        val child = measurables.single().measure(Constraints.fixed(paneWidth, paneHeight))
        layout(width, height) { child.placeRelative(x, y) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveModalSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val window = LocalAdaptiveWindow.current
    if (window.width < 600.dp && window.separatingFold == null) {
        ModalBottomSheet(onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = containerColor, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = modifier) { content() }
    } else {
        Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest)) {
                AdaptiveOverlayPane(Modifier.fillMaxSize().systemBarsPadding()) {
                    Surface(modifier.align(Alignment.CenterEnd).padding(20.dp).widthIn(max = 520.dp)
                        .fillMaxWidth().fillMaxHeight(0.92f).clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
                        shape = RoundedCornerShape(28.dp), color = containerColor, shadowElevation = 24.dp) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        AdaptiveOverlayPane(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 440.dp).fillMaxWidth(), shape = shape, color = containerColor, tonalElevation = 6.dp) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        title?.invoke()
                        text?.invoke()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
