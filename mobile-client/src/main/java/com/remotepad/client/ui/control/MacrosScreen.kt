package com.remotepad.client.ui.control

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotepad.client.data.local.MacroPreset
import kotlinx.coroutines.launch

@Composable
fun MacrosScreen(control: ControlViewModel) {
    val macros by control.macros.collectAsStateWithLifecycle()
    MacrosContent(macros, onRun = control::runMacro, onSave = control::saveMacro, onDelete = control::deleteMacro)
}

/** Стан відкритого редактора: existing == null означає новий макрос. */
private data class EditorState(val existing: MacroPreset?, val title: String, val sequence: String)

@Composable
fun MacrosContent(
    macros: List<MacroPreset>,
    onRun: (MacroPreset) -> Unit,
    onSave: suspend (String, String, MacroPreset?) -> String? = { _, _, _ -> null },
    onDelete: (MacroPreset) -> Unit = {}
) {
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(macros, key = { it.macroId }) { macro ->
                MacroCard(
                    macro,
                    onClick = { onRun(macro) },
                    onLongClick = { editor = EditorState(macro, macro.title, macro.keySequence) }
                )
            }
            item(key = "add") {
                AddCard { editor = EditorState(null, "", "") }
            }
        }

        editor?.let { state ->
            MacroEditor(
                isNew = state.existing == null,
                title = state.title,
                sequence = state.sequence,
                error = error,
                onTitleChange = { editor = state.copy(title = it); error = null },
                onSequenceChange = { editor = state.copy(sequence = it); error = null },
                onSave = {
                    scope.launch {
                        error = onSave(state.title, state.sequence, state.existing)
                        if (error == null) editor = null
                    }
                },
                onDelete = state.existing?.let { existing -> { onDelete(existing); editor = null } },
                onCancel = { editor = null; error = null }
            )
        }
    }
}

// довге натискання відкриває редагування: звичайний дотик має лишатися швидким запуском
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MacroCard(macro: MacroPreset, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(96.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(macro.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2)
            Text(macro.keySequence, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text(macro.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddCard(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(96.dp).combinedClickable(onClick = onClick)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = "Додати макрос")
        }
    }
}

/**
 * Редактор поверх сітки. Звичайний Dialog відкривається в окремому вікні, і тест знімків
 * екрана його не бачить, тому малюю картку з затемненням у тому ж дереві Compose.
 */
@Composable
fun MacroEditor(
    isNew: Boolean,
    title: String,
    sequence: String,
    error: String?,
    onTitleChange: (String) -> Unit,
    onSequenceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (isNew) "Новий макрос" else "Редагування макросу", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                OutlinedTextField(
                    value = title, onValueChange = onTitleChange, label = { Text("Назва") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sequence, onValueChange = onSequenceChange, label = { Text("Комбінація, напр. CTRL+SHIFT+T") },
                    singleLine = true, isError = error != null, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("Видалити", color = MaterialTheme.colorScheme.error) }
                    }
                    Box(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Скасувати") }
                    Button(onClick = onSave) { Text("Зберегти") }
                }
            }
        }
    }
}
