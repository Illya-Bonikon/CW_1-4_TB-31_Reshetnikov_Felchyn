package com.remotepad.server.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.File

/**
 * Надсилання через Firebase Cloud Messaging (HTTP v1, Admin SDK).
 * Ключ сервісного акаунта читається з файлу поза репозиторієм; шлях задає змінна середовища.
 */
class FcmPushNotifier private constructor(private val messaging: FirebaseMessaging) : PushNotifier {

    override val isEnabled = true

    override fun connectionLost(pushToken: String, hostName: String) =
        send(listOf(pushToken), PushMessages.connectionLost(hostName))

    override fun hostStopped(pushTokens: Collection<String>, hostName: String) =
        send(pushTokens, PushMessages.hostStopped(hostName))

    private fun send(tokens: Collection<String>, message: PushMessage) {
        if (tokens.isEmpty()) return
        val messages = tokens.map { token ->
            Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(message.title).setBody(message.body).build())
                .putData("type", message.type)
                .putData("host", message.hostName)
                // високий пріоритет: сповіщення про розрив має прийти, навіть якщо телефон у режимі Doze
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                .build()
        }
        runCatching { messaging.sendEach(messages) }
            .onFailure { System.err.println("FCM: не вдалося надіслати сповіщення: ${it.message}") }
    }

    companion object {
        const val CREDENTIALS_ENV = "REMOTEPAD_FCM_CREDENTIALS"

        /** Повертає FCM-реалізацію, якщо ключ знайдено, інакше NoopPushNotifier. */
        fun fromEnvironment(env: Map<String, String> = System.getenv()): PushNotifier {
            val path = env[CREDENTIALS_ENV] ?: env["GOOGLE_APPLICATION_CREDENTIALS"] ?: return NoopPushNotifier
            val file = File(path)
            if (!file.isFile) {
                System.err.println("FCM: файл ключа $path не знайдено, push-сповіщення вимкнено")
                return NoopPushNotifier
            }
            val options = FirebaseOptions.builder()
                .setCredentials(file.inputStream().use(GoogleCredentials::fromStream))
                .build()
            return FcmPushNotifier(FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options)))
        }
    }
}
