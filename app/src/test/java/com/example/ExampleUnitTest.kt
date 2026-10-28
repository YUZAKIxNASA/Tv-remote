package com.example

import com.example.model.CommandResult
import com.example.model.DiscoveredDevice
import com.example.model.PairingResult
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import com.example.network.ProtocolDispatcher
import com.example.network.adapter.AndroidTvRemoteAdapter
import com.example.network.adapter.MiTvHttpAdapter
import com.example.network.adapter.RokuEcpAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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

    // 1. Android TV Remote and Google Cast protocol separation
    @Test
    fun testAndroidTvRemoteAndGoogleCastProtocolSeparation() = runTest {
        val dispatcher = ProtocolDispatcher()

        val androidTvDevice = DiscoveredDevice(
            id = "192.168.1.50_6466",
            name = "Mi Box 4K",
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

        // Android TV has an AndroidTvRemoteAdapter
        val atvAdapter = dispatcher.getAdapter(androidTvDevice.protocol)
        assertNotNull(atvAdapter)
        assertTrue(atvAdapter is AndroidTvRemoteAdapter)

        // Google Cast has no remote navigation adapter
        assertNull(dispatcher.getAdapter(googleCastDevice.protocol))

        // Android TV Remote should yield PairingRequired when un-paired
        val atvVerify = dispatcher.verifyDevice(androidTvDevice)
        assertTrue(atvVerify is ProtocolVerificationResult.PairingRequired)

        // Google Cast should yield ProtocolUnverified
        val castVerify = dispatcher.verifyDevice(googleCastDevice)
        assertTrue(castVerify is ProtocolVerificationResult.ProtocolUnverified)
    }

    // 2. Android TV Remote PIN Pairing Handshake Flow
    @Test
    fun testAndroidTvPairingFlow() = runTest {
        val adapter = AndroidTvRemoteAdapter()
        val server = ServerSocket(0)
        val testPort = server.localPort
        val testIp = "127.0.0.1"

        try {
            // Initially unpaired
            assertFalse(adapter.isPaired(testIp))
            val verifyBefore = adapter.verifyConnection(testIp, testPort)
            assertTrue(verifyBefore is ProtocolVerificationResult.PairingRequired)

            // Commands must be blocked when unpaired
            val sendBefore = adapter.sendCommand(testIp, testPort, RemoteCommand.UP)
            assertTrue(sendBefore is CommandResult.Error)
            assertTrue((sendBefore as CommandResult.Error).message.contains("not paired", ignoreCase = true))

            // Start pairing request
            val startResult = adapter.startPairing(testIp, testPort)
            assertTrue(startResult is PairingResult.Success)

            // Invalid PIN (too short or blank)
            val shortPinResult = adapter.submitPairingPin(testIp, testPort, "12")
            assertTrue(shortPinResult is PairingResult.InvalidPin)

            // Valid PIN submission
            val pinResult = adapter.submitPairingPin(testIp, testPort, "4892")
            assertTrue(pinResult is PairingResult.Success)

            // Now device is paired!
            assertTrue(adapter.isPaired(testIp))

            // Commands are dispatched after pairing
            val sendAfter = adapter.sendCommand(testIp, testPort, RemoteCommand.UP)
            assertTrue(sendAfter is CommandResult.Success)

            // Reset paired status
            adapter.unpair(testIp)
            assertFalse(adapter.isPaired(testIp))
        } finally {
            server.close()
        }
    }

    // 3. Port 8009 is not classified as Xiaomi HTTP
    @Test
    fun testPort8009NotClassifiedAsXiaomiHttp() = runTest {
        val adapter = MiTvHttpAdapter()
        val verifyResult = adapter.verifyConnection("127.0.0.1", 8009)

        assertTrue(verifyResult is ProtocolVerificationResult.ProtocolUnverified)

        val sendResult = adapter.sendCommand("127.0.0.1", 8009, RemoteCommand.POWER)
        assertTrue(sendResult is CommandResult.Unsupported)
    }

    // 4. Unknown service remains unverified
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

    // 5. Open TCP port does not establish verified remote connection
    @Test
    fun testOpenTcpPortDoesNotEstablishVerifiedRemoteConnection() = runTest {
        val server = ServerSocket(0)
        val port = server.localPort

        try {
            val adapter = MiTvHttpAdapter()
            val pingResult = adapter.ping("127.0.0.1", port)
            assertFalse(pingResult)

            val verifyResult = adapter.verifyConnection("127.0.0.1", port)
            assertFalse(verifyResult is ProtocolVerificationResult.Verified)
        } finally {
            server.close()
        }
    }

    // 6. Connection timeouts and refused connections produce appropriate failures
    @Test
    fun testConnectionRefusedProducesFailure() = runTest {
        val adapter = MiTvHttpAdapter()
        val unusedPort = 59999
        val verifyResult = adapter.verifyConnection("127.0.0.1", unusedPort)

        assertTrue(verifyResult is ProtocolVerificationResult.Failed)
        val failed = verifyResult as ProtocolVerificationResult.Failed
        assertTrue(failed.reason.contains("refused", ignoreCase = true) || failed.reason.contains("failed", ignoreCase = true))
    }

    // 7. Supported commands for each protocol
    @Test
    fun testAndroidTvSupportedCommands() {
        val adapter = AndroidTvRemoteAdapter()
        val supported = adapter.getSupportedCommands()

        assertTrue(supported.contains(RemoteCommand.POWER))
        assertTrue(supported.contains(RemoteCommand.UP))
        assertTrue(supported.contains(RemoteCommand.DOWN))
        assertTrue(supported.contains(RemoteCommand.LEFT))
        assertTrue(supported.contains(RemoteCommand.RIGHT))
        assertTrue(supported.contains(RemoteCommand.OK))
        assertTrue(supported.contains(RemoteCommand.HOME))
        assertTrue(supported.contains(RemoteCommand.BACK))
        assertTrue(supported.contains(RemoteCommand.MENU))
        assertTrue(supported.contains(RemoteCommand.VOLUME_UP))
        assertTrue(supported.contains(RemoteCommand.VOLUME_DOWN))
        assertTrue(supported.contains(RemoteCommand.MUTE))
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
}
