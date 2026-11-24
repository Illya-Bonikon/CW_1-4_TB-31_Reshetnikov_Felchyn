package com.remotepad.client.ui.control

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Розпізнавання жестів тачпада. Стандартний detectDragGestures бачить лише один палець,
 * а тут потрібна кількість пальців: один рухає курсор, два прокручують, тап двома дає правий клік.
 */
fun Modifier.touchpadGestures(
    onMove: (Float, Float) -> Unit,
    onScroll: (Float) -> Unit,
    onTap: (Int) -> Unit
): Modifier = pointerInput(Unit) {
    val tapSlop = viewConfiguration.touchSlop
    val tapTimeout = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        val startTime = first.uptimeMillis
        var endTime = startTime
        var maxPointers = 1
        var travel = 0f

        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            maxPointers = maxOf(maxPointers, pressed.size)
            // беру лише пальці, що вже лежали на екрані: новий дотик дав би ривок
            val moving = pressed.filter { it.previousPressed }
            if (moving.isNotEmpty()) {
                var sum = Offset.Zero
                moving.forEach { sum += it.position - it.previousPosition }
                val (dx, dy) = sum / moving.size.toFloat()
                travel += abs(dx) + abs(dy)
                when {
                    pressed.size >= 2 -> onScroll(dy)
                    // після двопальцевого жесту палець, що лишився, курсор не рухає
                    maxPointers == 1 -> onMove(dx, dy)
                }
            }
            event.changes.forEach {
                endTime = maxOf(endTime, it.uptimeMillis)
                it.consume()
            }
        } while (event.changes.any { it.pressed })

        if (travel < tapSlop && endTime - startTime < tapTimeout) onTap(maxPointers)
    }
}
