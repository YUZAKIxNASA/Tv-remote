package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SavedDevicesRepository
import com.example.model.CommandResult
import com.example.model.DeviceConnectionState
import com.example.model.DiscoveredDevice
import com.example.model.PairingResult
import com.example.model.ProtocolVerificationResult
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
    private val protocolDispatcher = ProtocolDispatcher(application)

    private val _currentScreen = MutableStateFlow(AppScreen.DISCOVERY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val activeDevice: StateFlow<DiscoveredDevice?> = _activeDevice.asStateFlow()

    private val _connectionState = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.NotConnected)
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    // Pairing dialog & process state
    private val _isPairingDialogVisible = MutableStateFlow(false)
    val isPairingDialogVisible: StateFlow<Boolean> = _isPairingDialogVisible.asStateFlow()

    private val _pairingInProgress = MutableStateFlow(false)
    val pairingInProgress: StateFlow<Boolean> = _pairingInProgress.asStateFlow()

    private val _pairingErrorMessage = MutableStateFlow<String?>(null)
    val pairingErrorMessage: StateFlow<String?> = _pairingErrorMessage.asStateFlow()

    private val _pairingTargetDevice = MutableStateFlow<DiscoveredDevice?>(null)
    val pairingTargetDevice: StateFlow<DiscoveredDevice?> = _pairingTargetDevice.asStateFlow()

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
            when (val result = protocolDispatcher.verifyDevice(device)) {
                is ProtocolVerificationResult.Verified -> {
                    _connectionState.value = DeviceConnectionState.Connected(device, result.latencyMs)
                    savedRepository.saveDevice(device)
                }
                is ProtocolVerificationResult.ProtocolUnverified -> {
                    _connectionState.value = DeviceConnectionState.ProtocolUnverified(device, result.reason)
                }
                is ProtocolVerificationResult.PairingRequired -> {
                    _connectionState.value = DeviceConnectionState.PairingRequired(device, result.instructions)
                }
                is ProtocolVerificationResult.Unsupported -> {
                    _connectionState.value = DeviceConnectionState.UnsupportedProtocol(device, result.reason)
                }
                is ProtocolVerificationResult.Failed -> {
                    _connectionState.value = DeviceConnectionState.ConnectionFailed(device, result.reason)
                }
            }
        }
    }

    fun startPairing(device: DiscoveredDevice? = null) {
        val target = device ?: _activeDevice.value ?: return
        _pairingTargetDevice.value = target
        _pairingErrorMessage.value = null
        _isPairingDialogVisible.value = true

        viewModelScope.launch {
            _pairingInProgress.value = true
            _connectionState.value = DeviceConnectionState.PairingInProgress(target, "Requesting pairing code from TV...")
            val result = protocolDispatcher.startPairing(target)
            _pairingInProgress.value = false
            when (result) {
                is PairingResult.Success -> {
                    // Waiting for user to enter PIN
                }
                is PairingResult.Failed -> {
                    _pairingErrorMessage.value = result.reason
                    _connectionState.value = DeviceConnectionState.PairingRequired(target, result.reason)
                }
                is PairingResult.InvalidPin -> {
                    _pairingErrorMessage.value = result.reason
                }
            }
        }
    }

    fun submitPairingPin(pin: String) {
        val target = _pairingTargetDevice.value ?: _activeDevice.value ?: return
        if (pin.isBlank() || pin.length < 4) {
            _pairingErrorMessage.value = "Please enter the full 4 to 6 character code displayed on your TV."
            return
        }

        viewModelScope.launch {
            _pairingInProgress.value = true
            _pairingErrorMessage.value = null
            _connectionState.value = DeviceConnectionState.PairingInProgress(target, "Verifying PIN with TV...")
            when (val result = protocolDispatcher.submitPairingPin(target, pin.trim())) {
                is PairingResult.Success -> {
                    _pairingInProgress.value = false
                    _isPairingDialogVisible.value = false
                    _connectionState.value = DeviceConnectionState.Connected(target, result.latencyMs)
                    savedRepository.saveDevice(target)
                }
                is PairingResult.InvalidPin -> {
                    _pairingInProgress.value = false
                    _pairingErrorMessage.value = result.reason
                    _connectionState.value = DeviceConnectionState.PairingRequired(target, result.reason)
                }
                is PairingResult.Failed -> {
                    _pairingInProgress.value = false
                    _pairingErrorMessage.value = result.reason
                    _connectionState.value = DeviceConnectionState.ConnectionFailed(target, result.reason)
                }
            }
        }
    }

    fun dismissPairingDialog() {
        _isPairingDialogVisible.value = false
        _pairingErrorMessage.value = null
        _pairingInProgress.value = false
    }

    fun disconnect() {
        _connectionState.value = DeviceConnectionState.Disconnected
    }

    fun sendCommand(command: RemoteCommand) {
        val device = _activeDevice.value ?: run {
            _lastCommandResult.value = CommandResult.Error("No TV connected")
            return
        }

        if (_connectionState.value !is DeviceConnectionState.Connected) {
            _lastCommandResult.value = CommandResult.Error("Cannot send command: device protocol is not connected or unverified.")
            return
        }

        viewModelScope.launch {
            val result = protocolDispatcher.sendCommand(device, command)
            _lastCommandResult.value = result
            if (result is CommandResult.Error && result.message.contains("Cannot reach", ignoreCase = true)) {
                _connectionState.value = DeviceConnectionState.ConnectionFailed(device, result.message)
            }
        }
    }

    fun sendText(text: String) {
        val device = _activeDevice.value ?: return
        if (_connectionState.value !is DeviceConnectionState.Connected) {
            _lastCommandResult.value = CommandResult.Error("Cannot send text: device protocol is unverified.")
            return
        }
        viewModelScope.launch {
            val result = protocolDispatcher.sendText(device, text)
            _lastCommandResult.value = result
        }
    }

    fun launchApp(appPackage: String) {
        val device = _activeDevice.value ?: return
        if (_connectionState.value !is DeviceConnectionState.Connected) {
            _lastCommandResult.value = CommandResult.Error("Cannot launch app: device protocol is unverified.")
            return
        }
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

    fun getProtocolCommands(protocol: RemoteProtocol): Set<RemoteCommand> {
        return protocolDispatcher.getSupportedCommands(protocol)
    }
}
