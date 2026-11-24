package com.remotepad.client.network

import com.remotepad.protocol.AuthRequest
import com.remotepad.protocol.EventType
import com.remotepad.protocol.InputEventPacket
import com.remotepad.protocol.PacketCodec
import com.remotepad.protocol.ProtocolConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * З'єднання з ПК через OkHttp WebSocket: авторизація за PIN, PING для заміру RTT
 * і автоматичне перепідключення, якщо Wi-Fi на мить зник.
 */
class WebSocketConnection(
    private val scope: CoroutineScope,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AUTH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build(),
    private val clock: () -> Long = System::currentTimeMillis
) : RemotePadConnection {

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _rtt = MutableStateFlow<Long?>(null)
    override val rttMs: StateFlow<Long?> = _rtt.asStateFlow()

    private val sequence = AtomicLong()
    // кожне відкриття сокета отримує свій номер; події старих сокетів відкидаю за ним
    private val generation = AtomicLong()
    private val rttTracker = RttTracker()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var token: String? = null
    @Volatile private var lastAttempt: Triple<HostTarget, String, String>? = null
    private var pingJob: Job? = null

    override suspend fun connect(target: HostTarget, pin: String, deviceName: String): AuthResult {
        lastAttempt = Triple(target, pin, deviceName)
        _status.value = ConnectionStatus.Connecting
        val result = open(target, pin, deviceName)
        _status.value = when (result) {
            is AuthResult.Success -> ConnectionStatus.Connected(result.hostName)
            else -> ConnectionStatus.Disconnected
        }
        return result
    }

    override fun send(packet: InputEventPacket): Boolean {
        val ws = socket ?: return false
        val currentToken = token ?: return false
        val stamped = packet.copy(sequenceNumber = sequence.incrementAndGet(), sessionToken = currentToken)
        return ws.send(PacketCodec.encode(stamped))
    }

    override fun disconnect() {
        lastAttempt = null
        closeSocket()
        _status.value = ConnectionStatus.Disconnected
    }

    private suspend fun open(target: HostTarget, pin: String, deviceName: String): AuthResult {
        closeSocket()
        val myGeneration = generation.incrementAndGet()
        val auth = CompletableDeferred<AuthResult>()
        val request = Request.Builder()
            .url("ws://${target.ip}:${target.port}${ProtocolConstants.CONTROL_PATH}")
            .build()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val body = AuthRequest(deviceName = deviceName, clientIp = LocalAddress.siteLocalIpv4(), pinCode = pin)
                webSocket.send(PacketCodec.encode(
                    InputEventPacket(type = EventType.AUTH_REQUEST, text = PacketCodec.encodeAuthRequest(body))
                ))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (generation.get() != myGeneration) return
                val packet = runCatching { PacketCodec.decode(text) }.getOrNull() ?: return
                when (packet.type) {
                    EventType.AUTH_RESPONSE -> auth.complete(parseAuth(packet))
                    EventType.PONG -> _rtt.value = rttTracker.onPong(packet.timestamp, clock())
                    else -> Unit
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (generation.get() != myGeneration) return
                if (!auth.isCompleted) auth.complete(AuthResult.Unreachable(t.message ?: "Сервер недоступний"))
                else onDropped()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (generation.get() != myGeneration) return
                if (!auth.isCompleted) auth.complete(AuthResult.Rejected(reason.ifEmpty { "З'єднання закрито" }))
                else onDropped()
            }
        })
        socket = ws

        val result = withTimeoutOrNull(AUTH_TIMEOUT_MS) { auth.await() }
            ?: AuthResult.Unreachable("Сервер не відповів за ${AUTH_TIMEOUT_MS / 1000} с")
        if (result is AuthResult.Success) {
            token = result.token
            startPings()
        } else {
            closeSocket()
        }
        return result
    }

    private fun parseAuth(packet: InputEventPacket): AuthResult {
        val response = runCatching { PacketCodec.decodeAuthResponse(packet.text.orEmpty()) }.getOrNull()
            ?: return AuthResult.Rejected("Некоректна відповідь сервера")
        val newToken = response.sessionToken
        return if (response.isSuccess && newToken != null) {
            AuthResult.Success(newToken, response.serverHostName ?: "ПК")
        } else {
            AuthResult.Rejected(response.errorMessage ?: "Відмовлено в доступі")
        }
    }

    private fun startPings() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                socket?.send(PacketCodec.encode(
                    InputEventPacket(type = EventType.PING, sequenceNumber = sequence.incrementAndGet(), timestamp = clock())
                ))
                delay(PING_PERIOD_MS)
            }
        }
    }

    // Розрив після успішного входу: пробую відновитися з тими самими даними, поки користувач не втрутився
    private fun onDropped() {
        val attempt = lastAttempt
        closeSocket()
        if (attempt == null) {
            _status.value = ConnectionStatus.Disconnected
            return
        }
        scope.launch {
            val (target, pin, device) = attempt
            for (n in 1..MAX_RECONNECTS) {
                _status.value = ConnectionStatus.Reconnecting(n)
                delay(RECONNECT_DELAY_MS)
                if (lastAttempt == null) return@launch
                val result = open(target, pin, device)
                if (result is AuthResult.Success) {
                    _status.value = ConnectionStatus.Connected(result.hostName)
                    return@launch
                }
            }
            _status.value = ConnectionStatus.Lost(LOST_MESSAGE)
        }
    }

    private fun closeSocket() {
        pingJob?.cancel()
        pingJob = null
        generation.incrementAndGet()
        token = null
        rttTracker.reset()
        _rtt.value = null
        socket?.let { old ->
            socket = null
            old.close(NORMAL_CLOSURE, null)
        }
    }

    companion object {
        const val AUTH_TIMEOUT_MS = 3000L
        private const val PING_PERIOD_MS = 2000L
        private const val RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECTS = 3
        private const val NORMAL_CLOSURE = 1000
        private const val LOST_MESSAGE = "Зв'язок із ПК втрачено. Перевірте Wi-Fi або введіть IP вручну"
    }
}
