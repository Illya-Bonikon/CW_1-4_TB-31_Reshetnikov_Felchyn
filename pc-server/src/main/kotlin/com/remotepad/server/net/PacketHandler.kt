package com.remotepad.server.net

import com.remotepad.protocol.AuthResponse
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.PacketCodec
import com.remotepad.server.auth.PinAuthenticator
import com.remotepad.server.auth.SessionRegistry
import com.remotepad.server.input.CursorSmoother
import com.remotepad.server.input.InputDevice
import kotlin.math.roundToInt

/** Стан одного WebSocket-з'єднання: токен і лічильник помилкових PIN. */
class ConnectionState {
    var token: String? = null
    var failedAttempts = 0
}

/**
 * Розбирає пакети одного з'єднання і передає дії в InputDevice.
 * Мережевого коду тут немає, тому логіку можна перевірити звичайними юніт-тестами.
 */
class PacketHandler(
    private val authenticator: PinAuthenticator,
    private val sessions: SessionRegistry,
    private val input: InputDevice,
    private val smoother: CursorSmoother,
    private val hostName: String,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val maxPinAttempts: Int = 5
) {

    /** Повертає відповідь для клієнта або null, якщо відповідати нема чого. */
    fun handle(packet: InputEventPacket, state: ConnectionState): InputEventPacket? {
        when (packet.type) {
            EventType.AUTH_REQUEST -> return authenticate(packet, state)
            // PONG повертає час клієнта без змін, щоб той порахував RTT своїм годинником
            EventType.PING -> return InputEventPacket(
                type = EventType.PONG,
                sequenceNumber = packet.sequenceNumber,
                timestamp = packet.timestamp
            )
            else -> Unit
        }

        // без чинного токена жодна подія не дійде до системного введення
        if (state.token == null || packet.sessionToken != state.token || !sessions.isValid(state.token)) return null
        dispatch(packet)
        return null
    }

    fun onDisconnect(state: ConnectionState) {
        state.token?.let(sessions::close)
        state.token = null
        smoother.reset()
    }

    fun isBlocked(state: ConnectionState): Boolean = state.failedAttempts >= maxPinAttempts

    private fun authenticate(packet: InputEventPacket, state: ConnectionState): InputEventPacket {
        val request = runCatching { PacketCodec.decodeAuthRequest(packet.text.orEmpty()) }.getOrNull()
        val response = when {
            request == null -> AuthResponse(isSuccess = false, errorMessage = "Некоректний запит авторизації")
            isBlocked(state) -> AuthResponse(isSuccess = false, errorMessage = "Забагато спроб, перепідключіться")
            !authenticator.verify(request.pinCode) -> {
                state.failedAttempts++
                AuthResponse(isSuccess = false, errorMessage = "Невірний PIN-код")
            }
            else -> {
                val token = sessions.open(request.deviceName)
                if (token == null) {
                    AuthResponse(isSuccess = false, errorMessage = "Досягнуто ліміту підключених пристроїв")
                } else {
                    state.token = token
                    smoother.reset()
                    AuthResponse(
                        isSuccess = true,
                        sessionToken = token,
                        serverHostName = hostName,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight
                    )
                }
            }
        }
        return InputEventPacket(type = EventType.AUTH_RESPONSE, text = PacketCodec.encodeAuthResponse(response))
    }

    private fun dispatch(packet: InputEventPacket) {
        when (packet.type) {
            EventType.MOUSE_MOVE -> {
                val (dx, dy) = smoother.next(packet.deltaX, packet.deltaY)
                if (dx != 0 || dy != 0) input.moveBy(dx, dy)
            }
            EventType.MOUSE_CLICK -> input.click(packet.button)
            EventType.MOUSE_DOWN -> input.press(packet.button)
            EventType.MOUSE_UP -> input.release(packet.button)
            EventType.MOUSE_WHEEL -> packet.deltaY.roundToInt().takeIf { it != 0 }?.let(input::scroll)
            EventType.KEY_TYPE -> input.keyChord(packet.keyCode, packet.modifiers)
            EventType.KEY_DOWN -> input.keyDown(packet.keyCode)
            EventType.KEY_UP -> input.keyUp(packet.keyCode)
            EventType.TEXT_INPUT -> packet.text?.takeIf { it.isNotEmpty() }?.let(input::typeText)
            EventType.MEDIA_ACTION -> packet.mediaAction?.let(input::media)
            // макроси клієнт розгортає сам у KEY_TYPE, окремої обробки не потрібно
            else -> Unit
        }
    }
}
