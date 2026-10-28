package com.example.network.adapter

import com.example.model.CommandResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Verified protocol adapter for Xiaomi Smart TV / PatchWall / Mi Box HTTP API (Port 6095).
 */
class MiTvHttpAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .build()
) : TvProtocolAdapter {

    override val protocol: RemoteProtocol = RemoteProtocol.MI_TV_HTTP

    private val supportedCommandsSet = setOf(
        RemoteCommand.POWER,
        RemoteCommand.UP,
        RemoteCommand.DOWN,
        RemoteCommand.LEFT,
        RemoteCommand.RIGHT,
        RemoteCommand.OK,
        RemoteCommand.HOME,
        RemoteCommand.BACK,
        RemoteCommand.MENU,
        RemoteCommand.VOLUME_UP,
        RemoteCommand.VOLUME_DOWN,
        RemoteCommand.MUTE,
        RemoteCommand.KEYBOARD,
        RemoteCommand.APP_YOUTUBE,
        RemoteCommand.APP_NETFLIX,
        RemoteCommand.APP_PRIME
    )

    override fun getSupportedCommands(): Set<RemoteCommand> = supportedCommandsSet

    override suspend fun ping(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check TCP socket reachability on target port
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 1500)
            }
            true
        } catch (_: Exception) {
            // Try HTTP isalive fallback
            try {
                val request = Request.Builder()
                    .url("http://$ip:$port/request?action=isalive")
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
            val keycode = when (command) {
                RemoteCommand.POWER -> "power"
                RemoteCommand.UP -> "up"
                RemoteCommand.DOWN -> "down"
                RemoteCommand.LEFT -> "left"
                RemoteCommand.RIGHT -> "right"
                RemoteCommand.OK -> "enter"
                RemoteCommand.HOME -> "home"
                RemoteCommand.BACK -> "back"
                RemoteCommand.MENU -> "menu"
                RemoteCommand.VOLUME_UP -> "volumeup"
                RemoteCommand.VOLUME_DOWN -> "volumedown"
                RemoteCommand.MUTE -> "mute"
                RemoteCommand.APP_YOUTUBE -> return@withContext launchApp(ip, port, "com.google.android.youtube.tv")
                RemoteCommand.APP_NETFLIX -> return@withContext launchApp(ip, port, "com.netflix.ninja")
                RemoteCommand.APP_PRIME -> return@withContext launchApp(ip, port, "com.amazon.amazonvideo.livingroom")
                else -> return@withContext CommandResult.Unsupported("Command '${command.label}' is not supported by Mi TV HTTP protocol.")
            }

            val startTime = System.currentTimeMillis()
            val url = "http://$ip:$port/controller?action=keyevent&keycode=$keycode"

            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Sent '${command.label}' to Mi TV", latency)
                    } else {
                        CommandResult.Error("Mi TV returned HTTP ${response.code} for key '$keycode'")
                    }
                }
            } catch (e: IOException) {
                CommandResult.Error("Cannot reach Mi TV at $ip:$port (${e.localizedMessage ?: "Connection failed"})")
            } catch (e: Exception) {
                CommandResult.Error("Command error: ${e.localizedMessage ?: "Unknown failure"}")
            }
        }

    override suspend fun sendText(ip: String, port: Int, text: String): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val url = "http://$ip:$port/controller?action=edittext&text=$encodedText"
                val request = Request.Builder().url(url).get().build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Text sent to Mi TV: \"$text\"", latency)
                    } else {
                        CommandResult.Error("Mi TV rejected text input (HTTP ${response.code})")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("Text transmission failed: ${e.localizedMessage}")
            }
        }

    override suspend fun launchApp(ip: String, port: Int, appPackage: String): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val url = "http://$ip:$port/controller?action=startapp&package=$appPackage"
                val request = Request.Builder().url(url).get().build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Launched $appPackage", latency)
                    } else {
                        CommandResult.Error("Failed to launch app $appPackage (HTTP ${response.code})")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("App launch error: ${e.localizedMessage}")
            }
        }
}
