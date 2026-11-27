package com.remotepad.client.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.remotepad.client.data.local.Migrations
import com.remotepad.client.data.local.RemotePadDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Перевіряє міграцію на справжній схемі версії 1 з файлу schemas/.../1.json:
 * дані користувача мають пережити оновлення, а результат збігатися зі схемою версії 2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RemotePadDatabase::class.java
    )

    @Test
    fun `migration 1 to 2 keeps data and adds new columns`() {
        helper.createDatabase(DB, 1).apply {
            execSQL("INSERT INTO host_profiles (id, hostName, ipAddress, port, lastConnected) VALUES (1, 'PC', '10.0.0.5', 8080, 100)")
            execSQL("INSERT INTO device_sessions (sessionToken, hostId, deviceName, authTime, isActive) VALUES ('t1', 1, 'Pixel', 100, 1)")
            execSQL("INSERT INTO macro_presets (macroId, title, keySequence, iconType, category) VALUES ('m1', 'Мій', 'CTRL+M', 'custom', 'Мої')")
            close()
        }

        // validateDroppedTables = true: схема після міграції має точно збігатися з 2.json
        val db = helper.runMigrationsAndValidate(DB, 2, true, *Migrations.ALL)

        db.query("SELECT title, usageCount FROM macro_presets").use {
            assertTrue(it.moveToFirst())
            assertEquals("Мій", it.getString(0))
            assertEquals(0, it.getInt(1))
        }
        db.query("SELECT sessionToken, endTime FROM device_sessions").use {
            assertTrue(it.moveToFirst())
            assertEquals("t1", it.getString(0))
            assertTrue(it.isNull(1))
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
