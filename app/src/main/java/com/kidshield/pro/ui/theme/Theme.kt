package com.kidshield.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MidnightBg = Color(0xFF090E1A)
val CardSurface = Color(0xFF121C30)
val CardElevated = Color(0xFF1A2742)
val CyanAccent = Color(0xFF00E5FF)
val EmeraldAccent = Color(0xFF10B981)
val AmberAccent = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val KidShieldDarkScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = EmeraldAccent,
    tertiary = AmberAccent,
    background = MidnightBg,
    surface = CardSurface,
    onPrimary = MidnightBg,
    onSecondary = MidnightBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = DangerRed
)

@Composable
fun KidShieldProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidShieldDarkScheme,
        content = content
    )
}
