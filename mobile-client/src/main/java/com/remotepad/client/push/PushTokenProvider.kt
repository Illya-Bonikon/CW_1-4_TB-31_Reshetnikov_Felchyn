package com.remotepad.client.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Джерело токена для push-сповіщень. Без налаштованого Firebase токена немає, і це нормально. */
fun interface PushTokenProvider {
    suspend fun token(): String?
}

/**
 * Токен Firebase Cloud Messaging. Firebase ініціалізується з google-services.json, якого немає
 * в репозиторії; у збірці без нього initializeApp поверне null, і застосунок працює без push.
 */
class FirebasePushTokenProvider(private val context: Context) : PushTokenProvider {

    val isConfigured: Boolean by lazy {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }

    override suspend fun token(): String? {
        if (!isConfigured) return null
        // токен не повинен затримувати підключення: якщо Google Play Services мовчить, ідемо без нього
        return withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        }
    }

    private companion object {
        const val TOKEN_TIMEOUT_MS = 2000L
    }
}
