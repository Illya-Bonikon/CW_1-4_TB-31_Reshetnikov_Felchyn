package com.remotepad.client.data

import com.remotepad.client.data.local.DeviceSession
import com.remotepad.client.data.local.DeviceSessionDao
import com.remotepad.client.data.local.HostPCProfile
import com.remotepad.client.data.local.HostPCProfileDao
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/** Історія підключень: профілі ПК і журнал сесій. ViewModel не знає, які тут таблиці. */
class ConnectionHistoryRepository(
    private val hosts: HostPCProfileDao,
    private val sessions: DeviceSessionDao
) {
    val savedHosts: Flow<List<HostPCProfile>> = hosts.observeAll()

    /** Запам'ятовує успішний вхід і повертає id профілю ПК. */
    suspend fun recordLogin(
        ip: String,
        port: Int,
        hostName: String,
        token: String,
        deviceName: String,
        now: Long
    ): Long {
        val existing = hosts.find(ip, port)
        val hostId = if (existing == null) {
            hosts.insert(HostPCProfile(hostName = hostName, ipAddress = ip, port = port, lastConnected = now))
        } else {
            hosts.update(existing.copy(hostName = hostName, lastConnected = now))
            existing.id
        }
        sessions.endActive(now)
        sessions.insert(DeviceSession(token, hostId, deviceName, authTime = now, isActive = true))
        sessions.deleteOlderThan(now - SESSION_RETENTION_MS)
        return hostId
    }

    suspend fun endSession(now: Long) {
        sessions.endActive(now)
    }

    suspend fun forget(hostId: Long) {
        hosts.deleteById(hostId)
    }

    companion object {
        val SESSION_RETENTION_MS = TimeUnit.DAYS.toMillis(30)
    }
}
