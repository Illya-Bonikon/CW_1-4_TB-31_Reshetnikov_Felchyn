package com.remotepad.client.input

import com.remotepad.protocol.KeyCodes
import com.remotepad.protocol.ProtocolConstants

data class KeyChord(val keyCode: Int, val modifiers: Int)

/** Розбирає рядок макросу "CTRL+SHIFT+T" у код клавіші та маску модифікаторів. */
object KeyChordParser {
    private val modifierBits = mapOf(
        "CTRL" to ProtocolConstants.MOD_CTRL,
        "ALT" to ProtocolConstants.MOD_ALT,
        "SHIFT" to ProtocolConstants.MOD_SHIFT,
        "META" to ProtocolConstants.MOD_META,
        "WIN" to ProtocolConstants.MOD_META,
        "CMD" to ProtocolConstants.MOD_META
    )

    fun parse(sequence: String): KeyChord? {
        val parts = sequence.split('+').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        var modifiers = 0
        for (part in parts.dropLast(1)) {
            modifiers = modifiers or (modifierBits[part] ?: return null)
        }
        val key = KeyCodes.byName(parts.last()) ?: return null
        return KeyChord(key, modifiers)
    }
}
