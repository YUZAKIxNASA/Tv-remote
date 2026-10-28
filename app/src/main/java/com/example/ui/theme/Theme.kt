package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NASA WiFi Remote - strictly enforced White Theme per design specification
private val NasaWhiteColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueHover,
    secondary = SecondaryText,
    onSecondary = Color.White,
    secondaryContainer = SecondaryBg,
    onSecondaryContainer = DarkText,
    tertiary = PrimaryBlue,
    background = PrimaryBg,
    onBackground = DarkText,
    surface = CardSurface,
    onSurface = DarkText,
    surfaceVariant = SecondaryBg,
    onSurfaceVariant = SecondaryText,
    outline = BorderColor,
    outlineVariant = BorderColor,
    error = ErrorStatus,
    onError = Color.White,
    errorContainer = ErrorStatusBg,
    onErrorContainer = ErrorStatus
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // White theme is locked per design specification - no dark theme
    MaterialTheme(
        colorScheme = NasaWhiteColorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for convenience
@Composable
fun NasaRemoteTheme(
    content: @Composable () -> Unit
) {
    MyApplicationTheme(content = content)
}
