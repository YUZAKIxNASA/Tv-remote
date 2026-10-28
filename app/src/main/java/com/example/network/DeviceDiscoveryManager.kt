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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

/**
 * Robust Smart TV discovery manager supporting:
 * 1. SSDP / UPnP multicast probing (239.255.255.250:1900)
 * 2. Android NsdManager (mDNS _androidtvremote2, _googlecast, _http)
 * 3. Local Subnet TV Port scan (probe port 6095 for Mi TV, 8060 for Roku, etc.)
 */
class DeviceDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
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
                // Launch SSDP multicast probe concurrently
                launch { probeSsdp() }

                // Launch mDNS discovery
                launch { startMdnsDiscovery() }

                // Launch Local Subnet Port scan for Mi TV (port 6095) and Roku (port 8060)
                launch { probeLocalSubnet() }

                // Scan runs for 8 seconds then wraps up
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
            RemoteProtocol.UPNP_MEDIA -> "UPnP Media"
            RemoteProtocol.GENERIC_HTTP -> "Network TV"
        }
        val device = DiscoveredDevice(
            id = "manual_${ip}_$port",
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

    private fun addDevice(device: DiscoveredDevice) {
        _discoveredDevices.update { list ->
            val existingIndex = list.indexOfFirst { it.ipAddress == device.ipAddress && it.port == device.port }
            if (existingIndex >= 0) {
                list.toMutableList().apply { this[existingIndex] = device }
            } else {
                list + device
            }
        }
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

    private fun parseSsdpResponse(ip: String, response: String) {
        val upper = response.uppercase()
        var name = "Smart TV ($ip)"
        var protocol = RemoteProtocol.UPNP_MEDIA
        var port = 8080
        var manufacturer = "Smart TV"

        when {
            upper.contains("XIAOMI") || upper.contains("MITV") || upper.contains("PATCHWALL") -> {
                name = "Xiaomi Mi TV"
                manufacturer = "Xiaomi"
                protocol = RemoteProtocol.MI_TV_HTTP
                port = 6095
            }
            upper.contains("ROKU") -> {
                name = "Roku Smart TV"
                manufacturer = "Roku / TCL"
                protocol = RemoteProtocol.ROKU_ECP
                port = 8060
            }
            upper.contains("SAMSUNG") -> {
                name = "Samsung Smart TV"
                manufacturer = "Samsung"
            }
            upper.contains("LG") || upper.contains("WEBOS") -> {
                name = "LG webOS TV"
                manufacturer = "LG"
            }
            upper.contains("SONY") || upper.contains("BRAVIA") -> {
                name = "Sony BRAVIA TV"
                manufacturer = "Sony"
            }
        }

        val device = DiscoveredDevice(
            id = "ssdp_${ip}_$port",
            name = name,
            manufacturer = manufacturer,
            ipAddress = ip,
            port = port,
            protocol = protocol
        )
        addDevice(device)
    }

    /**
     * mDNS discovery via Android NsdManager
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
                        serviceInfo?.let { resolveNsdService(it) }
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

    private fun resolveNsdService(serviceInfo: NsdServiceInfo) {
        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(tag, "mDNS resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                resolvedInfo ?: return
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                val serviceName = resolvedInfo.serviceName

                val protocol = if (port == 6095 || serviceName.contains("Mi", ignoreCase = true)) {
                    RemoteProtocol.MI_TV_HTTP
                } else if (port == 8060 || serviceName.contains("Roku", ignoreCase = true)) {
                    RemoteProtocol.ROKU_ECP
                } else {
                    RemoteProtocol.UPNP_MEDIA
                }

                val device = DiscoveredDevice(
                    id = "mdns_${host}_$port",
                    name = serviceName.ifBlank { "Android TV ($host)" },
                    manufacturer = if (protocol == RemoteProtocol.MI_TV_HTTP) "Xiaomi" else "Android TV",
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
     * Subnet probe: Gets phone's current local subnet IP (e.g. 192.168.1.X)
     * and probes the specific TV ports (6095 for Mi TV, 8060 for Roku)
     * on local network IPs.
     */
    private suspend fun probeLocalSubnet() = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress() ?: return@withContext
        val subnet = localIp.substringBeforeLast(".") + "."

        // Scan common host address ranges (1..20 and local neighborhood)
        val hostsToProbe = mutableListOf<String>()
        val lastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: 100
        for (i in (lastOctet - 10).coerceAtLeast(1)..(lastOctet + 10).coerceAtMost(254)) {
            if (i != lastOctet) hostsToProbe.add("$subnet$i")
        }
        for (i in 1..15) {
            val ip = "$subnet$i"
            if (!hostsToProbe.contains(ip)) hostsToProbe.add(ip)
        }

        // Test each host concurrently with 250ms socket connect
        hostsToProbe.forEach { targetIp ->
            launch {
                // Check Mi TV port 6095
                if (isPortOpen(targetIp, 6095, 250)) {
                    addDevice(
                        DiscoveredDevice(
                            id = "subnet_${targetIp}_6095",
                            name = "Xiaomi Mi TV ($targetIp)",
                            manufacturer = "Xiaomi",
                            ipAddress = targetIp,
                            port = 6095,
                            protocol = RemoteProtocol.MI_TV_HTTP
                        )
                    )
                }
                // Check Roku port 8060
                if (isPortOpen(targetIp, 8060, 250)) {
                    addDevice(
                        DiscoveredDevice(
                            id = "subnet_${targetIp}_8060",
                            name = "Roku Smart TV ($targetIp)",
                            manufacturer = "Roku / TCL",
                            ipAddress = targetIp,
                            port = 8060,
                            protocol = RemoteProtocol.ROKU_ECP
                        )
                    )
                }
            }
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
