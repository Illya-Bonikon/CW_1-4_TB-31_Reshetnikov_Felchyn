package com.remotepad.client.ui.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.remotepad.client.AppContainer
import com.remotepad.client.data.InvalidMacroException
import com.remotepad.client.data.local.MacroPreset
import com.remotepad.client.input.GestureTranslator
import com.remotepad.client.input.KeyChordParser
import com.remotepad.client.input.MoveAccumulator
import com.remotepad.client.network.ConnectionStatus
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.MediaAction
import com.remotepad.protocol.MouseButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Спільна для всіх режимів керування ViewModel: тачпад, клавіатура, медіа і макроси шлють пакети через неї. */
class ControlViewModel(private val container: AppContainer) : ViewModel() {

    private val connection = container.connection
    private val translator = GestureTranslator()
    private val moves = MoveAccumulator()

    val status: StateFlow<ConnectionStatus> = connection.status
    val rttMs: StateFlow<Long?> = connection.rttMs
    val macros: StateFlow<List<MacroPreset>> = container.macros.macros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { container.macros.seedDefaults() }
        viewModelScope.launch {
            container.settings.preferences.collect { translator.preference = it }
        }
        viewModelScope.launch {
            status.collect { if (it !is ConnectionStatus.Connected) dropPendingInput() }
        }
        viewModelScope.launch {
            while (isActive) {
                moves.drain()?.let { (dx, dy) ->
                    send(InputEventPacket(EventType.MOUSE_MOVE, deltaX = dx, deltaY = dy))
                }
                delay(MOVE_FLUSH_MS)
            }
        }
    }

    fun onDrag(dx: Float, dy: Float) = moves.add(translator.move(dx, dy))

    fun onScroll(dy: Float) {
        val notches = translator.scroll(dy)
        if (notches != 0) send(InputEventPacket(type = EventType.MOUSE_WHEEL, deltaY = notches.toFloat()))
    }

    fun onTap(pointers: Int) {
        val button = translator.tap(pointers)
        if (button != MouseButton.NONE) click(button)
    }

    fun click(button: MouseButton) = send(InputEventPacket(type = EventType.MOUSE_CLICK, button = button))

    fun sendText(text: String) {
        if (text.isNotEmpty()) send(InputEventPacket(type = EventType.TEXT_INPUT, text = text))
    }

    fun pressKey(keyCode: Int, modifiers: Int = 0) =
        send(InputEventPacket(type = EventType.KEY_TYPE, keyCode = keyCode, modifiers = modifiers))

    fun media(action: MediaAction) = send(InputEventPacket(type = EventType.MEDIA_ACTION, mediaAction = action))

    fun runMacro(preset: MacroPreset) {
        val chord = KeyChordParser.parse(preset.keySequence) ?: return
        pressKey(chord.keyCode, chord.modifiers)
        viewModelScope.launch { container.macros.recordUse(preset.macroId) }
    }

    /** Повертає текст помилки для редактора або null, якщо макрос збережено. */
    suspend fun saveMacro(title: String, keySequence: String, existing: MacroPreset?): String? =
        try {
            container.macros.save(title, keySequence, existing)
            null
        } catch (e: InvalidMacroException) {
            e.message
        }

    fun deleteMacro(preset: MacroPreset) {
        viewModelScope.launch { container.macros.delete(preset) }
    }

    fun disconnect() {
        connection.disconnect()
        viewModelScope.launch { container.history.endSession(System.currentTimeMillis()) }
    }

    // накопичений рух після розриву вже неактуальний: відправлений пізніше, він смикне курсор
    private fun dropPendingInput() {
        moves.clear()
        translator.reset()
    }

    private fun send(packet: InputEventPacket) {
        connection.send(packet)
    }

    companion object {
        private const val MOVE_FLUSH_MS = 8L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ControlViewModel(container) }
        }
    }
}
