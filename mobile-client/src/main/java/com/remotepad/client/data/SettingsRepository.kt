package com.remotepad.client.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.remotepad.client.data.UserPreference.Companion.SENSITIVITY_RANGE
import com.remotepad.client.data.UserPreference.Companion.SMOOTHING_RANGE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Налаштування живуть у DataStore, а не в Room: це кілька пар ключ-значення,
 * і читати їх треба як потік, щоб тачпад одразу бачив нову чутливість.
 */
class SettingsRepository(private val context: Context) {

    val preferences: Flow<UserPreference> = context.dataStore.data.map { it.toUserPreference() }

    suspend fun update(transform: (UserPreference) -> UserPreference) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toUserPreference())
            // межі перевіряю тут, щоб жоден екран не зберіг неприйнятне значення
            prefs[SENSITIVITY] = next.sensitivity.coerceIn(SENSITIVITY_RANGE)
            prefs[ACCELERATION] = next.acceleration
            prefs[SMOOTHING] = next.smoothingFactor.coerceIn(SMOOTHING_RANGE)
            prefs[REVERSE_SCROLL] = next.reverseScroll
        }
    }

    private fun Preferences.toUserPreference() = UserPreference(
        sensitivity = this[SENSITIVITY] ?: UserPreference.DEFAULT_SENSITIVITY,
        acceleration = this[ACCELERATION] ?: true,
        smoothingFactor = this[SMOOTHING] ?: 0f,
        reverseScroll = this[REVERSE_SCROLL] ?: false
    )

    private companion object {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val ACCELERATION = booleanPreferencesKey("acceleration")
        val SMOOTHING = floatPreferencesKey("smoothing_factor")
        val REVERSE_SCROLL = booleanPreferencesKey("reverse_scroll")
    }
}
