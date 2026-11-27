package com.remotepad.server.discovery

import com.remotepad.protocol.ProtocolConstants
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * Оголошує сервер у мережі через mDNS/DNS-SD (бібліотека JmDNS). Телефон знаходить його
 * через Android NsdManager навіть там, де роутер блокує широкомовні UDP-пакети.
 */
class MdnsAdvertiser(
    private val address: InetAddress,
    private val hostName: String,
    private val port: Int
) : AutoCloseable {
    private var jmdns: JmDNS? = null

    val serviceInfo: ServiceInfo = ServiceInfo.create(
        "${ProtocolConstants.MDNS_SERVICE_TYPE}.local.",
        // ім'я сервісу має бути унікальним у мережі, тому додаю ім'я ПК
        "RemotePad $hostName",
        port,
        0,
        0,
        mapOf(ProtocolConstants.MDNS_ATTR_NAME to hostName, "path" to ProtocolConstants.CONTROL_PATH)
    )

    fun start(): MdnsAdvertiser {
        jmdns = JmDNS.create(address, hostName).also { it.registerService(serviceInfo) }
        return this
    }

    override fun close() {
        jmdns?.run {
            unregisterAllServices()
            close()
        }
        jmdns = null
    }
}
