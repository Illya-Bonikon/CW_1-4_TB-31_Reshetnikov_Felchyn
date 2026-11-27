package com.remotepad.client

import android.content.Context
import android.os.Build
import com.remotepad.client.data.ConnectionHistoryRepository
import com.remotepad.client.data.MacroRepository
import com.remotepad.client.data.SettingsRepository
import com.remotepad.client.data.local.RemotePadDatabase
import com.remotepad.client.network.CompositeHostDiscovery
import com.remotepad.client.network.HostDiscoverySource
import com.remotepad.client.network.NsdHostDiscovery
import com.remotepad.client.network.RemotePadConnection
import com.remotepad.client.network.UdpHostDiscovery
import com.remotepad.client.network.WebSocketConnection
import com.remotepad.client.push.FirebasePushTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Об'єкти, які мають жити весь час роботи програми: одне з'єднання, одна БД. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database: RemotePadDatabase = RemotePadDatabase.create(context)
    val history = ConnectionHistoryRepository(database.hosts(), database.sessions())
    val macros = MacroRepository(database.macros())
    val settings = SettingsRepository(context)
    val push = FirebasePushTokenProvider(context)
    val connection: RemotePadConnection = WebSocketConnection(appScope, pushTokens = push)
    val discovery: HostDiscoverySource = CompositeHostDiscovery(
        listOf(UdpHostDiscovery(), NsdHostDiscovery(context))
    )
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
}
