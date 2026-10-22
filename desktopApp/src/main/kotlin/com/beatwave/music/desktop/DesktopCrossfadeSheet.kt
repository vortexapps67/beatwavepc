package com.beatwave.music.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Quick Audio & Crossfade modal dialog accessible directly from the player.
 */
@Composable
internal fun DesktopAudioQuickDialog(
    manualCrossfade: Boolean,
    onManualCrossfadeChange: (Boolean) -> Unit,
    crossfadeSeconds: Float,
    onCrossfadeSecondsChange: (Float) -> Unit,
    visualMorphQuality: VisualMorphQuality,
    onVisualMorphQualityChange: (VisualMorphQuality) -> Unit,
    onOpenFullEqualizer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val eqEnabled by DesktopEqualizerSettings.enabled.collectAsState()
    val eqMode by DesktopEqualizerSettings.mode.collectAsState()

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 480) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DesktopAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = DesktopAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Audio & Crossfade",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "Instant sound and transition tuning",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesktopSecondary,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, "Close", tint = DesktopSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Section 1: Manual Skip Crossfade
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Manual Skip Crossfade",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Dual-deck equal-power blend when skipping tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = DesktopSecondary,
                            )
                        }
                        Switch(
                            checked = manualCrossfade,
                            onCheckedChange = onManualCrossfadeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DesktopAccent,
                                uncheckedThumbColor = DesktopSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                            ),
                        )
                    }

                    AnimatedVisibility(visible = manualCrossfade) {
                        Column(Modifier.padding(top = 16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Crossfade Duration",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                                Text(
                                    "${(crossfadeSeconds * 10).roundToInt() / 10f}s",
                                    fontWeight = FontWeight.Bold,
                                    color = DesktopAccent,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Slider(
                                value = crossfadeSeconds,
                                onValueChange = { onCrossfadeSecondsChange((it * 2).roundToInt() / 2f) },
                                valueRange = 1f..12f,
                                steps = 21,
                                colors = SliderDefaults.colors(
                                    thumbColor = DesktopAccent,
                                    activeTrackColor = DesktopAccent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                "Album Art Fluid Morph",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                VisualMorphQuality.values().forEach { quality ->
                                    val selected = quality == visualMorphQuality
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) DesktopAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, if (selected) DesktopAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { onVisualMorphQualityChange(quality) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = when (quality) {
                                                VisualMorphQuality.FLUID -> "Fluid Morph"
                                                VisualMorphQuality.CROSSFADE -> "Crossfade"
                                                VisualMorphQuality.REDUCED -> "Reduced"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) Color.White else DesktopSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Section 2: Equalizer Quick Tile
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Equalizer",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (eqEnabled) DesktopAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (eqEnabled) "ON" else "OFF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (eqEnabled) DesktopAccent else DesktopSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (eqEnabled) "Active curve: $eqMode" else "Shapes sound inside BeatWave PC",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesktopSecondary,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { DesktopEqualizerSettings.setEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DesktopAccent,
                                uncheckedThumbColor = DesktopSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                onDismiss()
                                onOpenFullEqualizer()
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Rounded.Tune, "Open Full Equalizer", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
