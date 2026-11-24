package com.remotepad.client.input

/**
 * Накопичує дрібні дельти між відправками. Сенсор дає подію на кожен кадр,
 * а сервер не потребує десятків крихітних пакетів, тому шлю суму раз на такт.
 * Після розриву зв'язку буфер очищую, інакше курсор стрибне при відновленні.
 */
class MoveAccumulator {
    private var x = 0f
    private var y = 0f

    @Synchronized
    fun add(delta: Delta) {
        x += delta.x
        y += delta.y
    }

    @Synchronized
    fun drain(): Delta? {
        if (x == 0f && y == 0f) return null
        return Delta(x, y).also { clear() }
    }

    @Synchronized
    fun clear() {
        x = 0f
        y = 0f
    }
}
