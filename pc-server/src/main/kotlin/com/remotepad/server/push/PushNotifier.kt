package com.remotepad.server.push

/** Надсилання push-сповіщень на телефони. Без ключа Firebase сервер працює з NoopPushNotifier. */
interface PushNotifier {
    /** Зв'язок із конкретним телефоном обірвався не з його ініціативи. */
    fun connectionLost(pushToken: String, hostName: String)

    /** Сервер зупиняється; сповіщення отримують усі підключені телефони. */
    fun hostStopped(pushTokens: Collection<String>, hostName: String)

    val isEnabled: Boolean
}

object NoopPushNotifier : PushNotifier {
    override fun connectionLost(pushToken: String, hostName: String) = Unit
    override fun hostStopped(pushTokens: Collection<String>, hostName: String) = Unit
    override val isEnabled = false
}

/** Тексти сповіщень зібрані окремо, щоб їх можна було перевірити тестом без Firebase. */
object PushMessages {
    const val TYPE_CONNECTION_LOST = "connection_lost"
    const val TYPE_HOST_STOPPED = "host_stopped"

    fun connectionLost(hostName: String) = PushMessage(
        type = TYPE_CONNECTION_LOST,
        title = "RemotePad: зв'язок втрачено",
        body = "З'єднання з $hostName розірвано. Відкрийте застосунок, щоб підключитися знову.",
        hostName = hostName
    )

    fun hostStopped(hostName: String) = PushMessage(
        type = TYPE_HOST_STOPPED,
        title = "RemotePad: ПК відключився",
        body = "Сервер на $hostName зупинено.",
        hostName = hostName
    )
}

data class PushMessage(val type: String, val title: String, val body: String, val hostName: String)
