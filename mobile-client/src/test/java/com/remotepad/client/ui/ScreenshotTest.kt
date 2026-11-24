package com.remotepad.client.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import androidx.compose.runtime.Composable
import com.remotepad.client.data.UserPreference
import com.remotepad.client.data.local.DefaultMacros
import com.remotepad.client.data.local.HostPCProfile
import com.remotepad.client.network.ConnectionStatus
import com.remotepad.client.network.DiscoveredHost
import com.remotepad.client.ui.connect.ConnectActions
import com.remotepad.client.ui.connect.ConnectContent
import com.remotepad.client.ui.connect.ConnectUiState
import com.remotepad.client.ui.control.ControlMode
import com.remotepad.client.ui.control.ControlScaffold
import com.remotepad.client.ui.control.KeyboardContent
import com.remotepad.client.ui.control.MacrosContent
import com.remotepad.client.ui.control.MediaContent
import com.remotepad.client.ui.control.TouchpadContent
import com.remotepad.client.ui.settings.SettingsContent
import com.remotepad.client.ui.theme.RemotePadTheme
import org.junit.Rule
import org.junit.Test

/**
 * Рендерить екрани без емулятора. Тест падає, якщо розмітка не збирається,
 * а отримані PNG я використовую у звіті як знімки реального інтерфейсу.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar"
    )

    private val connected = ConnectionStatus.Connected("WORK-PC")

    @Test
    fun connect() = snap {
        ConnectContent(
            state = ConnectUiState(
                ip = "192.168.1.24",
                pin = "48",
                discovered = listOf(DiscoveredHost("WORK-PC", "192.168.1.24", 8080))
            ),
            saved = listOf(HostPCProfile(id = 1, hostName = "LAB-PC-07", ipAddress = "192.168.1.31", port = 8080)),
            actions = ConnectActions()
        )
    }

    @Test
    fun connectWrongPin() = snap {
        ConnectContent(
            state = ConnectUiState(ip = "192.168.1.24", error = "Невірний PIN-код", wrongPin = true),
            saved = emptyList(),
            actions = ConnectActions()
        )
    }

    @Test
    fun touchpad() = control(ControlMode.TOUCHPAD) { TouchpadContent({ _, _ -> }, {}, {}, {}) }

    @Test
    fun keyboard() = control(ControlMode.KEYBOARD) { KeyboardContent({}, { _, _ -> }) }

    @Test
    fun media() = control(ControlMode.MEDIA) { MediaContent {} }

    @Test
    fun macros() = control(ControlMode.MACROS) { MacrosContent(DefaultMacros.all) {} }

    @Test
    fun settings() = control(ControlMode.SETTINGS) {
        SettingsContent(UserPreference(sensitivity = 1.4f), {}, {}, {}, {})
    }

    @Test
    fun touchpadLandscape() {
        // альбомна орієнтація: перевіряю, що сенсорна зона розтягується на всю ширину
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(orientation = ScreenOrientation.LANDSCAPE))
        control(ControlMode.TOUCHPAD) { TouchpadContent({ _, _ -> }, {}, {}, {}) }
    }

    private fun control(mode: ControlMode, content: @Composable () -> Unit) = snap {
        ControlScaffold(connected, rtt = 6, mode = mode, onModeChange = {}, onDisconnect = {}) { content() }
    }

    private fun snap(content: @Composable () -> Unit) = paparazzi.snapshot { RemotePadTheme(content) }
}
