# NASA WiFi Remote — Smart TV Compatibility Report

**Target Brand:** Mi TV / Xiaomi Smart TV (MIUI TV / PatchWall / Android TV)  
**Additional Targets:** Android TV / Google TV (Sony, TCL, Hisense, Philips, Chromecast), Roku / TCL, UPnP / DLNA Smart TVs

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

## 2. Android TV Remote v2 Protocol (Sony, TCL, Xiaomi Android TV, Google TV)

### Communication Architecture
Android TV Remote v2 operates on two dedicated TCP ports over local WiFi:
- **Port 6466 (Pairing Port):** Handles pairing request initiation, certificate exchange, and PIN challenge verification. When initiated, the TV displays a 4 to 6-character code.
- **Port 6467 (Remote Control Port):** Handles framed bidirectional keyevent packets once paired.

### Handshake & Security:
- **PIN Challenge:** A 4 to 6 alphanumeric code is rendered on screen.
- **Session State:** Once verified, the paired device is remembered in local secure storage, avoiding repeat prompts.
- **Keyevent Injection:** Framed Android keycodes:
  - `KEYCODE_DPAD_UP (19)`, `KEYCODE_DPAD_DOWN (20)`, `KEYCODE_DPAD_LEFT (21)`, `KEYCODE_DPAD_RIGHT (22)`, `KEYCODE_DPAD_CENTER (23)`
  - `KEYCODE_HOME (3)`, `KEYCODE_BACK (4)`, `KEYCODE_MENU (82)`
  - `KEYCODE_POWER (26)`, `KEYCODE_VOLUME_UP (24)`, `KEYCODE_VOLUME_DOWN (25)`, `KEYCODE_VOLUME_MUTE (164)`
  - `KEYCODE_MEDIA_PLAY_PAUSE (85)`

---

## 3. Multi-Brand Protocol Adapters

1. **Roku / TCL Roku TV (Port 8060):**  
   - Uses Roku External Control Protocol (ECP) over HTTP POST:  
     `http://<device_ip>:8060/keypress/<key>`  
   - Standard commands: `Home`, `Rev`, `Fwd`, `Play`, `Select`, `Left`, `Right`, `Down`, `Up`, `Back`, `VolumeUp`, `VolumeDown`, `VolumeMute`, `Power`.

2. **Google Cast (Port 8009):**  
   - Strictly classified as media streaming, not a remote control key protocol. NASA Remote correctly prevents false-positive attempts to dispatch keyevents to port 8009.

3. **UPnP / DLNA Smart TVs (Port 8080 / 1900):**  
   - Discovered via SSDP multicast (`239.255.255.250:1900`).  
   - SOAP rendering control for Volume and Play/Pause/Stop.

---

## 4. Pairing & Security Verification
- **Mi TV Local HTTP API:** Xiaomi PatchWall accepts commands directly over port 6095 within the trusted home LAN boundary.
- **Android TV Remote v2:** Requires user confirmation of the TV-generated PIN on port 6466 before port 6467 accepts navigation events.
- **Standby Limitation:** When in deep sleep/standby, some TVs power down their WiFi NIC. If power-off puts the TV into deep sleep, Wake-on-LAN (WOL UDP broadcast to port 9) is used where supported.
- **Real Feedback:** If the TV is unreachable (e.g., incorrect IP or TV on different WiFi band), NASA Remote displays the exact network error (`SocketTimeoutException` or `ConnectException`) instead of a fake success message.
