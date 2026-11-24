package com.remotepad.client

import android.app.Application

class RemotePadApplication : Application() {
    // Залежності створюю вручну: для шести екранів Hilt був би важчим за саму програму
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
