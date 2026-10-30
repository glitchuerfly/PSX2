<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Architecture

The private hybrid has four interacting layers:

| Layer | Included here | Required material omitted |
| --- | --- | --- |
| Host application and settings UI | Adapter entry points only | Legacy APK, decoded classes, UI resources and Smali hooks |
| Compatibility adapters | Selected Java/Kotlin policies and integration code | Held mapping tables, generated tables and host ABI stubs |
| Emulation runtime | JNI declarations and Remote Play source interfaces | ARMSX2 native core and dependency binaries |
| External content | Download/cache/import logic | Cover images, skin ZIPs, icons, avatar and game/BIOS data |

The private host calls adapters for settings translation, lifecycle/audio ownership, controller input, and UI extension. The adapter calls ARMSX2 through the JNI declarations in src/bridge/kr/co/iefriends/pcsx2/NativeApp.java.

## Source roots and package stability

src/bridge, src/controller, src/skins, src/covers, and src/remoteplay are separate source roots sharing the original namespaces. In particular, Java classes retain org.local.aetherarms and kr.co.iefriends.pcsx2. The newly authored CoreProbeActivity retains its declared xyz.aethersx2.android namespace; this is project instrumentation, not a decoded legacy class. Kotlin packages remain under com.armsx2.

Imports, reflection names, JNI class names, and source contents are unchanged. Cross-feature references require the roots to be considered together; these folders are not standalone libraries.

Vendor directories retain upstream snapshot layouts. Some vendor files are stored flat, as in the private snapshot, rather than under package directories; their declarations are verified unchanged and compilation would need explicit source inputs or an appropriate source-set configuration.

## Important boundaries

- Bridge.java targets both the legacy host ABI and the ARMSX2 native ABI. Names describing excluded classes do not mean those class implementations are bundled.
- Settings model/INI code is included, while GraphicsSchema.java and its category-C configuration input are held.
- Controller logic is included, while PadRoutes.java, its held configuration input, and legacy D-pad polygon data are absent.
- Remote Play vendor source is unmodified upstream source. The private generator's adapted backend output and native settings hooks are absent.
- Diagnostics are project instrumentation source. legacy_trace.cpp expects an excluded legacy native host and is not a replacement for that host.
- Resource names referenced by source are unresolved without independently licensed replacement resources.

Do not treat these layers as proof that GPL code and the legacy host form a legally separate aggregate. The integration and the release rights need their own review.

## Controller startup ordering

The initial layout resolves the controller profile file from the launch entry CRC before VM start and from the retained game CRC during temporary BIOS CRC-zero frames. ControllerSettings.profileFile selects the same global/per-game/named-profile layer as core input configuration. The private Activity.L integration applies that selection synchronously before creating child controls, so visibility, position, scale and skin share a layer.

SkinLoadState orders selections before main-thread dispatch, rejects obsolete decodes and publishes images/masks atomically. SkinStore caches two skins and keeps cold custom art pending rather than rendering Default. Controller-port icon code refers to Default resource identifiers; PNGs and legacy hooks are omitted.

ControllerLoadNotice waits for successful controller/native commit, presented game frames and ready skin, then posts through CoreOsd to the native queue described below. Its rendering and startup behavior are user-tested in the private app, not by this export's host tests.

### Startup correction

ControllerLayoutAccess.prepareLaunch runs after the original Activity's host views exist and before its VM thread starts. The private onCreate hook builds the real D/O controller layout at that point; O resolves the controller store at entry and reapplies child visibility/geometry/skin at exit even if its view type was reused. These steps do not wait for observer polling or a later native frame.

A missing game-list CRC can use the first nonzero boot identity remembered by exact launch path and matching serial, or one unambiguous existing serial/CRC profile pair. Current native identity supersedes hints; multiple ELF candidates are never guessed and an ended session's CRC is not reused as active state.

The left-column configuration acknowledgement requires Pad/UseGameSettingsForController to be enabled in the game's own profile. Global controller settings alone, a missing profile or a disabled switch do not produce that notice.

## Current native OSD integration

ControllerLoadNotice calls CoreOsd.post, which loads the small JNI libhybridosd adapter. Its C++ code locates the already loaded donor variant, checks its exported load-base anchor and 64-byte function prefix, then calls the original Host::AddKeyedOSDMessage function using the NDK string ABI. The original thread-safe core queue supplies message ordering, font, scale, cutout margins, fading and rendering beneath Android touch controls. The U+F11B gamepad prefix and two spaces match the existing icon-message format.

config/native-osd.json pins the two core hashes, the libc++ hash and executable call-site/function data. native_osd.py rejects an unreviewed donor, builds the shim and checks 16 KB ELF layout and absence of build-machine RPATH. A pure NDK ABI check tests short/long strings and ownership using the donor libc++, without loading the emulator or starting a VM. Runtime guards reject unsupported bindings. A future core update requires binding/ABI review; the internal function address is not a stable upstream API.

app_icon.py preserves the user PNG separately and produces five density sets of three bitmap slots. Its APK check compares decoded image pixels, allowing aapt PNG crunching without accepting an old icon. The GitHub asset is the original supplied PNG, not a resized launcher variant.

## PSX2 identity update

The public project name is PSX2 (formerly ARMSX2 Hybrid). The new application ID is `com.psx2.glitch`; internal Java/Kotlin/JNI namespaces remain unchanged. The ARMSX2 2.9.1 core retains its upstream name and credits. This documentation update does not refresh the partial source snapshot or make the complete APK buildable. The current README icon is a combined preview of the approved adaptive layers; older statements about the supplied PNG describe the historical snapshot.
