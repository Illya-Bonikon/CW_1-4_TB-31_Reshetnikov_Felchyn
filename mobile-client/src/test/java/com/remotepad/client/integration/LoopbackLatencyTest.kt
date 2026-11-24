package com.remotepad.client.integration

import com.remotepad.client.network.AuthResult
import com.remotepad.client.network.ConnectionStatus
import com.remotepad.client.network.HostTarget
import com.remotepad.client.network.WebSocketConnection
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.MouseButton
import com.remotepad.server.auth.PinAuthenticator
import com.remotepad.server.auth.SessionRegistry
import com.remotepad.server.input.CursorSmoother
import com.remotepad.server.net.ControlServer
import com.remotepad.server.net.PacketHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Наскрізна перевірка: справжній клієнт WebSocketConnection підключається до справжнього
 * ControlServer на loopback. Окрім перевірок, тест міряє затримки і пише їх у
 * build/measurements/loopback.json, звідки числа бере звіт.
 */
class LoopbackLatencyTest {
    private val pin = "4812"
    private val port = ServerSocket(0).use { it.localPort }
    private val input = RecordingInputDevice()
    private val server = ControlServer(
        port = port,
        handler = PacketHandler(
            authenticator = PinAuthenticator(pin),
            sessions = SessionRegistry(maxSessions = 100),
            input = input,
            // без згладжування кожен піксель доходить як є, і його можна порахувати
            smoother = CursorSmoother(alpha = 0f, sensitivity = 1f),
            hostName = "LOOPBACK",
            screenWidth = 1920,
            screenHeight = 1080
        ),
        log = {}
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connection = WebSocketConnection(scope)
    private val target = HostTarget("127.0.0.1", port)

    @Before
    fun startServer() = server.start(wait = false)

    @After
    fun stopAll() {
        connection.disconnect()
        server.stop()
        scope.cancel()
    }

    private fun connect(withPin: String = pin) = runBlocking { connection.connect(target, withPin, "JUnit") }

    @Test
    fun `wrong pin is rejected by the real server`() {
        val result = connect("0000")
        assertIs<AuthResult.Rejected>(result)
        assertEquals(ConnectionStatus.Disconnected, connection.status.value)
    }

    @Test
    fun `click sent by client reaches input device`() {
        assertIs<AuthResult.Success>(connect())
        assertTrue(connection.send(InputEventPacket(type = EventType.MOUSE_CLICK, button = MouseButton.RIGHT)))
        assertEquals(MouseButton.RIGHT, input.clicks.poll(2, TimeUnit.SECONDS))
    }

    @Test
    fun `measure authorization, event latency and burst throughput`() {
        val authMs = (1..AUTH_RUNS).map {
            val start = System.nanoTime()
            assertIs<AuthResult.Success>(connect())
            val ms = (System.nanoTime() - start) / 1e6
            connection.disconnect()
            ms
        }

        assertIs<AuthResult.Success>(connect())

        // перші події йдуть повільніше через прогрів JIT, у результати їх не беру
        repeat(WARMUP) { sendKeyAndWait(it) }
        val latencyUs = (1..LATENCY_RUNS).map { sendKeyAndWait(it) }

        val burstStart = System.nanoTime()
        repeat(BURST) {
            connection.send(InputEventPacket(type = EventType.MOUSE_MOVE, deltaX = 1f))
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (input.movedPixels.get() < BURST && System.nanoTime() < deadline) Thread.sleep(1)
        val burstMs = (System.nanoTime() - burstStart) / 1e6
        assertEquals(BURST, input.movedPixels.get(), "частина руху загубилася")

        writeResults(authMs, latencyUs, burstMs)
    }

    // повертає затримку від send() до виклику InputDevice у мікросекундах
    private fun sendKeyAndWait(marker: Int): Double {
        val code = 1000 + marker
        val start = System.nanoTime()
        assertTrue(connection.send(InputEventPacket(type = EventType.KEY_TYPE, keyCode = code)))
        val arrival = assertNotNull(input.keys.poll(2, TimeUnit.SECONDS), "подія $code не дійшла")
        assertEquals(code, arrival.keyCode)
        return (arrival.nanoTime - start) / 1e3
    }

    private fun writeResults(authMs: List<Double>, latencyUs: List<Double>, burstMs: Double) {
        fun arr(values: List<Double>) = values.joinToString(prefix = "[", postfix = "]") { "%.3f".format(java.util.Locale.US, it) }
        val runtime = Runtime.getRuntime()
        val json = """
            {
              "os": "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
              "java": "${System.getProperty("java.version")}",
              "cpus": ${runtime.availableProcessors()},
              "auth_runs": $AUTH_RUNS,
              "auth_ms": ${arr(authMs)},
              "warmup": $WARMUP,
              "latency_runs": $LATENCY_RUNS,
              "latency_us": ${arr(latencyUs)},
              "burst_packets": $BURST,
              "burst_ms": ${"%.3f".format(java.util.Locale.US, burstMs)}
            }
        """.trimIndent()
        File("build/measurements").apply { mkdirs() }.resolve("loopback.json").writeText(json)
    }

    private companion object {
        const val AUTH_RUNS = 20
        const val WARMUP = 200
        const val LATENCY_RUNS = 1000
        const val BURST = 5000
    }
}
