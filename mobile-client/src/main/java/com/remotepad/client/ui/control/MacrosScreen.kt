package com.remotepad.client.ui.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotepad.client.data.local.MacroPreset

@Composable
fun MacrosScreen(control: ControlViewModel) {
    val macros by control.macros.collectAsStateWithLifecycle()
    MacrosContent(macros, onRun = control::runMacro)
}

@Composable
fun MacrosContent(macros: List<MacroPreset>, onRun: (MacroPreset) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(macros, key = { it.macroId }) { macro ->
            Card(Modifier.fillMaxWidth().height(96.dp).clickable { onRun(macro) }) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(macro.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2)
                    Text(macro.keySequence, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text(macro.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
