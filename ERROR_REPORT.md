# NASA WiFi Remote — Error Report & Resolution

**Date:** 2026-10-27  
**Status:** All initial errors diagnosed and resolved.

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
