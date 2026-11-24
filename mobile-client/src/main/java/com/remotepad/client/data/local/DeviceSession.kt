package com.remotepad.client.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/** Журнал сесій: з ним видно, коли і до якого ПК телефон підключався востаннє. */
@Entity(
    tableName = "device_sessions",
    foreignKeys = [ForeignKey(
        entity = HostPCProfile::class,
        parentColumns = ["id"],
        childColumns = ["hostId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("hostId")]
)
data class DeviceSession(
    @PrimaryKey val sessionToken: String,
    val hostId: Long,
    val deviceName: String,
    val authTime: Long,
    val isActive: Boolean
)

@Dao
interface DeviceSessionDao {
    @Insert
    suspend fun insert(session: DeviceSession)

    // після розриву токен на сервері вже недійсний, тому позначаю всі сесії неактивними
    @Query("UPDATE device_sessions SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll()
}
