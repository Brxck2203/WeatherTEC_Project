package com.project.weathertec.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val TealPrimary = Color(0xFF01696F)
val TealContainer = Color(0xFFB2DFDB)
val TealOnContainer = Color(0xFF0F3638)
val DarkBackground = Color(0xFF171614)
val DarkSurface = Color(0xFF1C1B19)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    background = Color(0xFFF7F6F2),
    surface = Color(0xFFF9F8F5),
    surfaceVariant = Color(0xFFEDEAE5)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4F98A3),
    primaryContainer = Color(0xFF313B3B),
    onPrimaryContainer = Color(0xFFCDCCCA),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF22211F)
)

@Composable
fun WeatherTECTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
