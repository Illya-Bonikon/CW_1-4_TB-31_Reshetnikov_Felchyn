package com.remotepad.client.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.remotepad.client.data.local.RemotePadDatabase
import org.junit.After
import org.junit.Before

/** База в пам'яті для кожного тесту: тести не залежать один від одного і не лишають файлів. */
abstract class DatabaseTestBase {
    protected lateinit var db: RemotePadDatabase

    @Before
    fun openDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RemotePadDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()
}
