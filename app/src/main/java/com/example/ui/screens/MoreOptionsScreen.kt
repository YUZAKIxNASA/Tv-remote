package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceConnectionState
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.RemoteButton
import com.example.ui.theme.ConnectedStatus
import com.example.ui.theme.DarkText
import com.example.ui.theme.ErrorStatus
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBackgroundBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderAccent
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.GlassSurfaceSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.WarningStatus
import com.example.ui.viewmodel.RemoteViewModel

@Composable
fun MoreOptionsScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier
) {
    val device = viewModel.activeDevice.collectAsState().value
    val savedDevices by viewModel.savedDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var customText by remember { mutableStateOf("") }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GlassBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GlassBackgroundBrush)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP BAR (Glassmorphism)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GlassSurfaceSubtle,
                    border = BorderStroke(1.dp, GlassBorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.testTag("btn_back_options")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Options & Diagnostics",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = DarkText
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. DIRECT KEYBOARD TEXT SENDER
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp,
                        elevation = 2.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Direct Keyboard Input",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Transmit queries, search strings or URLs directly to TV",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                placeholder = { Text("Enter text to transmit...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = GlassSurfaceSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_direct_text")
                            )

                            Button(
                                onClick = {
                                    if (customText.isNotBlank()) {
                                        viewModel.sendText(customText)
                                        customText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Send", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 2. QUICK APPLICATION LAUNCHERS
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp,
                        elevation = 2.dp
                    ) {
                        Text(
                            text = "Smart TV Applications",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct app launchers for Android TV & Xiaomi PatchWall",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val apps = listOf(
                            Triple("YouTube", "com.google.android.youtube.tv", Color(0xFFFF0000)),
                            Triple("Netflix", "com.netflix.ninja", Color(0xFFE50914)),
                            Triple("Prime Video", "com.amazon.amazonvideo.livingroom", Color(0xFF00A8E1)),
                            Triple("Spotify", "com.spotify.tv.android", Color(0xFF1DB954)),
                            Triple("Disney+", "com.disney.disneyplus", Color(0xFF113CCF)),
                            Triple("Twitch", "tv.twitch.android.app", Color(0xFF9146FF))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            apps.chunked(3).forEach { rowApps ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowApps.forEach { (name, pkg, color) ->
                                        RemoteButton(
                                            onClick = { viewModel.launchApp(pkg) },
                                            text = name,
                                            iconColor = color,
                                            backgroundColor = GlassSurfaceCard,
                                            borderColor = GlassBorderLight,
                                            modifier = Modifier.weight(1f),
                                            minHeight = 44.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. INPUT SOURCE SELECTION
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp,
                        elevation = 2.dp
                    ) {
                        Text(
                            text = "Input Sources",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val sources = listOf("HDMI 1", "HDMI 2", "HDMI 3", "AV / RCA")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sources.forEach { src ->
                                OutlinedButton(
                                    onClick = { viewModel.sendText(src) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = src,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = DarkText
                                    )
                                }
                            }
                        }
                    }

                    // 4. SAVED DEVICES MANAGEMENT
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp,
                        elevation = 2.dp
                    ) {
                        Text(
                            text = "Saved Smart TVs (${savedDevices.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (savedDevices.isEmpty()) {
                            Text(
                                text = "No saved TVs yet. Connected and paired TVs are saved automatically.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = SecondaryText
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                savedDevices.forEach { sDevice ->
                                    val isCurrent = sDevice.ipAddress == device?.ipAddress && sDevice.port == device?.port
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCurrent) PrimaryBlueLight else GlassSurfaceSubtle,
                                        border = BorderStroke(1.dp, if (isCurrent) GlassBorderAccent else GlassBorderLight),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectDevice(sDevice) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = sDevice.name,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = DarkText
                                                    )
                                                    if (isCurrent) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(ConnectedStatus, CircleShape)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${sDevice.addressLabel} • ${sDevice.protocol.displayName}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = SecondaryText
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeSavedDevice(sDevice) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove",
                                                    tint = ErrorStatus,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. NETWORK DIAGNOSTICS
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 22.dp,
                        elevation = 2.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Network Diagnostics",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val statusText = when (val state = connectionState) {
                            is DeviceConnectionState.Connected -> "Active (Ping ${state.pingMs}ms)"
                            is DeviceConnectionState.Connecting -> "Probing connection..."
                            is DeviceConnectionState.PairingRequired -> "Pairing Required (Unpaired)"
                            is DeviceConnectionState.ConnectionFailed -> "Failed: ${state.reason}"
                            else -> "Idle"
                        }

                        Text(
                            text = "Target: ${device?.addressLabel ?: "None"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = DarkText
                        )
                        Text(
                            text = "Status: $statusText",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (connectionState is DeviceConnectionState.Connected) ConnectedStatus else if (connectionState is DeviceConnectionState.PairingRequired) WarningStatus else SecondaryText
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { device?.let { viewModel.connectToDevice(it) } },
                            enabled = device != null,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-test Latency", fontSize = 12.sp, color = DarkText, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
