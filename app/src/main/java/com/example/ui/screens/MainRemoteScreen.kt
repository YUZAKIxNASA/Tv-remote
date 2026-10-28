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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
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
import com.example.ui.components.DpadControl
import com.example.ui.components.RemoteButton
import com.example.ui.components.RockerControl
import com.example.ui.components.StatusPill
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ConnectedStatus
import com.example.ui.theme.ConnectedStatusBg
import com.example.ui.theme.DarkText
import com.example.ui.theme.ErrorStatus
import com.example.ui.theme.ErrorStatusBg
import com.example.ui.theme.PrimaryBg
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.SecondaryBg
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

    var showKeyboardDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    val isConnected = connectionState is DeviceConnectionState.Connected

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PrimaryBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TOP HEADER BAR
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
                            text = device?.addressLabel ?: "No Device",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SecondaryText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        isConnected = isConnected,
                        label = if (isConnected) "Connected" else "Offline"
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

            // FEEDBACK BANNER
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
                        shape = RoundedCornerShape(10.dp)
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

            // REMOTE CONTROLLER SCROLLABLE BODY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ROW 1: POWER, MUTE, INPUT SOURCE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power Button
                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.POWER) },
                        icon = Icons.Default.PowerSettingsNew,
                        text = "Power",
                        iconColor = Color(0xFFEF4444),
                        contentColor = Color(0xFFEF4444),
                        backgroundColor = Color(0xFFFEF2F2),
                        borderColor = Color(0xFFFCA5A5),
                        minWidth = 84.dp,
                        minHeight = 54.dp,
                        testTag = "btn_power"
                    )

                    // Mute Button
                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.MUTE) },
                        icon = Icons.Default.VolumeOff,
                        text = "Mute",
                        iconColor = PrimaryBlue,
                        minWidth = 84.dp,
                        minHeight = 54.dp,
                        testTag = "btn_mute"
                    )

                    // Input Source
                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.INPUT_SOURCE) },
                        icon = Icons.Default.Input,
                        text = "Source",
                        iconColor = DarkText,
                        minWidth = 84.dp,
                        minHeight = 54.dp,
                        testTag = "btn_source"
                    )
                }

                // DIRECTIONAL PAD (D-PAD)
                DpadControl(
                    onUp = { viewModel.sendCommand(RemoteCommand.UP) },
                    onDown = { viewModel.sendCommand(RemoteCommand.DOWN) },
                    onLeft = { viewModel.sendCommand(RemoteCommand.LEFT) },
                    onRight = { viewModel.sendCommand(RemoteCommand.RIGHT) },
                    onOk = { viewModel.sendCommand(RemoteCommand.OK) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // ROW 3: NAVIGATION (BACK, HOME, MENU)
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
                        minWidth = 86.dp,
                        minHeight = 56.dp,
                        testTag = "btn_tv_back"
                    )

                    // Home (Prominent)
                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.HOME) },
                        icon = Icons.Default.Home,
                        text = "Home",
                        iconColor = PrimaryBlue,
                        backgroundColor = PrimaryBlueLight,
                        borderColor = PrimaryBlue.copy(alpha = 0.3f),
                        minWidth = 96.dp,
                        minHeight = 56.dp,
                        testTag = "btn_tv_home"
                    )

                    // Menu
                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.MENU) },
                        icon = Icons.Default.Menu,
                        text = "Menu",
                        iconColor = DarkText,
                        minWidth = 86.dp,
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
                    // Volume Rocker
                    RockerControl(
                        title = "VOL",
                        icon = Icons.Default.VolumeUp,
                        onPlus = { viewModel.sendCommand(RemoteCommand.VOLUME_UP) },
                        onMinus = { viewModel.sendCommand(RemoteCommand.VOLUME_DOWN) },
                        plusTag = "btn_vol_up",
                        minusTag = "btn_vol_down"
                    )

                    // Channel Rocker
                    RockerControl(
                        title = "CH",
                        icon = Icons.Default.Tv,
                        onPlus = { viewModel.sendCommand(RemoteCommand.CHANNEL_UP) },
                        onMinus = { viewModel.sendCommand(RemoteCommand.CHANNEL_DOWN) },
                        plusTag = "btn_ch_up",
                        minusTag = "btn_ch_down"
                    )
                }

                // ROW 5: MEDIA CONTROLS
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.PREVIOUS) },
                            modifier = Modifier.testTag("btn_media_prev")
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Previous", tint = DarkText)
                        }

                        // Prominent Play/Pause
                        Button(
                            onClick = { viewModel.sendCommand(RemoteCommand.PLAY_PAUSE) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("btn_media_play_pause")
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play/Pause", tint = Color.White)
                        }

                        IconButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.STOP) },
                            modifier = Modifier.testTag("btn_media_stop")
                        ) {
                            Icon(Icons.Default.Stop, "Stop", tint = DarkText)
                        }

                        IconButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.NEXT) },
                            modifier = Modifier.testTag("btn_media_next")
                        ) {
                            Icon(Icons.Default.SkipNext, "Next", tint = DarkText)
                        }
                    }
                }

                // ROW 6: QUICK APP SHORTCUTS (Mi TV startapp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.APP_YOUTUBE) },
                        text = "YouTube",
                        iconColor = Color(0xFFFF0000),
                        modifier = Modifier.weight(1f),
                        minHeight = 46.dp,
                        testTag = "btn_app_youtube"
                    )

                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.APP_NETFLIX) },
                        text = "Netflix",
                        iconColor = Color(0xFFE50914),
                        modifier = Modifier.weight(1f),
                        minHeight = 46.dp,
                        testTag = "btn_app_netflix"
                    )

                    RemoteButton(
                        onClick = { viewModel.sendCommand(RemoteCommand.APP_PRIME) },
                        text = "Prime",
                        iconColor = Color(0xFF00A8E1),
                        modifier = Modifier.weight(1f),
                        minHeight = 46.dp,
                        testTag = "btn_app_prime"
                    )
                }

                // ROW 7: TEXT / KEYBOARD INPUT
                Button(
                    onClick = { showKeyboardDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryBg,
                        contentColor = DarkText
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_open_keyboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Type on TV (Keyboard)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                        unfocusedBorderColor = BorderColor
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
