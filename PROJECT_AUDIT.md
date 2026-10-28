# NASA WiFi Remote — Project Audit

**Date:** 2026-10-27  
**Auditor:** Senior Android Developer & Network Protocol Specialist  
**Project:** NASA WiFi Remote (Android Universal Smart TV Remote)

---

## 1. Existing Project Structure

- **Framework & Language:** Android SDK (compileSdk 36, minSdk 24, targetSdk 36) using Kotlin 2.2.10 and Jetpack Compose (BOM 2024.09.00).
- **Architecture:** Standard Android Gradle single-module template (`:app`), Gradle Kotlin DSL (`build.gradle.kts`).
- **Initial Activities & Code:**
  - `MainActivity.kt`: Contains basic boilerplate `Greeting("Android")`.
  - `Color.kt`, `Theme.kt`, `Type.kt`: Default Purple/Pink Material 3 template color scheme.
  - `metadata.json`: Initially empty placeholders (`"name": ""`, `"description": ""`).
- **Networking & Discovery Implementation:**
  - None existed in the template. No socket, HTTP, mDNS, or SSDP discovery code was present.
- **Permissions Configured:**
  - None! `AndroidManifest.xml` had zero `<uses-permission>` tags.
- **Dependencies Present:**
  - OkHttp 4.10.0, Retrofit 2.12.0, Moshi 1.15.2, Coroutines 1.10.2, Compose Material 3, Room 2.7.0.

---

## 2. Confirmed Errors & Root Causes

1. **Missing Android Network Permissions:**
   - *Symptom:* Any socket, HTTP, or multicast discovery operation throws `SecurityException: Permission denied (missing INTERNET / ACCESS_NETWORK_STATE / CHANGE_WIFI_MULTICAST_STATE)`.
   - *Fix:* Declared `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, and `VIBRATE` in `AndroidManifest.xml`.

2. **Cleartext HTTP Traffic Blocked by Android 9+ (API 28+):**
   - *Symptom:* Local TV communication to Xiaomi Mi TV (port 6095) or Roku ECP (port 8060) via HTTP fails with `java.io.IOException: Cleartext HTTP traffic to <ip> not permitted`.
   - *Fix:* Added `android:usesCleartextTraffic="true"` to `<application>` in `AndroidManifest.xml`.

3. **No Multicast Lock on Android:**
   - *Symptom:* SSDP / mDNS discovery packets on `239.255.255.250` or `224.0.0.251` are dropped by the Linux WiFi kernel driver to save power unless `WifiManager.createMulticastLock()` is acquired.
   - *Fix:* Implemented safe MulticastLock acquisition and release in `DeviceDiscoveryManager`.

4. **Placeholder App Identity:**
   - *Symptom:* `metadata.json` had empty title, `strings.xml` had "My Application", launcher icon was the generic green Android logo.
   - *Fix:* Set application name to "NASA Remote", updated `metadata.json`, and generated custom adaptive launcher icon and mipmap PNGs.

---

## 3. Recommended Repair & Development Sequence

1. **Phase 1-2:** Manifest & Network Permissions Audit & Repair (Completed).
2. **Phase 3:** Mi TV & Network Protocols Verification & Compatibility Architecture.
3. **Phase 4:** Technical Implementation Plan.
4. **Phase 5:** White Theme Design Implementation (Hex colors: #FFFFFF, #F7F9FC, #2563EB, #172033, #64748B, #E2E8F0, #16A34A, #DC2626).
5. **Phase 6:** Real Network Protocol Adapters (Mi TV HTTP, Roku ECP, UPnP, Generic Socket).
6. **Phase 7:** Discovery Engine (mDNS, SSDP, Subnet Scan, Manual IP Connect).
7. **Phase 8:** 4 Distinct Screens (Home Discovery, Connection Details, Main Remote Control, More Options).
8. **Phase 9:** Local Persistence for Saved Devices & Haptics.
9. **Phase 10:** Automated JVM / Robolectric & Unit Testing.
