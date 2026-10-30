<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Feature source coverage

Features below describe private-project implementations; “included” means source is present, not that the full feature can run from this export.

| Feature | Included source | Missing integration or content |
| --- | --- | --- |
| Settings Compatibility Bridge | GraphicsConfig, GraphicsSettingsModel, ProfileMigrator and controller-layer helpers | GraphicsSchema, source configuration tables, original preference resources and host hooks |
| Dynamic/Static Analog | AnalogGesture, AnalogTouch, controller preferences and RemotePadView | Legacy widget hooks, localized preference resources and application host |
| Custom Button Skin System | SkinArchive/Policy/Store/Repository/Renderer/Ui and related helpers | Skin images, catalog ZIPs, legacy UI hooks/resources |
| Automatic Cover Downloader | CoverManager and CoverPolicy | Legacy game-library hooks and fallback artwork |
| 2D/3D Game Library | Cache/style selection and image binding logic | Full library screen, resources and game cover artwork |
| Remote Play | Hybrid UI/runtime source, pad ownership policy and verified ARMSX2 backend snapshots | Generator adaptations, native WebRTC, core binaries, legacy settings tabs and resources |
| JNI/Native Core Integration | NativeApp contract and Bridge entry points | Core native implementation/binaries and legacy host |
| Diagnostics | legacy_trace.cpp | Legacy library it instruments and native build setup |
| Unit tests | Existing Java test sources; four suites run through the public runner | Android fakes, complete mappings, compiled Kotlin backend and other excluded dependencies |

The latest included pause-menu change calls the existing host page transaction with D(6, true), enabling the same fade path used by Settings. This export does not contain that legacy transaction implementation.

No game compatibility, performance, image quality, networking, or complete emulator runtime claim is established by the public host checks.

The controller startup path prepares the real layout before VM startup, shares profile selection with core input configuration and refreshes reused views immediately. It includes atomic artwork/cache state and stale-selection guards.

ControllerLoadNotice posts an enabled per-game configuration acknowledgement through CoreOsd to the original core OSD queue with a controller glyph. It uses the core's font/scale/stack/fade and renders behind touchscreen controls. CoreOsd.java, native/core_osd.cpp, config/native-osd.json and native_osd.py are included as partial integration source; their compiled shim/core binaries, private hooks and Default runtime PNGs remain absent.

The private app and GitHub icon use the supplied PNG. The exported source alone cannot demonstrate the running UI or build the complete APK.
