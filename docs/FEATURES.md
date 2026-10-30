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

The controller startup update shares profile-file selection between core settings and the initial legacy controller layout. It includes atomic artwork/cache state and stale-selection guards. ControllerLoadNotice provides a short Android overlay after successful configuration/native commit, using the OSD message switch/scale; it does not insert into the donor's native OSD queue. The private legacy hook, Default PNG assets and Android host remain excluded, so the exported source alone cannot demonstrate this UI behavior.
