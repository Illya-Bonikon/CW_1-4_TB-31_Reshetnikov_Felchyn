package com.remotepad.client

import android.content.Context
import android.os.Build
import com.remotepad.client.data.SettingsRepository
import com.remotepad.client.data.local.RemotePadDatabase
import com.remotepad.client.network.HostDiscovery
import com.remotepad.client.network.RemotePadConnection
import com.remotepad.client.network.WebSocketConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/** Об'єкти, які мають жити весь час роботи програми: одне з'єднання, одна БД. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database: RemotePadDatabase = RemotePadDatabase.create(context)
    val settings = SettingsRepository(context)
    val connection: RemotePadConnection = WebSocketConnection(appScope)
    val discovery = HostDiscovery()
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
}
