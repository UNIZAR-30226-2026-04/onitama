package com.example.onitama.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = azulOscuro,
    secondary = grisClaro,
    surface = blancoFondo
)

@Composable
fun OnitamaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}