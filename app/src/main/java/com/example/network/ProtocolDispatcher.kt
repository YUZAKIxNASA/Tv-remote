package com.example.network

import android.content.Context
import com.example.model.CommandResult
import com.example.model.DiscoveredDevice
import com.example.model.PairingResult
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.adapter.AndroidTvRemoteAdapter
import com.example.network.adapter.GenericTvAdapter
import com.example.network.adapter.MiTvHttpAdapter
import com.example.network.adapter.RokuEcpAdapter
import com.example.network.adapter.TvProtocolAdapter
import com.example.network.adapter.UpnpMediaAdapter

/**
 * Central protocol registry and dispatcher.
 *
 * Enforces strict protocol separation:
 * - Android TV Remote v2 is handled via AndroidTvRemoteAdapter with secure PIN pairing.
 * - Xiaomi Mi TV is handled via MiTvHttpAdapter (ports 6095/6097).
 * - Roku / TCL is handled via RokuEcpAdapter (port 8060).
 * - Google Cast is identified as media streaming, not a remote key protocol.
 * - Unknown or unverified protocols never silently fall back to an unrelated adapter.
 */
class ProtocolDispatcher(
    context: Context? = null,
    val miTvAdapter: MiTvHttpAdapter = MiTvHttpAdapter(),
    val rokuAdapter: RokuEcpAdapter = RokuEcpAdapter(),
    val androidTvAdapter: AndroidTvRemoteAdapter = AndroidTvRemoteAdapter(context),
    val upnpAdapter: UpnpMediaAdapter = UpnpMediaAdapter(),
    val genericAdapter: GenericTvAdapter = GenericTvAdapter()
) {
    fun getAdapter(protocol: RemoteProtocol): TvProtocolAdapter? {
        return when (protocol) {
            RemoteProtocol.MI_TV_HTTP -> miTvAdapter
            RemoteProtocol.ROKU_ECP -> rokuAdapter
            RemoteProtocol.ANDROID_TV_REMOTE -> androidTvAdapter
            RemoteProtocol.UPNP_MEDIA -> upnpAdapter
            RemoteProtocol.GENERIC_HTTP -> genericAdapter
            RemoteProtocol.GOOGLE_CAST,
            RemoteProtocol.UNVERIFIED -> null
        }
    }

    suspend fun verifyDevice(device: DiscoveredDevice): ProtocolVerificationResult {
        return when (device.protocol) {
            RemoteProtocol.ANDROID_TV_REMOTE -> {
                androidTvAdapter.verifyConnection(device.ipAddress, device.port)
            }
            RemoteProtocol.GOOGLE_CAST -> {
                ProtocolVerificationResult.ProtocolUnverified(
                    "Google Cast service on port ${device.port} is for media streaming, not TV remote navigation keys."
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

    suspend fun startPairing(device: DiscoveredDevice): PairingResult {
        return if (device.protocol == RemoteProtocol.ANDROID_TV_REMOTE) {
            androidTvAdapter.startPairing(device.ipAddress, device.port)
        } else {
            PairingResult.Failed("Pairing is not required or unsupported for ${device.protocol.displayName}.")
        }
    }

    suspend fun submitPairingPin(device: DiscoveredDevice, pin: String): PairingResult {
        return if (device.protocol == RemoteProtocol.ANDROID_TV_REMOTE) {
            androidTvAdapter.submitPairingPin(device.ipAddress, device.port, pin)
        } else {
            PairingResult.Failed("Pairing is not supported for ${device.protocol.displayName}.")
        }
    }

    suspend fun ping(device: DiscoveredDevice): Boolean {
        return verifyDevice(device) is ProtocolVerificationResult.Verified
    }

    suspend fun sendCommand(device: DiscoveredDevice, command: RemoteCommand): CommandResult {
        val adapter = getAdapter(device.protocol)
            ?: return when (device.protocol) {
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
        // 3. Try Android TV Remote on 6467
        if (androidTvAdapter.ping(ip, 6467)) {
            return RemoteProtocol.ANDROID_TV_REMOTE to 6467
        }
        // 4. Try UPnP on 8080
        if (upnpAdapter.ping(ip, 8080)) {
            return RemoteProtocol.UPNP_MEDIA to 8080
        }
        // 5. Try Generic on 80
        if (genericAdapter.ping(ip, 80)) {
            return RemoteProtocol.GENERIC_HTTP to 80
        }
        return null
    }
}
