package com.remotepad.client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HostPCProfile::class, DeviceSession::class, MacroPreset::class],
    version = 2,
    exportSchema = true
)
abstract class RemotePadDatabase : RoomDatabase() {
    abstract fun hosts(): HostPCProfileDao
    abstract fun sessions(): DeviceSessionDao
    abstract fun macros(): MacroPresetDao

    companion object {
        const val NAME = "remotepad.db"

        fun create(context: Context): RemotePadDatabase =
            Room.databaseBuilder(context, RemotePadDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                .build()
    }
}
