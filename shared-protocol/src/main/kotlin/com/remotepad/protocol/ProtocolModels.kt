package com.remotepad.protocol

import kotlinx.serialization.Serializable

/**
 * Типи вхідних подій периферії від мобільного клієнта до ПК-сервера.
 */
@Serializable
enum class EventType {
    // Події курсора та жестів тачпада
    MOUSE_MOVE,
    MOUSE_CLICK,
    MOUSE_DOWN,
    MOUSE_UP,
    MOUSE_WHEEL,
    
    // Події клавіатури та вводу тексту
    KEY_DOWN,
    KEY_UP,
    KEY_TYPE,
    TEXT_INPUT,
    
    // Макроси та системні комбінації
    MACRO_EXECUTE,
    MEDIA_ACTION,
    
    // Системні службові повідомлення
    PING,
    PONG,
    AUTH_REQUEST,
    AUTH_RESPONSE,
    DISCONNECT
}

/**
 * Ідентифікатори кнопок миші.
 */
@Serializable
enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    NONE
}

/**
 * Дії медіа-пульта.
 */
@Serializable
enum class MediaAction {
    PLAY_PAUSE,
    NEXT_TRACK,
    PREV_TRACK,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    PRESENTATION_NEXT,
    PRESENTATION_PREV,
    PRESENTATION_START,
    PRESENTATION_EXIT
}

/**
 * Основний пакет передавання подій введення через WebSocket / UDP.
 * Оптимізовано для мінімального оверхеду серіалізації.
 */
@Serializable
data class InputEventPacket(
    val type: EventType,
    val sequenceNumber: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Координати та дельти (для тачпада та скролінгу)
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    
    // Дані для кліків миші
    val button: MouseButton = MouseButton.NONE,
    
    // Дані для клавіатурного вводу
    val keyCode: Int = 0,
    val text: String? = null,
    val modifiers: Int = 0, // 1: Shift, 2: Ctrl, 4: Alt, 8: Meta
    
    // Дії макросів та медіа
    val macroId: String? = null,
    val mediaAction: MediaAction? = null,
    
    // Токен активної сесії
    val sessionToken: String? = null
)

/**
 * Модель запиту на авторизацію клієнта на ПК-сервері.
 */
@Serializable
data class AuthRequest(
    val deviceName: String,
    val clientIp: String,
    val pinCode: String,
    val clientVersion: String = "1.0.0",
    // токен Firebase Cloud Messaging; старі клієнти поле не шлють, і сервер просто не надсилає їм push
    val pushToken: String? = null
)

/**
 * Модель відповіді сервера на авторизацію.
 */
@Serializable
data class AuthResponse(
    val isSuccess: Boolean,
    val sessionToken: String? = null,
    val errorMessage: String? = null,
    val serverHostName: String? = null,
    val screenWidth: Int = 1920,
    val screenHeight: Int = 1080
)
