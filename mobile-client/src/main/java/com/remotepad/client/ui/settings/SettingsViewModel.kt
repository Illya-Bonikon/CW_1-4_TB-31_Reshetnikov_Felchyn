package com.remotepad.client.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.remotepad.client.AppContainer
import com.remotepad.client.data.SettingsRepository
import com.remotepad.client.data.UserPreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val preference: StateFlow<UserPreference> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreference())

    fun setSensitivity(value: Float) = update { it.copy(sensitivity = value) }
    fun setAcceleration(enabled: Boolean) = update { it.copy(acceleration = enabled) }
    fun setSmoothing(value: Float) = update { it.copy(smoothingFactor = value) }
    fun setReverseScroll(enabled: Boolean) = update { it.copy(reverseScroll = enabled) }

    private fun update(transform: (UserPreference) -> UserPreference) {
        viewModelScope.launch { repository.update(transform) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.settings) }
        }
    }
}
