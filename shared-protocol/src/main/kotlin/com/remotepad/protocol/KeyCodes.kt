package com.remotepad.protocol

/**
 * Коди клавіш у протоколі. Значення збігаються з java.awt.event.KeyEvent.VK_*,
 * тож сервер передає їх у Robot без перекладу, а Android-клієнт не тягне AWT.
 */
object KeyCodes {
    const val ENTER = 10
    const val BACKSPACE = 8
    const val TAB = 9
    const val ESCAPE = 27
    const val SPACE = 32
    const val PAGE_UP = 33
    const val PAGE_DOWN = 34
    const val END = 35
    const val HOME = 36
    const val LEFT = 37
    const val UP = 38
    const val RIGHT = 39
    const val DOWN = 40
    const val DELETE = 127
    const val F5 = 116

    // Літери й цифри в AWT мають коди своїх ASCII-символів у верхньому регістрі
    fun letter(c: Char): Int = c.uppercaseChar().code
    fun digit(d: Int): Int = '0'.code + d

    private val named = mapOf(
        "ENTER" to ENTER, "BACKSPACE" to BACKSPACE, "TAB" to TAB, "ESC" to ESCAPE,
        "SPACE" to SPACE, "PGUP" to PAGE_UP, "PGDN" to PAGE_DOWN, "END" to END, "HOME" to HOME,
        "LEFT" to LEFT, "UP" to UP, "RIGHT" to RIGHT, "DOWN" to DOWN, "DEL" to DELETE, "F5" to F5
    )

    /** Назва клавіші з макросу ("C", "7", "TAB") у код; null, якщо назву не знаю. */
    fun byName(name: String): Int? {
        val upper = name.trim().uppercase()
        named[upper]?.let { return it }
        if (upper.length != 1) return null
        val c = upper[0]
        return when (c) {
            in 'A'..'Z' -> letter(c)
            in '0'..'9' -> digit(c - '0')
            else -> null
        }
    }
}
