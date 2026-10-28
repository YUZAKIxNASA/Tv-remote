package com.example.network.adapter

import com.example.model.CommandResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
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

    override suspend fun ping(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 1500)
            }
            true
        } catch (_: Exception) {
            try {
                val request = Request.Builder()
                    .url("http://$ip:$port/query/device-info")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (_: Exception) {
                false
            }
        }
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
                RemoteCommand.APP_YOUTUBE -> return@withContext launchApp(ip, port, "837") // Roku YouTube App ID
                RemoteCommand.APP_NETFLIX -> return@withContext launchApp(ip, port, "12")  // Roku Netflix App ID
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
