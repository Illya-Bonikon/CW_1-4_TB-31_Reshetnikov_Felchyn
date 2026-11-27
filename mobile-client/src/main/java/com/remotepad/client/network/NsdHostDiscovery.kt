package com.remotepad.client.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.remotepad.protocol.ProtocolConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Пошук серверів через Android NsdManager (mDNS/DNS-SD). На відміну від UDP broadcast,
 * multicast mDNS зазвичай проходить і в мережах, де роутер блокує широкомовні пакети.
 */
class NsdHostDiscovery(
    context: Context,
    private val timeoutMs: Long = 2000
) : HostDiscoverySource {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    // до Android 14 NsdManager обробляє лише один resolveService за раз
    private val resolveLock = Mutex()

    override suspend fun discover(): List<DiscoveredHost> {
        val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) { found.trySend(info) }
            override fun onServiceLost(info: NsdServiceInfo) = Unit
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                found.close()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        val type = ProtocolConstants.MDNS_SERVICE_TYPE
        nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)

        val hosts = mutableListOf<DiscoveredHost>()
        withTimeoutOrNull(timeoutMs) {
            for (info in found) {
                resolve(info)?.let { hosts += it }
            }
        }
        runCatching { nsd.stopServiceDiscovery(listener) }
        return hosts
    }

    @Suppress("DEPRECATION") // registerServiceInfoCallback є лише з Android 14, а minSdk 26
    private suspend fun resolve(info: NsdServiceInfo): DiscoveredHost? = resolveLock.withLock {
        val result = CompletableDeferred<DiscoveredHost?>()
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolved: NsdServiceInfo) {
                result.complete(
                    NsdMapping.toHost(
                        serviceName = resolved.serviceName,
                        address = resolved.host?.hostAddress,
                        port = resolved.port,
                        attributes = resolved.attributes
                    )
                )
            }

            override fun onResolveFailed(failed: NsdServiceInfo, errorCode: Int) {
                result.complete(null)
            }
        })
        result.await()
    }
}

/** Перетворення знайденого сервісу на DiscoveredHost; окремо від NsdManager, щоб перевіряти тестами. */
object NsdMapping {
    private const val SERVICE_PREFIX = "RemotePad "

    fun toHost(serviceName: String, address: String?, port: Int, attributes: Map<String, ByteArray?>): DiscoveredHost? {
        if (address.isNullOrEmpty() || port !in 1..65535) return null
        val nameFromTxt = attributes[ProtocolConstants.MDNS_ATTR_NAME]?.toString(Charsets.UTF_8)
        val name = nameFromTxt?.takeIf { it.isNotBlank() } ?: serviceName.removePrefix(SERVICE_PREFIX)
        return DiscoveredHost(name, address, port)
    }
}
