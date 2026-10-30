<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Changelog

## Controller startup source update — 2026-10-30

Refreshed reviewed source from private checkpoint `a2de3945e2404dc8a26e221c70a16976492ef4a2`.

- Resolve controller profile before the first button layout, using the launch/retained game CRC through initial BIOS frames.
- Keep Add/Remove visibility, layout geometry and skin selection consistent at startup.
- Publish skin images and D-pad masks together; cache two skins and reject obsolete decoder results and queued selections.
- Use Default controller-port icon identifiers independently of the selected touchscreen skin; the artwork itself is omitted.
- Add a brief in-game configuration acknowledgement after successful controller application/native commit. This is an Android overlay styled like OSD; no donor native code is changed.
- Include four newly reviewed GPL source/test files and a fourth runnable public host suite for skin-loading races and controller-profile selection.
- Preserve the latest upstream README edit and all existing vendor notices.

This update adds no APK, native library, proprietary resources, signing material or private Git history. The repository remains partial and cannot build the complete APK. Visual assets already present in the remote repository retain their unresolved review status.

## Publication source snapshot — 2026-10-30

Exported selected source from private commit `f0276fa2804f9174492b69f7cebbcf4c338bcc86`.

- Grouped source into bridge, controller, skins, covers, and Remote Play source roots.
- Preserved included source bytes and Java/Kotlin package declarations.
- Added publication documentation, license scope, provenance mapping, exclusions, validation results, and checksums.
- Added a public runner for three existing host test suites.
- Excluded all C/D files, all binaries, all legacy decoded material, and private history.
- Additionally held generated GraphicsSchema.java and PadRoutes.java because their preferred configuration inputs remain category C.

These are export-layout/documentation changes, not a refactor of application behavior.

## Included private-development milestones

Dates are summarized from the project history; per-file change dates are in PUBLICATION-MANIFEST.json.

| Commit | Change in the private project |
| --- | --- |
| `b53f06c` | Initial JNI/adapter integration |
| `938c83d` | Graphics settings mapping and cover management |
| `021ebff` | Custom touchscreen skins |
| `6cf440c` | Dynamic/Static analog selection |
| `e2ba94e` | ARMSX2 Remote Play backend integration |
| `6b28187` | Native settings tabs for Remote Play |
| `c00dca9` | Credits and third-party notice organization |
| `4638b32` | Initial version marked 1.0.0-beta |
| `f0276fa` | Pause Remote Play enables the native Settings fade transaction |

Some associated patches, tables, resources, and native components are excluded. Listing a milestone does not mean this export contains everything needed to reproduce it.
