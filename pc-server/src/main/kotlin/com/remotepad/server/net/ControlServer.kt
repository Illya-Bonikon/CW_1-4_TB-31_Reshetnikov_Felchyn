package com.remotepad.server.net

import com.remotepad.protocol.EventType
import com.remotepad.protocol.PacketCodec
import com.remotepad.protocol.ProtocolConstants
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.time.Duration

/** WebSocket-транспорт: лише приймає кадри й віддає їх у PacketHandler. */
class ControlServer(
    private val port: Int,
    private val handler: PacketHandler,
    private val log: (String) -> Unit = ::println
) {
    private var engine: ApplicationEngine? = null

    fun start(wait: Boolean) {
        engine = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
            }
            routing {
                webSocket(ProtocolConstants.CONTROL_PATH) {
                    val state = ConnectionState()
                    log("Нове з'єднання")
                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val packet = runCatching { PacketCodec.decode(frame.readText()) }.getOrNull() ?: continue
                            handler.handle(packet, state)?.let { send(Frame.Text(PacketCodec.encode(it))) }
                            if (packet.type == EventType.AUTH_REQUEST && handler.isBlocked(state)) {
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Забагато спроб PIN"))
                            }
                        }
                    } finally {
                        handler.onDisconnect(state)
                        log("З'єднання закрито")
                    }
                }
            }
        }.start(wait = wait)
    }

    fun stop() {
        engine?.stop(500, 1000)
    }
}
