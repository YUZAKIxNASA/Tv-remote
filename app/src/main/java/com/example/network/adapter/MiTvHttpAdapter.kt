package com.example.network.adapter

import com.example.model.CommandResult
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Protocol adapter for Xiaomi Smart TV / PatchWall / Mi Box HTTP API (Port 6095 / 6097).
 *
 * NOTE: Does NOT send Xiaomi HTTP requests to Google Cast (port 8009) or
 * Android TV Remote v2 (port 6466/6467).
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

    /**
     * Verifies that the endpoint at ip:port is a true Xiaomi HTTP controller,
     * not merely an open generic TCP port.
     */
    override suspend fun verifyConnection(ip: String, port: Int): ProtocolVerificationResult =
        withContext(Dispatchers.IO) {
            // Reject known non-Xiaomi HTTP ports (e.g. Google Cast 8009, Android TV Remote 6466/6467)
            if (isKnownIncompatiblePort(port)) {
                return@withContext ProtocolVerificationResult.ProtocolUnverified(
                    "Port $port is used for other services (Google Cast/Android TV Remote v2), not Xiaomi HTTP PatchWall."
                )
            }

            // Step 1: Check basic socket reachability with quick timeout
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 1500)
                }
            } catch (e: ConnectException) {
                return@withContext ProtocolVerificationResult.Failed(
                    "Connection refused at $ip:$port. Mi TV remote service is not running on this port."
                )
            } catch (e: SocketTimeoutException) {
                return@withContext ProtocolVerificationResult.Failed(
                    "Connection timed out at $ip:$port. Check that the TV is powered ON and on the same WiFi network."
                )
            } catch (e: Exception) {
                return@withContext ProtocolVerificationResult.Failed(
                    "Network error at $ip:$port: ${e.localizedMessage ?: "Unable to connect"}"
                )
            }

            // Step 2: Protocol-level check. A bare socket connection is NOT proof of the Mi TV HTTP API.
            val startTime = System.currentTimeMillis()
            try {
                val request = Request.Builder()
                    .url("http://$ip:$port/request?action=isalive")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    when {
                        response.isSuccessful -> {
                            ProtocolVerificationResult.Verified(latency)
                        }
                        response.code in 401..403 -> {
                            ProtocolVerificationResult.PairingRequired(
                                "Mi TV at $ip:$port requires authorization/pairing (HTTP ${response.code})."
                            )
                        }
                        response.code == 404 -> {
                            ProtocolVerificationResult.ProtocolUnverified(
                                "Device at $ip:$port responded with HTTP 404. It does not implement the Xiaomi HTTP remote protocol."
                            )
                        }
                        else -> {
                            ProtocolVerificationResult.ProtocolUnverified(
                                "Device at $ip:$port returned unexpected HTTP status ${response.code}."
                            )
                        }
                    }
                }
            } catch (e: SocketTimeoutException) {
                ProtocolVerificationResult.Failed("HTTP alive probe timed out at $ip:$port")
            } catch (e: ConnectException) {
                ProtocolVerificationResult.Failed("HTTP connection refused at $ip:$port")
            } catch (e: IOException) {
                ProtocolVerificationResult.Failed("HTTP verification failed: ${e.localizedMessage}")
            } catch (e: Exception) {
                ProtocolVerificationResult.Failed("Verification error: ${e.localizedMessage}")
            }
        }

    override suspend fun ping(ip: String, port: Int): Boolean {
        return verifyConnection(ip, port) is ProtocolVerificationResult.Verified
    }

    override suspend fun sendCommand(ip: String, port: Int, command: RemoteCommand): CommandResult =
        withContext(Dispatchers.IO) {
            if (isKnownIncompatiblePort(port)) {
                return@withContext CommandResult.Unsupported(
                    "Port $port is incompatible with Xiaomi HTTP remote control."
                )
            }

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
                    } else if (response.code in 401..403) {
                        CommandResult.Error("Mi TV authorization required (HTTP ${response.code})")
                    } else {
                        CommandResult.Error("Mi TV returned HTTP ${response.code} for key '$keycode'")
                    }
                }
            } catch (e: SocketTimeoutException) {
                CommandResult.Error("Command timed out sending to $ip:$port")
            } catch (e: ConnectException) {
                CommandResult.Error("Connection refused by TV at $ip:$port")
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

    private fun isKnownIncompatiblePort(port: Int): Boolean {
        // 8009: Google Cast TLS/Protobuf
        // 6466, 6467: Android TV Remote v2 TLS
        // 8060: Roku ECP
        return port == 8009 || port == 6466 || port == 6467 || port == 8060
    }
}
