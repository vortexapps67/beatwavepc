package com.beatwave.music.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import java.awt.MouseInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Standard Windows title bar & caption controls

/** The strip where the system's title bar would have been. */
@Composable
internal fun DesktopTitleBar() {
    val enabled by DesktopTitleBarSetting.enabled.collectAsState()
    if (!DesktopPlatform.drawsOwnWindowFrame || !enabled) return
    DesktopTitleBarDragArea(
        Modifier
            .fillMaxWidth()
            .height(CAPTION_HEIGHT)
            .desktopWindowGlass(),
    ) {
        Box(Modifier.fillMaxSize()) {
            DesktopWindowButtons(Modifier.align(Alignment.CenterEnd))
        }
    }
}

/** Whether there is a title bar at all. */
internal object DesktopTitleBarSetting {

    internal const val KEY = "window_title_bar"

    private val _enabled = MutableStateFlow(DesktopPersistence().boolean(KEY, false))

    val enabled: StateFlow<Boolean> = _enabled

    fun set(value: Boolean) {
        DesktopPersistence().saveBoolean(KEY, value)
        _enabled.value = value
    }
}

/** What the caption buttons act on. */
internal class DesktopWindowActions(
    val minimize: () -> Unit,
    val toggleMaximize: () -> Unit,
    val close: () -> Unit,
)

internal val LocalDesktopWindowActions = staticCompositionLocalOf<DesktopWindowActions?> { null }

/** The window, for the drag area. */
internal val LocalDesktopWindowScope = staticCompositionLocalOf<WindowScope?> { null }

/** A region that moves the window, and maximizes it on a double click. */
@Composable
internal fun DesktopTitleBarDragArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val scope = LocalDesktopWindowScope.current
    val actions = LocalDesktopWindowActions.current
    if (!DesktopPlatform.drawsOwnWindowFrame || scope == null) {
        Box(modifier) { content() }
        return
    }
    Box(modifier) {
        Box(Modifier.matchParentSize().captionPress(scope, actions))
        content()
    }
}

/**
 * Standard Windows caption buttons (Minimize —, Maximize/Restore 🗖/🗗, Close ✕).
 * Positioned on the top-right of the window.
 */
@Composable
internal fun DesktopWindowButtons(modifier: Modifier = Modifier) {
    if (!DesktopPlatform.drawsOwnWindowFrame) return
    val actions = LocalDesktopWindowActions.current ?: return
    val maximized by DesktopWindowMode.maximized.collectAsState()

    Row(
        modifier = modifier.height(CAPTION_HEIGHT),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Minimize (—)
        WindowsCaptionButton(
            label = "Minimize",
            isClose = false,
            onClick = actions.minimize,
        ) { glyphColor ->
            val strokeW = 1.dp.toPx()
            val halfW = 5.dp.toPx()
            drawLine(
                color = glyphColor,
                start = Offset(center.x - halfW, center.y),
                end = Offset(center.x + halfW, center.y),
                strokeWidth = strokeW,
                cap = StrokeCap.Square,
            )
        }

        // 2. Maximize / Restore (🗖 / 🗗)
        WindowsCaptionButton(
            label = if (maximized) "Restore" else "Maximize",
            isClose = false,
            onClick = actions.toggleMaximize,
        ) { glyphColor ->
            val strokeW = 1.dp.toPx()
            if (!maximized) {
                // Single square outline (10x10)
                val half = 4.5.dp.toPx()
                drawRect(
                    color = glyphColor,
                    topLeft = Offset(center.x - half, center.y - half),
                    size = Size(half * 2f, half * 2f),
                    style = Stroke(width = strokeW),
                )
            } else {
                // Two overlapping rectangles (Restore)
                val sizePx = 7.dp.toPx()
                val off = 2.dp.toPx()
                // Back square
                drawRect(
                    color = glyphColor,
                    topLeft = Offset(center.x - sizePx / 2f + off, center.y - sizePx / 2f - off),
                    size = Size(sizePx, sizePx),
                    style = Stroke(width = strokeW),
                )
                // Front square
                drawRect(
                    color = glyphColor,
                    topLeft = Offset(center.x - sizePx / 2f - 1.dp.toPx(), center.y - sizePx / 2f + 1.dp.toPx()),
                    size = Size(sizePx, sizePx),
                    style = Stroke(width = strokeW),
                )
            }
        }

        // 3. Close (✕)
        WindowsCaptionButton(
            label = "Close",
            isClose = true,
            onClick = actions.close,
        ) { glyphColor ->
            val half = 4.5.dp.toPx()
            val strokeW = 1.dp.toPx()
            drawLine(
                color = glyphColor,
                start = Offset(center.x - half, center.y - half),
                end = Offset(center.x + half, center.y + half),
                strokeWidth = strokeW,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = glyphColor,
                start = Offset(center.x + half, center.y - half),
                end = Offset(center.x - half, center.y + half),
                strokeWidth = strokeW,
                cap = StrokeCap.Square,
            )
        }
    }
}

@Composable
private fun WindowsCaptionButton(
    label: String,
    isClose: Boolean,
    onClick: () -> Unit,
    glyph: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bg = when {
        hovered && isClose -> WIN_CLOSE_HOVER_BG
        hovered -> WIN_HOVER_BG
        else -> Color.Transparent
    }

    val glyphColor = when {
        hovered && isClose -> Color.White
        hovered -> Color.White
        else -> WIN_NORMAL_GLYPH
    }

    Box(
        Modifier
            .width(WIN_BUTTON_WIDTH)
            .height(CAPTION_HEIGHT)
            .background(bg)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Default)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .drawBehind {
                glyph(glyphColor)
            },
        contentAlignment = Alignment.Center,
    ) {}
}

/** Standard Windows caption button metrics */
private val WIN_BUTTON_WIDTH = 46.dp
private val CAPTION_HEIGHT = 32.dp

private val WIN_NORMAL_GLYPH = Color(0xFFCCCCCC)
private val WIN_HOVER_BG = Color(0x1FFFFFFF)
private val WIN_CLOSE_HOVER_BG = Color(0xFFE81123)

/**
 * A press on empty caption: moves the window, and a second one in quick succession maximizes it.
 */
private fun Modifier.captionPress(scope: WindowScope, actions: DesktopWindowActions?): Modifier =
    pointerInput(scope, actions) {
        var lastPressAt = 0L
        var lastPressPosition = Offset.Zero
        awaitEachGesture {
            val down = awaitFirstDown()
            val double = down.uptimeMillis - lastPressAt <= viewConfiguration.doubleTapTimeoutMillis &&
                (down.position - lastPressPosition).getDistance() <= viewConfiguration.touchSlop
            if (double) {
                lastPressAt = 0L
                down.consume()
                actions?.toggleMaximize()
                return@awaitEachGesture
            }
            lastPressAt = down.uptimeMillis
            lastPressPosition = down.position
            if (DesktopWindowsFrame.startDrag()) return@awaitEachGesture
            val window = scope.window
            val windowAtStart = window.location
            val pointerAtStart = MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
            drag(down.id) {
                val pointer = MouseInfo.getPointerInfo()?.location ?: return@drag
                window.setLocation(
                    windowAtStart.x + pointer.x - pointerAtStart.x,
                    windowAtStart.y + pointer.y - pointerAtStart.y,
                )
            }
        }
    }
