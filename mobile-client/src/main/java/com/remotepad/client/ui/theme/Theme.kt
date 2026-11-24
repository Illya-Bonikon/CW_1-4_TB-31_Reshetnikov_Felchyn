package com.remotepad.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Темна палітра: на AMOLED чорний фон майже не витрачає заряд, а в темній аудиторії не сліпить
private val RemotePadColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00222E),
    secondary = Color(0xFF90A4AE),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurface = Color(0xFFE6E6E6),
    error = Color(0xFFEF5350)
)

val ConnectedGreen = Color(0xFF4CAF50)

@Composable
fun RemotePadTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RemotePadColors, content = content)
}
