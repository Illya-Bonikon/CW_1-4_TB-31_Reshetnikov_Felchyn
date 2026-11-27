package com.remotepad.client.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompositeHostDiscoveryTest {

    private fun source(vararg hosts: DiscoveredHost) = object : HostDiscoverySource {
        override suspend fun discover() = hosts.toList()
    }

    private val failing = object : HostDiscoverySource {
        override suspend fun discover(): List<DiscoveredHost> = error("мережа недоступна")
    }

    @Test
    fun `results of both methods are merged without duplicates`() = runTest {
        val udp = source(DiscoveredHost("PC", "10.0.0.5", 8080))
        val mdns = source(DiscoveredHost("PC", "10.0.0.5", 8080), DiscoveredHost("LAB", "10.0.0.7", 8080))
        val result = CompositeHostDiscovery(listOf(udp, mdns)).discover()
        assertEquals(listOf("10.0.0.5", "10.0.0.7"), result.map { it.ip })
    }

    @Test
    fun `failure of one method does not hide the other`() = runTest {
        val result = CompositeHostDiscovery(listOf(failing, source(DiscoveredHost("PC", "10.0.0.5", 8080)))).discover()
        assertEquals(1, result.size)
    }
}

class NsdMappingTest {

    @Test
    fun `name is taken from TXT record`() {
        val host = NsdMapping.toHost("RemotePad WORK-PC", "10.0.0.5", 8080, mapOf("name" to "WORK-PC".toByteArray()))
        assertEquals(DiscoveredHost("WORK-PC", "10.0.0.5", 8080), host)
    }

    @Test
    fun `without TXT the service name prefix is removed`() {
        assertEquals("LAB", NsdMapping.toHost("RemotePad LAB", "10.0.0.7", 8080, emptyMap())?.hostName)
    }

    @Test
    fun `service without address or with bad port is skipped`() {
        assertNull(NsdMapping.toHost("RemotePad X", null, 8080, emptyMap()))
        assertNull(NsdMapping.toHost("RemotePad X", "10.0.0.7", 0, emptyMap()))
    }
}
