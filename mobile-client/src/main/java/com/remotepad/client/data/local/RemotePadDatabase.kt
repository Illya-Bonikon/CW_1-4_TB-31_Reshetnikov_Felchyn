package com.remotepad.client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HostPCProfile::class, DeviceSession::class, MacroPreset::class],
    version = 1,
    exportSchema = true
)
abstract class RemotePadDatabase : RoomDatabase() {
    abstract fun hosts(): HostPCProfileDao
    abstract fun sessions(): DeviceSessionDao
    abstract fun macros(): MacroPresetDao

    companion object {
        fun create(context: Context): RemotePadDatabase =
            Room.databaseBuilder(context, RemotePadDatabase::class.java, "remotepad.db").build()
    }
}
