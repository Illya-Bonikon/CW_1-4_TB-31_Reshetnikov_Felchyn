package com.remotepad.client.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotepad.client.data.UserPreference
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val pref by viewModel.preference.collectAsStateWithLifecycle()
    SettingsContent(
        pref = pref,
        onSensitivity = viewModel::setSensitivity,
        onSmoothing = viewModel::setSmoothing,
        onAcceleration = viewModel::setAcceleration,
        onReverseScroll = viewModel::setReverseScroll
    )
}

@Composable
fun SettingsContent(
    pref: UserPreference,
    onSensitivity: (Float) -> Unit,
    onSmoothing: (Float) -> Unit,
    onAcceleration: (Boolean) -> Unit,
    onReverseScroll: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Чутливість: ${format(pref.sensitivity)}")
        Slider(
            value = pref.sensitivity,
            onValueChange = onSensitivity,
            valueRange = UserPreference.SENSITIVITY_RANGE
        )

        Text("Згладжування: ${format(pref.smoothingFactor)}")
        Slider(
            value = pref.smoothingFactor,
            onValueChange = onSmoothing,
            valueRange = UserPreference.SMOOTHING_RANGE
        )

        SwitchRow("Прискорення курсора", pref.acceleration, onAcceleration)
        SwitchRow("Зворотна прокрутка", pref.reverseScroll, onReverseScroll)

        DeveloperCard()
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DeveloperCard() {
    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Про розробника", fontWeight = FontWeight.Medium)
            Text("ПІБ: Решетніков Ілля Володимирович", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            Text("Група: ТВ-31", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            Text("Варіант: 14", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private fun format(value: Float) = String.format(Locale.US, "%.2f", value)
