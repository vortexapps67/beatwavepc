package com.beatwave.music.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatwave.music.desktop.resources.Res
import com.beatwave.music.desktop.resources.upi_support
import com.beatwave.music.desktop.resources.vortex_logo
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private const val UPI_ID = "akshanshsinha67@axl"
private const val INSTAGRAM_URL = "https://instagram.com/vortex.apps"
private const val GITHUB_URL = "https://github.com/vortexapps"
private const val ANDROID_APP_URL = "https://beatwave.de5.net"

/**
 * About Page featuring Vortex Apps developer credits, links, and UPI support.
 * Styled in the futuristic dark/liquid-glass aesthetic of beatwave.de5.net.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopAboutPage(
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copiedUpi by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .lenisSmoothScroll(scrollState),
    ) {
        // Ambient background glow (Vortex cyan & purple)
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF7B2CBF).copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.15f),
                            radius = size.width * 0.5f,
                        ),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00B4D8).copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.35f),
                            radius = size.width * 0.45f,
                        ),
                    )
                },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero Card: Vortex Apps & BeatWave PC
            Box(
                Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF14141A).copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(32.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Vortex Logo in glowing rounded container
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A24))
                            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00B4D8), Color(0xFF7B2CBF))), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.vortex_logo),
                            contentDescription = "Vortex Apps",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "BeatWave PC",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Crafted with passion by Vortex Apps",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(10.dp))

                    // Badges
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        AboutPill(text = "v3.0.0 Release", color = Color(0xFF7B2CBF))
                        AboutPill(text = "100% Ad-Free", color = Color(0xFF2EC4B6))
                        AboutPill(text = "Hi-Res Audio Engine", color = Color(0xFFE76F51))
                        AboutPill(text = "Dual-Deck Crossfade", color = Color(0xFF00B4D8))
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "BeatWave is an ad-free, high-fidelity music player experience designed for audiophiles and music lovers. No subscriptions, no telemetry, no compromises—just pure music.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.widthIn(max = 620.dp),
                    )

                    Spacer(Modifier.height(24.dp))

                    // Social & Project Links
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SocialLinkButton(
                            label = "Instagram",
                            subtitle = "@vortex.apps",
                            icon = Icons.Rounded.Language,
                            onClick = { openUrl(INSTAGRAM_URL) },
                        )
                        SocialLinkButton(
                            label = "GitHub",
                            subtitle = "vortexapps",
                            icon = Icons.Rounded.Security,
                            onClick = { openUrl(GITHUB_URL) },
                        )
                        SocialLinkButton(
                            label = "Website",
                            subtitle = "beatwave.de5.net",
                            icon = Icons.Rounded.OpenInNew,
                            onClick = { openUrl(ANDROID_APP_URL) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Two-column layout for Android App Download and Support/UPI
            Row(
                Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Column 1: Android App Download Banner
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14141A).copy(alpha = 0.82f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00B4D8).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF00B4D8),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "BeatWave for Android",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Mobile companion app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DesktopSecondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Take BeatWave on the go with our Liquid Glass Android app, featuring real backdrop blur, synchronized lyrics, offline downloads, and zero ad breaks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            lineHeight = 20.sp,
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { openUrl(ANDROID_APP_URL) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00B4D8),
                                contentColor = Color.Black,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download APK (beatwave.de5.net)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Column 2: UPI Support & Donations
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14141A).copy(alpha = 0.82f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE76F51).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE76F51),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Support Development",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Keep BeatWave free & open-source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DesktopSecondary,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // QR Code Image
                        Box(
                            Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.upi_support),
                                contentDescription = "UPI QR Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // UPI ID Copy Row
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(UPI_ID))
                                    copiedUpi = true
                                    scope.launch {
                                        delay(2500)
                                        copiedUpi = false
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("UPI ID", fontSize = 10.sp, color = DesktopSecondary, fontWeight = FontWeight.Bold)
                                    Text(UPI_ID, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (copiedUpi) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy UPI ID",
                                        tint = if (copiedUpi) Color(0xFF2EC4B6) else DesktopSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (copiedUpi) "Copied!" else "Copy",
                                        fontSize = 12.sp,
                                        color = if (copiedUpi) Color(0xFF2EC4B6) else DesktopSecondary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Footer note
            Text(
                text = "BeatWave PC is a non-profit open-source project under the GPL-3.0 License. All audio streams and metadata are provided by YouTube Music.",
                style = MaterialTheme.typography.bodySmall,
                color = DesktopSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }
    }
}

@Composable
private fun AboutPill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SocialLinkButton(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (hovered) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (hovered) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = DesktopSecondary, fontSize = 10.sp)
            }
        }
    }
}
