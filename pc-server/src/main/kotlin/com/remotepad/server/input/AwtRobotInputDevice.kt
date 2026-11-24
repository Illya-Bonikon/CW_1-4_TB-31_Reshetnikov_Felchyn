package com.remotepad.server.input

import com.remotepad.protocol.KeyCodes
import com.remotepad.protocol.MediaAction
import com.remotepad.protocol.MouseButton
import com.remotepad.protocol.ProtocolConstants
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/** Реалізація введення через java.awt.Robot для Windows, macOS і Linux з X11. */
class AwtRobotInputDevice(private val robot: Robot = Robot()) : InputDevice {

    private val isMac = System.getProperty("os.name").lowercase().contains("mac")

    override fun moveBy(dx: Int, dy: Int) {
        val screen = Toolkit.getDefaultToolkit().screenSize
        val current = MouseInfo.getPointerInfo().location
        robot.mouseMove(
            (current.x + dx).coerceIn(0, screen.width - 1),
            (current.y + dy).coerceIn(0, screen.height - 1)
        )
    }

    override fun click(button: MouseButton) {
        val mask = button.toMask() ?: return
        robot.mousePress(mask)
        // без короткої паузи частина програм не встигає побачити натискання
        robot.delay(CLICK_HOLD_MS)
        robot.mouseRelease(mask)
    }

    override fun press(button: MouseButton) {
        button.toMask()?.let(robot::mousePress)
    }

    override fun release(button: MouseButton) {
        button.toMask()?.let(robot::mouseRelease)
    }

    override fun scroll(notches: Int) = robot.mouseWheel(notches)

    override fun keyChord(keyCode: Int, modifiers: Int) {
        val mods = modifierKeys(modifiers)
        mods.forEach(robot::keyPress)
        robot.keyPress(keyCode)
        robot.keyRelease(keyCode)
        mods.asReversed().forEach(robot::keyRelease)
    }

    override fun keyDown(keyCode: Int) = robot.keyPress(keyCode)

    override fun keyUp(keyCode: Int) = robot.keyRelease(keyCode)

    // Robot друкує лише те, що є на поточній розкладці, тому кирилицю вставляю через буфер обміну
    override fun typeText(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        val paste = if (isMac) ProtocolConstants.MOD_META else ProtocolConstants.MOD_CTRL
        keyChord(KeyEvent.VK_V, paste)
    }

    override fun media(action: MediaAction) {
        val key = when (action) {
            MediaAction.PRESENTATION_NEXT -> KeyCodes.RIGHT
            MediaAction.PRESENTATION_PREV -> KeyCodes.LEFT
            MediaAction.PRESENTATION_START -> KeyCodes.F5
            MediaAction.PRESENTATION_EXIT -> KeyCodes.ESCAPE
            // мультимедійних клавіш у AWT немає, тож пауза й гучність лягають на звичні гарячі клавіші плеєрів
            MediaAction.PLAY_PAUSE -> KeyCodes.SPACE
            MediaAction.NEXT_TRACK -> KeyEvent.VK_N
            MediaAction.PREV_TRACK -> KeyEvent.VK_P
            MediaAction.VOLUME_UP -> KeyCodes.UP
            MediaAction.VOLUME_DOWN -> KeyCodes.DOWN
            MediaAction.MUTE -> KeyEvent.VK_M
        }
        keyChord(key, 0)
    }

    private fun modifierKeys(modifiers: Int): List<Int> = buildList {
        if (modifiers and ProtocolConstants.MOD_CTRL != 0) add(KeyEvent.VK_CONTROL)
        if (modifiers and ProtocolConstants.MOD_ALT != 0) add(KeyEvent.VK_ALT)
        if (modifiers and ProtocolConstants.MOD_SHIFT != 0) add(KeyEvent.VK_SHIFT)
        if (modifiers and ProtocolConstants.MOD_META != 0) add(KeyEvent.VK_META)
    }

    private fun MouseButton.toMask(): Int? = when (this) {
        MouseButton.LEFT -> InputEvent.BUTTON1_DOWN_MASK
        MouseButton.MIDDLE -> InputEvent.BUTTON2_DOWN_MASK
        MouseButton.RIGHT -> InputEvent.BUTTON3_DOWN_MASK
        MouseButton.NONE -> null
    }

    private companion object {
        const val CLICK_HOLD_MS = 15
    }
}
