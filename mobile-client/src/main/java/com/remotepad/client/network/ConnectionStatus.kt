package com.remotepad.client.network

sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data class Connected(val hostName: String) : ConnectionStatus
    data class Reconnecting(val attempt: Int) : ConnectionStatus
    data class Lost(val reason: String) : ConnectionStatus
}

sealed interface AuthResult {
    data class Success(val token: String, val hostName: String) : AuthResult
    data class Rejected(val message: String) : AuthResult
    data class Unreachable(val message: String) : AuthResult
}

data class HostTarget(val ip: String, val port: Int)
