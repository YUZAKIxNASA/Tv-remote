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
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Protocol adapter for Roku / TCL Smart TVs running Roku OS (Port 8060).
 */
class RokuEcpAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .build()
) : TvProtocolAdapter {

    override val protocol: RemoteProtocol = RemoteProtocol.ROKU_ECP

    private val supportedCommandsSet = setOf(
        RemoteCommand.POWER,
        RemoteCommand.UP,
        RemoteCommand.DOWN,
        RemoteCommand.LEFT,
        RemoteCommand.RIGHT,
        RemoteCommand.OK,
        RemoteCommand.HOME,
        RemoteCommand.BACK,
        RemoteCommand.VOLUME_UP,
        RemoteCommand.VOLUME_DOWN,
        RemoteCommand.MUTE,
        RemoteCommand.PLAY_PAUSE,
        RemoteCommand.PREVIOUS,
        RemoteCommand.NEXT,
        RemoteCommand.KEYBOARD,
        RemoteCommand.APP_YOUTUBE,
        RemoteCommand.APP_NETFLIX
    )

    override fun getSupportedCommands(): Set<RemoteCommand> = supportedCommandsSet

    override suspend fun verifyConnection(ip: String, port: Int): ProtocolVerificationResult =
        withContext(Dispatchers.IO) {
            // Check basic socket reachability
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 1500)
                }
            } catch (e: ConnectException) {
                return@withContext ProtocolVerificationResult.Failed("Connection refused at $ip:$port")
            } catch (e: SocketTimeoutException) {
                return@withContext ProtocolVerificationResult.Failed("Connection timed out at $ip:$port")
            } catch (e: Exception) {
                return@withContext ProtocolVerificationResult.Failed("Network error at $ip:$port: ${e.localizedMessage}")
            }

            // Real Roku ECP verification: query/device-info must return Roku XML
            val startTime = System.currentTimeMillis()
            try {
                val request = Request.Builder()
                    .url("http://$ip:$port/query/device-info")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful && body.contains("<device-info", ignoreCase = true)) {
                        ProtocolVerificationResult.Verified(latency)
                    } else if (response.isSuccessful) {
                        ProtocolVerificationResult.ProtocolUnverified(
                            "Device at $ip:$port responded to HTTP but does not return Roku device-info XML."
                        )
                    } else {
                        ProtocolVerificationResult.ProtocolUnverified(
                            "Device at $ip:$port returned HTTP ${response.code} on Roku query/device-info."
                        )
                    }
                }
            } catch (e: Exception) {
                ProtocolVerificationResult.Failed("Roku ECP verification failed: ${e.localizedMessage}")
            }
        }

    override suspend fun ping(ip: String, port: Int): Boolean {
        return verifyConnection(ip, port) is ProtocolVerificationResult.Verified
    }

    override suspend fun sendCommand(ip: String, port: Int, command: RemoteCommand): CommandResult =
        withContext(Dispatchers.IO) {
            val key = when (command) {
                RemoteCommand.POWER -> "Power"
                RemoteCommand.UP -> "Up"
                RemoteCommand.DOWN -> "Down"
                RemoteCommand.LEFT -> "Left"
                RemoteCommand.RIGHT -> "Right"
                RemoteCommand.OK -> "Select"
                RemoteCommand.HOME -> "Home"
                RemoteCommand.BACK -> "Back"
                RemoteCommand.VOLUME_UP -> "VolumeUp"
                RemoteCommand.VOLUME_DOWN -> "VolumeDown"
                RemoteCommand.MUTE -> "VolumeMute"
                RemoteCommand.PLAY_PAUSE -> "Play"
                RemoteCommand.PREVIOUS -> "Rev"
                RemoteCommand.NEXT -> "Fwd"
                RemoteCommand.APP_YOUTUBE -> return@withContext launchApp(ip, port, "837")
                RemoteCommand.APP_NETFLIX -> return@withContext launchApp(ip, port, "12")
                else -> return@withContext CommandResult.Unsupported("Command '${command.label}' is not supported on Roku ECP.")
            }

            val startTime = System.currentTimeMillis()
            val url = "http://$ip:$port/keypress/$key"
            val emptyBody = "".toRequestBody("text/plain".toMediaType())

            try {
                val request = Request.Builder().url(url).post(emptyBody).build()
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Sent '$key' to Roku TV", latency)
                    } else {
                        CommandResult.Error("Roku returned HTTP ${response.code}")
                    }
                }
            } catch (e: IOException) {
                CommandResult.Error("Cannot reach Roku TV at $ip:$port (${e.localizedMessage})")
            } catch (e: Exception) {
                CommandResult.Error("Roku command failed: ${e.localizedMessage}")
            }
        }

    override suspend fun sendText(ip: String, port: Int, text: String): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val emptyBody = "".toRequestBody("text/plain".toMediaType())
                for (char in text) {
                    val encoded = when (char) {
                        ' ' -> "%20"
                        else -> char.toString()
                    }
                    val url = "http://$ip:$port/keypress/Lit_$encoded"
                    val request = Request.Builder().url(url).post(emptyBody).build()
                    client.newCall(request).execute().close()
                }
                val latency = System.currentTimeMillis() - startTime
                CommandResult.Success("Sent text to Roku: \"$text\"", latency)
            } catch (e: Exception) {
                CommandResult.Error("Failed to send text to Roku: ${e.localizedMessage}")
            }
        }

    override suspend fun launchApp(ip: String, port: Int, appPackage: String): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val emptyBody = "".toRequestBody("text/plain".toMediaType())
                val url = "http://$ip:$port/launch/$appPackage"
                val request = Request.Builder().url(url).post(emptyBody).build()
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Launched channel $appPackage on Roku", latency)
                    } else {
                        CommandResult.Error("Failed to launch Roku channel $appPackage")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("Roku app launch error: ${e.localizedMessage}")
            }
        }
}
