package com.territorywars.presentation.theme

import androidx.compose.ui.graphics.Color

// --- Primary palette (green — territory capture accent) ---
val Primary = Color(0xFF00C853)
val PrimaryDark = Color(0xFF00963D)
val PrimaryContainer = Color(0xFF003D18)
val OnPrimaryContainer = Color(0xFFB9F6CA)

val Secondary = Color(0xFF1565C0)
val SecondaryDark = Color(0xFF003C8F)
val SecondaryContainer = Color(0xFF000E3A)
val OnSecondaryContainer = Color(0xFFBBDEFB)

val Tertiary = Color(0xFFFF6D00)
val TertiaryContainer = Color(0xFF3D1A00)
val OnTertiaryContainer = Color(0xFFFFD0B0)

// --- Dark scheme surfaces ---
val BackgroundDark = Color(0xFF0D0D0D)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceVariantDark = Color(0xFF252525)
val OutlineDark = Color(0xFF3D3D3D)
val OutlineVariantDark = Color(0xFF2A2A2A)
val OverlayDark = Color(0xCC0D0D0D)

// --- Light scheme surfaces ---
val BackgroundLight = Color(0xFFF5F5F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEEEEEE)
val OutlineLight = Color(0xFF9E9E9E)
val OutlineVariantLight = Color(0xFFDDDDDD)

// --- On-colors (dark theme) ---
val OnBackgroundDark = Color(0xFFEEEEEE)
val OnSurfaceDark = Color(0xFFDDDDDD)
val OnSurfaceVariantDark = Color(0xFF9E9E9E)

// --- On-colors (light theme) ---
val OnBackgroundLight = Color(0xFF111111)
val OnSurfaceLight = Color(0xFF212121)
val OnSurfaceVariantLight = Color(0xFF616161)

// --- Status colors ---
val Error = Color(0xFFD32F2F)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFF4D0000)
val OnErrorContainer = Color(0xFFFFB4AB)

// --- Map overlay ---
val MyTerritoryFill = Color(0x6600C853)   // 40% opacity
val MyTerritoryStroke = Color(0xFF00C853)
val OtherTerritoryFillAlpha = 0.35f
val OtherTerritoryStrokeAlpha = 0.85f
val MapOverlayBackground = Color(0xCC1A1A1A)
val MapButtonBackground = Color(0xFF1A1A1A)
val MapButtonBorder = Color(0x3300C853)

// --- 12 preset territory colors ---
val TerritoryColors = listOf(
    Color(0xFFE53935),
    Color(0xFFFF6D00),
    Color(0xFFFFD600),
    Color(0xFF76FF03),
    Color(0xFF00C853),
    Color(0xFF1DE9B6),
    Color(0xFF40C4FF),
    Color(0xFF2979FF),
    Color(0xFFD500F9),
    Color(0xFFFF4081),
    Color(0xFFFF1744),
    Color(0xFF6D4C41),
)
