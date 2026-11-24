package com.remotepad.client.ui.connect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotepad.client.data.local.HostPCProfile
import com.remotepad.protocol.ProtocolConstants

@Composable
fun ConnectScreen(viewModel: ConnectViewModel, onConnected: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.savedHosts.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.wrongPin) {
        if (state.wrongPin) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    ConnectContent(
        state = state,
        saved = saved,
        actions = ConnectActions(
            onSearch = viewModel::search,
            onSelect = viewModel::selectHost,
            onForget = viewModel::forgetHost,
            onIpChange = viewModel::onIpChange,
            onPortChange = viewModel::onPortChange,
            onPinChange = viewModel::onPinChange,
            onConnect = { viewModel.connect(onConnected) }
        )
    )
}

/** Колбеки екрана зібрані в один об'єкт, щоб не тягнути сім параметрів крізь кожну функцію. */
class ConnectActions(
    val onSearch: () -> Unit = {},
    val onSelect: (String, Int) -> Unit = { _, _ -> },
    val onForget: (Long) -> Unit = {},
    val onIpChange: (String) -> Unit = {},
    val onPortChange: (String) -> Unit = {},
    val onPinChange: (String) -> Unit = {},
    val onConnect: () -> Unit = {}
)

@Composable
fun ConnectContent(state: ConnectUiState, saved: List<HostPCProfile>, actions: ConnectActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Підключення до ПК", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = actions.onSearch) { Icon(Icons.Filled.Refresh, "Шукати ще раз") }
                }
            }
        }

        item { SectionTitle("Знайдені в мережі") }
        if (state.discovered.isEmpty() && !state.isSearching) {
            item { Hint("Сервер не знайдено. Запустіть RemotePad на ПК або введіть IP вручну.") }
        }
        items(state.discovered, key = { "found_" + it.ip }) { host ->
            HostCard(Icons.Filled.Computer, host.hostName, "${host.ip}:${host.port}", selected = host.ip == state.ip) {
                actions.onSelect(host.ip, host.port)
            }
        }

        if (saved.isNotEmpty()) {
            item { SectionTitle("Історія з'єднань") }
            items(saved, key = { "saved_" + it.id }) { host ->
                HostCard(
                    Icons.Filled.History, host.hostName, "${host.ipAddress}:${host.port}",
                    selected = host.ipAddress == state.ip,
                    onDelete = { actions.onForget(host.id) }
                ) { actions.onSelect(host.ipAddress, host.port) }
            }
        }

        item { SectionTitle("Адреса вручну") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.ip, onValueChange = actions.onIpChange,
                    label = { Text("IP-адреса") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = state.port, onValueChange = actions.onPortChange,
                    label = { Text("Порт") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SectionTitle("PIN-код із вікна сервера") }
        item { PinField(state.pin, isError = state.wrongPin, onChange = actions.onPinChange) }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        }

        item {
            Button(
                onClick = actions.onConnect,
                enabled = state.canConnect,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 4.dp)
            ) {
                if (state.isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Підключитися", fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
}

@Composable
private fun HostCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Забути цей ПК") }
            }
        }
    }
}

/** Чотири комірки поверх одного прихованого поля: фокус «переходить» сам, бо введення одне. */
@Composable
private fun PinField(pin: String, isError: Boolean, onChange: (String) -> Unit) {
    BasicTextField(
        value = pin,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(ProtocolConstants.PIN_LENGTH) { index ->
                    val borderColor = when {
                        isError -> MaterialTheme.colorScheme.error
                        index == pin.length -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    val digit = pin.getOrNull(index)?.toString().orEmpty()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, borderColor),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(digit, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    )
}
