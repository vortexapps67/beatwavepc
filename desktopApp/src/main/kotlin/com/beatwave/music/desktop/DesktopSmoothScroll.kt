package com.beatwave.music.desktop

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Lenis-inspired smooth scroll modifier for Compose Desktop mouse wheel scrolling.
 *
 * Characteristics:
 * 1. Bounded scroll per tick: Clamps delta so fast wheel flicks do not jump vast distances at once.
 * 2. Inertia blending: Accumulates input velocity smoothly rather than snapping.
 * 3. Fluid easing: Uses organic cubic-bezier deceleration for a luxury web/macOS-like scrolling feel.
 */
@Composable
internal fun Modifier.lenisSmoothScroll(
    state: ScrollableState,
    maxStepPx: Float = 120f,
    multiplier: Float = 42f,
    durationMs: Int = 240,
): Modifier {
    val scope = rememberCoroutineScope()
    val scrollAccumulator = remember { FloatArray(1) } // mutable float container
    val currentJob = remember { arrayOfNulls<Job>(1) }
    val lenisEasing = remember { CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) }

    return this.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val rawDeltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (abs(rawDeltaY) > 0.001f) {
                        event.changes.forEach { it.consume() }

                        // Bounded delta per tick ("not more scroll at a time")
                        val clampedTick = (sign(rawDeltaY) * minOf(abs(rawDeltaY), 2.2f)) * multiplier

                        // Blend into current accumulator
                        scrollAccumulator[0] = ((scrollAccumulator[0] * 0.35f) + clampedTick).coerceIn(-maxStepPx, maxStepPx)
                        val deltaToScroll = scrollAccumulator[0]

                        currentJob[0]?.cancel()
                        currentJob[0] = scope.launch {
                            state.animateScrollBy(
                                value = deltaToScroll,
                                animationSpec = tween(durationMillis = durationMs, easing = lenisEasing),
                            )
                            scrollAccumulator[0] = 0f
                        }
                    }
                }
            }
        }
    }
}
