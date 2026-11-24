package com.remotepad.client.integration

import com.remotepad.protocol.MediaAction
import com.remotepad.protocol.MouseButton
import com.remotepad.server.input.InputDevice
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Замість java.awt.Robot записує, що дійшло до «операційної системи», і коли саме.
 * Час беру System.nanoTime() у тому ж процесі, що й клієнт, тож годинники не розходяться.
 */
class RecordingInputDevice : InputDevice {
    data class Arrival(val keyCode: Int, val nanoTime: Long)

    val keys = LinkedBlockingQueue<Arrival>()
    val movedPixels = AtomicInteger()
    val clicks = LinkedBlockingQueue<MouseButton>()

    override fun keyChord(keyCode: Int, modifiers: Int) {
        keys.put(Arrival(keyCode, System.nanoTime()))
    }

    override fun moveBy(dx: Int, dy: Int) {
        movedPixels.addAndGet(dx)
    }

    override fun click(button: MouseButton) = clicks.put(button)
    override fun press(button: MouseButton) = Unit
    override fun release(button: MouseButton) = Unit
    override fun scroll(notches: Int) = Unit
    override fun keyDown(keyCode: Int) = Unit
    override fun keyUp(keyCode: Int) = Unit
    override fun typeText(text: String) = Unit
    override fun media(action: MediaAction) = Unit
}
