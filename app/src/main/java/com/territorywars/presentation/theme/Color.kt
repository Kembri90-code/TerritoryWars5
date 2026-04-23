package com.territorywars.presentation.theme

import androidx.compose.ui.graphics.Color

// ── Dark theme tokens ─────────────────────────────────────────────────────────
val DarkBg             = Color(0xFF0C0C15)
val DarkBgAlt          = Color(0xFF111120)
val DarkSurface        = Color(0xFF14141F)
val DarkSurfCont       = Color(0xFF1C1C2A)
val DarkSurfContHigh   = Color(0xFF232336)
val DarkSurfContLow    = Color(0xFF171724)

val DarkPrimary        = Color(0xFF7C6EF8)
val DarkPrimaryDim     = Color(0xFF5E50D4)
val DarkOnPrimary      = Color(0xFFFFFFFF)
val DarkSecondary      = Color(0xFF60A5FA)
val DarkTertiary       = Color(0xFFA78BFA)
val DarkError          = Color(0xFFF87171)
val DarkErrorCont      = Color(0x26F87171)  // rgba(248,113,113,0.15)
val DarkWarning        = Color(0xFFFBBF24)

val DarkOnBg           = Color(0xFFECEBFF)
val DarkOnSurf         = Color(0xFFE2E1F5)
val DarkOnSurfVar      = Color(0xFF8885B0)
val DarkOutline        = Color(0xFF2E2E46)
val DarkOutlineVar     = Color(0xFF1F1F30)

// ── Light theme tokens ────────────────────────────────────────────────────────
val LightBg            = Color(0xFFF7F6FF)
val LightBgAlt         = Color(0xFFFFFFFF)
val LightSurface       = Color(0xFFFFFFFF)
val LightSurfCont      = Color(0xFFEFEEFD)
val LightSurfContHigh  = Color(0xFFE6E4FC)
val LightSurfContLow   = Color(0xFFF5F4FF)

val LightPrimary       = Color(0xFF5644E8)
val LightPrimaryDim    = Color(0xFF4433C8)
val LightOnPrimary     = Color(0xFFFFFFFF)
val LightSecondary     = Color(0xFF2563EB)
val LightTertiary      = Color(0xFF7C3AED)
val LightError         = Color(0xFFDC2626)
val LightErrorCont     = Color(0x1ADC2626)  // rgba(220,38,38,0.10)
val LightWarning       = Color(0xFFD97706)

val LightOnBg          = Color(0xFF1A1835)
val LightOnSurf        = Color(0xFF1A1835)
val LightOnSurfVar     = Color(0xFF6B67A0)
val LightOutline       = Color(0xFFD8D6F0)
val LightOutlineVar    = Color(0xFFEAE9F8)

// ── Preset colors (territory / clan) ─────────────────────────────────────────
val PresetColors = listOf(
    "#7C6EF8", "#60A5FA", "#F87171", "#FBBF24",
    "#A78BFA", "#34D399", "#F472B6", "#FB923C",
    "#38BDF8", "#4ADE80", "#E879F9", "#FDE68A",
)

val PresetColorValues = listOf(
    Color(0xFF7C6EF8), Color(0xFF60A5FA), Color(0xFFF87171), Color(0xFFFBBF24),
    Color(0xFFA78BFA), Color(0xFF34D399), Color(0xFFF472B6), Color(0xFFFB923C),
    Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFE879F9), Color(0xFFFDE68A),
)

// ── Helpers ───────────────────────────────────────────────────────────────────
fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { DarkPrimary }
