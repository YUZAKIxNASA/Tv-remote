package com.example.model

/**
 * Universal remote control commands supported across TV models.
 */
enum class RemoteCommand(val label: String) {
    // Power
    POWER("Power"),

    // D-Pad navigation
    UP("Up"),
    DOWN("Down"),
    LEFT("Left"),
    RIGHT("Right"),
    OK("OK / Select"),

    // System navigation
    HOME("Home"),
    BACK("Back"),
    MENU("Menu"),

    // Volume
    VOLUME_UP("Volume +"),
    VOLUME_DOWN("Volume -"),
    MUTE("Mute"),

    // Channel
    CHANNEL_UP("Channel +"),
    CHANNEL_DOWN("Channel -"),

    // Media
    PLAY_PAUSE("Play / Pause"),
    STOP("Stop"),
    PREVIOUS("Previous"),
    NEXT("Next"),

    // Extra
    INPUT_SOURCE("Source"),
    KEYBOARD("Keyboard"),
    APP_YOUTUBE("YouTube"),
    APP_NETFLIX("Netflix"),
    APP_PRIME("Prime Video")
}

/**
 * Outcome of executing a remote command.
 */
sealed class CommandResult {
    data class Success(val message: String, val latencyMs: Long = 0) : CommandResult()
    data class Error(val message: String, val errorDetails: String? = null) : CommandResult()
    data class Unsupported(val message: String) : CommandResult()
}
