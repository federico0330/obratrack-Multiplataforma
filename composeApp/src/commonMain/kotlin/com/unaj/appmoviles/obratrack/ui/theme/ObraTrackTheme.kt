package com.unaj.appmoviles.obratrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF365D4A),
    onPrimary = Color.White,
    secondary = Color(0xFF78613C),
    background = Color(0xFFF7F8F5),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D0B3),
    onPrimary = Color(0xFF0D3825),
    secondary = Color(0xFFE0C28F),
)

@Composable
fun ObraTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
