package com.remotepad.client

import android.app.Application
import com.remotepad.client.push.ConnectionNotifications

class RemotePadApplication : Application() {
    // Залежності створюю вручну: для шести екранів Hilt був би важчим за саму програму
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ConnectionNotifications.createChannel(this)
    }
}
