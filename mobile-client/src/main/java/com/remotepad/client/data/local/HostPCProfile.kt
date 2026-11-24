package com.remotepad.client.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** ПК, до якого телефон уже підключався. Унікальність за адресою і портом, щоб не плодити дублікати. */
@Entity(
    tableName = "host_profiles",
    indices = [Index(value = ["ipAddress", "port"], unique = true)]
)
data class HostPCProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostName: String,
    val ipAddress: String,
    val port: Int,
    val lastConnected: Long = System.currentTimeMillis()
)

@Dao
interface HostPCProfileDao {
    @Query("SELECT * FROM host_profiles ORDER BY lastConnected DESC")
    fun observeAll(): Flow<List<HostPCProfile>>

    @Query(
        "SELECT * FROM host_profiles " +
            "WHERE ipAddress = :ip AND port = :port LIMIT 1"
    )
    suspend fun find(ip: String, port: Int): HostPCProfile?

    @Upsert
    suspend fun upsert(host: HostPCProfile): Long

    @Query("DELETE FROM host_profiles WHERE id = :id")
    suspend fun delete(id: Long)
}
