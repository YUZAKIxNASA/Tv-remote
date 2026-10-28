package com.example.model

/**
 * Protocols supported or detected by NASA WiFi Remote for various Smart TV ecosystems.
 */
enum class RemoteProtocol(
    val displayName: String,
    val defaultPort: Int,
    val defaultPath: String
) {
    MI_TV_HTTP(
        displayName = "Xiaomi Mi TV (HTTP)",
        defaultPort = 6095,
        defaultPath = "/controller"
    ),
    ROKU_ECP(
        displayName = "Roku / TCL ECP",
        defaultPort = 8060,
        defaultPath = "/keypress"
    ),
    ANDROID_TV_REMOTE(
        displayName = "Android TV Remote (v2)",
        defaultPort = 6466,
        defaultPath = ""
    ),
    GOOGLE_CAST(
        displayName = "Google Cast",
        defaultPort = 8009,
        defaultPath = ""
    ),
    UPNP_MEDIA(
        displayName = "UPnP / DLNA TV",
        defaultPort = 8080,
        defaultPath = "/upnp/control/rendctrl1"
    ),
    GENERIC_HTTP(
        displayName = "Generic Network TV",
        defaultPort = 80,
        defaultPath = "/api"
    ),
    UNVERIFIED(
        displayName = "Unverified Protocol",
        defaultPort = 0,
        defaultPath = ""
    )
}
