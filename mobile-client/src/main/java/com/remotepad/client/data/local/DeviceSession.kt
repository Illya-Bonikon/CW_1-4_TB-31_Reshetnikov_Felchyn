package com.remotepad.client.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Журнал сесій: з ним видно, коли і до якого ПК телефон підключався і скільки тривала робота. */
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
    val isActive: Boolean,
    // з'явилося у версії 2 бази; для старих записів лишається null
    val endTime: Long? = null
)

@Dao
interface DeviceSessionDao {
    @Insert
    suspend fun insert(session: DeviceSession)

    @Query("SELECT * FROM device_sessions WHERE hostId = :hostId ORDER BY authTime DESC")
    fun observeForHost(hostId: Long): Flow<List<DeviceSession>>

    @Query("SELECT * FROM device_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): DeviceSession?

    // після розриву токен на сервері вже недійсний, тому закриваю всі відкриті сесії
    @Query("UPDATE device_sessions SET isActive = 0, endTime = :now WHERE isActive = 1")
    suspend fun endActive(now: Long): Int

    @Delete
    suspend fun delete(session: DeviceSession): Int

    // журнал не потрібен вічно: старі записи прибираю, щоб база не росла
    @Query("DELETE FROM device_sessions WHERE isActive = 0 AND authTime < :before")
    suspend fun deleteOlderThan(before: Long): Int
}
