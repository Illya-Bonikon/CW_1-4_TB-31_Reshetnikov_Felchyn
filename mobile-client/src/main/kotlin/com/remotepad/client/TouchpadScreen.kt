package com.remotepad.client

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.MouseButton

/**
 * Екран віртуального тачпада в Android-додатку (Розробник: Решетніков І. В.).
 * Обробляє жести користувача (переміщення, тапи, скролінг двома пальцями)
 * та транслює їх у вихідний потік мережевих пакетів.
 */
@Composable
fun TouchpadScreen(
    onSendPacket: (InputEventPacket) -> Unit,
    connectionPingMs: Int = 8,
    isConnected: Boolean = true
) {
    var sensitivityMultiplier by remember { mutableStateOf(1.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Верхній інформаційний статус-бар
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isConnected) "● Підключено до PC-WORKSTATION" else "○ Відключено",
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontSize = 14.sp
            )
            Text(
                text = "RTT: $connectionPingMs ms",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }

        // Основна сенсорна область (Virtual Touchpad Canvas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Одиночний тап -> лівий клік
                            onSendPacket(
                                InputEventPacket(
                                    type = EventType.MOUSE_CLICK,
                                    button = MouseButton.LEFT
                                )
                            )
                        },
                        onDoubleTap = {
                            // Подвійний тап
                            onSendPacket(
                                InputEventPacket(
                                    type = EventType.MOUSE_CLICK,
                                    button = MouseButton.LEFT
                                )
                            )
                            onSendPacket(
                                InputEventPacket(
                                    type = EventType.MOUSE_CLICK,
                                    button = MouseButton.LEFT
                                )
                            )
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Переміщення курсора
                        onSendPacket(
                            InputEventPacket(
                                type = EventType.MOUSE_MOVE,
                                deltaX = dragAmount.x * sensitivityMultiplier,
                                deltaY = dragAmount.y * sensitivityMultiplier
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Сенсорна зона тачпада\n(Ковзайте для переміщення курсора)",
                color = Color.DarkGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Блок фізичних клавіш миші для зручності керування великими пальцями
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onSendPacket(
                        InputEventPacket(
                            type = EventType.MOUSE_CLICK,
                            button = MouseButton.LEFT
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Text(text = "LMB (Ліва)", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = {
                    onSendPacket(
                        InputEventPacket(
                            type = EventType.MOUSE_CLICK,
                            button = MouseButton.RIGHT
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Text(text = "RMB (Права)", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
