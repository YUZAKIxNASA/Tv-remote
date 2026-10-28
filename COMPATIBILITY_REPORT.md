# NASA WiFi Remote — Smart TV Compatibility Report

**Target Brand:** Mi TV / Xiaomi Smart TV (MIUI TV / PatchWall / Android TV)  
**Additional Targets:** Android TV / Google TV, Roku / TCL, UPnP / DLNA Smart TVs

---

## 1. Xiaomi Mi TV Network Control Research

### Communication Architecture
Xiaomi Smart TVs and Mi Box streaming players run either **MIUI TV / PatchWall** (custom Android OS) or **Android TV with PatchWall launcher**.

On local WiFi networks, Mi TV exposes an embedded HTTP remote controller service listening on port **6095** (and secondary port **6097**).

### Verified Mi TV HTTP Protocol
- **Base Endpoint:** `http://<device_ip>:6095/controller`
- **Method:** `GET` or `POST`
- **Ping / Alive Check:** `http://<device_ip>:6095/request?action=isalive` or `GET http://<device_ip>:6095/`
  - Successful response returns JSON: `{"status":0,"msg":"ok"}`.
- **Keyevent Command Format:**  
  `http://<device_ip>:6095/controller?action=keyevent&keycode=<keycode>`

### Supported Keycodes for Xiaomi Mi TV:
| NASA Remote Button | Mi TV Keycode | Result |
| :--- | :--- | :--- |
| **Power** | `power` | Toggle power / Standby |
| **Up** | `up` | Focus navigation up |
| **Down** | `down` | Focus navigation down |
| **Left** | `left` | Focus navigation left |
| **Right** | `right` | Focus navigation right |
| **OK / Select** | `enter` | Select current item |
| **Home** | `home` | Return to PatchWall home screen |
| **Back** | `back` | Return to previous view |
| **Menu** | `menu` | Open context/options menu |
| **Volume Up** | `volumeup` | Increase TV volume |
| **Volume Down** | `volumedown` | Decrease TV volume |
| **Mute** | `mute` | Toggle audio mute |

### Extended Mi TV Actions:
1. **Remote Keyboard / Text Input:**  
   `GET http://<device_ip>:6095/controller?action=edittext&text=<url_encoded_text>`  
   Allows typing directly into search boxes on the TV.
2. **Start Application:**  
   `GET http://<device_ip>:6095/controller?action=startapp&package=<package_name>`  
   Example packages:  
   - YouTube: `com.google.android.youtube.tv`  
   - Netflix: `com.netflix.ninja`  
   - Prime Video: `com.amazon.amazonvideo.livingroom`  

---

## 2. Multi-Brand Protocol Adapters

1. **Roku / TCL Roku TV (Port 8060):**  
   - Uses Roku External Control Protocol (ECP) over HTTP POST:  
     `http://<device_ip>:8060/keypress/<key>`  
   - Standard commands: `Home`, `Rev`, `Fwd`, `Play`, `Select`, `Left`, `Right`, `Down`, `Up`, `Back`, `VolumeUp`, `VolumeDown`, `VolumeMute`, `Power`.

2. **Android TV / Google TV:**  
   - DIAL (Discovery and Launch) on port 8008 for app launching and discovery.
   - mDNS service discovery on `_androidtvremote2._tcp` and `_googlecast._tcp`.

3. **UPnP / DLNA Smart TVs (Port 8080 / 1900):**  
   - Discovered via SSDP multicast (`239.255.255.250:1900`).  
   - SOAP rendering control for Volume and Play/Pause/Stop.

---

## 3. Pairing & Security Verification
- **Mi TV Local HTTP API:** When enabled on the local subnet, Xiaomi PatchWall accepts commands directly over port 6095 without an OAuth token because it operates within the trusted home LAN boundary.
- **Standby Limitation:** When in deep sleep/standby, some Mi TVs power down their WiFi NIC. If power-off puts the TV into deep sleep, Wake-on-LAN (WOL UDP broadcast to port 9) is used where supported.
- **Error Handling:** If the TV is unreachable (e.g., incorrect IP or TV on different WiFi band), NASA Remote displays the exact network error (`SocketTimeoutException` or `ConnectException`) instead of a fake success message.
