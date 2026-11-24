package com.remotepad.server

import com.remotepad.server.auth.PinAuthenticator
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinAuthenticatorTest {

    @Test
    fun `verifies only exact pin`() {
        val auth = PinAuthenticator("0420")
        assertTrue(auth.verify("0420"))
        assertFalse(auth.verify("420"))
        assertFalse(auth.verify("0421"))
    }

    @Test
    fun `random pin has four digits`() {
        repeat(50) {
            val pin = PinAuthenticator.random(SecureRandom()).pin
            assertTrue(pin.length == 4 && pin.all(Char::isDigit))
        }
    }

    @Test
    fun `malformed pin is rejected`() {
        assertFailsWith<IllegalArgumentException> { PinAuthenticator("12a4") }
    }
}
