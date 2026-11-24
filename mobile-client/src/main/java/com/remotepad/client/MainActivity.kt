package com.remotepad.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.remotepad.client.ui.RemotePadNavHost
import com.remotepad.client.ui.theme.RemotePadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as RemotePadApplication).container
        setContent {
            RemotePadTheme {
                RemotePadNavHost(container)
            }
        }
    }
}
