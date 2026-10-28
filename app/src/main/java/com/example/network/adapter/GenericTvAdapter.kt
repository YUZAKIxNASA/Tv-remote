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
 * Generic Network TV adapter with JSON/REST fallback for custom IP network TVs.
 */
class GenericTvAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2000, TimeUnit.MILLISECONDS)
        .readTimeout(2000, TimeUnit.MILLISECONDS)
        .build()
) : TvProtocolAdapter {

    override val protocol: RemoteProtocol = RemoteProtocol.GENERIC_HTTP

    override fun getSupportedCommands(): Set<RemoteCommand> = RemoteCommand.entries.toSet()

    override suspend fun verifyConnection(ip: String, port: Int): ProtocolVerificationResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 1500)
                }
                val latency = System.currentTimeMillis() - startTime
                ProtocolVerificationResult.Verified(latency)
            } catch (e: Exception) {
                ProtocolVerificationResult.Failed("Generic network TV unreachable: ${e.localizedMessage}")
            }
        }

    override suspend fun ping(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        verifyConnection(ip, port) is ProtocolVerificationResult.Verified
    }

    override suspend fun sendCommand(ip: String, port: Int, command: RemoteCommand): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val payload = """{"command":"${command.name.lowercase()}","label":"${command.label}"}"""
            val body = payload.toRequestBody("application/json".toMediaType())
            val url = "http://$ip:$port/api/command"

            try {
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Command sent: ${command.label}", latency)
                    } else {
                        CommandResult.Error("Device returned HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("Connection error: ${e.localizedMessage}")
            }
        }

    override suspend fun sendText(ip: String, port: Int, text: String): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val payload = """{"text":"$text"}"""
            val body = payload.toRequestBody("application/json".toMediaType())
            val url = "http://$ip:$port/api/text"

            try {
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Text sent", latency)
                    } else {
                        CommandResult.Error("HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("Failed: ${e.localizedMessage}")
            }
        }

    override suspend fun launchApp(ip: String, port: Int, appPackage: String): CommandResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val payload = """{"launch":"$appPackage"}"""
            val body = payload.toRequestBody("application/json".toMediaType())
            val url = "http://$ip:$port/api/launch"

            try {
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        CommandResult.Success("Launched $appPackage", latency)
                    } else {
                        CommandResult.Error("HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                CommandResult.Error("Failed: ${e.localizedMessage}")
            }
        }
}
