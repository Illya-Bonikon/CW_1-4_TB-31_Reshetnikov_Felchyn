package com.remotepad.client.ui.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotepad.protocol.MediaAction

private data class MediaKey(val label: String, val icon: ImageVector, val action: MediaAction)

@Composable
fun MediaScreen(control: ControlViewModel) {
    MediaContent(onAction = control::media)
}

@Composable
fun MediaContent(onAction: (MediaAction) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Презентація", color = MaterialTheme.colorScheme.secondary)
        // «Назад» і «Далі» найбільші: під час доповіді їх натискають наосліп
        MediaRow(
            listOf(
                MediaKey("Назад", Icons.Filled.ChevronLeft, MediaAction.PRESENTATION_PREV),
                MediaKey("Далі", Icons.Filled.ChevronRight, MediaAction.PRESENTATION_NEXT)
            ),
            onAction, height = 120
        )
        MediaRow(
            listOf(
                MediaKey("Почати (F5)", Icons.Filled.Slideshow, MediaAction.PRESENTATION_START),
                MediaKey("Вийти (Esc)", Icons.Filled.Close, MediaAction.PRESENTATION_EXIT)
            ),
            onAction
        )

        Text("Мультимедіа", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
        MediaRow(
            listOf(
                MediaKey("Попер.", Icons.Filled.SkipPrevious, MediaAction.PREV_TRACK),
                MediaKey("Пауза", Icons.Filled.PlayArrow, MediaAction.PLAY_PAUSE),
                MediaKey("Наст.", Icons.Filled.SkipNext, MediaAction.NEXT_TRACK)
            ),
            onAction
        )
        MediaRow(
            listOf(
                MediaKey("Тихіше", Icons.AutoMirrored.Filled.VolumeDown, MediaAction.VOLUME_DOWN),
                MediaKey("Без звуку", Icons.AutoMirrored.Filled.VolumeOff, MediaAction.MUTE),
                MediaKey("Гучніше", Icons.AutoMirrored.Filled.VolumeUp, MediaAction.VOLUME_UP)
            ),
            onAction
        )
    }
}

@Composable
private fun MediaRow(keys: List<MediaKey>, onAction: (MediaAction) -> Unit, height: Int = 72) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.forEach { key ->
            FilledTonalButton(
                onClick = { onAction(key.action) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(height.dp)
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(key.icon, contentDescription = null)
                    Text(key.label, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}
