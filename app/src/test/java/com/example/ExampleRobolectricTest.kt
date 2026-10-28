package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SavedDevicesRepository
import com.example.model.DiscoveredDevice
import com.example.model.RemoteProtocol
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
}
