package com.remotepad.server.input

import com.remotepad.protocol.MediaAction
import com.remotepad.protocol.MouseButton

/** Абстракція над системним введенням. У тестах її підміняє фейк, бо Robot не працює без дисплея. */
interface InputDevice {
    fun moveBy(dx: Int, dy: Int)
    fun click(button: MouseButton)
    fun press(button: MouseButton)
    fun release(button: MouseButton)
    fun scroll(notches: Int)
    fun keyChord(keyCode: Int, modifiers: Int)
    fun keyDown(keyCode: Int)
    fun keyUp(keyCode: Int)
    fun typeText(text: String)
    fun media(action: MediaAction)
}
