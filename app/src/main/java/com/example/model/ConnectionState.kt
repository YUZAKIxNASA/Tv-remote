package com.example.model

/**
 * Real device connection states across the discovery and control lifecycle.
 */
sealed class DeviceConnectionState {
    data object NotConnected : DeviceConnectionState()
    data object Discovering : DeviceConnectionState()
    data class Discovered(val device: DiscoveredDevice) : DeviceConnectionState()
    data class Unverified(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data class ProtocolUnverified(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data class PairingRequired(val device: DiscoveredDevice, val instructions: String) : DeviceConnectionState()
    data class PairingInProgress(val device: DiscoveredDevice, val stepDescription: String) : DeviceConnectionState()
    data class Connecting(val device: DiscoveredDevice) : DeviceConnectionState()
    data class Connected(val device: DiscoveredDevice, val pingMs: Long) : DeviceConnectionState()
    data class ConnectionFailed(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data class Offline(val device: DiscoveredDevice, val reason: String = "Device is offline or unreachable") : DeviceConnectionState()
    data class UnsupportedProtocol(val device: DiscoveredDevice, val reason: String) : DeviceConnectionState()
    data object Disconnected : DeviceConnectionState()
}

/**
 * Result of protocol verification on a target device.
 */
sealed class ProtocolVerificationResult {
    data class Verified(val latencyMs: Long) : ProtocolVerificationResult()
    data class ProtocolUnverified(val reason: String) : ProtocolVerificationResult()
    data class PairingRequired(val instructions: String) : ProtocolVerificationResult()
    data class Unsupported(val reason: String) : ProtocolVerificationResult()
    data class Failed(val reason: String) : ProtocolVerificationResult()
}

/**
 * Result of a device pairing handshake.
 */
sealed class PairingResult {
    data class Success(val message: String, val latencyMs: Long = 0) : PairingResult()
    data class Failed(val reason: String) : PairingResult()
    data class InvalidPin(val reason: String = "Incorrect pairing PIN. Please check your TV screen and try again.") : PairingResult()
}
