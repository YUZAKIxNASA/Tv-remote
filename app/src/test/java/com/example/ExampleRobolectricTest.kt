package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SavedDevicesRepository
import com.example.model.DiscoveredDevice
import com.example.model.RemoteProtocol
import com.example.network.DeviceDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    // Requirement 8: Duplicate discovery results do not corrupt device identity or protocol metadata
    @Test
    fun testDuplicateDiscoveryPreservesHighFidelityMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val discoveryManager = DeviceDiscoveryManager(context, CoroutineScope(Dispatchers.Unconfined))

        // First: device discovered with specific name and protocol
        val initialDevice = DiscoveredDevice(
            id = "192.168.1.50_6466",
            name = "Living Room TV",
            manufacturer = "Android TV",
            ipAddress = "192.168.1.50",
            port = 6466,
            protocol = RemoteProtocol.ANDROID_TV_REMOTE
        )
        discoveryManager.addDevice(initialDevice)

        // Second: duplicate/subsequent scan results with generic name and UNVERIFIED protocol
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

        // Verify that high-fidelity name, manufacturer, and protocol are preserved
        assertEquals("Living Room TV", merged.name)
        assertEquals("Android TV", merged.manufacturer)
        assertEquals(RemoteProtocol.ANDROID_TV_REMOTE, merged.protocol)
    }

    // Requirement 9: Existing manual device selection and saved-device behavior are preserved
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
}
