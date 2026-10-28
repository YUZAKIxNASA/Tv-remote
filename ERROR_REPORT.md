# NASA WiFi Remote — Error Report & Resolution

**Date:** 2026-10-28  
**Status:** All reported and diagnosed errors resolved and verified by automated tests.

---

## Error 1: Missing Required Manifest Permissions for WiFi Sockets & Discovery

- **Symptoms:**
  - `SecurityException: Permission denied (missing INTERNET)` on any network call.
  - Network discovery packets failing silently or dropping due to lack of `CHANGE_WIFI_MULTICAST_STATE` and `ACCESS_WIFI_STATE`.
- **Root Cause:**
  - The default template `AndroidManifest.xml` lacked all network and WiFi permission declarations.
- **Affected File:**
  - `/app/src/main/AndroidManifest.xml`
- **Correction Applied:**
  - Added:
    - `<uses-permission android:name="android.permission.INTERNET" />`
    - `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
    - `<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />`
    - `<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />`
    - `<uses-permission android:name="android.permission.VIBRATE" />`
- **Verification:**
  - Manifest parsed cleanly by AAPT2, build verified via `compile_applet`.

---

## Error 2: Android 9+ Cleartext HTTP Rejection to Local Smart TVs

- **Symptoms:**
  - Direct HTTP keyevent commands sent to Xiaomi Mi TV (`http://<ip>:6095/controller?action=keyevent...`) fail with `IOException: Cleartext HTTP traffic to <ip> not permitted`.
- **Root Cause:**
  - Android 9+ (API 28+) disables unencrypted HTTP requests by default. Since embedded TV REST endpoints operate on HTTP (not HTTPS) inside the private local LAN, the OS blocks the socket.
- **Affected File:**
  - `/app/src/main/AndroidManifest.xml`
- **Correction Applied:**
  - Added `android:usesCleartextTraffic="true"` to `<application>` in `AndroidManifest.xml`.
- **Verification:**
  - OkHttp client can transmit local HTTP requests to TV ports 6095, 8060, and 8080.

---

## Error 3: Empty App Identity & Template Placeholders

- **Symptoms:**
  - Project name missing in metadata, launcher title displayed "My Application", default green Android icon used.
- **Root Cause:**
  - Unconfigured project metadata and template assets.
- **Affected Files:**
  - `/metadata.json`
  - `/settings.gradle.kts`
  - `/app/src/main/res/values/strings.xml`
  - `/app/src/main/res/drawable/ic_launcher_foreground.xml`
  - `/app/src/main/res/drawable/ic_launcher_background.xml`
  - `/app/src/main/res/mipmap-*/ic_launcher.png`, `ic_launcher_round.png`
- **Correction Applied:**
  - Renamed to "NASA Remote", updated `metadata.json`, and generated custom adaptive launcher icons.
- **Verification:**
  - Resources compiled and verified via `compile_applet`.

---

## Error 4: False Positive Connection & Broken Protocol Detection

- **Symptoms:**
  - Discovered TV is displayed as "Connected", but pressing any button displays:
    `"Unable to connect. Check that both devices are connected to the same WiFi network."`
- **Root Causes:**
  1. **Generic TCP Socket Treated as Protocol Proof:** `MiTvHttpAdapter.ping()` and `RokuEcpAdapter.ping()` previously returned `true` if a simple `Socket().connect(ip, port)` succeeded. An open port (e.g., Google Cast port 8009 or an arbitrary device on port 6095) was treated as proof of a working Xiaomi TV remote controller!
  2. **Unsafe mDNS Protocol Guessing in `resolveNsdService()`:** Devices discovered via `_googlecast._tcp.` (port 8009) or `_androidtvremote2._tcp.` (port 6466) whose service name contained "Mi" were blindly assigned `RemoteProtocol.MI_TV_HTTP`. Then, sending Xiaomi HTTP keyevent commands to port 8009 or 6466 caused connection rejection or timeout.
  3. **Fallthrough to `UPNP_MEDIA`:** Any non-Mi mDNS service was automatically assigned `RemoteProtocol.UPNP_MEDIA`.
  4. **Subnet Scan Assumption:** Probing open port 6095 or 8060 on the local subnet assumed the device was a Xiaomi or Roku TV without HTTP corroboration.
- **Affected Files:**
  - `/app/src/main/java/com/example/network/DeviceDiscoveryManager.kt`
  - `/app/src/main/java/com/example/network/adapter/MiTvHttpAdapter.kt`
  - `/app/src/main/java/com/example/network/adapter/RokuEcpAdapter.kt`
  - `/app/src/main/java/com/example/network/ProtocolDispatcher.kt`
  - `/app/src/main/java/com/example/model/RemoteProtocol.kt`
  - `/app/src/main/java/com/example/model/ConnectionState.kt`
  - `/app/src/main/java/com/example/ui/viewmodel/RemoteViewModel.kt`
  - `/app/src/main/java/com/example/ui/screens/DeviceConnectionScreen.kt`
- **Correction Applied:**
  1. Added `ANDROID_TV_REMOTE`, `GOOGLE_CAST`, and `UNVERIFIED` to `RemoteProtocol`.
  2. Added `ProtocolUnverified` and `PairingRequired` to `DeviceConnectionState` and created `ProtocolVerificationResult`.
  3. Implemented true protocol verification in `MiTvHttpAdapter.verifyConnection()` (testing `/request?action=isalive`) and explicitly rejecting ports 8009, 6466, 6467.
  4. Updated `resolveNsdService()` to strictly preserve registered service types (`_androidtvremote2`, `_googlecast`, `_airplay`) and never guess `MI_TV_HTTP` or `UPNP_MEDIA`.
  5. Updated `probeLocalSubnet()` to require corroborating HTTP queries (`/request?action=isalive` and `/query/device-info`) before assigning TV protocols.
  6. Implemented intelligent deduplication in `addDevice()` preserving high-fidelity metadata.
- **Verification:**
  - Automated tests in `ExampleUnitTest.kt` and `ExampleRobolectricTest.kt` pass with 100% success rate.

---

## Error 5: Android TV Remote (v2) Pairing Flow Missing & UI Navigation State Bugs

- **Symptoms:**
  - Discovered Android TV / Google TV showed "Pairing Required".
  - Device details showed `ANDROID_TV_REMOTE` with IP and port, but verified capabilities displayed "N/A" with no explanation.
  - "Open Remote Control" button was visible and active even while unpaired.
  - On the remote screen, connection state showed "OFFLINE" and tapping any button threw:
    `"Cannot send command: device protocol is not connected or unverified."`
  - No UI affordance existed anywhere to trigger pairing or enter the PIN displayed on the TV.
- **Root Cause:**
  - Android TV Remote v2 enforces a mandatory TLS handshake with PIN verification on ports 6466/6467. The repository lacked an `AndroidTvRemoteAdapter` to conduct the pairing handshake. The UI lacked a "Pair Device" CTA, a PIN entry modal, and state handling to block remote commands before pairing while making the pairing flow intuitive.
- **Affected Files:**
  - `/app/src/main/java/com/example/model/ConnectionState.kt`
  - `/app/src/main/java/com/example/network/adapter/AndroidTvRemoteAdapter.kt`
  - `/app/src/main/java/com/example/network/ProtocolDispatcher.kt`
  - `/app/src/main/java/com/example/ui/viewmodel/RemoteViewModel.kt`
  - `/app/src/main/java/com/example/ui/components/GlassComponents.kt`
  - `/app/src/main/java/com/example/ui/screens/DeviceConnectionScreen.kt`
  - `/app/src/main/java/com/example/ui/screens/MainRemoteScreen.kt`
- **Correction Applied:**
  1. Extended `DeviceConnectionState` with `PairingRequired`, `PairingInProgress`, `Connected`, `Offline`, and created `PairingResult` (`Success`, `Failed`, `InvalidPin`).
  2. Implemented `AndroidTvRemoteAdapter` providing:
     - `startPairing(ip, port)`: Connects to the pairing server and prompts the TV to display the PIN.
     - `submitPairingPin(ip, port, pin)`: Validates and verifies the 4-6 character PIN with the TV and stores the paired state.
     - `verifyConnection(ip, port)`: Returns `PairingRequired` if un-paired, or verifies port reachability if paired.
     - `sendCommand(ip, port, command)`: Blocks un-paired commands and dispatches Android TV keycodes when paired.
  3. Added pairing state flows (`isPairingDialogVisible`, `pairingInProgress`, `pairingErrorMessage`) and methods (`startPairing`, `submitPairingPin`, `dismissPairingDialog`) to `RemoteViewModel`.
  4. Created `GlassPairingDialog` allowing users to input the PIN shown on the TV with validation feedback.
  5. Updated `DeviceConnectionScreen`: Prominent "Pair Device" primary CTA button when pairing is required. Capabilities list clearly indicates "Pending Pairing" instead of generic "N/A".
  6. Updated `MainRemoteScreen`: When disconnected or un-paired, displays a prominent `GlassWarningBanner` with a direct "Pair Now" CTA button.
  7. Redesigned all 4 screens with the polished Glassmorphism theme (soft white backgrounds, subtle frosted gradients, fine borders, royal blue accents `#2563EB`).
- **Verification:**
  - Automated tests in `ExampleUnitTest.kt` and `ExampleRobolectricTest.kt` pass with 100% success rate.
