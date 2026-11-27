package com.remotepad.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class PacketCodecTest {

    @Test
    fun `move packet survives round trip`() {
        val packet = InputEventPacket(
            type = EventType.MOUSE_MOVE,
            sequenceNumber = 42,
            timestamp = 1_000,
            deltaX = 3.5f,
            deltaY = -1.25f,
            sessionToken = "abc"
        )
        assertEquals(packet, PacketCodec.decode(PacketCodec.encode(packet)))
    }

    @Test
    fun `default fields are not sent`() {
        val raw = PacketCodec.encode(InputEventPacket(type = EventType.PING, timestamp = 1))
        assertFalse(raw.contains("deltaX"))
        assertFalse(raw.contains("macroId"))
    }

    @Test
    fun `unknown fields from newer version are ignored`() {
        val packet = PacketCodec.decode("""{"type":"PING","timestamp":5,"futureField":true}""")
        assertEquals(EventType.PING, packet.type)
    }

    @Test
    fun `auth request is nested into text field`() {
        val request = AuthRequest(deviceName = "Pixel 7", clientIp = "192.168.0.12", pinCode = "4812")
        assertEquals(request, PacketCodec.decodeAuthRequest(PacketCodec.encodeAuthRequest(request)))
    }

    @Test
    fun `discovery response is parsed`() {
        val raw = PacketCodec.encodeDiscoveryResponse("WORK-PC", 8080)
        assertEquals("WORK-PC" to 8080, PacketCodec.decodeDiscoveryResponse(raw))
    }

    @Test
    fun `foreign udp message is rejected`() {
        assertNull(PacketCodec.decodeDiscoveryResponse("HELLO;x;1"))
        assertNull(PacketCodec.decodeDiscoveryResponse("REMOTEPAD_HOST;x;port"))
    }
}

class KeyCodesTest {

    @Test
    fun `letters and digits use ascii codes like awt`() {
        assertEquals(67, KeyCodes.byName("c"))
        assertEquals(55, KeyCodes.byName("7"))
    }

    @Test
    fun `named keys are resolved`() {
        assertEquals(KeyCodes.ESCAPE, KeyCodes.byName("Esc"))
        assertEquals(KeyCodes.F5, KeyCodes.byName("F5"))
    }

    @Test
    fun `unknown key gives null`() {
        assertNull(KeyCodes.byName("F13"))
        assertNull(KeyCodes.byName("ї"))
    }
}

class AuthRequestCompatibilityTest {

    @Test
    fun `push token survives round trip`() {
        val request = AuthRequest("Pixel", "10.0.0.2", "4812", pushToken = "fcm-token")
        assertEquals("fcm-token", PacketCodec.decodeAuthRequest(PacketCodec.encodeAuthRequest(request)).pushToken)
    }

    @Test
    fun `request from older client without push token is accepted`() {
        val old = """{"deviceName":"Pixel","clientIp":"10.0.0.2","pinCode":"4812"}"""
        assertNull(PacketCodec.decodeAuthRequest(old).pushToken)
    }
}
