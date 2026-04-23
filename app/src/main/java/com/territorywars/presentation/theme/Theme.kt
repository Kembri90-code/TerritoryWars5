package com.territorywars.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryDim,
    onPrimaryContainer     = DarkOnPrimary,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnPrimary,
    secondaryContainer     = Color(0xFF1E3A5F),
    onSecondaryContainer   = DarkSecondary,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnPrimary,
    tertiaryContainer      = Color(0xFF2D1F5E),
    onTertiaryContainer    = DarkTertiary,
    error                  = DarkError,
    onError                = Color(0xFF1A0000),
    errorContainer         = DarkErrorCont,
    onErrorContainer       = DarkError,
    background             = DarkBg,
    onBackground           = DarkOnBg,
    surface                = DarkSurface,
    onSurface              = DarkOnSurf,
    surfaceVariant         = DarkSurfCont,
    onSurfaceVariant       = DarkOnSurfVar,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVar,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow    = DarkSurfContLow,
    surfaceContainer       = DarkSurfCont,
    surfaceContainerHigh   = DarkSurfContHigh,
    surfaceContainerHighest= DarkSurfContHigh,
    inverseSurface         = DarkOnBg,
    inverseOnSurface       = DarkBg,
    inversePrimary         = LightPrimary,
    scrim                  = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary                = LightPrimary,
    onPrimary              = LightOnPrimary,
    primaryContainer       = LightSurfContHigh,
    onPrimaryContainer     = LightPrimary,
    secondary              = LightSecondary,
    onSecondary            = LightOnPrimary,
    secondaryContainer     = Color(0xFFDBEAFE),
    onSecondaryContainer   = LightSecondary,
    tertiary               = LightTertiary,
    onTertiary             = LightOnPrimary,
    tertiaryContainer      = Color(0xFFEDE9FE),
    onTertiaryContainer    = LightTertiary,
    error                  = LightError,
    onError                = LightOnPrimary,
    errorContainer         = LightErrorCont,
    onErrorContainer       = LightError,
    background             = LightBg,
    onBackground           = LightOnBg,
    surface                = LightSurface,
    onSurface              = LightOnSurf,
    surfaceVariant         = LightSurfCont,
    onSurfaceVariant       = LightOnSurfVar,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVar,
    surfaceContainerLowest = LightBg,
    surfaceContainerLow    = LightSurfContLow,
    surfaceContainer       = LightSurfCont,
    surfaceContainerHigh   = LightSurfContHigh,
    surfaceContainerHighest= LightSurfContHigh,
    inverseSurface         = LightOnBg,
    inverseOnSurface       = LightBg,
    inversePrimary         = DarkPrimary,
    scrim                  = Color(0xFF000000),
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
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
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
