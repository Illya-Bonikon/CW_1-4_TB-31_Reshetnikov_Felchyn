package com.remotepad.server.net

import com.remotepad.protocol.PacketCodec
import com.remotepad.protocol.ProtocolConstants
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import kotlin.concurrent.thread

/**
 * Відповідає на широкомовний UDP-запит клієнта, щоб телефон знайшов ПК без введення IP.
 * Слухає той самий номер порту, що й WebSocket: UDP і TCP між собою не конфліктують.
 */
class DiscoveryResponder(
    private val port: Int,
    private val hostName: String
) {
    private var socket: DatagramSocket? = null

    fun start() {
        val udp = DatagramSocket(port).also { socket = it }
        thread(name = "discovery", isDaemon = true) {
            val buffer = ByteArray(256)
            while (!udp.isClosed) {
                val request = DatagramPacket(buffer, buffer.size)
                try {
                    udp.receive(request)
                } catch (_: SocketException) {
                    break
                }
                val text = String(request.data, 0, request.length, Charsets.UTF_8).trim()
                if (text != ProtocolConstants.DISCOVERY_REQUEST) continue
                val reply = PacketCodec.encodeDiscoveryResponse(hostName, port).toByteArray(Charsets.UTF_8)
                udp.send(DatagramPacket(reply, reply.size, request.address, request.port))
            }
        }
    }

    fun stop() {
        socket?.close()
    }
}
