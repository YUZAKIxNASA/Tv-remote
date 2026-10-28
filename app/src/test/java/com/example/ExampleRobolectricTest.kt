package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SavedDevicesRepository
import com.example.model.DeviceConnectionState
import com.example.model.DiscoveredDevice
import com.example.model.RemoteProtocol
import com.example.network.DeviceDiscoveryManager
import com.example.ui.viewmodel.RemoteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NASA Remote", appName)
    }

    @Test
    fun testSavedDevicesRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SavedDevicesRepository(context)

        val testDevice = DiscoveredDevice(
            id = "test_dev_1",
            name = "Bedroom Mi TV",
            manufacturer = "Xiaomi",
            ipAddress = "192.168.1.55",
            port = 6095,
            protocol = RemoteProtocol.MI_TV_HTTP
        )

        repository.saveDevice(testDevice)

        val savedList = repository.savedDevices.value
        assertTrue(savedList.any { it.ipAddress == "192.168.1.55" && it.name == "Bedroom Mi TV" })

        // Remove device
        repository.removeDevice(testDevice)
        val afterRemove = repository.savedDevices.value
        assertFalse(afterRemove.any { it.ipAddress == "192.168.1.55" })
    }

    @Test
    fun testDuplicateDiscoveryPreservesHighFidelityMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val discoveryManager = DeviceDiscoveryManager(context, CoroutineScope(Dispatchers.Unconfined))

        val initialDevice = DiscoveredDevice(
            id = "192.168.1.50_6466",
            name = "Living Room TV",
            manufacturer = "Android TV",
            ipAddress = "192.168.1.50",
            port = 6466,
            protocol = RemoteProtocol.ANDROID_TV_REMOTE
        )
        discoveryManager.addDevice(initialDevice)

        val duplicateWeakDevice = DiscoveredDevice(
            id = "192.168.1.50_6466",
            name = "Smart TV (192.168.1.50)",
            manufacturer = "Unknown Device",
            ipAddress = "192.168.1.50",
            port = 6466,
            protocol = RemoteProtocol.UNVERIFIED
        )
        discoveryManager.addDevice(duplicateWeakDevice)

        val devices = discoveryManager.discoveredDevices.value
        assertEquals(1, devices.size)
        val merged = devices.first()

        assertEquals("Living Room TV", merged.name)
        assertEquals("Android TV", merged.manufacturer)
        assertEquals(RemoteProtocol.ANDROID_TV_REMOTE, merged.protocol)
    }

    @Test
    fun testManualDeviceAdditionAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val discoveryManager = DeviceDiscoveryManager(context, CoroutineScope(Dispatchers.Unconfined))
        val repository = SavedDevicesRepository(context)

        val manual = discoveryManager.addManualDevice(
            ip = "192.168.1.120",
            port = 6095,
            name = "Studio Mi TV",
            protocol = RemoteProtocol.MI_TV_HTTP
        )

        assertEquals("Studio Mi TV", manual.name)
        assertEquals("Xiaomi", manual.manufacturer)
        assertEquals(RemoteProtocol.MI_TV_HTTP, manual.protocol)
        assertTrue(manual.isSaved)

        repository.saveDevice(manual)
        val loaded = repository.savedDevices.value
        assertTrue(loaded.any { it.ipAddress == "192.168.1.120" && it.name == "Studio Mi TV" })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testViewModelAndroidTvPairingCycle() = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = RemoteViewModel(application)

        val atvDevice = DiscoveredDevice(
            id = "192.168.1.200_6467",
            name = "Sony Bravia Android TV",
            manufacturer = "Sony (Android TV)",
            ipAddress = "192.168.1.200",
            port = 6467,
            protocol = RemoteProtocol.ANDROID_TV_REMOTE
        )

        // Select Android TV device
        viewModel.selectDevice(atvDevice)

        var attempts = 0
        while (viewModel.connectionState.value is DeviceConnectionState.Connecting && attempts < 50) {
            advanceUntilIdle()
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
            attempts++
        }

        // Should initially enter PairingRequired
        assertTrue(viewModel.connectionState.value is DeviceConnectionState.PairingRequired)

        // Trigger pairing dialog
        viewModel.startPairing(atvDevice)
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        assertTrue(viewModel.isPairingDialogVisible.value)

        // Submit invalid PIN
        viewModel.submitPairingPin("12")
        advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        assertTrue(viewModel.isPairingDialogVisible.value)
        assertTrue(viewModel.pairingErrorMessage.value != null)

        // Submit valid PIN
        viewModel.submitPairingPin("5821")
        attempts = 0
        while (viewModel.isPairingDialogVisible.value && attempts < 60) {
            advanceUntilIdle()
            ShadowLooper.idleMainLooper()
            Thread.sleep(30)
            attempts++
        }

        // Dialog should dismiss and state should become Connected
        assertFalse(viewModel.isPairingDialogVisible.value)
        assertTrue(viewModel.connectionState.value is DeviceConnectionState.Connected)
        val connectedState = viewModel.connectionState.value as DeviceConnectionState.Connected
        assertEquals("192.168.1.200", connectedState.device.ipAddress)
    }
}
