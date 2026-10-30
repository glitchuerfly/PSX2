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
