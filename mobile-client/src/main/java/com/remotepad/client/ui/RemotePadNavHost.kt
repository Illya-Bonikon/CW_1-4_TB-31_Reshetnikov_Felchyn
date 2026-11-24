package com.remotepad.client.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.remotepad.client.AppContainer
import com.remotepad.client.ui.connect.ConnectScreen
import com.remotepad.client.ui.connect.ConnectViewModel
import com.remotepad.client.ui.control.ControlScreen
import com.remotepad.client.ui.control.ControlViewModel
import com.remotepad.client.ui.settings.SettingsViewModel

private object Routes {
    const val CONNECT = "connect"
    const val CONTROL = "control"
}

/** Два маршрути: підключення і керування. Режими керування перемикаються нижньою панеллю всередині. */
@Composable
fun RemotePadNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.CONNECT) {
        composable(Routes.CONNECT) {
            val vm: ConnectViewModel = viewModel(factory = ConnectViewModel.factory(container))
            ConnectScreen(vm, onConnected = {
                navController.navigate(Routes.CONTROL) { launchSingleTop = true }
            })
        }
        composable(Routes.CONTROL) {
            val control: ControlViewModel = viewModel(factory = ControlViewModel.factory(container))
            val settings: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
            ControlScreen(control, settings, onDisconnected = {
                navController.popBackStack(Routes.CONNECT, inclusive = false)
            })
        }
    }
}
