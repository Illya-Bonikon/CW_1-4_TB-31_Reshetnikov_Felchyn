package com.remotepad.server

import com.remotepad.protocol.AuthRequest
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.MouseButton
import com.remotepad.protocol.PacketCodec
import com.remotepad.server.auth.PinAuthenticator
import com.remotepad.server.auth.SessionRegistry
import com.remotepad.server.input.CursorSmoother
import com.remotepad.server.net.ConnectionState
import com.remotepad.server.net.PacketHandler
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PacketHandlerTest {
    private lateinit var input: FakeInputDevice
    private lateinit var sessions: SessionRegistry
    private lateinit var handler: PacketHandler

    @BeforeTest
    fun setUp() {
        input = FakeInputDevice()
        sessions = SessionRegistry(maxSessions = 1)
        handler = PacketHandler(
            authenticator = PinAuthenticator("4812"),
            sessions = sessions,
            input = input,
            smoother = CursorSmoother(alpha = 0f, sensitivity = 1f),
            hostName = "TEST-PC",
            screenWidth = 1920,
            screenHeight = 1080,
            maxPinAttempts = 2
        )
    }

    private fun authPacket(pin: String) = InputEventPacket(
        type = EventType.AUTH_REQUEST,
        text = PacketCodec.encodeAuthRequest(AuthRequest("Pixel", "10.0.0.2", pin))
    )

    private fun login(state: ConnectionState): String {
        val reply = handler.handle(authPacket("4812"), state)!!
        return PacketCodec.decodeAuthResponse(reply.text!!).sessionToken!!
    }

    @Test
    fun `correct pin gives token and host info`() {
        val reply = handler.handle(authPacket("4812"), ConnectionState())!!
        val response = PacketCodec.decodeAuthResponse(reply.text!!)
        assertTrue(response.isSuccess)
        assertEquals("TEST-PC", response.serverHostName)
        assertTrue(sessions.isValid(response.sessionToken))
    }

    @Test
    fun `wrong pin is rejected and counted`() {
        val state = ConnectionState()
        val response = PacketCodec.decodeAuthResponse(handler.handle(authPacket("0000"), state)!!.text!!)
        assertFalse(response.isSuccess)
        assertEquals(1, state.failedAttempts)
    }

    @Test
    fun `connection is blocked after too many wrong pins even with correct one`() {
        val state = ConnectionState()
        repeat(2) { handler.handle(authPacket("1111"), state) }
        val response = PacketCodec.decodeAuthResponse(handler.handle(authPacket("4812"), state)!!.text!!)
        assertFalse(response.isSuccess)
        assertTrue(handler.isBlocked(state))
    }

    @Test
    fun `events without token do not reach input device`() {
        handler.handle(InputEventPacket(type = EventType.MOUSE_CLICK, button = MouseButton.LEFT), ConnectionState())
        assertTrue(input.calls.isEmpty())
    }

    @Test
    fun `token of another connection is not accepted`() {
        val owner = ConnectionState()
        val token = login(owner)
        handler.handle(
            InputEventPacket(type = EventType.MOUSE_CLICK, button = MouseButton.LEFT, sessionToken = token),
            ConnectionState()
        )
        assertTrue(input.calls.isEmpty())
    }

    @Test
    fun `authorized events are dispatched`() {
        val state = ConnectionState()
        val token = login(state)
        handler.handle(InputEventPacket(type = EventType.MOUSE_MOVE, deltaX = 3f, deltaY = -2f, sessionToken = token), state)
        handler.handle(InputEventPacket(type = EventType.MOUSE_CLICK, button = MouseButton.RIGHT, sessionToken = token), state)
        handler.handle(InputEventPacket(type = EventType.MOUSE_WHEEL, deltaY = 2f, sessionToken = token), state)
        handler.handle(InputEventPacket(type = EventType.TEXT_INPUT, text = "привіт", sessionToken = token), state)
        assertEquals(listOf("move 3 -2", "click RIGHT", "scroll 2", "text привіт"), input.calls)
    }

    @Test
    fun `ping is answered with the same timestamp`() {
        val reply = handler.handle(InputEventPacket(type = EventType.PING, sequenceNumber = 7, timestamp = 123), ConnectionState())!!
        assertEquals(EventType.PONG, reply.type)
        assertEquals(123, reply.timestamp)
        assertEquals(7, reply.sequenceNumber)
    }

    @Test
    fun `session limit is respected and freed on disconnect`() {
        val first = ConnectionState()
        login(first)
        val second = PacketCodec.decodeAuthResponse(handler.handle(authPacket("4812"), ConnectionState())!!.text!!)
        assertFalse(second.isSuccess)

        handler.onDisconnect(first)
        assertEquals(0, sessions.activeCount)
        assertNull(first.token)
    }
}
