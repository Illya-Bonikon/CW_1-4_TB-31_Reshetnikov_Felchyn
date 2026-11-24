package com.remotepad.client.ui.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotepad.client.network.ConnectionStatus
import com.remotepad.client.ui.settings.SettingsScreen
import com.remotepad.client.ui.settings.SettingsViewModel
import com.remotepad.client.ui.theme.ConnectedGreen

enum class ControlMode(val title: String, val icon: ImageVector) {
    TOUCHPAD("Тачпад", Icons.Filled.TouchApp),
    KEYBOARD("Клавіатура", Icons.Filled.Keyboard),
    MEDIA("Медіа", Icons.Filled.Slideshow),
    MACROS("Макроси", Icons.Filled.Apps),
    SETTINGS("Налаштування", Icons.Filled.Settings)
}

@Composable
fun ControlScreen(
    control: ControlViewModel,
    settings: SettingsViewModel,
    onDisconnected: () -> Unit
) {
    val status by control.status.collectAsStateWithLifecycle()
    val rtt by control.rttMs.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(ControlMode.TOUCHPAD) }

    LaunchedEffect(status) {
        if (status == ConnectionStatus.Disconnected) onDisconnected()
    }

    ControlScaffold(status, rtt, mode, onModeChange = { mode = it }, onDisconnect = control::disconnect) {
        when (it) {
            ControlMode.TOUCHPAD -> TouchpadScreen(control)
            ControlMode.KEYBOARD -> KeyboardScreen(control)
            ControlMode.MEDIA -> MediaScreen(control)
            ControlMode.MACROS -> MacrosScreen(control)
            ControlMode.SETTINGS -> SettingsScreen(settings)
        }
    }
}

/** Рамка екрана керування без ViewModel: рядок стану, нижня панель режимів і вміст обраного режиму. */
@Composable
fun ControlScaffold(
    status: ConnectionStatus,
    rtt: Long?,
    mode: ControlMode,
    onModeChange: (ControlMode) -> Unit,
    onDisconnect: () -> Unit,
    content: @Composable (ControlMode) -> Unit
) {
    Scaffold(
        topBar = { StatusBar(status, rtt, onDisconnect) },
        bottomBar = {
            NavigationBar {
                ControlMode.entries.forEach { item ->
                    NavigationBarItem(
                        selected = mode == item,
                        onClick = { onModeChange(item) },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 9.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { content(mode) }
    }
}

@Composable
private fun StatusBar(status: ConnectionStatus, rtt: Long?, onDisconnect: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (text, color) = when (status) {
                is ConnectionStatus.Connected -> "Підключено: ${status.hostName}" to ConnectedGreen
                is ConnectionStatus.Reconnecting -> "Перепідключення, спроба ${status.attempt}" to MaterialTheme.colorScheme.secondary
                is ConnectionStatus.Lost -> "Зв'язок втрачено" to MaterialTheme.colorScheme.error
                ConnectionStatus.Connecting -> "Підключення..." to MaterialTheme.colorScheme.secondary
                ConnectionStatus.Disconnected -> "Відключено" to MaterialTheme.colorScheme.error
            }
            Text(text, color = color, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
            Text(rtt?.let { "RTT: $it мс" } ?: "RTT: -", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            IconButton(onClick = onDisconnect) { Icon(Icons.Filled.LinkOff, "Відключитися") }
        }
        if (status is ConnectionStatus.Lost) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status.reason, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onDisconnect) { Text("До підключення") }
            }
        }
    }
}
