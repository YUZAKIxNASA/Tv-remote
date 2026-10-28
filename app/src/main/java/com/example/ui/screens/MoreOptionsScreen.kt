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
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceConnectionState
import com.example.model.DiscoveredDevice
import com.example.ui.components.RemoteButton
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ConnectedStatus
import com.example.ui.theme.DarkText
import com.example.ui.theme.ErrorStatus
import com.example.ui.theme.PrimaryBg
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.SecondaryBg
import com.example.ui.theme.SecondaryText
import com.example.ui.viewmodel.AppScreen
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
        containerColor = PrimaryBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    text = "Options & Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = DarkText
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. DIRECT KEYBOARD TEXT SENDER
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Direct Keyboard Input",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Send query, username, or URL directly to TV screen",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { customText = it },
                                placeholder = { Text("Enter text to transmit...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor
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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }

                // 2. QUICK APPLICATION LAUNCHERS
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Smart TV Applications",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct launch on compatible Xiaomi & Android TV OS",
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
                                            modifier = Modifier.weight(1f),
                                            minHeight = 44.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. INPUT SOURCE SELECTION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Input Sources",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val sources = listOf("HDMI 1", "HDMI 2", "HDMI 3", "AV / RCA")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sources.forEach { src ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.sendText(src)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = src,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = DarkText
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. SAVED DEVICES MANAGEMENT
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Saved Devices (${savedDevices.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (savedDevices.isEmpty()) {
                            Text(
                                text = "No saved TVs yet. Connected devices are saved automatically.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = SecondaryText
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                savedDevices.forEach { sDevice ->
                                    val isCurrent = sDevice.ipAddress == device?.ipAddress && sDevice.port == device?.port
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isCurrent) PrimaryBlueLight else SecondaryBg, RoundedCornerShape(12.dp))
                                            .clickable { viewModel.selectDevice(sDevice) }
                                            .padding(12.dp),
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
                                            modifier = Modifier.size(32.dp)
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SecondaryBg,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            color = if (connectionState is DeviceConnectionState.Connected) ConnectedStatus else SecondaryText
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { device?.let { viewModel.connectToDevice(it) } },
                            enabled = device != null,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-test Latency", fontSize = 12.sp, color = DarkText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
