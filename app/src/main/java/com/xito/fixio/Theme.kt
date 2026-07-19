package com.xito.fixio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF6C63FF)
val VioletDark = Color(0xFF8B85FF)
val Teal = Color(0xFF00BFA6)

private val LightScheme = lightColorScheme(
    primary = Violet,
    secondary = Teal,
    background = Color(0xFFF6F6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDECF7),
    onPrimary = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = VioletDark,
    secondary = Teal,
    background = Color(0xFF14141C),
    surface = Color(0xFF1D1D28),
    surfaceVariant = Color(0xFF262633)
)

private val AmoledScheme = darkColorScheme(
    primary = VioletDark,
    secondary = Teal,
    background = Color.Black,
    surface = Color(0xFF0A0A0F),
    surfaceVariant = Color(0xFF15151C)
)

@Composable
fun FixioTheme(mode: String, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        "light" -> LightScheme
        "dark" -> DarkScheme
        "amoled" -> AmoledScheme
        else -> if (isSystemInDarkTheme()) DarkScheme else LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
