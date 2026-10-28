package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceConnectionState
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPairingDialog
import com.example.ui.components.GlassStatusPill
import com.example.ui.components.GlassWarningBanner
import com.example.ui.components.StatusType
import com.example.ui.theme.ConnectedStatus
import com.example.ui.theme.DarkText
import com.example.ui.theme.ErrorStatus
import com.example.ui.theme.ErrorStatusBg
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
import com.example.ui.theme.WarningStatusBg
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RemoteViewModel

@Composable
fun DeviceConnectionScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier
) {
    val device = viewModel.activeDevice.collectAsState().value
    val connectionState by viewModel.connectionState.collectAsState()
    val isPairingDialogVisible by viewModel.isPairingDialogVisible.collectAsState()
    val pairingInProgress by viewModel.pairingInProgress.collectAsState()
    val pairingErrorMessage by viewModel.pairingErrorMessage.collectAsState()

    val isConnected = connectionState is DeviceConnectionState.Connected
    val isPairingRequired = connectionState is DeviceConnectionState.PairingRequired ||
            (device?.protocol == RemoteProtocol.ANDROID_TV_REMOTE && !isConnected)

    // Collect capabilities: protocol-level capabilities + live-verified capabilities
    val protocolCommands = if (device != null) viewModel.getProtocolCommands(device.protocol) else emptySet()

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
                            modifier = Modifier.testTag("btn_back_to_discovery")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = DarkText
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Device Connection",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = DarkText
                        )
                    }
                }

                if (device == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No device selected", color = SecondaryText)
                    }
                    return@Scaffold
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TV DETAILS HERO CARD (Frosted Glass)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 24.dp,
                        elevation = 4.dp,
                        borderWidth = 1.dp,
                        borderColor = if (isConnected) GlassBorderAccent else GlassBorderLight
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(PrimaryBlueLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = DarkText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${device.manufacturer} • ${device.protocol.displayName}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = SecondaryText
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GlassSurfaceSubtle,
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Text(
                                    text = "IP: ${device.addressLabel}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = DarkText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // CONNECTION STATUS CARD
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 20.dp,
                        elevation = 2.dp,
                        borderColor = GlassBorderLight
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connection State",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkText
                            )

                            when (val state = connectionState) {
                                is DeviceConnectionState.Connected -> {
                                    GlassStatusPill(
                                        label = "Connected (${state.pingMs}ms)",
                                        statusType = StatusType.CONNECTED
                                    )
                                }
                                is DeviceConnectionState.PairingRequired -> {
                                    GlassStatusPill(
                                        label = "Pairing Required",
                                        statusType = StatusType.PAIRING_REQUIRED
                                    )
                                }
                                is DeviceConnectionState.PairingInProgress -> {
                                    GlassStatusPill(
                                        label = "Pairing in Progress...",
                                        statusType = StatusType.CONNECTING
                                    )
                                }
                                is DeviceConnectionState.Connecting -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = PrimaryBlue,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verifying...", fontSize = 12.sp, color = PrimaryBlue)
                                    }
                                }
                                is DeviceConnectionState.ProtocolUnverified -> {
                                    GlassStatusPill(
                                        label = "Unverified Protocol",
                                        statusType = StatusType.WARNING
                                    )
                                }
                                is DeviceConnectionState.ConnectionFailed -> {
                                    GlassStatusPill(
                                        label = "Connection Failed",
                                        statusType = StatusType.FAILED
                                    )
                                }
                                is DeviceConnectionState.Offline -> {
                                    GlassStatusPill(
                                        label = "Offline",
                                        statusType = StatusType.OFFLINE
                                    )
                                }
                                is DeviceConnectionState.UnsupportedProtocol -> {
                                    GlassStatusPill(
                                        label = "Unsupported",
                                        statusType = StatusType.WARNING
                                    )
                                }
                                else -> {
                                    GlassStatusPill(
                                        label = "Not Connected",
                                        statusType = StatusType.DISCONNECTED
                                    )
                                }
                            }
                        }

                        // State explanations
                        when (val state = connectionState) {
                            is DeviceConnectionState.PairingRequired -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassWarningBanner(
                                    title = "Pairing Required",
                                    message = state.instructions,
                                    actionLabel = "Pair Now",
                                    onAction = { viewModel.startPairing(device) },
                                    statusType = StatusType.PAIRING_REQUIRED
                                )
                            }
                            is DeviceConnectionState.PairingInProgress -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PrimaryBlueLight,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = PrimaryBlue,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = state.stepDescription,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                            }
                            is DeviceConnectionState.ConnectionFailed -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassWarningBanner(
                                    title = "Connection Failed",
                                    message = state.reason,
                                    actionLabel = "Retry",
                                    onAction = { viewModel.connectToDevice(device) },
                                    statusType = StatusType.FAILED
                                )
                            }
                            is DeviceConnectionState.ProtocolUnverified -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassWarningBanner(
                                    title = "Protocol Not Verified",
                                    message = state.reason,
                                    actionLabel = "Retry",
                                    onAction = { viewModel.connectToDevice(device) },
                                    statusType = StatusType.WARNING
                                )
                            }
                            else -> {}
                        }
                    }

                    // PRIMARY ACTIONS SECTION
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. PROMINENT "PAIR DEVICE" BUTTON (when pairing is required)
                        if (isPairingRequired) {
                            GlassButton(
                                text = "Pair Device (Enter TV PIN)",
                                onClick = { viewModel.startPairing(device) },
                                icon = Icons.Default.Lock,
                                isPrimary = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_pair_device")
                            )
                        }

                        // 2. OPEN REMOTE CONTROL BUTTON
                        GlassButton(
                            text = if (isConnected) "Open Remote Control" else "Open Remote (Preview)",
                            onClick = { viewModel.navigateTo(AppScreen.REMOTE) },
                            icon = Icons.Default.SettingsRemote,
                            isPrimary = isConnected,
                            enabled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_open_remote")
                        )

                        if (!isConnected) {
                            Text(
                                text = if (isPairingRequired)
                                    "Note: Android TV requires pairing before navigation commands can be sent."
                                else
                                    "Note: Connect to TV to enable sending commands.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SecondaryText,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // SECONDARY UTILITIES (Test Ping & Save TV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.connectToDevice(device) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_retest_connection")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ping", fontSize = 12.sp, color = DarkText, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (device.isSaved) {
                                    viewModel.removeSavedDevice(device)
                                } else {
                                    viewModel.addManualDevice(device.ipAddress, device.port, device.name, device.protocol)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_toggle_save")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (device.isSaved) Color(0xFFF59E0B) else SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (device.isSaved) "Saved" else "Save TV",
                                fontSize = 12.sp,
                                color = DarkText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // PROTOCOL CAPABILITIES SECTION (Clear Explanation of Pending vs Verified)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 20.dp,
                        elevation = 2.dp,
                        borderColor = GlassBorderLight
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Protocol Capabilities",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DarkText
                            )

                            Text(
                                text = if (isConnected) "Active & Verified" else "Pending Verification",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = if (isConnected) ConnectedStatus else WarningStatus
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val capabilityList = listOf(
                            RemoteCommand.POWER to "Power On / Standby",
                            RemoteCommand.UP to "Directional D-Pad & Center OK",
                            RemoteCommand.VOLUME_UP to "Volume Up, Down & Mute",
                            RemoteCommand.HOME to "Home Launcher",
                            RemoteCommand.BACK to "Back Navigation",
                            RemoteCommand.MENU to "Context Options Menu",
                            RemoteCommand.KEYBOARD to "Direct WiFi Text Input",
                            RemoteCommand.APP_YOUTUBE to "Direct App Launcher (YouTube)"
                        )

                        capabilityList.forEach { (cmd, label) ->
                            val isSupportedByProtocol = protocolCommands.contains(cmd)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSupportedByProtocol) FontWeight.Medium else FontWeight.Normal
                                    ),
                                    color = if (isSupportedByProtocol) DarkText else SecondaryText
                                )

                                when {
                                    isSupportedByProtocol && isConnected -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Verified",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = ConnectedStatus
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Verified",
                                                tint = ConnectedStatus,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    isSupportedByProtocol && !isConnected -> {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = WarningStatusBg,
                                            border = BorderStroke(0.5.dp, WarningStatus.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = if (isPairingRequired) "Pending Pairing" else "Pending Connect",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = WarningStatus,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "Not supported",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // PAIRING PIN DIALOG MODAL
            if (isPairingDialogVisible && device != null) {
                GlassPairingDialog(
                    deviceName = device.name,
                    deviceIp = device.ipAddress,
                    isLoading = pairingInProgress,
                    errorMessage = pairingErrorMessage,
                    onPinSubmit = { pin -> viewModel.submitPairingPin(pin) },
                    onDismiss = { viewModel.dismissPairingDialog() }
                )
            }
        }
    }
}
