package com.remotepad.server

import com.remotepad.protocol.AuthRequest
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.PacketCodec
import com.remotepad.server.auth.PinAuthenticator
import com.remotepad.server.auth.SessionRegistry
import com.remotepad.server.input.CursorSmoother
import com.remotepad.server.net.ConnectionState
import com.remotepad.server.net.PacketHandler
import com.remotepad.server.push.FcmPushNotifier
import com.remotepad.server.push.NoopPushNotifier
import com.remotepad.server.push.PushMessages
import com.remotepad.server.push.PushNotifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Записує, кому і що сервер намагався надіслати. */
class RecordingNotifier : PushNotifier {
    val lost = mutableListOf<String>()
    val stopped = mutableListOf<Collection<String>>()
    override fun connectionLost(pushToken: String, hostName: String) { lost += pushToken }
    override fun hostStopped(pushTokens: Collection<String>, hostName: String) { stopped += pushTokens }
    override val isEnabled = true
}

class PushTest {
    private val notifier = RecordingNotifier()
    private val handler = PacketHandler(
        authenticator = PinAuthenticator("4812"),
        sessions = SessionRegistry(),
        input = FakeInputDevice(),
        smoother = CursorSmoother(),
        hostName = "TEST-PC",
        screenWidth = 1920,
        screenHeight = 1080,
        notifier = notifier
    )

    private fun login(pushToken: String?): ConnectionState {
        val state = ConnectionState()
        handler.handle(
            InputEventPacket(
                type = EventType.AUTH_REQUEST,
                text = PacketCodec.encodeAuthRequest(AuthRequest("Pixel", "10.0.0.2", "4812", pushToken = pushToken))
            ),
            state
        )
        return state
    }

    @Test
    fun `unexpected drop notifies the phone`() {
        handler.onDisconnect(login("token-a"))
        assertEquals(listOf("token-a"), notifier.lost)
    }

    @Test
    fun `intentional disconnect does not notify`() {
        val state = login("token-a")
        handler.handle(InputEventPacket(type = EventType.DISCONNECT, sessionToken = state.token), state)
        handler.onDisconnect(state)
        assertTrue(notifier.lost.isEmpty())
    }

    @Test
    fun `client without push token is not notified`() {
        handler.onDisconnect(login(null))
        assertTrue(notifier.lost.isEmpty())
    }

    @Test
    fun `server shutdown notifies all phones once`() {
        val a = login("token-a")
        val b = login("token-b")
        handler.onServerStopping()
        handler.onDisconnect(a)
        handler.onDisconnect(b)
        assertEquals(setOf("token-a", "token-b"), notifier.stopped.single().toSet())
        assertTrue(notifier.lost.isEmpty(), "під час зупинки окремі сповіщення про розрив не потрібні")
    }

    @Test
    fun `messages mention host name`() {
        assertTrue(PushMessages.connectionLost("WORK-PC").body.contains("WORK-PC"))
        assertEquals(PushMessages.TYPE_HOST_STOPPED, PushMessages.hostStopped("WORK-PC").type)
    }

    @Test
    fun `without credentials push is disabled`() {
        assertSame(NoopPushNotifier, FcmPushNotifier.fromEnvironment(emptyMap()))
        assertSame(NoopPushNotifier, FcmPushNotifier.fromEnvironment(mapOf(FcmPushNotifier.CREDENTIALS_ENV to "/no/such/key.json")))
    }
}
