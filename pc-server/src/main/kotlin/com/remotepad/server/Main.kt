package com.remotepad.server

import com.remotepad.protocol.ProtocolConstants
import com.remotepad.server.auth.PinAuthenticator
import com.remotepad.server.auth.SessionRegistry
import com.remotepad.server.input.AwtRobotInputDevice
import com.remotepad.server.input.CursorSmoother
import com.remotepad.server.net.ControlServer
import com.remotepad.server.net.DiscoveryResponder
import com.remotepad.server.net.PacketHandler
import java.awt.Toolkit
import java.net.InetAddress
import java.net.NetworkInterface

/** Запуск: gradlew :pc-server:run --args="[порт] [PIN]". Без PIN генерую випадковий. */
fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: ProtocolConstants.DEFAULT_PORT
    val authenticator = args.getOrNull(1)?.let(::PinAuthenticator) ?: PinAuthenticator.random()
    val hostName = InetAddress.getLocalHost().hostName
    val screen = Toolkit.getDefaultToolkit().screenSize

    val handler = PacketHandler(
        authenticator = authenticator,
        sessions = SessionRegistry(),
        input = AwtRobotInputDevice(),
        smoother = CursorSmoother(),
        hostName = hostName,
        screenWidth = screen.width,
        screenHeight = screen.height
    )

    println("RemotePad server")
    println("Хост: $hostName, екран ${screen.width}x${screen.height}")
    println("Адреси в локальній мережі: ${lanAddresses().joinToString()}")
    println("Порт: $port, PIN: ${authenticator.pin}")

    DiscoveryResponder(port, hostName).start()
    ControlServer(port, handler).start(wait = true)
}

private fun lanAddresses(): List<String> =
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filter { it.isSiteLocalAddress }
        .map { it.hostAddress }
