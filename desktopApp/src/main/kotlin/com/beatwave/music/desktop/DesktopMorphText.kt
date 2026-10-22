package com.beatwave.music.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Staggered vertical roll & blur text morph for Song Title and Artist.
 * Outgoing slides up by -16px with fade & blur.
 * Incoming slides in from +16px to 0 with fade & blur.
 */
@Composable
fun DesktopMorphText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
    staggerDelayMs: Int = 0,
    morphQuality: VisualMorphQuality = VisualMorphQuality.FLUID,
) {
    if (morphQuality == VisualMorphQuality.REDUCED) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            modifier = modifier,
        ) { targetText ->
            Text(
                text = targetText,
                color = color,
                style = style,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                textAlign = textAlign,
            )
        }
    } else {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                val enter = slideInVertically(
                    animationSpec = tween(450, delayMillis = staggerDelayMs, easing = MorphEasing),
                    initialOffsetY = { 24 },
                ) + fadeIn(tween(400, delayMillis = staggerDelayMs))

                val exit = slideOutVertically(
                    animationSpec = tween(350, easing = MorphEasing),
                    targetOffsetY = { -24 },
                ) + fadeOut(tween(300))

                enter togetherWith exit
            },
            modifier = modifier,
        ) { targetText ->
            Text(
                text = targetText,
                color = color,
                style = style,
                fontWeight = fontWeight,
                maxLines = maxLines,
                overflow = overflow,
                textAlign = textAlign,
            )
        }
    }
}
