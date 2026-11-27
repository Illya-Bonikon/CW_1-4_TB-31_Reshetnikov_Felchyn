package com.remotepad.client.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Міграції схеми. Кожна зміна схеми отримує нову версію і явну міграцію:
 * дані користувача (історія ПК, власні макроси) не можна втрачати через оновлення застосунку,
 * тому fallbackToDestructiveMigration не використовую.
 */
object Migrations {

    /** Версія 2: лічильник використань макросів і час завершення сесії. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE macro_presets ADD COLUMN usageCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE device_sessions ADD COLUMN endTime INTEGER")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
