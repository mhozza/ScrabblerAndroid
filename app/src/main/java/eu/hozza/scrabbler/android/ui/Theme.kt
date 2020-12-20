package eu.hozza.scrabbler.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(0xFFff6d00),
    primaryVariant = Color(0xFFc43c00),
    secondary = Color(0xFFffc947),
    surface = Color(0xFF212121)
)

private val LightColorPalette = lightColors(
    primary = Color(0xFFff6d00),
    primaryVariant = Color(0xFFff9e40),
    secondary = Color(0xFFff9800),
    secondaryVariant = Color(0xFFff9800),
    surface = Color(0xFFffefe0)
)

@Composable
fun ScrabblerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}