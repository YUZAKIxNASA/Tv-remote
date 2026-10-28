package com.example.model

/**
 * Represents a network-discovered or manually configured Smart TV device.
 */
data class DiscoveredDevice(
    val id: String,
    val name: String,
    val manufacturer: String,
    val model: String = "Smart TV",
    val ipAddress: String,
    val port: Int,
    val protocol: RemoteProtocol,
    val deviceType: String = "Smart TV",
    val isSaved: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val addressLabel: String
        get() = "$ipAddress:$port"
}
