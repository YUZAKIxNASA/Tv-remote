package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DiscoveredDevice
import com.example.model.RemoteProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists user-saved TV devices locally using Android SharedPreferences.
 */
class SavedDevicesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nasa_remote_saved_devices", Context.MODE_PRIVATE)

    private val _savedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val savedDevices: StateFlow<List<DiscoveredDevice>> = _savedDevices.asStateFlow()

    init {
        loadSavedDevices()
    }

    private fun loadSavedDevices() {
        val jsonString = prefs.getString("saved_devices_json", null) ?: return
        try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<DiscoveredDevice>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val protocolName = obj.optString("protocol", RemoteProtocol.MI_TV_HTTP.name)
                val protocol = try {
                    RemoteProtocol.valueOf(protocolName)
                } catch (_: Exception) {
                    RemoteProtocol.UNVERIFIED
                }
                list.add(
                    DiscoveredDevice(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        manufacturer = obj.optString("manufacturer", "Smart TV"),
                        model = obj.optString("model", "Smart TV"),
                        ipAddress = obj.getString("ipAddress"),
                        port = obj.optInt("port", protocol.defaultPort),
                        protocol = protocol,
                        deviceType = obj.optString("deviceType", "Smart TV"),
                        isSaved = true,
                        lastSeenTimestamp = obj.optLong("lastSeenTimestamp", System.currentTimeMillis())
                    )
                )
            }
            _savedDevices.value = list
        } catch (_: Exception) {}
    }

    fun saveDevice(device: DiscoveredDevice) {
        val current = _savedDevices.value.filter { it.ipAddress != device.ipAddress || it.port != device.port }
        val updated = current + device.copy(isSaved = true, lastSeenTimestamp = System.currentTimeMillis())
        _savedDevices.value = updated
        persist(updated)
    }

    fun removeDevice(device: DiscoveredDevice) {
        val updated = _savedDevices.value.filter { !(it.ipAddress == device.ipAddress && it.port == device.port) }
        _savedDevices.value = updated
        persist(updated)
    }

    private fun persist(list: List<DiscoveredDevice>) {
        val array = JSONArray()
        list.forEach { dev ->
            val obj = JSONObject().apply {
                put("id", dev.id)
                put("name", dev.name)
                put("manufacturer", dev.manufacturer)
                put("model", dev.model)
                put("ipAddress", dev.ipAddress)
                put("port", dev.port)
                put("protocol", dev.protocol.name)
                put("deviceType", dev.deviceType)
                put("lastSeenTimestamp", dev.lastSeenTimestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_devices_json", array.toString()).apply()
    }
}
