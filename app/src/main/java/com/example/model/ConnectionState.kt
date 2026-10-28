package com.example.model

/**
 * Real device connection states.
 */
sealed class DeviceConnectionState {
    data object NotConnected : DeviceConnectionState()
    data class Connecting(val device: DiscoveredDevice) : DeviceConnectionState()
    data class Connected(val device: DiscoveredDevice, val pingMs: Long) : DeviceConnectionState()
    data class ConnectionFailed(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data object Disconnected : DeviceConnectionState()
}
