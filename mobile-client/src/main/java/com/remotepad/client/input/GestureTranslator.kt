package com.remotepad.client.input

import com.remotepad.client.data.UserPreference
import com.remotepad.protocol.MouseButton
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.truncate

/** Зміщення курсора в пікселях екрана ПК. */
data class Delta(val x: Float, val y: Float)

/**
 * Перетворює рух пальців на тачпаді в дії миші з урахуванням налаштувань користувача.
 * Від Compose не залежить, тож працює в звичайних юніт-тестах.
 */
class GestureTranslator(var preference: UserPreference = UserPreference()) {

    private var smoothX = 0f
    private var smoothY = 0f
    private var scrollRest = 0f

    fun move(dx: Float, dy: Float): Delta {
        // прискорення: швидкий жест проходить більшу відстань, повільний лишається точним
        val gain = if (preference.acceleration) 1f + min(hypot(dx, dy) / ACCEL_DIVISOR, MAX_EXTRA_GAIN) else 1f
        val k = preference.sensitivity * gain
        val a = preference.smoothingFactor
        smoothX = a * smoothX + (1 - a) * dx * k
        smoothY = a * smoothY + (1 - a) * dy * k
        return Delta(smoothX, smoothY)
    }

    /** Вертикальний рух двома пальцями в цілих «клацаннях» коліщатка; залишок чекає наступного руху. */
    fun scroll(dy: Float): Int {
        // напрям як на тачпадах ноутбуків: палець угору прокручує сторінку вниз
        val direction = if (preference.reverseScroll) 1f else -1f
        scrollRest += direction * dy / SCROLL_STEP_PX
        val notches = truncate(scrollRest).toInt()
        scrollRest -= notches
        return notches
    }

    fun tap(pointers: Int): MouseButton = when (pointers) {
        1 -> MouseButton.LEFT
        2 -> MouseButton.RIGHT
        3 -> MouseButton.MIDDLE
        else -> MouseButton.NONE
    }

    fun reset() {
        smoothX = 0f; smoothY = 0f; scrollRest = 0f
    }

    companion object {
        const val SCROLL_STEP_PX = 40f
        private const val ACCEL_DIVISOR = 25f
        private const val MAX_EXTRA_GAIN = 1.5f
    }
}
