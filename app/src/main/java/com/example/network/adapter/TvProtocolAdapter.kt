package com.example.network.adapter

import com.example.model.CommandResult
import com.example.model.ProtocolVerificationResult
import com.example.model.RemoteCommand
import com.example.model.RemoteProtocol

/**
 * Common contract for all TV network protocol adapters.
 */
interface TvProtocolAdapter {
    val protocol: RemoteProtocol

    /**
     * Checks if the TV is reachable at the given IP and port using protocol verification.
     */
    suspend fun ping(ip: String, port: Int): Boolean

    /**
     * Deeply verifies that the target device at IP:port genuinely implements this protocol.
     */
    suspend fun verifyConnection(ip: String, port: Int): ProtocolVerificationResult

    /**
     * Dispatches a remote command to the target TV.
     */
    suspend fun sendCommand(ip: String, port: Int, command: RemoteCommand): CommandResult

    /**
     * Sends arbitrary text (for search/keyboard input) to the target TV.
     */
    suspend fun sendText(ip: String, port: Int, text: String): CommandResult

    /**
     * Launches a specific application package on the TV.
     */
    suspend fun launchApp(ip: String, port: Int, appPackage: String): CommandResult

    /**
     * Returns the set of commands genuinely supported by this protocol.
     */
    fun getSupportedCommands(): Set<RemoteCommand>
}
