package com.remotepad.client.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotepad.protocol.MouseButton

@Composable
fun TouchpadScreen(control: ControlViewModel) {
    TouchpadContent(
        onMove = control::onDrag,
        onScroll = control::onScroll,
        onTap = control::onTap,
        onClick = control::click
    )
}

@Composable
fun TouchpadContent(
    onMove: (Float, Float) -> Unit,
    onScroll: (Float) -> Unit,
    onTap: (Int) -> Unit,
    onClick: (MouseButton) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .touchpadGestures(onMove = onMove, onScroll = onScroll, onTap = onTap),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Один палець: курсор і клік\nДва пальці: прокрутка і правий клік",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MouseKey("Ліва", MouseButton.LEFT, onClick, Modifier.weight(1f))
            MouseKey("Права", MouseButton.RIGHT, onClick, Modifier.weight(1f))
        }
    }
}

// Великі кнопки внизу: їх легко знайти великим пальцем, не дивлячись на екран
@Composable
private fun MouseKey(label: String, button: MouseButton, onClick: (MouseButton) -> Unit, modifier: Modifier) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick(button)
        },
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
    }
}
