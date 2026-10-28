package com.example.network

import com.example.model.CommandResult
import com.example.model.DiscoveredDevice
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.adapter.GenericTvAdapter
import com.example.network.adapter.MiTvHttpAdapter
import com.example.network.adapter.RokuEcpAdapter
import com.example.network.adapter.TvProtocolAdapter
import com.example.network.adapter.UpnpMediaAdapter

/**
 * Central protocol registry and dispatcher.
 *
 * Enforces strict protocol separation:
 * - Each device is routed only to its verified protocol adapter.
 * - Unknown or unverified protocols never silently fall back to an unrelated adapter.
 * - Google Cast, Android TV Remote, Xiaomi HTTP, Roku ECP, and UPnP are distinct and never interchangeable.
 */
class ProtocolDispatcher(
    private val miTvAdapter: MiTvHttpAdapter = MiTvHttpAdapter(),
    private val rokuAdapter: RokuEcpAdapter = RokuEcpAdapter(),
    private val upnpAdapter: UpnpMediaAdapter = UpnpMediaAdapter(),
    private val genericAdapter: GenericTvAdapter = GenericTvAdapter()
) {
    fun getAdapter(protocol: RemoteProtocol): TvProtocolAdapter? {
        return when (protocol) {
            RemoteProtocol.MI_TV_HTTP -> miTvAdapter
            RemoteProtocol.ROKU_ECP -> rokuAdapter
            RemoteProtocol.UPNP_MEDIA -> upnpAdapter
            RemoteProtocol.GENERIC_HTTP -> genericAdapter
            RemoteProtocol.ANDROID_TV_REMOTE,
            RemoteProtocol.GOOGLE_CAST,
            RemoteProtocol.UNVERIFIED -> null
        }
    }

    suspend fun verifyDevice(device: DiscoveredDevice): ProtocolVerificationResult {
        return when (device.protocol) {
            RemoteProtocol.ANDROID_TV_REMOTE -> {
                ProtocolVerificationResult.PairingRequired(
                    "Android TV Remote v2 detected on port ${device.port}. Pairing via TLS key exchange is required before commands can be sent."
                )
            }
            RemoteProtocol.GOOGLE_CAST -> {
                ProtocolVerificationResult.ProtocolUnverified(
                    "Google Cast service on port ${device.port} is for media streaming, not remote control keyevents."
                )
            }
            RemoteProtocol.UNVERIFIED -> {
                ProtocolVerificationResult.ProtocolUnverified(
                    "Device protocol at ${device.addressLabel} is unverified. Please confirm your TV model or use Manual IP."
                )
            }
            RemoteProtocol.MI_TV_HTTP,
            RemoteProtocol.ROKU_ECP,
            RemoteProtocol.UPNP_MEDIA,
            RemoteProtocol.GENERIC_HTTP -> {
                val adapter = getAdapter(device.protocol)
                adapter?.verifyConnection(device.ipAddress, device.port)
                    ?: ProtocolVerificationResult.ProtocolUnverified("No adapter registered for ${device.protocol.displayName}")
            }
        }
    }

    suspend fun ping(device: DiscoveredDevice): Boolean {
        return verifyDevice(device) is ProtocolVerificationResult.Verified
    }

    suspend fun sendCommand(device: DiscoveredDevice, command: RemoteCommand): CommandResult {
        val adapter = getAdapter(device.protocol)
            ?: return when (device.protocol) {
                RemoteProtocol.ANDROID_TV_REMOTE -> CommandResult.Unsupported(
                    "Android TV Remote v2 requires TLS pairing before remote commands can be sent."
                )
                RemoteProtocol.GOOGLE_CAST -> CommandResult.Unsupported(
                    "Google Cast does not support TV keyevents."
                )
                RemoteProtocol.UNVERIFIED -> CommandResult.Unsupported(
                    "Cannot send commands: device protocol is unverified."
                )
                else -> CommandResult.Unsupported(
                    "Protocol '${device.protocol.displayName}' does not have a functional remote adapter."
                )
            }
        return adapter.sendCommand(device.ipAddress, device.port, command)
    }

    suspend fun sendText(device: DiscoveredDevice, text: String): CommandResult {
        val adapter = getAdapter(device.protocol)
            ?: return CommandResult.Unsupported("Text input is unsupported for ${device.protocol.displayName}")
        return adapter.sendText(device.ipAddress, device.port, text)
    }

    suspend fun launchApp(device: DiscoveredDevice, appPackage: String): CommandResult {
        val adapter = getAdapter(device.protocol)
            ?: return CommandResult.Unsupported("App launching is unsupported for ${device.protocol.displayName}")
        return adapter.launchApp(device.ipAddress, device.port, appPackage)
    }

    fun getSupportedCommands(protocol: RemoteProtocol): Set<RemoteCommand> {
        return getAdapter(protocol)?.getSupportedCommands() ?: emptySet()
    }

    /**
     * Attempts to auto-detect the smart TV protocol on an IP address using actual protocol verification.
     */
    suspend fun detectProtocol(ip: String): Pair<RemoteProtocol, Int>? {
        // 1. Try Xiaomi Mi TV on 6095 with HTTP verification
        if (miTvAdapter.ping(ip, 6095)) {
            return RemoteProtocol.MI_TV_HTTP to 6095
        }
        // 2. Try Roku ECP on 8060 with device-info verification
        if (rokuAdapter.ping(ip, 8060)) {
            return RemoteProtocol.ROKU_ECP to 8060
        }
        // 3. Try UPnP on 8080
        if (upnpAdapter.ping(ip, 8080)) {
            return RemoteProtocol.UPNP_MEDIA to 8080
        }
        // 4. Try Generic on 80
        if (genericAdapter.ping(ip, 80)) {
            return RemoteProtocol.GENERIC_HTTP to 80
        }
        return null
    }
}
