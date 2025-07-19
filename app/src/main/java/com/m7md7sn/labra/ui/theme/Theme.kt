package com.m7md7sn.labra.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LabraDarkColorScheme = darkColorScheme(
    primary = LabraPrimaryDarkTheme,
    secondary = LabraSecondary,
    tertiary = LabraAccent,
    background = LabraBackgroundDarkTheme,
    surface = LabraSurfaceDarkTheme,
    onPrimary = LabraOnPrimary,
    onSecondary = LabraOnSecondary,
    onTertiary = LabraOnBackground,
    onBackground = LabraOnBackgroundDarkTheme,
    onSurface = LabraOnSurfaceDarkTheme,
    error = LabraError,
    onError = LabraOnError
)

private val LabraLightColorScheme = lightColorScheme(
    primary = LabraPrimary,
    secondary = LabraSecondary,
    tertiary = LabraAccent,
    background = LabraBackground,
    surface = LabraSurface,
    onPrimary = LabraOnPrimary,
    onSecondary = LabraOnSecondary,
    onTertiary = LabraOnBackground,
    onBackground = LabraOnBackground,
    onSurface = LabraOnSurface,
    error = LabraError,
    onError = LabraOnError,
    surfaceVariant = LabraBackground,
    onSurfaceVariant = LabraOnBackground,
    outline = LabraNeutral,
    outlineVariant = LabraNeutral
)

@Composable
fun LabraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use Labra brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LabraDarkColorScheme
        else -> LabraLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LabraPrimaryDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}