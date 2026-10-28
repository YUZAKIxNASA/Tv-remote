package com.example.model

/**
 * Real device connection states.
 */
sealed class DeviceConnectionState {
    data object NotConnected : DeviceConnectionState()
    data class Discovered(val device: DiscoveredDevice) : DeviceConnectionState()
    data class ProtocolUnverified(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data class PairingRequired(val device: DiscoveredDevice, val instructions: String) : DeviceConnectionState()
    data class Connecting(val device: DiscoveredDevice) : DeviceConnectionState()
    data class Connected(val device: DiscoveredDevice, val pingMs: Long) : DeviceConnectionState()
    data class ConnectionFailed(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data object Disconnected : DeviceConnectionState()
}

/**
 * Result of protocol verification on a target device.
 */
sealed class ProtocolVerificationResult {
    data class Verified(val latencyMs: Long) : ProtocolVerificationResult()
    data class ProtocolUnverified(val reason: String) : ProtocolVerificationResult()
    data class PairingRequired(val instructions: String) : ProtocolVerificationResult()
    data class Failed(val reason: String) : ProtocolVerificationResult()
}
