package com.carribu.parentnative.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CarribuBlue = Color(0xFF217F9F)
val CarribuBlueDark = Color(0xFF0B3849)
val CarribuBlueDeep = Color(0xFF124F65)
val CarribuBlueLight = Color(0xFFD5EDF5)
val CarribuMaroon = Color(0xFF8F2638)
val CarribuMaroonDark = Color(0xFF4B101B)
val CarribuMaroonLight = Color(0xFFF4D2D7)
val CarribuRed = Color(0xFFD64535)
val CarribuText = Color(0xFF4B101B)
val CarribuTextMuted = Color(0xFF621725)
val CarribuAmber = Color(0xFFE79A25)

private val LightColors = lightColorScheme(
    primary = CarribuBlue,
    onPrimary = Color.White,
    primaryContainer = CarribuBlueLight,
    onPrimaryContainer = CarribuBlueDark,
    secondary = CarribuMaroon,
    onSecondary = Color.White,
    secondaryContainer = CarribuMaroonLight,
    onSecondaryContainer = CarribuMaroonDark,
    tertiary = CarribuRed,
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = CarribuText,
    surface = Color.White,
    onSurface = CarribuText,
    surfaceVariant = Color(0xFFEAF7FB),
    onSurfaceVariant = CarribuTextMuted,
    outline = Color(0xFF80C7DC),
    error = CarribuRed,
    errorContainer = Color(0xFFFFE0DC),
    onErrorContainer = Color(0xFF702019)
)

@Composable
fun CarribuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}
