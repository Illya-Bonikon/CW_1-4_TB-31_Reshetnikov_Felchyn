package com.remotepad.client.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.remotepad.client.AppContainer
import com.remotepad.client.data.local.HostPCProfile
import com.remotepad.client.input.PinValidator
import com.remotepad.client.network.AuthResult
import com.remotepad.client.network.DiscoveredHost
import com.remotepad.client.network.HostTarget
import com.remotepad.protocol.ProtocolConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectUiState(
    val ip: String = "",
    val port: String = ProtocolConstants.DEFAULT_PORT.toString(),
    val pin: String = "",
    val discovered: List<DiscoveredHost> = emptyList(),
    val isSearching: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    // окремий прапорець, щоб екран підсвітив саме поле PIN і дав вібровідгук
    val wrongPin: Boolean = false
) {
    val canConnect: Boolean
        get() = ip.isNotBlank() && port.toIntOrNull() != null && PinValidator.isComplete(pin) && !isConnecting
}

class ConnectViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    val savedHosts: StateFlow<List<HostPCProfile>> = container.history.savedHosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        search()
    }

    fun onIpChange(value: String) = _state.update { it.copy(ip = value.trim(), error = null) }

    fun onPortChange(value: String) = _state.update { it.copy(port = value.filter(Char::isDigit).take(5), error = null) }

    fun onPinChange(value: String) =
        _state.update { it.copy(pin = PinValidator.sanitize(value), error = null, wrongPin = false) }

    fun selectHost(ip: String, port: Int) =
        _state.update { it.copy(ip = ip, port = port.toString(), error = null) }

    fun search() {
        if (_state.value.isSearching) return
        _state.update { it.copy(isSearching = true) }
        viewModelScope.launch {
            val hosts = runCatching { container.discovery.discover() }.getOrDefault(emptyList())
            _state.update { it.copy(discovered = hosts, isSearching = false) }
        }
    }

    fun forgetHost(id: Long) {
        viewModelScope.launch { container.history.forget(id) }
    }

    fun connect(onSuccess: () -> Unit) {
        val current = _state.value
        val port = current.port.toIntOrNull() ?: return
        if (!current.canConnect) return
        _state.update { it.copy(isConnecting = true, error = null, wrongPin = false) }

        viewModelScope.launch {
            val result = container.connection.connect(HostTarget(current.ip, port), current.pin, container.deviceName)
            when (result) {
                is AuthResult.Success -> {
                    container.history.recordLogin(
                        ip = current.ip,
                        port = port,
                        hostName = result.hostName,
                        token = result.token,
                        deviceName = container.deviceName,
                        now = System.currentTimeMillis()
                    )
                    _state.update { it.copy(isConnecting = false, pin = "") }
                    onSuccess()
                }
                is AuthResult.Rejected -> _state.update {
                    it.copy(isConnecting = false, error = result.message, wrongPin = true, pin = "")
                }
                is AuthResult.Unreachable -> _state.update {
                    it.copy(isConnecting = false, error = "${result.message}. $UNREACHABLE_HINT")
                }
            }
        }
    }

    companion object {
        private const val UNREACHABLE_HINT =
            "Перевірте, що ПК і телефон в одній мережі Wi-Fi, або введіть IP вручну"

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ConnectViewModel(container) }
        }
    }
}
