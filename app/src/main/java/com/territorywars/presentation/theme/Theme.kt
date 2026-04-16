package com.territorywars.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = BackgroundDark,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Secondary,
    tertiary = Tertiary,
    onTertiary = BackgroundDark,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Tertiary,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = Error,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = BackgroundLight,
    primaryContainer = 4е544Color(0xFFB9F6CA),
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = BackgroundLight,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    onTertiary = BackgroundLight,
    error = Error,
    onError = OnError,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
)

@Composable
fun TerritoryWarsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// Вспомогательная функция — цвет территории игрока в формате Compose Color
fun territoryColorByHex(hex: String): androidx.compose.ui.graphics.Color {
    return try {
        val colorLong = android.graphics.Color.parseColor(hex).toLong()
        androidx.compose.ui.graphics.Color(colorLong or 0xFF000000L)
    } catch (e: Exception) {
        Primary
    }
}
