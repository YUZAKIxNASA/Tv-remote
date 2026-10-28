package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// NASA WiFi Remote - Premium White & Blue Glassmorphism Color System
val GlassBackground = Color(0xFFF8FAFC)
val GlassBackgroundAlt = Color(0xFFF1F5F9)
val GlassSurface = Color(0xE6FFFFFF)          // 90% opacity white frosted glass
val GlassSurfaceCard = Color(0xF5FFFFFF)      // 96% opacity white frosted glass
val GlassSurfaceSubtle = Color(0xB3FFFFFF)    // 70% opacity white frosted glass
val GlassSurfaceTranslucent = Color(0x66FFFFFF) // 40% translucent
val GlassBorder = Color(0x80CBD5E1)           // Subtle slate border
val GlassBorderLight = Color(0xB3FFFFFF)      // Soft white hairline highlight
val GlassBorderAccent = Color(0x402563EB)     // Translucent blue border

val PrimaryBlue = Color(0xFF2563EB)           // Royal Blue Primary
val PrimaryBlueHover = Color(0xFF1D4ED8)
val PrimaryBlueLight = Color(0xFFEFF6FF)
val PrimaryBlueSoft = Color(0x1F2563EB)

val DarkText = Color(0xFF0F172A)              // Deep slate navy
val SecondaryText = Color(0xFF64748B)         // Cool slate
val MutedText = Color(0xFF94A3B8)             // Light slate

val ConnectedStatus = Color(0xFF10B981)       // Emerald
val ConnectedStatusBg = Color(0xFFECFDF5)
val WarningStatus = Color(0xFFF59E0B)         // Amber
val WarningStatusBg = Color(0xFFFFFBEB)
val ErrorStatus = Color(0xFFEF4444)           // Rose red
val ErrorStatusBg = Color(0xFFFEF2F2)
val InfoStatus = Color(0xFF3B82F6)            // Blue
val InfoStatusBg = Color(0xFFEFF6FF)

// Legacy aliases for full backward compatibility
val PrimaryBg = GlassBackground
val SecondaryBg = GlassBackgroundAlt
val BorderColor = Color(0xFFE2E8F0)
val CardSurface = GlassSurfaceCard
val DpadSurface = Color(0xEBF1F5F9)
val DpadPressed = Color(0xFFE2E8F0)

// Frosted glass gradient brushes
val GlassBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF8FAFC),
        Color(0xFFEFF4F9)
    )
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFAFFFFFF),
        Color(0xEEF8FAFC)
    )
)

val GlassBlueGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF2563EB),
        Color(0xFF1D4ED8)
    )
)
