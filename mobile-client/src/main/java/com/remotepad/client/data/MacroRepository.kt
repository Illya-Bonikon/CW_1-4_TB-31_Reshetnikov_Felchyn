package com.remotepad.client.data

import com.remotepad.client.data.local.DefaultMacros
import com.remotepad.client.data.local.MacroPreset
import com.remotepad.client.data.local.MacroPresetDao
import com.remotepad.client.input.KeyChordParser
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Помилка перевірки макросу; текст показується просто під полем редактора. */
class InvalidMacroException(message: String) : IllegalArgumentException(message)

class MacroRepository(private val dao: MacroPresetDao) {

    val macros: Flow<List<MacroPreset>> = dao.observeAll()

    suspend fun seedDefaults() = dao.insertAll(DefaultMacros.all)

    suspend fun recordUse(macroId: String) {
        dao.incrementUsage(macroId)
    }

    /**
     * Створює новий макрос або оновлює наявний. Комбінацію перевіряю до запису:
     * некоректний рядок у базі дав би картку, яка нічого не робить.
     */
    suspend fun save(title: String, keySequence: String, existing: MacroPreset? = null): MacroPreset {
        val cleanTitle = title.trim()
        val cleanSequence = keySequence.trim().uppercase()
        if (cleanTitle.isEmpty()) throw InvalidMacroException("Вкажіть назву")
        if (KeyChordParser.parse(cleanSequence) == null) {
            throw InvalidMacroException("Невідома комбінація, приклад: CTRL+SHIFT+T")
        }
        return if (existing == null) {
            MacroPreset(
                macroId = UUID.randomUUID().toString(),
                title = cleanTitle,
                keySequence = cleanSequence,
                iconType = "custom",
                category = DefaultMacros.CUSTOM_CATEGORY
            ).also { dao.insert(it) }
        } else {
            existing.copy(title = cleanTitle, keySequence = cleanSequence).also { dao.update(it) }
        }
    }

    suspend fun delete(preset: MacroPreset) {
        dao.delete(preset)
    }
}
