# NASA WiFi Remote — Implementation Plan

**Objective:** Build a robust, real, production-ready WiFi Remote Android Application for Mi TV and compatible Smart TVs with a modern White Theme UI.

---

## Architecture Design

```
com.example
├── MainActivity.kt                # Host Activity, Edge-to-Edge setup, Navigation & Screen switching
├── model
│   ├── DiscoveredDevice.kt        # TV Device entity (name, ip, port, protocol, manufacturer)
│   ├── RemoteProtocol.kt          # Supported TV protocols (Mi TV, Roku ECP, UPnP, Generic)
│   ├── RemoteCommand.kt           # Standard command set (Power, D-Pad, Volume, Media, Apps)
│   └── ConnectionState.kt         # Real connection states (NotConnected, Connecting, Connected, Failed)
├── network
│   ├── DeviceDiscoveryManager.kt  # mDNS NsdManager + SSDP UDP multicast + Subnet probe + Manual IP
│   ├── ProtocolDispatcher.kt      # Routes commands to the appropriate protocol adapter
│   └── adapter
│       ├── TvProtocolAdapter.kt   # Common protocol adapter interface
│       ├── MiTvHttpAdapter.kt     # Xiaomi Mi TV / PatchWall HTTP REST client (Port 6095)
│       ├── RokuEcpAdapter.kt      # Roku / TCL ECP client (Port 8060)
│       ├── UpnpMediaAdapter.kt    # UPnP DLNA AVTransport client (Port 8080)
│       └── GenericTvAdapter.kt    # Generic HTTP / Socket fallback
├── data
│   └── SavedDevicesRepository.kt  # SharedPreferences / Local storage for saved TVs
├── ui
│   ├── theme
│   │   ├── Color.kt               # Strict white-theme color system (#FFFFFF, #2563EB, etc.)
│   │   ├── Theme.kt               # M3 theme locking white background
│   │   └── Type.kt                # Typography
│   ├── components
│   │   ├── RemoteButtons.kt       # Rounded rectangular buttons, haptics, blue icons
│   │   ├── DpadControl.kt         # Elevated directional pad with central OK button
│   │   ├── DualRocker.kt          # Modern volume & channel rockers
│   │   └── DeviceCard.kt          # Discovered device card with status badges
│   ├── screens
│   │   ├── DeviceDiscoveryScreen.kt # Screen 1: Discovery, Scan, Manual IP entry
│   │   ├── DeviceConnectionScreen.kt# Screen 2: Connection details, ping test, capabilities
│   │   ├── MainRemoteScreen.kt      # Screen 3: White-theme primary remote control
│   │   └── MoreOptionsScreen.kt     # Screen 4: App launch, keyboard, saved devices, troubleshooting
│   └── viewmodel
│       └── RemoteViewModel.kt       # UI state, coroutine dispatching, active device state
```

---

## Phased Milestones

- **Phase 1:** Project audit, permissions, manifest configuration (Completed).
- **Phase 2:** Launcher icon generation & M3 White Theme configuration (Completed).
- **Phase 3:** Compatibility research & Documentation generation (Completed).
- **Phase 4:** Models, Protocol Adapters, and Network Discovery implementation.
- **Phase 5:** Saved Devices Repository and RemoteViewModel state management.
- **Phase 6:** UI Screens (Discovery, Connection, Main Remote, More Options) in White Theme.
- **Phase 7:** Edge-to-Edge handling, Haptics, and BackHandler navigation.
- **Phase 8:** Robolectric & Unit Testing to verify discovery logic, command routing, and UI state.
