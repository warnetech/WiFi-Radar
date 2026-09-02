package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RadarColorScheme = darkColorScheme(
    primary = RadarCyan,
    onPrimary = RadarDarkBackground,
    primaryContainer = RadarDarkSurfaceVariant,
    onPrimaryContainer = RadarCyan,
    secondary = RadarThreatRed,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF330C12),
    onSecondaryContainer = Color(0xFFFFB4AB),
    tertiary = RadarGreen,
    background = RadarDarkBackground,
    onBackground = TextPrimary,
    surface = RadarDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = RadarDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = RadarThreatRed,
    onError = TextPrimary,
    outline = RadarDarkBorder
)

@Composable
fun WiFiRadarTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = RadarDarkBackground.toArgb()
            window.navigationBarColor = RadarDarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = RadarColorScheme,
        typography = Typography,
        content = content
    )
}
