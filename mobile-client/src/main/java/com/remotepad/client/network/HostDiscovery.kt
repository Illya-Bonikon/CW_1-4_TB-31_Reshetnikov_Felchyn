package com.remotepad.client.network

import com.remotepad.protocol.PacketCodec
import com.remotepad.protocol.ProtocolConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class DiscoveredHost(val hostName: String, val ip: String, val port: Int)

/** Шукає сервери RemotePad широкомовним UDP-запитом і збирає відповіді протягом тайм-ауту. */
class HostDiscovery(
    private val port: Int = ProtocolConstants.DEFAULT_PORT,
    private val timeoutMs: Int = 1500
) {
    suspend fun discover(): List<DiscoveredHost> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DiscoveredHost>()
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.soTimeout = timeoutMs
            val request = ProtocolConstants.DISCOVERY_REQUEST.toByteArray(Charsets.UTF_8)
            socket.send(DatagramPacket(request, request.size, InetAddress.getByName("255.255.255.255"), port))

            val deadline = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArray(256)
            while (System.currentTimeMillis() < deadline) {
                val reply = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(reply)
                } catch (_: SocketTimeoutException) {
                    break
                }
                val (name, hostPort) = PacketCodec.decodeDiscoveryResponse(
                    String(reply.data, 0, reply.length, Charsets.UTF_8)
                ) ?: continue
                val ip = reply.address.hostAddress ?: continue
                // один ПК може відповісти кількома інтерфейсами, тому ключ саме IP
                found[ip] = DiscoveredHost(name, ip, hostPort)
            }
        }
        found.values.toList()
    }
}
