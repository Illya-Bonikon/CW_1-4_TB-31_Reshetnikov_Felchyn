package com.remotepad.server

import com.remotepad.protocol.MediaAction
import com.remotepad.protocol.MouseButton
import com.remotepad.server.input.InputDevice

/** Записує виклики замість реального введення, щоб тести бачили, що дійшло до системи. */
class FakeInputDevice : InputDevice {
    val calls = mutableListOf<String>()

    override fun moveBy(dx: Int, dy: Int) { calls += "move $dx $dy" }
    override fun click(button: MouseButton) { calls += "click $button" }
    override fun press(button: MouseButton) { calls += "press $button" }
    override fun release(button: MouseButton) { calls += "release $button" }
    override fun scroll(notches: Int) { calls += "scroll $notches" }
    override fun keyChord(keyCode: Int, modifiers: Int) { calls += "chord $keyCode $modifiers" }
    override fun keyDown(keyCode: Int) { calls += "down $keyCode" }
    override fun keyUp(keyCode: Int) { calls += "up $keyCode" }
    override fun typeText(text: String) { calls += "text $text" }
    override fun media(action: MediaAction) { calls += "media $action" }
}
