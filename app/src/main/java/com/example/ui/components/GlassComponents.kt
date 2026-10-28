package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ConnectedStatus
import com.example.ui.theme.ConnectedStatusBg
import com.example.ui.theme.DarkText
import com.example.ui.theme.ErrorStatus
import com.example.ui.theme.ErrorStatusBg
import com.example.ui.theme.GlassBlueGradient
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderAccent
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassCardGradient
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceCard
import com.example.ui.theme.GlassSurfaceSubtle
import com.example.ui.theme.GlassSurfaceTranslucent
import com.example.ui.theme.InfoStatus
import com.example.ui.theme.InfoStatusBg
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.WarningStatus
import com.example.ui.theme.WarningStatusBg

/**
 * Premium frosted Glassmorphism card container.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 22.dp,
    elevation: Dp = 3.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = GlassBorderLight,
    backgroundColor: Color = GlassSurfaceCard,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(shapeRadius)
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x0D0F172A),
                spotColor = Color(0x1A0F172A)
            )
            .clip(shape),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .background(GlassCardGradient)
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Premium Glass button with tactile feedback and minimum 48dp touch target.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = "glass_button"
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)

    val backgroundModifier = if (isPrimary && enabled) {
        Modifier.background(GlassBlueGradient, shape)
    } else if (enabled) {
        Modifier.background(GlassSurface, shape)
    } else {
        Modifier.background(GlassSurfaceTranslucent, shape)
    }

    val contentColor = if (isPrimary && enabled) Color.White else if (enabled) DarkText else SecondaryText
    val borderColor = if (isPrimary && enabled) Color.Transparent else if (enabled) GlassBorder else GlassBorderLight

    Surface(
        modifier = modifier
            .height(50.dp)
            .shadow(
                elevation = if (isPrimary && enabled) 4.dp else 1.dp,
                shape = shape,
                ambientColor = Color(0x1A2563EB),
                spotColor = Color(0x2B2563EB)
            )
            .clip(shape)
            .then(backgroundModifier)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = if (isPrimary) Color.White else PrimaryBlue)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .testTag(testTag),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.3.sp
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Circular or squircle Glass Icon Button meeting 48dp minimum touch target.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = DarkText,
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorderLight,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
    testTag: String = "glass_icon_button"
) {
    val haptic = LocalHapticFeedback.current
    val shape = CircleShape

    Surface(
        modifier = modifier
            .size(size)
            .shadow(2.dp, shape, ambientColor = Color(0x0A0F172A), spotColor = Color(0x140F172A))
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = PrimaryBlue)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .testTag(testTag),
        shape = shape,
        color = if (enabled) backgroundColor else GlassSurfaceSubtle,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else SecondaryText,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Status Pill with Glassmorphism frosted capsule and glowing dot.
 */
@Composable
fun GlassStatusPill(
    label: String,
    statusType: StatusType = StatusType.DISCONNECTED,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgBadgeColor, textColor) = when (statusType) {
        StatusType.CONNECTED -> Triple(ConnectedStatus, ConnectedStatusBg, Color(0xFF065F46))
        StatusType.PAIRING_REQUIRED, StatusType.WARNING -> Triple(WarningStatus, WarningStatusBg, Color(0xFF92400E))
        StatusType.ERROR, StatusType.FAILED -> Triple(ErrorStatus, ErrorStatusBg, Color(0xFF991B1B))
        StatusType.CONNECTING, StatusType.INFO -> Triple(InfoStatus, InfoStatusBg, Color(0xFF1E40AF))
        StatusType.DISCONNECTED, StatusType.OFFLINE -> Triple(Color(0xFF94A3B8), Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = bgBadgeColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Glowing circular indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = textColor
            )
        }
    }
}

enum class StatusType {
    CONNECTED,
    PAIRING_REQUIRED,
    WARNING,
    ERROR,
    FAILED,
    CONNECTING,
    INFO,
    DISCONNECTED,
    OFFLINE
}

/**
 * Dialog for entering the Android TV PIN pairing code.
 */
@Composable
fun GlassPairingDialog(
    deviceName: String,
    deviceIp: String,
    isLoading: Boolean,
    errorMessage: String?,
    onPinSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("dialog_pairing"),
            shapeRadius = 24.dp,
            elevation = 12.dp,
            borderColor = GlassBorderAccent,
            backgroundColor = Color(0xFDF8FAFC)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(PrimaryBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Pair with TV",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = DarkText
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_pairing_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TV Device Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GlassSurfaceSubtle,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkText
                        )
                        Text(
                            text = "IP: $deviceIp • Android TV Remote v2",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Look at your TV screen. A 4 to 6 character pairing code should be displayed now. Enter it below:",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PIN Input TextField
            OutlinedTextField(
                value = pinText,
                onValueChange = { if (it.length <= 8) pinText = it.uppercase() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_pairing_pin"),
                placeholder = {
                    Text(
                        text = "e.g. 7X4A or 1234",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    color = DarkText
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = GlassSurface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (pinText.isNotBlank()) onPinSubmit(pinText)
                    }
                )
            )

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ErrorStatusBg,
                    border = BorderStroke(1.dp, ErrorStatus.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorStatus,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = ErrorStatus
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    isPrimary = false,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_cancel_pairing"
                )

                GlassButton(
                    text = "Verify PIN",
                    onClick = { onPinSubmit(pinText) },
                    isPrimary = true,
                    enabled = pinText.trim().length >= 4 && !isLoading,
                    isLoading = isLoading,
                    modifier = Modifier.weight(1.3f),
                    testTag = "btn_submit_pin"
                )
            }
        }
    }
}

/**
 * Frosted Glass Warning / Disconnection Banner with inline CTA.
 */
@Composable
fun GlassWarningBanner(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
    statusType: StatusType = StatusType.WARNING
) {
    val (icon, tint, bgColor, borderColor) = when (statusType) {
        StatusType.PAIRING_REQUIRED, StatusType.WARNING -> Quadruple(
            Icons.Default.Lock,
            WarningStatus,
            WarningStatusBg,
            WarningStatus.copy(alpha = 0.35f)
        )
        StatusType.ERROR, StatusType.FAILED -> Quadruple(
            Icons.Default.Warning,
            ErrorStatus,
            ErrorStatusBg,
            ErrorStatus.copy(alpha = 0.35f)
        )
        else -> Quadruple(
            Icons.Default.Info,
            InfoStatus,
            InfoStatusBg,
            InfoStatus.copy(alpha = 0.35f)
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x14000000))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = DarkText
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    color = SecondaryText
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = tint),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
