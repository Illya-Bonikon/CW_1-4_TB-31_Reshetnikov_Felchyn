package com.remotepad.client.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Картка макросу. keySequence зберігаю рядком на зразок "CTRL+SHIFT+T", його розбирає KeyChordParser. */
@Entity(tableName = "macro_presets")
data class MacroPreset(
    @PrimaryKey val macroId: String,
    val title: String,
    val keySequence: String,
    val iconType: String,
    val category: String
)

@Dao
interface MacroPresetDao {
    @Query("SELECT * FROM macro_presets ORDER BY category, title")
    fun observeAll(): Flow<List<MacroPreset>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(presets: List<MacroPreset>)
}

/** Набір за замовчуванням, щоб панель не була порожньою після встановлення. */
object DefaultMacros {
    val all = listOf(
        MacroPreset("copy", "Копіювати", "CTRL+C", "copy", "Редагування"),
        MacroPreset("paste", "Вставити", "CTRL+V", "paste", "Редагування"),
        MacroPreset("cut", "Вирізати", "CTRL+X", "cut", "Редагування"),
        MacroPreset("undo", "Скасувати", "CTRL+Z", "undo", "Редагування"),
        MacroPreset("select_all", "Виділити все", "CTRL+A", "select", "Редагування"),
        MacroPreset("save", "Зберегти", "CTRL+S", "save", "Файл"),
        MacroPreset("switch_window", "Інше вікно", "ALT+TAB", "window", "Вікна"),
        MacroPreset("close_tab", "Закрити вкладку", "CTRL+W", "close", "Вікна"),
        MacroPreset("reopen_tab", "Повернути вкладку", "CTRL+SHIFT+T", "restore", "Вікна")
    )
}
