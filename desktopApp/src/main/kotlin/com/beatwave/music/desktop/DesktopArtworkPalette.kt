package com.beatwave.music.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DesktopArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val breatheScale: Float = 1.0f,
) {
    companion object {
        val Default = DesktopArtworkPalette(Color(0xFFFF375F), Color(0xFF4E6DFF))
    }
}

@Composable
fun rememberDesktopArtworkPalette(url: String?): DesktopArtworkPalette {
    val palette by produceState(DesktopArtworkPalette.Default, key1 = url) {
        value = DesktopArtworkPalette.Default
        value = withContext(Dispatchers.IO) { DesktopArtworkPalette.from(DesktopArtworkCache.load(url)) }
    }
    return palette
}

/**
 * Ambient background glow palette that morphs from Track A's palette to Track B's palette
 * over the crossfade timeline, with a subtle bass-reactive breathing pulse.
 */
@Composable
fun rememberDesktopMorphPalette(
    url: String?,
    durationMs: Int = 4000,
    isPlaying: Boolean = true,
): DesktopArtworkPalette {
    val incoming = rememberDesktopArtworkPalette(url)
    var previousPalette by remember { mutableStateOf(incoming) }
    var currentPalette by remember { mutableStateOf(incoming) }
    val progress = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "ambientBreathe")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.035f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )

    LaunchedEffect(incoming) {
        if (incoming != currentPalette) {
            previousPalette = currentPalette
            currentPalette = incoming
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMs.coerceIn(1000, 12000), easing = MorphEasing),
            )
        }
    }

    val t = progress.value
    return remember(t, previousPalette, currentPalette, breathe) {
        DesktopArtworkPalette(
            primary = lerp(previousPalette.primary, currentPalette.primary, t),
            secondary = lerp(previousPalette.secondary, currentPalette.secondary, t),
            breatheScale = breathe,
        )
    }
}

private fun DesktopArtworkPalette.Companion.from(bitmap: ImageBitmap?): DesktopArtworkPalette {
    if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return DesktopArtworkPalette.Default
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.readPixels(pixels)
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L
    pixels.forEachIndexed { index, pixel ->
        if (index % 16 == 0) {
            red += pixel shr 16 and 0xFF
            green += pixel shr 8 and 0xFF
            blue += pixel and 0xFF
            count++
        }
    }
    if (count == 0L) return DesktopArtworkPalette.Default
    val average = Color(
        red = (red / count).toFloat() / 255f,
        green = (green / count).toFloat() / 255f,
        blue = (blue / count).toFloat() / 255f,
    )
    return DesktopArtworkPalette(
        primary = average.copy(alpha = 1f),
        secondary = Color(
            red = (average.red * 0.55f + 0.22f).coerceAtMost(1f),
            green = (average.green * 0.45f + 0.18f).coerceAtMost(1f),
            blue = (average.blue * 0.70f + 0.24f).coerceAtMost(1f),
        ),
    )
}
