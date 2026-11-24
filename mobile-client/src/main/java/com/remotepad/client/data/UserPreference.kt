package com.remotepad.client.data

/** Налаштування тачпада. Межі значень тримаю тут, щоб слайдери і сховище не розходилися. */
data class UserPreference(
    val sensitivity: Float = DEFAULT_SENSITIVITY,
    val acceleration: Boolean = true,
    val smoothingFactor: Float = 0f,
    val reverseScroll: Boolean = false
) {
    companion object {
        const val DEFAULT_SENSITIVITY = 1.0f
        val SENSITIVITY_RANGE = 0.5f..3.0f
        val SMOOTHING_RANGE = 0f..0.8f
    }
}
