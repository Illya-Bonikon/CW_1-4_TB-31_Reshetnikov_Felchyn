package com.remotepad.client.ui.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotepad.protocol.KeyCodes
import com.remotepad.protocol.ProtocolConstants

private data class SpecialKey(val label: String, val code: Int)

private val specialKeys = listOf(
    listOf(SpecialKey("Esc", KeyCodes.ESCAPE), SpecialKey("Tab", KeyCodes.TAB), SpecialKey("Del", KeyCodes.DELETE), SpecialKey("Bksp", KeyCodes.BACKSPACE)),
    listOf(SpecialKey("Home", KeyCodes.HOME), SpecialKey("Up", KeyCodes.UP), SpecialKey("End", KeyCodes.END), SpecialKey("PgUp", KeyCodes.PAGE_UP)),
    listOf(SpecialKey("Left", KeyCodes.LEFT), SpecialKey("Down", KeyCodes.DOWN), SpecialKey("Right", KeyCodes.RIGHT), SpecialKey("PgDn", KeyCodes.PAGE_DOWN))
)

private val modifierChips = listOf(
    "Ctrl" to ProtocolConstants.MOD_CTRL,
    "Alt" to ProtocolConstants.MOD_ALT,
    "Shift" to ProtocolConstants.MOD_SHIFT,
    "Win" to ProtocolConstants.MOD_META
)

/**
 * Текст набирається системною клавіатурою телефона і йде на ПК одним пакетом TEXT_INPUT,
 * тож працюють і українська розкладка, і автовиправлення. Службові клавіші шлються кодами.
 */
@Composable
fun KeyboardScreen(control: ControlViewModel) {
    KeyboardContent(onSendText = control::sendText, onKey = control::pressKey)
}

@Composable
fun KeyboardContent(onSendText: (String) -> Unit, onKey: (Int, Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var modifiers by rememberSaveable { mutableIntStateOf(0) }
    var numpad by rememberSaveable { mutableStateOf(false) }

    // модифікатор діє на одне натискання, як залипання клавіш у Windows
    val press: (Int) -> Unit = { code ->
        onKey(code, modifiers)
        modifiers = 0
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Текст для ПК") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendText(text); text = "" })
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSendText(text); text = "" }, modifier = Modifier.weight(1f)) { Text("Надіслати") }
            FilledTonalButton(onClick = { press(KeyCodes.ENTER) }, modifier = Modifier.weight(1f)) { Text("Enter") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modifierChips.forEach { (label, bit) ->
                FilterChip(
                    selected = modifiers and bit != 0,
                    onClick = { modifiers = modifiers xor bit },
                    label = { Text(label) }
                )
            }
        }

        specialKeys.forEach { row -> KeyRow(row, press) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Цифровий блок", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
            Switch(checked = numpad, onCheckedChange = { numpad = it })
        }
        if (numpad) {
            listOf(listOf(7, 8, 9), listOf(4, 5, 6), listOf(1, 2, 3)).forEach { row ->
                KeyRow(row.map { SpecialKey(it.toString(), KeyCodes.digit(it)) }, press)
            }
            KeyRow(listOf(SpecialKey("0", KeyCodes.digit(0)), SpecialKey("Enter", KeyCodes.ENTER)), press)
        }
    }
}

@Composable
private fun KeyRow(keys: List<SpecialKey>, onPress: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { key ->
            FilledTonalButton(
                onClick = { onPress(key.code) },
                shape = RoundedCornerShape(10.dp),
                // стандартні бокові відступи кнопки з'їдають місце, і «Home» обрізається до «Hom»
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text(key.label, fontSize = 14.sp, maxLines = 1) }
        }
    }
}
