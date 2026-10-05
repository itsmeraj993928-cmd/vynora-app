package com.example.ui.theme

import android.app.Activity
import android.os.Build
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

private val VynoraDarkColorScheme = darkColorScheme(
    primary = VynoraNeonPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0B24),
    onPrimaryContainer = Color(0xFFFFD8E7),
    secondary = VynoraNeonCyan,
    onSecondary = Color(0xFF003643),
    secondaryContainer = Color(0xFF004E60),
    onSecondaryContainer = Color(0xFFBAEFFF),
    tertiary = VynoraNeonPurple,
    onTertiary = Color.White,
    background = VynoraBlack,
    onBackground = VynoraTextPrimaryDark,
    surface = VynoraDarkSurface,
    onSurface = VynoraTextPrimaryDark,
    surfaceVariant = VynoraDarkSurfaceVariant,
    onSurfaceVariant = VynoraTextSecondaryDark,
    outline = Color(0xFF334155),
    outlineVariant = Color(0x33FFFFFF)
)

private val VynoraLightColorScheme = lightColorScheme(
    primary = VynoraPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E7),
    onPrimaryContainer = Color(0xFF3B0B24),
    secondary = VynoraSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7F0FF),
    onSecondaryContainer = Color(0xFF003544),
    tertiary = VynoraNeonPurple,
    onTertiary = Color.White,
    background = VynoraLightBg,
    onBackground = VynoraTextPrimaryLight,
    surface = VynoraLightSurface,
    onSurface = VynoraTextPrimaryLight,
    surfaceVariant = VynoraLightSurfaceVariant,
    onSurfaceVariant = VynoraTextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0x1F000000)
)

@Composable
fun VynoraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VynoraDarkColorScheme else VynoraLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VynoraTheme(darkTheme = darkTheme, content = content)
}
