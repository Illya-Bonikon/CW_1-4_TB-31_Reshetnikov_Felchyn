package com.remotepad.client.network

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class DiscoveredHost(val hostName: String, val ip: String, val port: Int)

/** Спосіб знайти сервер у мережі: широкомовний UDP або mDNS/DNS-SD. */
interface HostDiscoverySource {
    suspend fun discover(): List<DiscoveredHost>
}

/**
 * Запускає всі способи пошуку паралельно і зводить результати. Один ПК часто знаходиться
 * обома способами, тому дублікати за IP прибираю; помилка одного способу не зупиняє інші.
 */
class CompositeHostDiscovery(private val sources: List<HostDiscoverySource>) : HostDiscoverySource {
    override suspend fun discover(): List<DiscoveredHost> = coroutineScope {
        sources.map { source -> async { runCatching { source.discover() }.getOrDefault(emptyList()) } }
            .awaitAll()
            .flatten()
            .distinctBy { it.ip }
    }
}
