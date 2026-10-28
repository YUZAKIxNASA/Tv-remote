package com.example

import com.example.model.CommandResult
import com.example.model.DiscoveredDevice
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.ProtocolDispatcher
import com.example.network.adapter.MiTvHttpAdapter
import com.example.network.adapter.RokuEcpAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testProtocolDefaults() {
        assertEquals(6095, RemoteProtocol.MI_TV_HTTP.defaultPort)
        assertEquals(8060, RemoteProtocol.ROKU_ECP.defaultPort)
        assertEquals(8080, RemoteProtocol.UPNP_MEDIA.defaultPort)
    }

    @Test
    fun testDiscoveredDeviceModel() {
        val device = DiscoveredDevice(
            id = "test_1",
            name = "Living Room Mi TV",
            manufacturer = "Xiaomi",
            ipAddress = "192.168.1.100",
            port = 6095,
            protocol = RemoteProtocol.MI_TV_HTTP
        )

        assertEquals("192.168.1.100:6095", device.addressLabel)
        assertEquals("Xiaomi", device.manufacturer)
    }

    @Test
    fun testMiTvAdapterSupportedCommands() {
        val adapter = MiTvHttpAdapter()
        val supported = adapter.getSupportedCommands()

        assertTrue(supported.contains(RemoteCommand.POWER))
        assertTrue(supported.contains(RemoteCommand.UP))
        assertTrue(supported.contains(RemoteCommand.DOWN))
        assertTrue(supported.contains(RemoteCommand.LEFT))
        assertTrue(supported.contains(RemoteCommand.RIGHT))
        assertTrue(supported.contains(RemoteCommand.OK))
        assertTrue(supported.contains(RemoteCommand.HOME))
        assertTrue(supported.contains(RemoteCommand.VOLUME_UP))
        assertTrue(supported.contains(RemoteCommand.VOLUME_DOWN))
        assertTrue(supported.contains(RemoteCommand.MUTE))
    }

    @Test
    fun testRokuAdapterSupportedCommands() {
        val adapter = RokuEcpAdapter()
        val supported = adapter.getSupportedCommands()

        assertTrue(supported.contains(RemoteCommand.HOME))
        assertTrue(supported.contains(RemoteCommand.PLAY_PAUSE))
        assertTrue(supported.contains(RemoteCommand.VOLUME_UP))
    }

    @Test
    fun testProtocolDispatcherRouting() {
        val dispatcher = ProtocolDispatcher()
        val adapter = dispatcher.getAdapter(RemoteProtocol.MI_TV_HTTP)
        assertNotNull(adapter)
        assertEquals(RemoteProtocol.MI_TV_HTTP, adapter.protocol)
    }

    @Test
    fun testUnsupportedCommandHandling() = runTest {
        val adapter = MiTvHttpAdapter()
        // Channel up is not supported by Mi TV HTTP keyevent API
        val result = adapter.sendCommand("127.0.0.1", 6095, RemoteCommand.CHANNEL_UP)
        assertTrue(result is CommandResult.Unsupported)
    }
}
