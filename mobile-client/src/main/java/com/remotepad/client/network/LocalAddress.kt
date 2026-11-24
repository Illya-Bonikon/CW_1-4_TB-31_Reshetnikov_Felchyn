package com.remotepad.client.network

import java.net.Inet4Address
import java.net.NetworkInterface

/** IPv4-адреса телефона в локальній мережі; сервер пише її в журнал підключень. */
object LocalAddress {
    fun siteLocalIpv4(): String =
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull() ?: "0.0.0.0"
}
