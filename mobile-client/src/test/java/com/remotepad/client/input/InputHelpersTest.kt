package com.remotepad.client.input

import com.remotepad.protocol.KeyCodes
import com.remotepad.protocol.ProtocolConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoveAccumulatorTest {

    @Test
    fun `deltas are summed until drained`() {
        val acc = MoveAccumulator()
        acc.add(Delta(1f, 2f))
        acc.add(Delta(0.5f, -1f))
        assertEquals(Delta(1.5f, 1f), acc.drain())
        assertNull(acc.drain())
    }

    @Test
    fun `clear drops pending movement`() {
        val acc = MoveAccumulator()
        acc.add(Delta(30f, 30f))
        acc.clear()
        assertNull(acc.drain())
    }
}

class KeyChordParserTest {

    @Test
    fun `parses modifiers and key`() {
        val chord = KeyChordParser.parse("Ctrl+Shift+T")!!
        assertEquals(KeyCodes.letter('T'), chord.keyCode)
        assertEquals(ProtocolConstants.MOD_CTRL or ProtocolConstants.MOD_SHIFT, chord.modifiers)
    }

    @Test
    fun `single named key without modifiers`() {
        assertEquals(KeyChord(KeyCodes.F5, 0), KeyChordParser.parse("F5"))
    }

    @Test
    fun `unknown modifier or key gives null`() {
        assertNull(KeyChordParser.parse("HYPER+C"))
        assertNull(KeyChordParser.parse("CTRL+F13"))
        assertNull(KeyChordParser.parse(""))
    }
}

class PinValidatorTest {

    @Test
    fun `non digits are removed and length is limited`() {
        assertEquals("1234", PinValidator.sanitize("1a2 3-45"))
    }

    @Test
    fun `only four digits are complete`() {
        assertTrue(PinValidator.isComplete("0007"))
        assertFalse(PinValidator.isComplete("123"))
    }
}
