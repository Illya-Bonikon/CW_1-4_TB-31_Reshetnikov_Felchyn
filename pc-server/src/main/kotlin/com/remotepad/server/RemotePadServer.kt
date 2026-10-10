package com.remotepad.server

import com.remotepad.protocol.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Серверний компонент RemotePad (Розробник: Фельчин М. Б.).
 * Забезпечує WebSocket транспорт, фільтрацію пакетів та емуляцію дій через AWT Robot.
 */
class RemotePadServer(
    private val port: Int = 8080,
    private val expectedPin: String = "4812"
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val activeSessions = ConcurrentHashMap<String, String>() // token -> deviceId
    private val robot: Robot = Robot()
    private val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize

    // Параметри фільтрації та чутливості курсора
    private var sensitivity: Float = 1.35f
    private var smoothedX: Float = 0f
    private var smoothedY: Float = 0f
    private val smoothingFactor: Float = 0.65f // Експоненційне згладжування (EMA)

    fun start() {
        println("=== RemotePad Server Daemon ===")
        println("Роздільна здатність екрана: ${screenSize.width}x${screenSize.height}")
        println("Порт прослуховування: $port")
        println("Згенерований PIN авторизації: $expectedPin")

        embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/control") {
                    println("Нове клієнтське з'єднання встановлено.")
                    var authenticatedToken: String? = null

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleIncomingPacket(text, this, onAuthSuccess = { token ->
                                    authenticatedToken = token
                                })
                            }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        println("Клієнт коректно завершив сесію.")
                    } catch (e: Throwable) {
                        println("Помилка під час обробки каналу сокета: ${e.message}")
                    } finally {
                        authenticatedToken?.let { token ->
                            activeSessions.remove(token)
                            println("Сесію $token видалено з реєстру активних підключень.")
                        }
                    }
                }
            }
        }.start(wait = true)
    }

    private suspend fun handleIncomingPacket(
        payload: String,
        session: DefaultWebSocketServerSession,
        onAuthSuccess: (String) -> Unit
    ) {
        try {
            val packet = json.decodeFromString<InputEventPacket>(payload)

            when (packet.type) {
                EventType.AUTH_REQUEST -> {
                    val authReq = json.decodeFromString<AuthRequest>(packet.text ?: "")
                    if (authReq.pinCode == expectedPin) {
                        val token = "sess_${System.currentTimeMillis()}_${authReq.deviceName}"
                        activeSessions[token] = authReq.deviceName
                        onAuthSuccess(token)

                        val resp = AuthResponse(
                            isSuccess = true,
                            sessionToken = token,
                            serverHostName = "PC-WORKSTATION-DESKTOP",
                            screenWidth = screenSize.width,
                            screenHeight = screenSize.height
                        )
                        session.send(Frame.Text(json.encodeToString(InputEventPacket(
                            type = EventType.AUTH_RESPONSE,
                            text = json.encodeToString(resp)
                        ))))
                        println("Успішна авторизація пристрою: ${authReq.deviceName} (IP: ${authReq.clientIp})")
                    } else {
                        val resp = AuthResponse(
                            isSuccess = false,
                            errorMessage = "Невірний PIN-код авторизації сервера"
                        )
                        session.send(Frame.Text(json.encodeToString(InputEventPacket(
                            type = EventType.AUTH_RESPONSE,
                            text = json.encodeToString(resp)
                        ))))
                        println("Відхилено спробу підключення: невірний PIN (${authReq.deviceName})")
                    }
                }

                EventType.MOUSE_MOVE -> {
                    if (packet.sessionToken != null && activeSessions.containsKey(packet.sessionToken)) {
                        applyMouseMove(packet.deltaX, packet.deltaY)
                    }
                }

                EventType.MOUSE_CLICK -> {
                    if (packet.sessionToken != null && activeSessions.containsKey(packet.sessionToken)) {
                        applyMouseClick(packet.button)
                    }
                }

                EventType.MOUSE_WHEEL -> {
                    if (packet.sessionToken != null && activeSessions.containsKey(packet.sessionToken)) {
                        robot.mouseWheel(packet.deltaY.roundToInt())
                    }
                }

                EventType.MEDIA_ACTION -> {
                    if (packet.sessionToken != null && activeSessions.containsKey(packet.sessionToken)) {
                        applyMediaAction(packet.mediaAction)
                    }
                }

                EventType.PING -> {
                    session.send(Frame.Text(json.encodeToString(InputEventPacket(type = EventType.PONG))))
                }

                else -> {
                    // Інші типи подій
                }
            }
        } catch (e: Exception) {
            System.err.println("Помилка декодування пакета: ${e.message}")
        }
    }

    private fun applyMouseMove(dx: Float, dy: Float) {
        // Експоненційне згладжування тремтіння
        smoothedX = smoothedX * smoothingFactor + dx * (1f - smoothingFactor)
        smoothedY = smoothedY * smoothingFactor + dy * (1f - smoothingFactor)

        val currentPos = MouseInfo.getPointerInfo().location
        val targetX = (currentPos.x + smoothedX * sensitivity).roundToInt()
            .coerceIn(0, screenSize.width - 1)
        val targetY = (currentPos.y + smoothedY * sensitivity).roundToInt()
            .coerceIn(0, screenSize.height - 1)

        robot.mouseMove(targetX, targetY)
    }

    private fun applyMouseClick(button: MouseButton) {
        val mask = when (button) {
            MouseButton.LEFT -> InputEvent.BUTTON1_DOWN_MASK
            MouseButton.RIGHT -> InputEvent.BUTTON3_DOWN_MASK
            MouseButton.MIDDLE -> InputEvent.BUTTON2_DOWN_MASK
            MouseButton.NONE -> return
        }
        robot.mousePress(mask)
        robot.delay(15) // Симуляція природного затискання кнопки
        robot.mouseRelease(mask)
    }

    private fun applyMediaAction(action: MediaAction?) {
        when (action) {
            MediaAction.PRESENTATION_NEXT -> {
                robot.keyPress(KeyEvent.VK_RIGHT)
                robot.keyRelease(KeyEvent.VK_RIGHT)
            }
            MediaAction.PRESENTATION_PREV -> {
                robot.keyPress(KeyEvent.VK_LEFT)
                robot.keyRelease(KeyEvent.VK_LEFT)
            }
            MediaAction.PRESENTATION_START -> {
                robot.keyPress(KeyEvent.VK_F5)
                robot.keyRelease(KeyEvent.VK_F5)
            }
            MediaAction.PRESENTATION_EXIT -> {
                robot.keyPress(KeyEvent.VK_ESCAPE)
                robot.keyRelease(KeyEvent.VK_ESCAPE)
            }
            else -> {}
        }
    }
}

fun main() {
    RemotePadServer().start()
}
