package com.remotepad.client.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
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
    @Insert
    suspend fun insert(host: HostPCProfile): Long

    @Query("SELECT * FROM host_profiles ORDER BY lastConnected DESC")
    fun observeAll(): Flow<List<HostPCProfile>>

    @Query("SELECT * FROM host_profiles WHERE id = :id")
    suspend fun getById(id: Long): HostPCProfile?

    @Query(
        "SELECT * FROM host_profiles " +
            "WHERE ipAddress = :ip AND port = :port LIMIT 1"
    )
    suspend fun find(ip: String, port: Int): HostPCProfile?

    @Update
    suspend fun update(host: HostPCProfile): Int

    @Delete
    suspend fun delete(host: HostPCProfile): Int

    // сесії цього ПК зникнуть разом із ним завдяки ON DELETE CASCADE
    @Query("DELETE FROM host_profiles WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
