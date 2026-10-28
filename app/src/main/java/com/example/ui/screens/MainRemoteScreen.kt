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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CommandResult
import com.example.model.DeviceConnectionState
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.ui.components.DpadControl
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPairingDialog
import com.example.ui.components.GlassStatusPill
import com.example.ui.components.GlassWarningBanner
import com.example.ui.components.RemoteButton
import com.example.ui.components.RockerControl
import com.example.ui.components.StatusType
import com.example.ui.theme.DarkText
import com.example.ui.theme.ErrorStatus
import com.example.ui.theme.ErrorStatusBg
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBackgroundBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderAccent
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.GlassSurfaceSubtle
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.SecondaryText
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RemoteViewModel

@Composable
fun MainRemoteScreen(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier
) {
    val device = viewModel.activeDevice.collectAsState().value
    val connectionState by viewModel.connectionState.collectAsState()
    val lastResult by viewModel.lastCommandResult.collectAsState()
    val isPairingDialogVisible by viewModel.isPairingDialogVisible.collectAsState()
    val pairingInProgress by viewModel.pairingInProgress.collectAsState()
    val pairingErrorMessage by viewModel.pairingErrorMessage.collectAsState()

    var showKeyboardDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    val isConnected = connectionState is DeviceConnectionState.Connected
    val isPairingRequired = connectionState is DeviceConnectionState.PairingRequired ||
            (device?.protocol == RemoteProtocol.ANDROID_TV_REMOTE && !isConnected)

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
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.navigateBack() },
                                modifier = Modifier.testTag("btn_back")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DarkText
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = device?.name ?: "NASA Remote",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = DarkText,
                                    maxLines = 1
                                )
                                Text(
                                    text = device?.addressLabel ?: "No TV Connected",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = SecondaryText
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GlassStatusPill(
                                label = if (isConnected) "Connected" else if (isPairingRequired) "Pairing Required" else "Offline",
                                statusType = if (isConnected) StatusType.CONNECTED else if (isPairingRequired) StatusType.PAIRING_REQUIRED else StatusType.OFFLINE
                            )
                            IconButton(
                                onClick = { viewModel.navigateTo(AppScreen.MORE_OPTIONS) },
                                modifier = Modifier.testTag("btn_more_options")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = DarkText
                                )
                            }
                        }
                    }
                }

                // WARNING BANNER WHEN NOT CONNECTED OR PAIRING REQUIRED
                if (!isConnected) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                        if (isPairingRequired) {
                            GlassWarningBanner(
                                title = "Pairing Required",
                                message = "Android TV requires a one-time PIN pairing handshake before commands can be sent.",
                                actionLabel = "Pair Now",
                                onAction = { viewModel.startPairing(device) },
                                statusType = StatusType.PAIRING_REQUIRED
                            )
                        } else {
                            GlassWarningBanner(
                                title = "TV Not Connected",
                                message = "Connect to TV to transmit navigation and power commands over WiFi.",
                                actionLabel = "Connect",
                                onAction = {
                                    if (device != null) {
                                        viewModel.connectToDevice(device)
                                    } else {
                                        viewModel.navigateTo(AppScreen.DISCOVERY)
                                    }
                                },
                                statusType = StatusType.WARNING
                            )
                        }
                    }
                }

                // FEEDBACK TOAST / BANNER
                AnimatedVisibility(
                    visible = lastResult != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    lastResult?.let { result ->
                        val isError = result is CommandResult.Error
                        val bannerBg = if (isError) ErrorStatusBg else PrimaryBlueLight
                        val textColor = if (isError) ErrorStatus else PrimaryBlue

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            color = bannerBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(textColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (result) {
                                        is CommandResult.Success -> "${result.message} (${result.latencyMs}ms)"
                                        is CommandResult.Error -> result.message
                                        is CommandResult.Unsupported -> result.message
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = textColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // REMOTE CONTROLLER BODY
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ROW 1: POWER, MUTE, SOURCE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Power Button (Red glass)
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.POWER) },
                            icon = Icons.Default.PowerSettingsNew,
                            text = "Power",
                            iconColor = Color(0xFFEF4444),
                            contentColor = Color(0xFFEF4444),
                            backgroundColor = Color(0xFFFEF2F2),
                            borderColor = Color(0xFFFECACA),
                            minWidth = 88.dp,
                            minHeight = 54.dp,
                            testTag = "btn_power"
                        )

                        // Mute Button
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.MUTE) },
                            icon = Icons.Default.VolumeOff,
                            text = "Mute",
                            iconColor = PrimaryBlue,
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            minWidth = 88.dp,
                            minHeight = 54.dp,
                            testTag = "btn_mute"
                        )

                        // Input Source Button
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.INPUT_SOURCE) },
                            icon = Icons.Default.Input,
                            text = "Source",
                            iconColor = DarkText,
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            minWidth = 88.dp,
                            minHeight = 54.dp,
                            testTag = "btn_source"
                        )
                    }

                    // DIRECTIONAL PAD (Large Glass D-Pad)
                    DpadControl(
                        onUp = { viewModel.sendCommand(RemoteCommand.UP) },
                        onDown = { viewModel.sendCommand(RemoteCommand.DOWN) },
                        onLeft = { viewModel.sendCommand(RemoteCommand.LEFT) },
                        onRight = { viewModel.sendCommand(RemoteCommand.RIGHT) },
                        onOk = { viewModel.sendCommand(RemoteCommand.OK) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // ROW 3: BACK, HOME, MENU
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.BACK) },
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            text = "Back",
                            iconColor = DarkText,
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            minWidth = 88.dp,
                            minHeight = 56.dp,
                            testTag = "btn_tv_back"
                        )

                        // Home (Prominent Frosted Blue)
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.HOME) },
                            icon = Icons.Default.Home,
                            text = "Home",
                            iconColor = PrimaryBlue,
                            backgroundColor = PrimaryBlueLight,
                            borderColor = GlassBorderAccent,
                            minWidth = 98.dp,
                            minHeight = 56.dp,
                            testTag = "btn_tv_home"
                        )

                        // Menu
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.MENU) },
                            icon = Icons.Default.Menu,
                            text = "Menu",
                            iconColor = DarkText,
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            minWidth = 88.dp,
                            minHeight = 56.dp,
                            testTag = "btn_tv_menu"
                        )
                    }

                    // ROW 4: DUAL ROCKERS (VOLUME & CHANNEL)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RockerControl(
                            title = "VOL",
                            icon = Icons.Default.VolumeUp,
                            onPlus = { viewModel.sendCommand(RemoteCommand.VOLUME_UP) },
                            onMinus = { viewModel.sendCommand(RemoteCommand.VOLUME_DOWN) },
                            plusTag = "btn_vol_up",
                            minusTag = "btn_vol_down"
                        )

                        RockerControl(
                            title = "CH",
                            icon = Icons.Default.Tv,
                            onPlus = { viewModel.sendCommand(RemoteCommand.CHANNEL_UP) },
                            onMinus = { viewModel.sendCommand(RemoteCommand.CHANNEL_DOWN) },
                            plusTag = "btn_ch_up",
                            minusTag = "btn_ch_down"
                        )
                    }

                    // ROW 5: MEDIA CONTROLS (Glass Card)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shapeRadius = 20.dp,
                        elevation = 2.dp,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.sendCommand(RemoteCommand.PREVIOUS) },
                                modifier = Modifier.size(48.dp).testTag("btn_media_prev")
                            ) {
                                Icon(Icons.Default.SkipPrevious, "Previous", tint = DarkText)
                            }

                            // Prominent Play/Pause Button
                            Button(
                                onClick = { viewModel.sendCommand(RemoteCommand.PLAY_PAUSE) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("btn_media_play_pause")
                            ) {
                                Icon(Icons.Default.PlayArrow, "Play/Pause", tint = Color.White)
                            }

                            IconButton(
                                onClick = { viewModel.sendCommand(RemoteCommand.STOP) },
                                modifier = Modifier.size(48.dp).testTag("btn_media_stop")
                            ) {
                                Icon(Icons.Default.Stop, "Stop", tint = DarkText)
                            }

                            IconButton(
                                onClick = { viewModel.sendCommand(RemoteCommand.NEXT) },
                                modifier = Modifier.size(48.dp).testTag("btn_media_next")
                            ) {
                                Icon(Icons.Default.SkipNext, "Next", tint = DarkText)
                            }
                        }
                    }

                    // ROW 6: QUICK APP SHORTCUTS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.APP_YOUTUBE) },
                            text = "YouTube",
                            iconColor = Color(0xFFFF0000),
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            modifier = Modifier.weight(1f),
                            minHeight = 46.dp,
                            testTag = "btn_app_youtube"
                        )

                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.APP_NETFLIX) },
                            text = "Netflix",
                            iconColor = Color(0xFFE50914),
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            modifier = Modifier.weight(1f),
                            minHeight = 46.dp,
                            testTag = "btn_app_netflix"
                        )

                        RemoteButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.APP_PRIME) },
                            text = "Prime",
                            iconColor = Color(0xFF00A8E1),
                            backgroundColor = GlassSurfaceCard,
                            borderColor = GlassBorderLight,
                            modifier = Modifier.weight(1f),
                            minHeight = 46.dp,
                            testTag = "btn_app_prime"
                        )
                    }

                    // ROW 7: TEXT / KEYBOARD INPUT
                    GlassButton(
                        text = "Type on TV (Keyboard)",
                        onClick = { showKeyboardDialog = true },
                        icon = Icons.Default.Keyboard,
                        isPrimary = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_keyboard")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // PAIRING PIN DIALOG (when triggered from the remote screen)
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

    // QUICK KEYBOARD DIALOG
    if (showKeyboardDialog) {
        RemoteKeyboardDialog(
            onDismiss = { showKeyboardDialog = false },
            onSendText = { text ->
                viewModel.sendText(text)
                showKeyboardDialog = false
            }
        )
    }
}

@Composable
fun RemoteKeyboardDialog(
    onDismiss: () -> Unit,
    onSendText: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Send Text to TV", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Type search terms or text to transmit directly to your TV over WiFi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Search movies, music...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = GlassBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tv_text")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Send to TV")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
