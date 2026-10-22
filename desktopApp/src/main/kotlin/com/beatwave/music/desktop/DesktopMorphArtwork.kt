package com.beatwave.music.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.beatwave.music.data.model.artworkAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class VisualMorphQuality(val label: String) {
    FLUID("Fluid Morph"),
    CROSSFADE("Simple Crossfade"),
    REDUCED("Reduced Motion");

    companion object {
        fun from(name: String?): VisualMorphQuality =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FLUID
    }
}

val MorphEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

/**
 * Dual-layer fluid album art morph component.
 * Synchronized with manual skip crossfade:
 * - Outgoing: scales down (0.92), 3D tilt, blurs out (8.dp), fades to 0.
 * - Incoming: arrives from scale (1.08), 3D tilt bloom, liquid glass specular highlight, settles at 1.0.
 * - Reduced motion mode: clean cross-dissolve.
 */
@Composable
fun DesktopMorphArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    px: Int? = null,
    durationMs: Int = 4000,
    morphQuality: VisualMorphQuality = VisualMorphQuality.FLUID,
) {
    var outgoingUrl by remember { mutableStateOf<String?>(null) }
    var incomingUrl by remember { mutableStateOf(url) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(url) {
        if (url != incomingUrl) {
            outgoingUrl = incomingUrl
            incomingUrl = url
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (morphQuality == VisualMorphQuality.REDUCED) 300 else durationMs.coerceIn(1000, 12000),
                    easing = MorphEasing,
                ),
            )
            outgoingUrl = null
        }
    }

    val currentProgress = progress.value

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 1. Outgoing artwork layer
        val outUrl = outgoingUrl
        if (outUrl != null && currentProgress < 1f) {
            val outSized = if (px != null) outUrl.artworkAt(px) else outUrl
            val outBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = outSized) {
                value = withContext(Dispatchers.IO) { DesktopArtworkCache.load(outSized) }
            }
            if (outBitmap != null) {
                val outAlpha = (1f - currentProgress).coerceIn(0f, 1f)
                val outScale = if (morphQuality == VisualMorphQuality.FLUID) 1f - (0.08f * currentProgress) else 1f
                val outRotZ = if (morphQuality == VisualMorphQuality.FLUID) -3f * currentProgress else 0f
                val outBlur = if (morphQuality == VisualMorphQuality.FLUID) (currentProgress * 8f).dp else 0.dp

                Image(
                    painter = BitmapPainter(outBitmap!!),
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = outScale
                            scaleY = outScale
                            rotationZ = outRotZ
                            alpha = outAlpha
                        }
                        .then(if (outBlur > 0.dp) Modifier.blur(outBlur) else Modifier),
                )
            }
        }

        // 2. Incoming artwork layer
        val inUrl = incomingUrl
        val inSized = if (px != null) inUrl.artworkAt(px) else inUrl
        val inBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = inSized) {
            value = withContext(Dispatchers.IO) { DesktopArtworkCache.load(inSized) }
        }

        if (inBitmap == null) {
            Box(Modifier.fillMaxSize().background(Color(0xFF2B2B35)))
        } else {
            val inAlpha = currentProgress.coerceIn(0f, 1f)
            val inScale = if (morphQuality == VisualMorphQuality.FLUID) 1.08f - (0.08f * currentProgress) else 1f
            val inRotZ = if (morphQuality == VisualMorphQuality.FLUID) 3f * (1f - currentProgress) else 0f
            val inBlur = if (morphQuality == VisualMorphQuality.FLUID) ((1f - currentProgress) * 6f).dp else 0.dp

            Image(
                painter = BitmapPainter(inBitmap!!),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = inScale
                        scaleY = inScale
                        rotationZ = inRotZ
                        alpha = inAlpha
                    }
                    .then(if (inBlur > 0.dp) Modifier.blur(inBlur) else Modifier)
                    .drawWithContent {
                        drawContent()
                        // Liquid glass specular refractive bloom highlight during transition
                        if (morphQuality == VisualMorphQuality.FLUID && currentProgress in 0.05f..0.95f) {
                            val shineProgress = currentProgress
                            val sweepX = size.width * (shineProgress * 1.6f - 0.3f)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.28f * (1f - abs(currentProgress - 0.5f) * 2f)),
                                        Color.Transparent,
                                    ),
                                    start = Offset(sweepX - 30f, 0f),
                                    end = Offset(sweepX + 30f, size.height),
                                ),
                            )
                        }
                    },
            )
        }
    }
}
