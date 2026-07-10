package com.pouyaApp.metoronome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TealPrimary = Color(0xFF00897B)
private val TealDark = Color(0xFF00695C)
private val AccentBlue = Color(0xFF1E88E5)
private val AccentGreen = Color(0xFF4CAF50)
private val SurfaceDark = Color(0xFF121820)
private val SurfaceContainer = Color(0xFF1A2332)

private val DarkColors = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    secondary = AccentBlue,
    tertiary = AccentGreen,
    background = SurfaceDark,
    surface = SurfaceContainer,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
)

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    secondary = AccentBlue,
    tertiary = AccentGreen,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
)

@Composable
fun MetronomeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
