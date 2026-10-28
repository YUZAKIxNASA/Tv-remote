package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiscoveredDevice
import com.example.model.RemoteProtocol
import com.example.ui.components.DeviceCard
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.ConnectedStatus
import com.example.ui.theme.DarkText
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
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RemoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsState()
    val devices by viewModel.allDiscoveredDevices.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

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
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(PrimaryBlueLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "NASA REMOTE",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 16.sp
                                    ),
                                    color = DarkText
                                )
                                Text(
                                    text = "WiFi Universal Smart TV",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = SecondaryText
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier.testTag("btn_help")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Help & Troubleshooting",
                                    tint = SecondaryText
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(AppScreen.MORE_OPTIONS) },
                                modifier = Modifier.testTag("btn_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Options",
                                    tint = SecondaryText
                                )
                            }
                        }
                    }
                }

                // Scanning Progress bar
                AnimatedVisibility(visible = isScanning, enter = fadeIn(), exit = fadeOut()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue,
                        trackColor = PrimaryBlueLight
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO CARD
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shapeRadius = 24.dp,
                            elevation = 3.dp,
                            borderColor = GlassBorderLight
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Discover Smart TVs",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = DarkText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Connect both your Android device and TV to the same WiFi network.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = SecondaryText
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassButton(
                                        text = if (isScanning) "Scanning..." else "Scan WiFi",
                                        onClick = { viewModel.startScan() },
                                        icon = if (!isScanning) Icons.Default.Refresh else null,
                                        isPrimary = true,
                                        isLoading = isScanning,
                                        modifier = Modifier.weight(1.2f),
                                        testTag = "btn_scan_refresh"
                                    )

                                    OutlinedButton(
                                        onClick = { showManualDialog = true },
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, GlassBorderAccent),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("btn_add_manual")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Manual IP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                scanStatusMessage?.let { status ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isScanning) PrimaryBlue else SecondaryText
                                    )
                                }
                            }
                        }
                    }

                    // DETECTED DEVICES SECTION HEADER
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Detected TVs (${devices.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = DarkText
                            )
                            if (isScanning) {
                                Text(
                                    text = "Scanning mDNS & SSDP...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }

                    // DEVICES LIST OR EMPTY PLACEHOLDER
                    if (devices.isEmpty()) {
                        item {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shapeRadius = 20.dp,
                                elevation = 2.dp,
                                contentPadding = PaddingValues(28.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(PrimaryBlueLight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "No Smart TVs Found Yet",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = DarkText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Ensure your TV is powered on, connected to WiFi, or connect directly using 'Manual IP'.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = SecondaryText,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { showManualDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, GlassBorderAccent)
                                    ) {
                                        Text("Connect via Direct IP", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(devices, key = { "${it.ipAddress}:${it.port}" }) { device ->
                            DeviceCard(
                                device = device,
                                onConnect = { viewModel.selectDevice(device) }
                            )
                        }
                    }

                    // HELPFUL PROTOCOL CARD
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHelpDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            color = GlassSurfaceSubtle,
                            border = BorderStroke(1.dp, GlassBorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connection & Pairing Guide",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = DarkText
                                    )
                                    Text(
                                        text = "Android TV uses PIN pairing (port 6467). Xiaomi Mi TV uses HTTP (port 6095). Tap for tips.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = SecondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // MANUAL IP DIALOG (Glassmorphic)
    if (showManualDialog) {
        ManualIpDialog(
            onDismiss = { showManualDialog = false },
            onAdd = { ip, port, name, protocol ->
                viewModel.addManualDevice(ip, port, name, protocol)
                showManualDialog = false
            }
        )
    }

    // HELP DIALOG
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualIpDialog(
    onDismiss: () -> Unit,
    onAdd: (ip: String, port: Int, name: String, protocol: RemoteProtocol) -> Unit
) {
    var ip by remember { mutableStateOf("192.168.1.") }
    var portText by remember { mutableStateOf("6467") }
    var name by remember { mutableStateOf("Living Room TV") }
    var selectedProtocol by remember { mutableStateOf(RemoteProtocol.ANDROID_TV_REMOTE) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add TV via Direct IP", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your TV's local network IP address (check TV Settings > Network).",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = GlassBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manual_ip")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_manual_port")
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Device Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("input_manual_name")
                    )
                }

                // Protocol selection dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProtocol.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Protocol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        RemoteProtocol.entries.forEach { proto ->
                            DropdownMenuItem(
                                text = { Text(proto.displayName) },
                                onClick = {
                                    selectedProtocol = proto
                                    portText = proto.defaultPort.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: selectedProtocol.defaultPort
                    if (ip.isNotBlank()) {
                        onAdd(ip.trim(), port, name.trim(), selectedProtocol)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("WiFi Remote Troubleshooting", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Android TV Remote (v2):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkText
                )
                Text(
                    text = "Android TV Remote uses a TLS PIN pairing handshake on ports 6466/6467. Tap 'Pair Device' and enter the 4 to 6 character code displayed on the TV.",
                    fontSize = 12.sp,
                    color = SecondaryText
                )

                Text(
                    text = "2. Xiaomi Mi TV (PatchWall HTTP):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkText
                )
                Text(
                    text = "Xiaomi TVs listen on port 6095. Direct key commands work immediately without a PIN over local HTTP.",
                    fontSize = 12.sp,
                    color = SecondaryText
                )

                Text(
                    text = "3. Same Local Network Required:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkText
                )
                Text(
                    text = "Ensure both devices are on the exact same WiFi network. AP Isolation or Guest WiFi must be disabled on your router.",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Understood")
            }
        }
    )
}
