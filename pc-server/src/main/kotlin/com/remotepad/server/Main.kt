package com.remotepad.server

import com.remotepad.protocol.ProtocolConstants
import com.remotepad.server.auth.PinAuthenticator
import com.remotepad.server.auth.SessionRegistry
import com.remotepad.server.discovery.MdnsAdvertiser
import com.remotepad.server.input.AwtRobotInputDevice
import com.remotepad.server.input.CursorSmoother
import com.remotepad.server.net.ControlServer
import com.remotepad.server.net.DiscoveryResponder
import com.remotepad.server.net.PacketHandler
import com.remotepad.server.push.FcmPushNotifier
import java.awt.Toolkit
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Запуск: gradlew :pc-server:run --args="[порт] [PIN]". Без PIN генерую випадковий.
 * Для push-сповіщень змінна REMOTEPAD_FCM_CREDENTIALS має вказувати на ключ сервісного акаунта Firebase.
 */
fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: ProtocolConstants.DEFAULT_PORT
    val authenticator = args.getOrNull(1)?.let(::PinAuthenticator) ?: PinAuthenticator.random()
    val hostName = InetAddress.getLocalHost().hostName
    val screen = Toolkit.getDefaultToolkit().screenSize
    val notifier = FcmPushNotifier.fromEnvironment()

    val handler = PacketHandler(
        authenticator = authenticator,
        sessions = SessionRegistry(),
        input = AwtRobotInputDevice(),
        smoother = CursorSmoother(),
        hostName = hostName,
        screenWidth = screen.width,
        screenHeight = screen.height,
        notifier = notifier
    )
    val addresses = lanAddresses()

    println("RemotePad server")
    println("Хост: $hostName, екран ${screen.width}x${screen.height}")
    println("Адреси в локальній мережі: ${addresses.joinToString { it.hostAddress }}")
    println("Порт: $port, PIN: ${authenticator.pin}")
    println("Push-сповіщення: ${if (notifier.isEnabled) "увімкнено" else "вимкнено (немає ключа Firebase)"}")

    DiscoveryResponder(port, hostName).start()
    val mdns = addresses.firstOrNull()?.let { MdnsAdvertiser(it, hostName, port).start() }
    val server = ControlServer(port, handler)

    // Ctrl+C або вимкнення ПК: спершу сповіщаю телефони, потім знімаю оголошення mDNS
    Runtime.getRuntime().addShutdownHook(Thread {
        handler.onServerStopping()
        mdns?.close()
        server.stop()
    })
    server.start(wait = true)
}

private fun lanAddresses(): List<InetAddress> =
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filter { it.isSiteLocalAddress }
