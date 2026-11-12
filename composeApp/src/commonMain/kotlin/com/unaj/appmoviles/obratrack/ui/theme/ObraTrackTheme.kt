package com.unaj.appmoviles.obratrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF365D4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9E8CC),
    onPrimaryContainer = Color(0xFF0B2A1B),
    secondary = Color(0xFF78613C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDEA8),
    onSecondaryContainer = Color(0xFF281900),
    background = Color(0xFFF7F8F5),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDDE5DC),
    onSurfaceVariant = Color(0xFF414942),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4EF),
    surfaceContainer = Color(0xFFECEFE9),
    surfaceContainerHigh = Color(0xFFE6E9E3),
    surfaceContainerHighest = Color(0xFFE0E4DD),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1C9C0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D0B3),
    onPrimary = Color(0xFF0D3825),
    primaryContainer = Color(0xFF1F4B37),
    onPrimaryContainer = Color(0xFFB9E8CC),
    secondary = Color(0xFFE0C28F),
    onSecondary = Color(0xFF3F2D05),
    secondaryContainer = Color(0xFF5A4520),
    onSecondaryContainer = Color(0xFFFFDEA8),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF121411),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9C0),
    surfaceContainerLowest = Color(0xFF0D0F0C),
    surfaceContainerLow = Color(0xFF1A1C19),
    surfaceContainer = Color(0xFF1E201D),
    surfaceContainerHigh = Color(0xFF282B27),
    surfaceContainerHighest = Color(0xFF333532),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414942),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Tipografía base de Material con más peso en títulos, sin fuentes propias. */
private val ObraTrackTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

private val ObraTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ObraTrackTheme(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) DarkColors else LightColors,
        typography = ObraTrackTypography,
        shapes = ObraTrackShapes,
        content = content,
    )
}
