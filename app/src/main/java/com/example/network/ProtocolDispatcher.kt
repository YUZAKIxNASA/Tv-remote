package com.example.network

import com.example.model.CommandResult
import com.example.model.DiscoveredDevice
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.adapter.GenericTvAdapter
import com.example.network.adapter.MiTvHttpAdapter
import com.example.network.adapter.RokuEcpAdapter
import com.example.network.adapter.TvProtocolAdapter
import com.example.network.adapter.UpnpMediaAdapter

/**
 * Central protocol registry and dispatcher that routes commands to the proper adapter.
 */
class ProtocolDispatcher(
    private val miTvAdapter: MiTvHttpAdapter = MiTvHttpAdapter(),
    private val rokuAdapter: RokuEcpAdapter = RokuEcpAdapter(),
    private val upnpAdapter: UpnpMediaAdapter = UpnpMediaAdapter(),
    private val genericAdapter: GenericTvAdapter = GenericTvAdapter()
) {
    fun getAdapter(protocol: RemoteProtocol): TvProtocolAdapter {
        return when (protocol) {
            RemoteProtocol.MI_TV_HTTP -> miTvAdapter
            RemoteProtocol.ROKU_ECP -> rokuAdapter
            RemoteProtocol.UPNP_MEDIA -> upnpAdapter
            RemoteProtocol.GENERIC_HTTP -> genericAdapter
        }
    }

    suspend fun ping(device: DiscoveredDevice): Boolean {
        return getAdapter(device.protocol).ping(device.ipAddress, device.port)
    }

    suspend fun sendCommand(device: DiscoveredDevice, command: RemoteCommand): CommandResult {
        return getAdapter(device.protocol).sendCommand(device.ipAddress, device.port, command)
    }

    suspend fun sendText(device: DiscoveredDevice, text: String): CommandResult {
        return getAdapter(device.protocol).sendText(device.ipAddress, device.port, text)
    }

    suspend fun launchApp(device: DiscoveredDevice, appPackage: String): CommandResult {
        return getAdapter(device.protocol).launchApp(device.ipAddress, device.port, appPackage)
    }

    fun getSupportedCommands(protocol: RemoteProtocol): Set<RemoteCommand> {
        return getAdapter(protocol).getSupportedCommands()
    }

    /**
     * Attempts to auto-detect the smart TV protocol on an IP address.
     */
    suspend fun detectProtocol(ip: String): Pair<RemoteProtocol, Int>? {
        // 1. Try Mi TV on 6095
        if (miTvAdapter.ping(ip, 6095)) {
            return RemoteProtocol.MI_TV_HTTP to 6095
        }
        // 2. Try Roku ECP on 8060
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
