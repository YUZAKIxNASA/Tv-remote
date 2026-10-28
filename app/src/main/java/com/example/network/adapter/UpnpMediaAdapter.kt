package com.example.network.adapter

import com.example.model.CommandResult
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Protocol adapter for UPnP / DLNA AVTransport and RenderingControl (Port 8080 or detected port).
 */
class UpnpMediaAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .build()
) : TvProtocolAdapter {

    override val protocol: RemoteProtocol = RemoteProtocol.UPNP_MEDIA

    private val supportedCommandsSet = setOf(
        RemoteCommand.PLAY_PAUSE,
        RemoteCommand.STOP,
        RemoteCommand.VOLUME_UP,
        RemoteCommand.VOLUME_DOWN,
        RemoteCommand.MUTE
    )

    override fun getSupportedCommands(): Set<RemoteCommand> = supportedCommandsSet

    override suspend fun verifyConnection(ip: String, port: Int): ProtocolVerificationResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 1500)
                }
                // Check if UPnP root description or control endpoint responds
                val request = Request.Builder()
                    .url("http://$ip:$port/upnp/control/avtransport1")
                    .get()
                    .build()
                try {
                    client.newCall(request).execute().close()
                } catch (_: Exception) {}
                val latency = System.currentTimeMillis() - startTime
                ProtocolVerificationResult.Verified(latency)
            } catch (e: Exception) {
                ProtocolVerificationResult.Failed("UPnP connection failed: ${e.localizedMessage}")
            }
        }

    override suspend fun ping(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        verifyConnection(ip, port) is ProtocolVerificationResult.Verified
    }

    override suspend fun sendCommand(ip: String, port: Int, command: RemoteCommand): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val action = when (command) {
                RemoteCommand.PLAY_PAUSE -> "Play"
                RemoteCommand.STOP -> "Stop"
                RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN -> "SetVolume"
                RemoteCommand.MUTE -> "SetMute"
                else -> return@withContext CommandResult.Unsupported("UPnP adapter does not support ${command.label}")
            }

            // Standard SOAP envelope for UPnP AVTransport / RenderingControl
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                   <s:Body>
                      <u:$action xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                         <InstanceID>0</InstanceID>
                         <Speed>1</Speed>
                      </u:$action>
                   </s:Body>
                </s:Envelope>
            """.trimIndent()

            try {
                val request = Request.Builder()
                    .url("http://$ip:$port/upnp/control/avtransport1")
                    .addHeader("SOAPAction", "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"")
                    .post(soapBody.toRequestBody("text/xml; charset=\"utf-8\"".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("UPnP: Executed $action", latency)
                    } else {
                        CommandResult.Error("UPnP TV returned status ${response.code}")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("UPnP command error: ${e.localizedMessage}")
            }
        }

    override suspend fun sendText(ip: String, port: Int, text: String): CommandResult =
        CommandResult.Unsupported("UPnP does not support text input")

    override suspend fun launchApp(ip: String, port: Int, appPackage: String): CommandResult =
        CommandResult.Unsupported("UPnP does not support app launching")
}
