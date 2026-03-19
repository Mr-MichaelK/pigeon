package com.example.pigeon.domain.network

sealed class ConnectionStatus {
    object OFF : ConnectionStatus()
    object PASSIVE : ConnectionStatus()
    object ACTIVE : ConnectionStatus()
}
