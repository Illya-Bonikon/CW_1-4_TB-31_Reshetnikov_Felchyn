package com.remotepad.server

import com.remotepad.protocol.ProtocolConstants
import com.remotepad.server.discovery.MdnsAdvertiser
import org.junit.Assume.assumeTrue
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Перевіряє, що оголошення mDNS видно в мережі: другий екземпляр JmDNS на тому ж інтерфейсі
 * шукає сервіс так само, як це робить Android NsdManager на телефоні.
 */
class MdnsAdvertiserTest {

    private fun multicastAddress(): InetAddress? =
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && it.supportsMulticast() }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address }

    @Test
    fun `advertised service is resolvable with name, port and attributes`() {
        val address = multicastAddress()
        assumeTrue("немає мережевого інтерфейсу з multicast", address != null)

        MdnsAdvertiser(address!!, "TEST-PC", 8765).start().use { advertiser ->
            JmDNS.create(address, "listener").use { listener ->
                val info = listener.getServiceInfo(
                    advertiser.serviceInfo.type, advertiser.serviceInfo.name, true, 8_000
                )
                assertNotNull(info, "сервіс не знайдено за 8 с")
                assertEquals(8765, info.port)
                assertEquals("TEST-PC", info.getPropertyString(ProtocolConstants.MDNS_ATTR_NAME))
                assertEquals("${ProtocolConstants.MDNS_SERVICE_TYPE}.local.", info.type)
            }
        }
    }
}
