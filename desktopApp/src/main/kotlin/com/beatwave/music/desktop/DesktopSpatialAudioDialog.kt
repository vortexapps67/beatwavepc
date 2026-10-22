package com.beatwave.music.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 8D Spatial Audio & 7.1 Virtual Surround Sound Lab Dialog
 */
@Composable
internal fun DesktopSpatialAudioDialog(
    mode: SpatialAudioMode,
    onModeChange: (SpatialAudioMode) -> Unit,
    eightDSpeedHz: Float,
    onEightDSpeedChange: (Float) -> Unit,
    eightDDepth: Float,
    onEightDDepthChange: (Float) -> Unit,
    surroundRoom: SurroundRoomPreset,
    onSurroundRoomChange: (SurroundRoomPreset) -> Unit,
    bassBoostEnabled: Boolean,
    onBassBoostChange: (Boolean) -> Unit,
    telemetryProvider: () -> SpatialAudioTelemetry,
    onDismissRequest: () -> Unit,
) {
    // 60fps telemetry polling
    var telemetry by remember { mutableStateOf(telemetryProvider()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                telemetry = telemetryProvider()
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .width(540.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141416).copy(alpha = 0.96f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                .shadow(32.dp, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Spatial Acoustics Lab",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "360° Binaural Orbit & 7.1 Virtual Surround",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    DesktopToolbarButton(
                        onClick = onDismissRequest,
                        size = 32.dp,
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Mode Selector Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                ) {
                    SpatialAudioMode.entries.forEach { entry ->
                        val selected = entry == mode
                        val bg = if (selected) {
                            Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(bg)
                                .clickable { onModeChange(entry) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = when (entry) {
                                    SpatialAudioMode.OFF -> "Off (Stereo)"
                                    SpatialAudioMode.EIGHT_D -> "8D Orbit"
                                    SpatialAudioMode.SURROUND_7_1 -> "7.1 Surround"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Interactive 3D Radar Visualizer
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF1E212B), Color(0xFF0F1116)),
                                radius = 380f,
                            )
                        )
                        .border(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SpatialRadarCanvas(
                        mode = mode,
                        telemetry = telemetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Real-time HUD Status readout
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (mode) {
                        SpatialAudioMode.OFF -> {
                            Text(
                                "STANDARD STEREO PASSTHROUGH",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                        SpatialAudioMode.EIGHT_D -> {
                            val az = telemetry.azimuthDegrees.toInt()
                            val lapSec = if (eightDSpeedHz > 0f) 1f / eightDSpeedHz else 8f
                            val hemi = if (telemetry.isBehind) "REAR (HEAD SHADOW)" else "FRONT"
                            Text(
                                "AZIMUTH: $az°",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "LAP: ${String.format("%.1f", lapSec)}s",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                            )
                            Text(
                                hemi,
                                color = if (telemetry.isBehind) Color(0xFFF59E0B) else Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        SpatialAudioMode.SURROUND_7_1 -> {
                            Text(
                                "8-SPEAKER BINAURAL UPMIX",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "ROOM: ${surroundRoom.name}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Customization Controls for 8D Mode
                AnimatedVisibility(
                    visible = mode == SpatialAudioMode.EIGHT_D,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        val lapSec = if (eightDSpeedHz > 0f) 1f / eightDSpeedHz else 8f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Rotation Speed", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${String.format("%.1f", lapSec)}s / lap (${String.format("%.2f", eightDSpeedHz)} Hz)",
                                color = Color(0xFF60A5FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = lapSec,
                            onValueChange = { s -> onEightDSpeedChange(1f / s.coerceIn(2.5f, 20f)) },
                            valueRange = 2.5f..20f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                            ),
                        )

                        // Quick Speed Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                "Hyper" to 2.5f,
                                "Fast" to 4.5f,
                                "Standard" to 8.0f,
                                "Chill" to 14.0f,
                                "Cosmic" to 20.0f,
                            ).forEach { (label, sec) ->
                                val selected = (lapSec - sec).let { if (it < 0) -it else it } < 0.6f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (selected) Color(0xFF3B82F6) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { onEightDSpeedChange(1f / sec) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Orbit Depth
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Orbit Width / Depth", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${(eightDDepth * 100).roundToInt()}%",
                                color = Color(0xFF60A5FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = eightDDepth,
                            onValueChange = onEightDDepthChange,
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B82F6),
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }

                // Customization Controls for 7.1 Virtual Surround Mode
                AnimatedVisibility(
                    visible = mode == SpatialAudioMode.SURROUND_7_1,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            "Room Acoustic Space",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SurroundRoomPreset.entries.forEach { preset ->
                                val selected = preset == surroundRoom
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) Color(0xFF3B82F6).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (selected) Color(0xFF3B82F6) else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { onSurroundRoomChange(preset) }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            preset.label.substringBefore(" ("),
                                            color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                        Text(
                                            "${preset.earlyDelayMs.toInt()}ms delay",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Headphone Bass Boost
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Headphone Bass Enhancement",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "+4.5dB Sub-Bass @ 80Hz & Harmonic Warmth",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                )
                            }
                            Switch(
                                checked = bassBoostEnabled,
                                onCheckedChange = onBassBoostChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Radar visualizer Canvas drawing listener, range rings, and acoustic source nodes.
 */
@Composable
private fun SpatialRadarCanvas(
    mode: SpatialAudioMode,
    telemetry: SpatialAudioTelemetry,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = (min(size.width, size.height) / 2f) - 22.dp.toPx()

        // 1. Concentric Range Rings
        listOf(0.33f, 0.66f, 1.0f).forEach { fraction ->
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = maxRadius * fraction,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // 2. Crosshair Axis Lines
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 1.dp.toPx(),
        )

        // 3. Subtle rotating radar beam
        if (mode != SpatialAudioMode.OFF) {
            val sweepRad = Math.toRadians((radarSweepAngle - 90.0))
            val beamX = centerX + (maxRadius * cos(sweepRad)).toFloat()
            val beamY = centerY + (maxRadius * sin(sweepRad)).toFloat()
            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = maxRadius,
                ),
                start = Offset(centerX, centerY),
                end = Offset(beamX, beamY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 4. Central Listener [YOU]
        drawListenerNode(centerX, centerY)

        // 5. Dynamic Mode Overlays
        when (mode) {
            SpatialAudioMode.OFF -> {
                // Static stereo indicators Left and Right
                drawSpeakerGlyph(centerX - (maxRadius * 0.7f), centerY, "L", 0.5f, Color(0xFF60A5FA))
                drawSpeakerGlyph(centerX + (maxRadius * 0.7f), centerY, "R", 0.5f, Color(0xFF60A5FA))
            }
            SpatialAudioMode.EIGHT_D -> {
                // Orbital Path
                val orbitRadius = (maxRadius * telemetry.orbitRadius.coerceIn(0.2f, 1.0f))
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.20f),
                    radius = orbitRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // Current Orbit Angle (0° = 12 o'clock / Front)
                val angleRad = Math.toRadians(telemetry.azimuthDegrees.toDouble() - 90.0)
                val nodeX = centerX + (orbitRadius * cos(angleRad)).toFloat()
                val nodeY = centerY + (orbitRadius * sin(angleRad)).toFloat()

                // Ray from center to orbiting node
                drawLine(
                    color = if (telemetry.isBehind) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFF60A5FA).copy(alpha = 0.4f),
                    start = Offset(centerX, centerY),
                    end = Offset(nodeX, nodeY),
                    strokeWidth = 1.dp.toPx(),
                )

                // Outer acoustic wavefront aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (telemetry.isBehind) {
                            listOf(Color(0xFFF59E0B).copy(alpha = 0.5f), Color(0xFFE11D48).copy(alpha = 0.2f), Color.Transparent)
                        } else {
                            listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF3B82F6).copy(alpha = 0.25f), Color.Transparent)
                        },
                        center = Offset(nodeX, nodeY),
                        radius = 24.dp.toPx(),
                    ),
                    radius = 24.dp.toPx(),
                    center = Offset(nodeX, nodeY),
                )

                // Core sound node
                drawCircle(
                    color = if (telemetry.isBehind) Color(0xFFF59E0B) else Color(0xFF00E5FF),
                    radius = 7.dp.toPx(),
                    center = Offset(nodeX, nodeY),
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(nodeX, nodeY),
                )
            }
            SpatialAudioMode.SURROUND_7_1 -> {
                // 8 Fixed Virtual Speakers around perimeter
                // 0: FL (-30°, 1.5m)
                // 1: FC (0°, 1.4m)
                // 2: FR (+30°, 1.5m)
                // 3: SL (-90°, 1.2m)
                // 4: SR (+90°, 1.2m)
                // 5: RL (-135°, 1.8m)
                // 6: RR (+135°, 1.8m)
                // 7: SUB (LFE)
                val energies = telemetry.speakerEnergies

                val speakers = listOf(
                    Triple(1, 0.0, "FC"),       // 0° (Top)
                    Triple(0, -30.0, "FL"),     // -30° (Top Left)
                    Triple(2, 30.0, "FR"),      // +30° (Top Right)
                    Triple(3, -90.0, "SL"),     // -90° (Left)
                    Triple(4, 90.0, "SR"),      // +90° (Right)
                    Triple(5, -135.0, "RL"),    // -135° (Bottom Left)
                    Triple(6, 135.0, "RR"),     // +135° (Bottom Right)
                    Triple(7, 180.0, "SUB"),    // 180° (Bottom Center)
                )

                speakers.forEach { (idx, deg, label) ->
                    val rad = Math.toRadians(deg - 90.0)
                    val energy = if (idx < energies.size) energies[idx] else 0.3f
                    val spkRadius = if (label == "SUB") maxRadius * 0.72f else maxRadius * 0.94f
                    val sx = centerX + (spkRadius * cos(rad)).toFloat()
                    val sy = centerY + (spkRadius * sin(rad)).toFloat()

                    // Pulse wave towards center if active
                    if (energy > 0.15f) {
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = energy.coerceIn(0.1f, 0.6f)),
                            start = Offset(sx, sy),
                            end = Offset(
                                sx + (centerX - sx) * 0.35f,
                                sy + (centerY - sy) * 0.35f,
                            ),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }

                    val color = if (label == "SUB") Color(0xFF8B5CF6) else Color(0xFF38BDF8)
                    drawSpeakerGlyph(sx, sy, label, energy, color)
                }
            }
        }
    }
}

/**
 * Draws the central listener node [YOU] with headphones.
 */
private fun DrawScope.drawListenerNode(centerX: Float, centerY: Float) {
    val headRadius = 14.dp.toPx()

    // Outer subtle listener halo
    drawCircle(
        color = Color(0xFF3B82F6).copy(alpha = 0.12f),
        radius = headRadius * 1.6f,
        center = Offset(centerX, centerY),
    )

    // Head base
    drawCircle(
        color = Color(0xFF1E222D),
        radius = headRadius,
        center = Offset(centerX, centerY),
    )
    drawCircle(
        color = Color(0xFF3B82F6),
        radius = headRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.dp.toPx()),
    )

    // Nose direction indicator (Facing UP / Front)
    val nosePath = Path().apply {
        moveTo(centerX, centerY - headRadius - 4.dp.toPx())
        lineTo(centerX - 3.dp.toPx(), centerY - headRadius + 1.dp.toPx())
        lineTo(centerX + 3.dp.toPx(), centerY - headRadius + 1.dp.toPx())
        close()
    }
    drawPath(nosePath, color = Color(0xFF60A5FA))

    // Headphones band & ear-cups
    val cupWidth = 4.dp.toPx()
    val cupHeight = 10.dp.toPx()
    // Left ear cup
    drawRoundRect(
        color = Color(0xFF60A5FA),
        topLeft = Offset(centerX - headRadius - cupWidth + 1.dp.toPx(), centerY - cupHeight / 2f),
        size = Size(cupWidth, cupHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
    )
    // Right ear cup
    drawRoundRect(
        color = Color(0xFF60A5FA),
        topLeft = Offset(centerX + headRadius - 1.dp.toPx(), centerY - cupHeight / 2f),
        size = Size(cupWidth, cupHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
    )
}

/**
 * Draws an individual virtual speaker glyph with dynamic energy halo.
 */
private fun DrawScope.drawSpeakerGlyph(
    x: Float,
    y: Float,
    label: String,
    energy: Float,
    accentColor: Color,
) {
    val baseRadius = 11.dp.toPx()
    val glowRadius = baseRadius + (energy * 10.dp.toPx())

    // Glowing aura proportional to audio energy
    if (energy > 0.08f) {
        drawCircle(
            color = accentColor.copy(alpha = (energy * 0.45f).coerceIn(0f, 0.45f)),
            radius = glowRadius,
            center = Offset(x, y),
        )
    }

    // Speaker enclosure
    drawCircle(
        color = Color(0xFF181B22),
        radius = baseRadius,
        center = Offset(x, y),
    )
    drawCircle(
        color = accentColor.copy(alpha = (0.4f + energy * 0.6f).coerceIn(0.4f, 1.0f)),
        radius = baseRadius,
        center = Offset(x, y),
        style = Stroke(width = 1.5.dp.toPx()),
    )

    // Center speaker cone
    drawCircle(
        color = if (energy > 0.3f) Color.White else accentColor,
        radius = 3.dp.toPx() + (energy * 2.dp.toPx()),
        center = Offset(x, y),
    )
}
