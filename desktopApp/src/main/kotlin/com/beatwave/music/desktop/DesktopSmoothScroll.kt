package com.beatwave.music.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
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
 * 1. Intercepts on [PointerEventPass.Initial] to consume the event and suppress default abrupt snapping.
 * 2. Clamps delta per wheel tick ("not more scroll at a time") to prevent jarring multi-page leaps.
 * 3. Smooth momentum accumulation and cubic-bezier interpolation (0.16, 1.0, 0.3, 1.0).
 */
@Composable
internal fun Modifier.lenisSmoothScroll(
    state: ScrollableState,
    maxStepPx: Float = 140f,
    multiplier: Float = 42f,
    durationMs: Int = 260,
): Modifier {
    val scope = rememberCoroutineScope()
    val scrollAccumulator = remember { FloatArray(1) }
    val currentJob = remember { arrayOfNulls<Job>(1) }
    val lenisEasing = remember { CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) }

    return this.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                // Intercept on Initial pass BEFORE any child or built-in scroll consumes it
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Scroll) {
                    val rawDeltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (abs(rawDeltaY) > 0.001f) {
                        event.changes.forEach { it.consume() }

                        // Bounded delta per tick ("not more scroll at a time")
                        val clampedTick = (sign(rawDeltaY) * minOf(abs(rawDeltaY), 1.6f)) * multiplier

                        // Blend into current accumulator
                        scrollAccumulator[0] = ((scrollAccumulator[0] * 0.35f) + clampedTick).coerceIn(-maxStepPx, maxStepPx)
                        val targetDelta = scrollAccumulator[0]

                        currentJob[0]?.cancel()
                        currentJob[0] = scope.launch {
                            state.scroll {
                                val anim = Animatable(0f)
                                var lastValue = 0f
                                anim.animateTo(
                                    targetValue = targetDelta,
                                    animationSpec = tween(durationMillis = durationMs, easing = lenisEasing),
                                ) {
                                    val delta = value - lastValue
                                    lastValue = value
                                    scrollBy(delta)
                                }
                            }
                            scrollAccumulator[0] = 0f
                        }
                    }
                }
            }
        }
    }
}
