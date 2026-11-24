package com.remotepad.protocol

import kotlinx.serialization.json.Json

/**
 * Єдине місце, де налаштовано JSON. Клієнт і сервер мають кодувати пакети однаково,
 * інакше нове поле в одному модулі зламає розбір в іншому.
 */
object PacketCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        // поля зі значенням за замовчуванням не шлю: пакет MOUSE_MOVE стає втричі коротшим
        encodeDefaults = false
        explicitNulls = false
    }

    fun encode(packet: InputEventPacket): String = json.encodeToString(InputEventPacket.serializer(), packet)

    fun decode(raw: String): InputEventPacket = json.decodeFromString(InputEventPacket.serializer(), raw)

    fun encodeAuthRequest(request: AuthRequest): String = json.encodeToString(AuthRequest.serializer(), request)

    fun decodeAuthRequest(raw: String): AuthRequest = json.decodeFromString(AuthRequest.serializer(), raw)

    fun encodeAuthResponse(response: AuthResponse): String = json.encodeToString(AuthResponse.serializer(), response)

    fun decodeAuthResponse(raw: String): AuthResponse = json.decodeFromString(AuthResponse.serializer(), raw)

    /** Відповідь сервера на UDP-пошук: "REMOTEPAD_HOST;<ім'я>;<порт>". */
    fun encodeDiscoveryResponse(hostName: String, port: Int): String =
        listOf(ProtocolConstants.DISCOVERY_RESPONSE_PREFIX, hostName.replace(";", " "), port).joinToString(";")

    fun decodeDiscoveryResponse(raw: String): Pair<String, Int>? {
        val parts = raw.trim().split(";")
        if (parts.size != 3 || parts[0] != ProtocolConstants.DISCOVERY_RESPONSE_PREFIX) return null
        val port = parts[2].toIntOrNull() ?: return null
        return parts[1] to port
    }
}
