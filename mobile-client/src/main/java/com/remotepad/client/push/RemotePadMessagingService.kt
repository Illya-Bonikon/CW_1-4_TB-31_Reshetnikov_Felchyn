package com.remotepad.client.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Приймає сповіщення від сервера. Коли застосунок у фоні, Android показує notification-частину сам,
 * а тут обробляється випадок, коли застосунок відкритий.
 */
class RemotePadMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        ConnectionNotifications.show(
            context = this,
            title = notification.title ?: "RemotePad",
            body = notification.body.orEmpty()
        )
    }

    // новий токен сервер отримає під час наступного підключення в AuthRequest, зберігати його ніде не треба
    override fun onNewToken(token: String) = Unit
}
