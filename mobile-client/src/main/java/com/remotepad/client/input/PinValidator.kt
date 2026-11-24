package com.remotepad.client.input

import com.remotepad.protocol.ProtocolConstants

/** Правила поля PIN: лише цифри і не більше чотирьох, щоб зайве не йшло на сервер. */
object PinValidator {
    fun sanitize(raw: String): String = raw.filter(Char::isDigit).take(ProtocolConstants.PIN_LENGTH)

    fun isComplete(pin: String): Boolean =
        pin.length == ProtocolConstants.PIN_LENGTH && pin.all(Char::isDigit)
}
