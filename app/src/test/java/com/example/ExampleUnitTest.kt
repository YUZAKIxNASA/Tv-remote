package com.example

import com.example.model.CommandResult
import com.example.model.DiscoveredDevice
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.ProtocolDispatcher
import com.example.network.adapter.MiTvHttpAdapter
import com.example.network.adapter.RokuEcpAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket

class ExampleUnitTest {

    @Test
    fun testProtocolDefaults() {
        assertEquals(6095, RemoteProtocol.MI_TV_HTTP.defaultPort)
        assertEquals(8060, RemoteProtocol.ROKU_ECP.defaultPort)
        assertEquals(6466, RemoteProtocol.ANDROID_TV_REMOTE.defaultPort)
        assertEquals(8009, RemoteProtocol.GOOGLE_CAST.defaultPort)
        assertEquals(8080, RemoteProtocol.UPNP_MEDIA.defaultPort)
        assertEquals(0, RemoteProtocol.UNVERIFIED.defaultPort)
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

    // 1. Requirement 1 & 2: _androidtvremote2 and _googlecast are never classified as Xiaomi HTTP
    @Test
    fun testAndroidTvRemoteAndGoogleCastProtocolSeparation() = runTest {
        val dispatcher = ProtocolDispatcher()

        val androidTvDevice = DiscoveredDevice(
            id = "192.168.1.50_6466",
            name = "Mi Box 4K", // contains 'Mi' in service name
            manufacturer = "Xiaomi (Android TV)",
            ipAddress = "192.168.1.50",
            port = 6466,
            protocol = RemoteProtocol.ANDROID_TV_REMOTE
        )

        val googleCastDevice = DiscoveredDevice(
            id = "192.168.1.50_8009",
            name = "Mi TV Cast",
            manufacturer = "Google Cast / Android TV",
            ipAddress = "192.168.1.50",
            port = 8009,
            protocol = RemoteProtocol.GOOGLE_CAST
        )

        // Neither should resolve to MiTvHttpAdapter
        assertNull(dispatcher.getAdapter(androidTvDevice.protocol))
        assertNull(dispatcher.getAdapter(googleCastDevice.protocol))

        // Android TV Remote should yield PairingRequired
        val atvVerify = dispatcher.verifyDevice(androidTvDevice)
        assertTrue(atvVerify is ProtocolVerificationResult.PairingRequired)

        // Google Cast should yield ProtocolUnverified
        val castVerify = dispatcher.verifyDevice(googleCastDevice)
        assertTrue(castVerify is ProtocolVerificationResult.ProtocolUnverified)
    }

    // 2. Requirement 3: A generic service with port 8009 is not automatically classified as Xiaomi HTTP
    @Test
    fun testPort8009NotClassifiedAsXiaomiHttp() = runTest {
        val adapter = MiTvHttpAdapter()
        val verifyResult = adapter.verifyConnection("127.0.0.1", 8009)

        // Must reject port 8009 as incompatible rather than sending Xiaomi PatchWall HTTP commands
        assertTrue(verifyResult is ProtocolVerificationResult.ProtocolUnverified)

        val sendResult = adapter.sendCommand("127.0.0.1", 8009, RemoteCommand.POWER)
        assertTrue(sendResult is CommandResult.Unsupported)
    }

    // 3. Requirement 4: An unknown service remains unverified instead of being assigned an unrelated protocol
    @Test
    fun testUnknownServiceRemainsUnverified() = runTest {
        val dispatcher = ProtocolDispatcher()

        val unverifiedDevice = DiscoveredDevice(
            id = "192.168.1.80_1234",
            name = "Unknown Smart Device",
            manufacturer = "Unknown Device",
            ipAddress = "192.168.1.80",
            port = 1234,
            protocol = RemoteProtocol.UNVERIFIED
        )

        assertNull(dispatcher.getAdapter(unverifiedDevice.protocol))

        val verifyResult = dispatcher.verifyDevice(unverifiedDevice)
        assertTrue(verifyResult is ProtocolVerificationResult.ProtocolUnverified)

        val sendResult = dispatcher.sendCommand(unverifiedDevice, RemoteCommand.POWER)
        assertTrue(sendResult is CommandResult.Unsupported)
    }

    // 4. Requirement 5: A generic open TCP port does not establish a verified remote connection
    @Test
    fun testOpenTcpPortDoesNotEstablishVerifiedRemoteConnection() = runTest {
        // Open a raw dummy TCP socket that accepts connections but does not speak Mi TV HTTP
        val server = ServerSocket(0)
        val port = server.localPort

        try {
            val adapter = MiTvHttpAdapter()
            // Even though the port is open and accepts a TCP connection, ping() must return false
            // because it is not a genuine Xiaomi HTTP remote controller!
            val pingResult = adapter.ping("127.0.0.1", port)
            assertFalse(pingResult)

            val verifyResult = adapter.verifyConnection("127.0.0.1", port)
            assertFalse(verifyResult is ProtocolVerificationResult.Verified)
        } finally {
            server.close()
        }
    }

    // 5. Requirement 6: Connection timeouts and refused connections produce appropriate failures
    @Test
    fun testConnectionRefusedProducesFailure() = runTest {
        val adapter = MiTvHttpAdapter()
        // Pick an unused local port
        val unusedPort = 59999
        val verifyResult = adapter.verifyConnection("127.0.0.1", unusedPort)

        assertTrue(verifyResult is ProtocolVerificationResult.Failed)
        val failed = verifyResult as ProtocolVerificationResult.Failed
        assertTrue(failed.reason.contains("refused", ignoreCase = true) || failed.reason.contains("failed", ignoreCase = true))
    }

    // 6. Requirement 7: Unsupported commands do not return false success
    @Test
    fun testUnsupportedCommandHandling() = runTest {
        val adapter = MiTvHttpAdapter()
        val result = adapter.sendCommand("127.0.0.1", 6095, RemoteCommand.CHANNEL_UP)
        assertTrue(result is CommandResult.Unsupported)
    }

    // 7. Supported commands check
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
        assertEquals(RemoteProtocol.MI_TV_HTTP, adapter?.protocol)
    }
}
