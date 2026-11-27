package com.remotepad.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.remotepad.client.ui.RemotePadNavHost
import com.remotepad.client.ui.theme.RemotePadTheme

class MainActivity : ComponentActivity() {

    // відмова не заважає роботі: застосунок просто не показуватиме сповіщень про розрив
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as RemotePadApplication).container
        if (container.push.isConfigured) askNotificationPermission()
        setContent {
            RemotePadTheme {
                RemotePadNavHost(container)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
