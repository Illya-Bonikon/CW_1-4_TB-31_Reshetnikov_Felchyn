package com.remotepad.server.auth

import com.remotepad.protocol.ProtocolConstants
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Перевіряє PIN, який оператор бачить у консолі сервера.
 * Порівнюю через MessageDigest.isEqual, щоб час відповіді не підказував правильні цифри.
 */
class PinAuthenticator(val pin: String) {

    init {
        require(pin.length == ProtocolConstants.PIN_LENGTH && pin.all(Char::isDigit)) {
            "PIN має складатися з ${ProtocolConstants.PIN_LENGTH} цифр"
        }
    }

    fun verify(candidate: String): Boolean =
        MessageDigest.isEqual(pin.toByteArray(), candidate.toByteArray())

    companion object {
        fun random(random: SecureRandom = SecureRandom()): PinAuthenticator {
            val pin = (1..ProtocolConstants.PIN_LENGTH).joinToString("") { random.nextInt(10).toString() }
            return PinAuthenticator(pin)
        }
    }
}
