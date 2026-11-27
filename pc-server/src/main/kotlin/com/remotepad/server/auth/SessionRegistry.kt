package com.remotepad.server.auth

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Дані виданої сесії. pushToken потрібен, щоб повідомити телефон, коли зв'язок зникне. */
data class SessionInfo(val deviceName: String, val pushToken: String?)

/**
 * Реєстр виданих токенів. Токен випадковий: у старій версії він складався з часу й імені
 * пристрою, і його можна було вгадати.
 */
class SessionRegistry(private val maxSessions: Int = 3) {
    private val sessions = ConcurrentHashMap<String, SessionInfo>()

    /** Повертає новий токен або null, якщо ліміт одночасних пристроїв вичерпано. */
    fun open(deviceName: String, pushToken: String? = null): String? {
        if (sessions.size >= maxSessions) return null
        val token = UUID.randomUUID().toString()
        sessions[token] = SessionInfo(deviceName, pushToken)
        return token
    }

    fun isValid(token: String?): Boolean = token != null && sessions.containsKey(token)

    fun info(token: String): SessionInfo? = sessions[token]

    fun close(token: String): SessionInfo? = sessions.remove(token)

    fun pushTokens(): List<String> = sessions.values.mapNotNull { it.pushToken }

    val activeCount: Int get() = sessions.size
}
