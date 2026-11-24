package com.remotepad.server.auth

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Реєстр виданих токенів. Токен випадковий: у старій версії він складався з часу й імені
 * пристрою, і його можна було вгадати.
 */
class SessionRegistry(private val maxSessions: Int = 3) {
    private val sessions = ConcurrentHashMap<String, String>()

    /** Повертає новий токен або null, якщо ліміт одночасних пристроїв вичерпано. */
    fun open(deviceName: String): String? {
        if (sessions.size >= maxSessions) return null
        val token = UUID.randomUUID().toString()
        sessions[token] = deviceName
        return token
    }

    fun isValid(token: String?): Boolean = token != null && sessions.containsKey(token)

    fun close(token: String) {
        sessions.remove(token)
    }

    val activeCount: Int get() = sessions.size
}
