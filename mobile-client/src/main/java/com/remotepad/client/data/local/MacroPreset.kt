package com.remotepad.client.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Картка макросу. keySequence зберігаю рядком на зразок "CTRL+SHIFT+T", його розбирає KeyChordParser. */
@Entity(tableName = "macro_presets")
data class MacroPreset(
    @PrimaryKey val macroId: String,
    val title: String,
    val keySequence: String,
    val iconType: String,
    val category: String,
    // з версії 2: часті макроси піднімаються на початок сітки
    @ColumnInfo(defaultValue = "0") val usageCount: Int = 0
)

@Dao
interface MacroPresetDao {
    @Insert
    suspend fun insert(preset: MacroPreset)

    // набір за замовчуванням додається при кожному запуску, вже наявні записи не чіпаю
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(presets: List<MacroPreset>)

    @Query("SELECT * FROM macro_presets ORDER BY usageCount DESC, category, title")
    fun observeAll(): Flow<List<MacroPreset>>

    @Query("SELECT * FROM macro_presets WHERE macroId = :id")
    suspend fun getById(id: String): MacroPreset?

    @Update
    suspend fun update(preset: MacroPreset): Int

    @Query("UPDATE macro_presets SET usageCount = usageCount + 1 WHERE macroId = :id")
    suspend fun incrementUsage(id: String): Int

    @Delete
    suspend fun delete(preset: MacroPreset): Int
}

/** Набір за замовчуванням, щоб панель не була порожньою після встановлення. */
object DefaultMacros {
    const val CUSTOM_CATEGORY = "Мої"

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
