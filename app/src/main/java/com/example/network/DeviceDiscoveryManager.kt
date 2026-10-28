package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.DiscoveredDevice
import com.example.model.RemoteProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URI
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Smart TV discovery manager supporting:
 * 1. Android NsdManager (mDNS with strict service-type preservation)
 * 2. SSDP / UPnP multicast probing (239.255.255.250:1900)
 * 3. Local Subnet scan with protocol verification
 *
 * Guarantees:
 * - _androidtvremote2._tcp is NEVER classified as Xiaomi HTTP.
 * - _googlecast._tcp is NEVER classified as Xiaomi HTTP or UPnP.
 * - Unknown services are marked UNVERIFIED instead of guessing.
 * - Generic open ports do NOT establish verified protocol identity.
 */
class DeviceDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val probeClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.MILLISECONDS)
        .readTimeout(600, TimeUnit.MILLISECONDS)
        .build()
) {
    private val tag = "DeviceDiscoveryManager"

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private var scanJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val activeNsdListeners = mutableListOf<NsdManager.DiscoveryListener>()

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanStatusMessage.value = "Scanning local WiFi network for Smart TVs..."

        acquireMulticastLock()

        scanJob = scope.launch(Dispatchers.IO) {
            try {
                // 1. SSDP multicast search
                launch { probeSsdp() }

                // 2. mDNS discovery
                launch { startMdnsDiscovery() }

                // 3. Subnet probe with corroborating protocol checks
                launch { probeLocalSubnet() }

                delay(8000)
            } catch (e: Exception) {
                Log.e(tag, "Discovery error: ${e.message}", e)
                _scanStatusMessage.value = "Discovery error: ${e.localizedMessage}"
            } finally {
                stopDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        scanJob?.cancel()
        scanJob = null
        stopMdnsDiscovery()
        releaseMulticastLock()
        _isScanning.value = false
        if (_discoveredDevices.value.isEmpty()) {
            _scanStatusMessage.value = "No TV devices detected. Try Manual IP or verify WiFi connection."
        } else {
            _scanStatusMessage.value = "Found ${_discoveredDevices.value.size} device(s) on network."
        }
    }

    fun addManualDevice(ip: String, port: Int, name: String, protocol: RemoteProtocol): DiscoveredDevice {
        val manufacturer = when (protocol) {
            RemoteProtocol.MI_TV_HTTP -> "Xiaomi"
            RemoteProtocol.ROKU_ECP -> "Roku / TCL"
            RemoteProtocol.ANDROID_TV_REMOTE -> "Android TV"
            RemoteProtocol.GOOGLE_CAST -> "Google Cast"
            RemoteProtocol.UPNP_MEDIA -> "UPnP Media"
            RemoteProtocol.GENERIC_HTTP -> "Network TV"
            RemoteProtocol.UNVERIFIED -> "Custom Device"
        }
        val device = DiscoveredDevice(
            id = "${ip}_$port",
            name = name.ifBlank { "$manufacturer ($ip)" },
            manufacturer = manufacturer,
            model = "Smart TV",
            ipAddress = ip,
            port = port,
            protocol = protocol,
            isSaved = true
        )
        addDevice(device)
        return device
    }

    fun addDevice(device: DiscoveredDevice) {
        _discoveredDevices.update { list ->
            val existingIndex = list.indexOfFirst {
                it.ipAddress == device.ipAddress && it.port == device.port
            }

            if (existingIndex >= 0) {
                val existing = list[existingIndex]
                // Deduplication: Avoid replacing high-fidelity metadata with weaker metadata
                val mergedDevice = mergeDeviceMetadata(existing, device)
                list.toMutableList().apply { this[existingIndex] = mergedDevice }
            } else {
                list + device
            }
        }
    }

    /**
     * Merges two discovery records for the same endpoint, preserving higher-fidelity information.
     */
    private fun mergeDeviceMetadata(existing: DiscoveredDevice, update: DiscoveredDevice): DiscoveredDevice {
        // 1. Protocol: Prefer verified / specific protocols over UNVERIFIED
        val protocol = if (existing.protocol != RemoteProtocol.UNVERIFIED && update.protocol == RemoteProtocol.UNVERIFIED) {
            existing.protocol
        } else {
            update.protocol
        }

        // 2. Name: Prefer meaningful names over generic fallbacks
        val name = if (isGenericName(update.name) && !isGenericName(existing.name)) {
            existing.name
        } else {
            update.name
        }

        // 3. Manufacturer: Prefer specific manufacturer over generic
        val manufacturer = if (existing.manufacturer != "Smart TV" && existing.manufacturer != "Unknown Device" &&
            (update.manufacturer == "Smart TV" || update.manufacturer == "Unknown Device")
        ) {
            existing.manufacturer
        } else {
            update.manufacturer
        }

        return existing.copy(
            name = name,
            manufacturer = manufacturer,
            protocol = protocol,
            isSaved = existing.isSaved || update.isSaved,
            lastSeenTimestamp = System.currentTimeMillis()
        )
    }

    private fun isGenericName(name: String): Boolean {
        return name.startsWith("Smart TV (") ||
                name.startsWith("Network Device (") ||
                name.startsWith("Device on port ")
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("NasaRemoteMulticast")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not acquire MulticastLock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing MulticastLock: ${e.message}")
        }
    }

    /**
     * SSDP discovery over UDP multicast (239.255.255.250:1900)
     */
    private suspend fun probeSsdp() = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 3000
            val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: ssdp:all\r\n\r\n"

            val queryBytes = ssdpQuery.toByteArray()
            val group = InetAddress.getByName("239.255.255.250")
            val packet = DatagramPacket(queryBytes, queryBytes.size, group, 1900)
            socket.send(packet)

            val receiveBuffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 4000) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val ip = receivePacket.address.hostAddress ?: continue
                    parseSsdpResponse(ip, response)
                } catch (_: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "SSDP search exception: ${e.message}")
        } finally {
            socket?.close()
        }
    }

    fun parseSsdpResponse(ip: String, response: String) {
        val upper = response.uppercase()

        // Extract port from LOCATION header if present
        var extractedPort = extractPortFromLocation(response)
        var name = "Smart TV ($ip)"
        var protocol = RemoteProtocol.UNVERIFIED
        var manufacturer = "Smart TV"

        when {
            upper.contains("XIAOMI") || upper.contains("MITV") || upper.contains("PATCHWALL") -> {
                name = "Xiaomi Mi TV"
                manufacturer = "Xiaomi"
                protocol = RemoteProtocol.MI_TV_HTTP
                if (extractedPort == null || extractedPort == 80) extractedPort = 6095
            }
            upper.contains("ROKU") || upper.contains("ROKU:ECP") -> {
                name = "Roku Smart TV"
                manufacturer = "Roku / TCL"
                protocol = RemoteProtocol.ROKU_ECP
                if (extractedPort == null || extractedPort == 80) extractedPort = 8060
            }
            upper.contains("URN:SCHEMAS-UPNP-ORG:DEVICE:MEDIARENDERER") -> {
                name = "UPnP Media Renderer ($ip)"
                manufacturer = "UPnP TV"
                protocol = RemoteProtocol.UPNP_MEDIA
                if (extractedPort == null) extractedPort = 8080
            }
            upper.contains("SAMSUNG") -> {
                name = "Samsung Smart TV"
                manufacturer = "Samsung"
                protocol = RemoteProtocol.UNVERIFIED
            }
            upper.contains("LG") || upper.contains("WEBOS") -> {
                name = "LG webOS TV"
                manufacturer = "LG"
                protocol = RemoteProtocol.UNVERIFIED
            }
            upper.contains("SONY") || upper.contains("BRAVIA") -> {
                name = "Sony BRAVIA TV"
                manufacturer = "Sony"
                protocol = RemoteProtocol.UNVERIFIED
            }
        }

        val port = extractedPort ?: protocol.defaultPort.takeIf { it > 0 } ?: 80

        val device = DiscoveredDevice(
            id = "${ip}_$port",
            name = name,
            manufacturer = manufacturer,
            ipAddress = ip,
            port = port,
            protocol = protocol
        )
        addDevice(device)
    }

    private fun extractPortFromLocation(response: String): Int? {
        val locationLine = response.lines().firstOrNull { it.trim().startsWith("LOCATION:", ignoreCase = true) }
            ?: return null
        val urlString = locationLine.substringAfter(":", "").trim()
        return try {
            val uri = URI(urlString)
            if (uri.port > 0) uri.port else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * mDNS discovery via Android NsdManager with strict service type preservation.
     */
    private fun startMdnsDiscovery() {
        if (nsdManager == null) return
        val serviceTypes = listOf("_androidtvremote2._tcp.", "_googlecast._tcp.", "_airplay._tcp.")

        serviceTypes.forEach { serviceType ->
            try {
                val listener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                        Log.w(tag, "mDNS start failed for $serviceType: $errorCode")
                    }

                    override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                        Log.w(tag, "mDNS stop failed for $serviceType: $errorCode")
                    }

                    override fun onDiscoveryStarted(serviceType: String?) {
                        Log.d(tag, "mDNS discovery started: $serviceType")
                    }

                    override fun onDiscoveryStopped(serviceType: String?) {
                        Log.d(tag, "mDNS discovery stopped: $serviceType")
                    }

                    override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                        serviceInfo?.let { resolveNsdService(it, serviceType) }
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                        Log.d(tag, "mDNS service lost: ${serviceInfo?.serviceName}")
                    }
                }
                activeNsdListeners.add(listener)
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Log.w(tag, "Failed to start Nsd for $serviceType: ${e.message}")
            }
        }
    }

    fun resolveNsdService(serviceInfo: NsdServiceInfo, registeredServiceType: String) {
        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(tag, "mDNS resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                resolvedInfo ?: return
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                val serviceName = resolvedInfo.serviceName

                val effectiveType = resolvedInfo.serviceType?.ifBlank { registeredServiceType } ?: registeredServiceType
                val normalizedType = effectiveType.trim().trimEnd('.').lowercase()

                // Protocol determination based on genuine service type, NEVER solely on port or 'Mi' in service name
                val (protocol, manufacturer) = when {
                    normalizedType.contains("_androidtvremote2") || registeredServiceType.contains("_androidtvremote2") -> {
                        val mfg = if (serviceName.contains("Xiaomi", ignoreCase = true) || serviceName.contains("Mi ", ignoreCase = true)) {
                            "Xiaomi (Android TV)"
                        } else {
                            "Android TV"
                        }
                        RemoteProtocol.ANDROID_TV_REMOTE to mfg
                    }
                    normalizedType.contains("_googlecast") || registeredServiceType.contains("_googlecast") -> {
                        RemoteProtocol.GOOGLE_CAST to "Google Cast / Android TV"
                    }
                    normalizedType.contains("_airplay") || registeredServiceType.contains("_airplay") -> {
                        RemoteProtocol.UNVERIFIED to "Apple AirPlay"
                    }
                    else -> {
                        RemoteProtocol.UNVERIFIED to "Unknown Device"
                    }
                }

                val device = DiscoveredDevice(
                    id = "${host}_$port",
                    name = serviceName.ifBlank { "Android Device ($host)" },
                    manufacturer = manufacturer,
                    ipAddress = host,
                    port = port,
                    protocol = protocol
                )
                addDevice(device)
            }
        })
    }

    private fun stopMdnsDiscovery() {
        if (nsdManager == null) return
        activeNsdListeners.forEach { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (_: Exception) {}
        }
        activeNsdListeners.clear()
    }

    /**
     * Subnet probe: scans local IPs and requires corroborating HTTP protocol verification
     * before assigning a specific TV remote protocol.
     */
    private suspend fun probeLocalSubnet() = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress() ?: return@withContext
        val subnet = localIp.substringBeforeLast(".") + "."

        val hostsToProbe = mutableListOf<String>()
        val lastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: 100
        for (i in (lastOctet - 10).coerceAtLeast(1)..(lastOctet + 10).coerceAtMost(254)) {
            if (i != lastOctet) hostsToProbe.add("$subnet$i")
        }
        for (i in 1..15) {
            val ip = "$subnet$i"
            if (!hostsToProbe.contains(ip)) hostsToProbe.add(ip)
        }

        hostsToProbe.forEach { targetIp ->
            launch {
                // Check port 6095 with HTTP corroboration
                if (isPortOpen(targetIp, 6095, 250)) {
                    val isVerifiedMiTv = verifyMiTvSubnet(targetIp, 6095)
                    addDevice(
                        DiscoveredDevice(
                            id = "${targetIp}_6095",
                            name = if (isVerifiedMiTv) "Xiaomi Mi TV ($targetIp)" else "Device on port 6095 ($targetIp)",
                            manufacturer = if (isVerifiedMiTv) "Xiaomi" else "Unknown",
                            ipAddress = targetIp,
                            port = 6095,
                            protocol = if (isVerifiedMiTv) RemoteProtocol.MI_TV_HTTP else RemoteProtocol.UNVERIFIED
                        )
                    )
                }

                // Check port 8060 with HTTP corroboration
                if (isPortOpen(targetIp, 8060, 250)) {
                    val isVerifiedRoku = verifyRokuSubnet(targetIp, 8060)
                    addDevice(
                        DiscoveredDevice(
                            id = "${targetIp}_8060",
                            name = if (isVerifiedRoku) "Roku Smart TV ($targetIp)" else "Device on port 8060 ($targetIp)",
                            manufacturer = if (isVerifiedRoku) "Roku / TCL" else "Unknown",
                            ipAddress = targetIp,
                            port = 8060,
                            protocol = if (isVerifiedRoku) RemoteProtocol.ROKU_ECP else RemoteProtocol.UNVERIFIED
                        )
                    )
                }
            }
        }
    }

    private fun verifyMiTvSubnet(ip: String, port: Int): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://$ip:$port/request?action=isalive")
                .get()
                .build()
            probeClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun verifyRokuSubnet(ip: String, port: Int): Boolean {
        return try {
            val request = Request.Builder()
                .url("http://$ip:$port/query/device-info")
                .get()
                .build()
            probeClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                response.isSuccessful && body.contains("<device-info", ignoreCase = true)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.indexOf(':') == -1) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
