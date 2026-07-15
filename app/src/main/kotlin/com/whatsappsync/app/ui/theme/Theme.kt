package com.whatsappsync.app.ui.theme

import androidx.compose.foundation.isSystemInDarkMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    secondary = md_theme_dark_secondary,
    tertiary = md_theme_dark_tertiary
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    secondary = md_theme_light_secondary,
    tertiary = md_theme_light_tertiary
)

@Composable
fun WhatsAppSyncTheme(
    darkTheme: Boolean = isSystemInDarkMode(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
