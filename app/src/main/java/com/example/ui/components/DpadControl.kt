package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DarkText
import com.example.ui.theme.DpadSurface
import com.example.ui.theme.PrimaryBlue

@Composable
fun DpadControl(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(240.dp)
            .shadow(4.dp, CircleShape, ambientColor = Color(0x0A000000), spotColor = Color(0x1F000000))
            .clip(CircleShape)
            .background(DpadSurface)
            .testTag("dpad_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer border
        Surface(
            modifier = Modifier.size(240.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, BorderColor)
        ) {}

        // UP Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 96.dp, height = 76.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = PrimaryBlue)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUp()
                }
                .testTag("btn_dpad_up"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = PrimaryBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        // DOWN Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 96.dp, height = 76.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = PrimaryBlue)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDown()
                }
                .testTag("btn_dpad_down"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = PrimaryBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        // LEFT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = 76.dp, height = 96.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = PrimaryBlue)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLeft()
                }
                .testTag("btn_dpad_left"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = PrimaryBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        // RIGHT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 76.dp, height = 96.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = PrimaryBlue)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRight()
                }
                .testTag("btn_dpad_right"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Right",
                tint = PrimaryBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        // CENTER OK Button
        Surface(
            modifier = Modifier
                .size(90.dp)
                .shadow(3.dp, CircleShape, ambientColor = Color(0x12000000), spotColor = Color(0x24000000))
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = PrimaryBlue)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOk()
                }
                .testTag("btn_dpad_ok"),
            shape = CircleShape,
            color = CardSurface,
            border = BorderStroke(1.5.dp, BorderColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    ),
                    color = PrimaryBlue
                )
            }
        }
    }
}
