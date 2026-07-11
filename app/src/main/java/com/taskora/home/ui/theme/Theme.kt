package com.taskora.home.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = HouseNavy,
    onPrimary = Color.White,
    primaryContainer = WarmSand,
    onPrimaryContainer = DeepNavy,
    secondary = LaundryBlueGray,
    onSecondary = Color.White,
    tertiary = KitchenAmber,
    onTertiary = DeepText,
    background = AppBackground,
    onBackground = DeepText,
    surface = SurfaceWhite,
    onSurface = DeepText,
    surfaceVariant = LightSand,
    onSurfaceVariant = SecondaryText,
    outline = DividerColor,
    error = OverdueRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = WarmSand,
    onPrimary = DeepNavy,
    primaryContainer = HouseNavy,
    onPrimaryContainer = LightSand,
    secondary = LaundryBlueGray,
    onSecondary = DeepNavy,
    tertiary = KitchenAmber,
    onTertiary = DeepNavy,
    // Dark scheme uses a few local dark neutrals (brand palette is light-first).
    background = Color(0xFF14191E),
    onBackground = LightSand,
    surface = Color(0xFF1C2329),
    onSurface = LightSand,
    surfaceVariant = Color(0xFF2A333B),
    onSurfaceVariant = Color(0xFFC3C8CC),
    outline = Color(0xFF3C464E),
    error = Color(0xFFE08A88),
    onError = DeepNavy
)

@Composable
fun TaskoraHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Fixed brand palette (no dynamic color) so the identity stays consistent.
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = TaskoraTypography,
        content = content
    )
}
