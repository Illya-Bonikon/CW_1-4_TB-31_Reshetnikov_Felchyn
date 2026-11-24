package com.remotepad.client.network

import com.remotepad.protocol.InputEventPacket
import kotlinx.coroutines.flow.StateFlow

/** Канал до ПК. Інтерфейс потрібен, щоб ViewModel не знала про OkHttp і тестувалася з фейком. */
interface RemotePadConnection {
    val status: StateFlow<ConnectionStatus>
    val rttMs: StateFlow<Long?>

    suspend fun connect(target: HostTarget, pin: String, deviceName: String): AuthResult

    /** Повертає false, якщо з'єднання немає і пакет не відправлено. */
    fun send(packet: InputEventPacket): Boolean

    fun disconnect()
}
