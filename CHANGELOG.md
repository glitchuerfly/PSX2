<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Changelog

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
