package com.remotepad.server.input

import kotlin.math.roundToInt

/**
 * Експоненційне згладжування (EMA) дельт курсора: s = a * s + (1 - a) * d.
 * Дробову частину не викидаю, а переношу на наступний крок, інакше повільний рух
 * пальця округлюється до нуля і курсор стоїть.
 */
class CursorSmoother(
    private val alpha: Float = 0.35f,
    private val sensitivity: Float = 1.35f
) {
    init {
        require(alpha in 0f..<1f) { "alpha має бути в [0; 1)" }
    }

    private var smoothX = 0f
    private var smoothY = 0f
    private var restX = 0f
    private var restY = 0f

    fun next(dx: Float, dy: Float): Pair<Int, Int> {
        smoothX = alpha * smoothX + (1 - alpha) * dx
        smoothY = alpha * smoothY + (1 - alpha) * dy
        val fullX = smoothX * sensitivity + restX
        val fullY = smoothY * sensitivity + restY
        val stepX = fullX.roundToInt()
        val stepY = fullY.roundToInt()
        restX = fullX - stepX
        restY = fullY - stepY
        return stepX to stepY
    }

    fun reset() {
        smoothX = 0f; smoothY = 0f; restX = 0f; restY = 0f
    }
}
