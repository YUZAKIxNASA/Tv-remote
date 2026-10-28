package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SavedDevicesRepository
import com.example.model.CommandResult
import com.example.model.DeviceConnectionState
import com.example.model.DiscoveredDevice
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.DeviceDiscoveryManager
import com.example.network.ProtocolDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    DISCOVERY,
    CONNECTION,
    REMOTE,
    MORE_OPTIONS
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val discoveryManager = DeviceDiscoveryManager(application, viewModelScope)
    private val savedRepository = SavedDevicesRepository(application)
    private val protocolDispatcher = ProtocolDispatcher()

    private val _currentScreen = MutableStateFlow(AppScreen.DISCOVERY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val activeDevice: StateFlow<DiscoveredDevice?> = _activeDevice.asStateFlow()

    private val _connectionState = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.NotConnected)
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    val isScanning: StateFlow<Boolean> = discoveryManager.isScanning
    val scanStatusMessage: StateFlow<String?> = discoveryManager.scanStatusMessage
    val savedDevices: StateFlow<List<DiscoveredDevice>> = savedRepository.savedDevices

    // Merge discovered devices and saved devices
    val allDiscoveredDevices: StateFlow<List<DiscoveredDevice>> = combine(
        discoveryManager.discoveredDevices,
        savedRepository.savedDevices
    ) { discovered, saved ->
        val savedMap = saved.associateBy { "${it.ipAddress}:${it.port}" }
        val merged = discovered.map { dev ->
            val key = "${dev.ipAddress}:${dev.port}"
            if (savedMap.containsKey(key)) dev.copy(isSaved = true) else dev
        }.toMutableList()

        // Also add any saved devices not in discovered list
        saved.forEach { sDev ->
            val key = "${sDev.ipAddress}:${sDev.port}"
            if (merged.none { "${it.ipAddress}:${it.port}" == key }) {
                merged.add(sDev)
            }
        }
        merged
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial auto-scan
        startScan()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        return when (_currentScreen.value) {
            AppScreen.MORE_OPTIONS -> {
                _currentScreen.value = AppScreen.REMOTE
                true
            }
            AppScreen.REMOTE -> {
                _currentScreen.value = AppScreen.CONNECTION
                true
            }
            AppScreen.CONNECTION -> {
                _currentScreen.value = AppScreen.DISCOVERY
                true
            }
            AppScreen.DISCOVERY -> false
        }
    }

    fun startScan() {
        discoveryManager.startDiscovery()
    }

    fun stopScan() {
        discoveryManager.stopDiscovery()
    }

    fun selectDevice(device: DiscoveredDevice) {
        _activeDevice.value = device
        _currentScreen.value = AppScreen.CONNECTION
        connectToDevice(device)
    }

    fun connectToDevice(device: DiscoveredDevice) {
        _activeDevice.value = device
        _connectionState.value = DeviceConnectionState.Connecting(device)

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val reachable = protocolDispatcher.ping(device)
            val latency = System.currentTimeMillis() - startTime

            if (reachable) {
                _connectionState.value = DeviceConnectionState.Connected(device, latency)
                // Automatically save verified device
                savedRepository.saveDevice(device)
            } else {
                _connectionState.value = DeviceConnectionState.ConnectionFailed(
                    device,
                    "Device at ${device.addressLabel} did not respond. Verify TV is powered ON and connected to the same WiFi network."
                )
            }
        }
    }

    fun disconnect() {
        _connectionState.value = DeviceConnectionState.Disconnected
    }

    fun sendCommand(command: RemoteCommand) {
        val device = _activeDevice.value ?: run {
            _lastCommandResult.value = CommandResult.Error("No TV connected")
            return
        }

        viewModelScope.launch {
            val result = protocolDispatcher.sendCommand(device, command)
            _lastCommandResult.value = result
            // If connection failed due to host unreachable, update connection state
            if (result is CommandResult.Error && result.message.contains("Cannot reach", ignoreCase = true)) {
                _connectionState.value = DeviceConnectionState.ConnectionFailed(device, result.message)
            } else if (result is CommandResult.Success && _connectionState.value !is DeviceConnectionState.Connected) {
                _connectionState.value = DeviceConnectionState.Connected(device, result.latencyMs)
            }
        }
    }

    fun sendText(text: String) {
        val device = _activeDevice.value ?: return
        viewModelScope.launch {
            val result = protocolDispatcher.sendText(device, text)
            _lastCommandResult.value = result
        }
    }

    fun launchApp(appPackage: String) {
        val device = _activeDevice.value ?: return
        viewModelScope.launch {
            val result = protocolDispatcher.launchApp(device, appPackage)
            _lastCommandResult.value = result
        }
    }

    fun addManualDevice(ip: String, port: Int, name: String, protocol: RemoteProtocol) {
        val device = discoveryManager.addManualDevice(ip, port, name, protocol)
        savedRepository.saveDevice(device)
        selectDevice(device)
    }

    fun removeSavedDevice(device: DiscoveredDevice) {
        savedRepository.removeDevice(device)
        if (_activeDevice.value?.id == device.id) {
            _activeDevice.value = null
            _connectionState.value = DeviceConnectionState.NotConnected
            _currentScreen.value = AppScreen.DISCOVERY
        }
    }

    fun getSupportedCommands(): Set<RemoteCommand> {
        val device = _activeDevice.value ?: return emptySet()
        return protocolDispatcher.getSupportedCommands(device.protocol)
    }
}
