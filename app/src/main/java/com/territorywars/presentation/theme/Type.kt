package com.territorywars.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.territorywars.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

val PlusJakartaSans = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.SemiBold),
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.Bold),
    Font(GoogleFont("Plus Jakarta Sans"), provider, weight = FontWeight.ExtraBold),
)

val DmMono = FontFamily(
    Font(GoogleFont("DM Mono"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("DM Mono"), provider, weight = FontWeight.Medium),
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 28.sp,
        lineHeight   = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 22.sp,
        lineHeight   = 30.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Bold,
        fontSize     = 18.sp,
        lineHeight   = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Bold,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 15.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Bold,
        fontSize     = 15.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily   = PlusJakartaSans,
        fontWeight   = FontWeight.Bold,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 1.2.sp,
    ),
)

val MonoStyle = TextStyle(
    fontFamily   = DmMono,
    fontWeight   = FontWeight.Medium,
    fontSize     = 19.sp,
    letterSpacing = (-0.5).sp,
)
