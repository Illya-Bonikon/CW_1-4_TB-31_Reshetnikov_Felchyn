package com.remotepad.protocol

/**
 * Спільні для клієнта й сервера константи, щоб порт і шлях не розходилися між модулями.
 */
object ProtocolConstants {
    const val DEFAULT_PORT = 8080
    const val CONTROL_PATH = "/control"

    // Запит і відповідь UDP-пошуку: короткі рядки, бо їх шлють широкомовно всій підмережі
    const val DISCOVERY_REQUEST = "REMOTEPAD_DISCOVER"
    const val DISCOVERY_RESPONSE_PREFIX = "REMOTEPAD_HOST"

    const val PIN_LENGTH = 4

    // Біти поля modifiers у InputEventPacket
    const val MOD_SHIFT = 1
    const val MOD_CTRL = 2
    const val MOD_ALT = 4
    const val MOD_META = 8
}
