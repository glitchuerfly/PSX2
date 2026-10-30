<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Excluded files and dependencies

The table lists all 59 tracked files from the private hybrid snapshot that are not copied. Original A/B/C categories are retained; two B generated files are held more conservatively because their inputs remain C. No C/D file is admitted to the export.

| Original path | Audit | Reason |
| --- | --- | --- |
| `aether-armsx2-port/.gitignore` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/AGENTS.md` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/PORT-STATUS.txt` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/PROJECT.txt` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/RELEASE-STATUS.txt` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/analog_strings.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/app-assets/about/profile.jpg` | C | No verified artist/redistribution grant. |
| `aether-armsx2-port/app-assets/about/provenance.json` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/app-assets/notices/style.css` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/backup-project.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/build.py` | C | Unresolved legacy native/Smali integration and signing-password arguments; not copied. |
| `aether-armsx2-port/check_alignment.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/compile-stubs/androidx/preference/Preference.java` | C | ABI reconstruction or test-stub grant not sufficiently established. |
| `aether-armsx2-port/config/analog-translations.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/config/app.json` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/config/controller-audit.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/config/controller-pad-routes.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/config/controller-properties-translations.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/config/donor.lock.json` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/config/github-release.json` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/config/graphics-mapping.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/config/remote-play-ui-translations.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/config/skin-translations.json` | C | Mapping/translation provenance not resolved per entry. |
| `aether-armsx2-port/controller_audit.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/fetch-donor.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/graphics_schema.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/notices.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/patch_analog.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_controller_settings.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_controls.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_dpad.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_launch.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_library.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_lifecycle.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/patch_pause_menu.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_remoteplay.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_skins.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/patch_touch_targets.py` | C | Legacy Smali/resource patch material needs ownership and redistribution review. |
| `aether-armsx2-port/project_config.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/remote-smali/org/local/aetherarms/RemotePageFragment.smali` | C | Legacy method bodies/ABI integration need redistribution verification. |
| `aether-armsx2-port/remote-smali/org/local/aetherarms/RemotePlaySettingsActivity.smali` | C | Legacy method bodies/ABI integration need redistribution verification. |
| `aether-armsx2-port/remote-smali/org/local/aetherarms/RemoteTabTitles.smali` | C | Legacy method bodies/ABI integration need redistribution verification. |
| `aether-armsx2-port/remote-smali/org/local/aetherarms/RemoteTabsAdapter.smali` | C | Legacy method bodies/ABI integration need redistribution verification. |
| `aether-armsx2-port/remote-smali/org/local/aetherarms/RemoteTabsFragment.smali` | C | Legacy method bodies/ABI integration need redistribution verification. |
| `aether-armsx2-port/skin_strings.py` | B | Private build/backup/generator context; omitted to avoid presenting an incomplete or unsafe APK pipeline. |
| `aether-armsx2-port/src/org/local/aetherarms/DpadGeometry.java` | C | Copied legacy artwork geometry; grant not established. |
| `aether-armsx2-port/src/org/local/aetherarms/GraphicsSchema.java` | B | Additional conservative hold: generated from a category-C configuration input; source header alone does not clear that input. |
| `aether-armsx2-port/src/org/local/aetherarms/PadRoutes.java` | B | Additional conservative hold: generated from a category-C configuration input; source header alone does not clear that input. |
| `aether-armsx2-port/tests/check-remote-play.py` | B | Private full-suite runner depends on excluded decoded/build inputs and original layout. |
| `aether-armsx2-port/tests/controller-ui-stubs/android/app/Activity.java` | C | ABI reconstruction or test-stub grant not sufficiently established. |
| `aether-armsx2-port/tests/controller-ui-stubs/android/app/Dialog.java` | C | ABI reconstruction or test-stub grant not sufficiently established. |
| `aether-armsx2-port/tests/controller-ui-stubs/android/content/DialogInterface.java` | C | ABI reconstruction or test-stub grant not sufficiently established. |
| `aether-armsx2-port/tests/controller-ui-stubs/android/view/View.java` | C | ABI reconstruction or test-stub grant not sufficiently established. |
| `aether-armsx2-port/tests/remote-deps/json-20250517.jar` | A | All binary dependencies are omitted; native dependency rights/closure remain separate where category C. |
| `aether-armsx2-port/tests/remote-deps/provenance.json` | A | Optional internal document, metadata, styling or provenance not needed by this partial source export. |
| `aether-armsx2-port/tests/run-checks.py` | B | Private full-suite runner depends on excluded decoded/build inputs and original layout. |
| `aether-armsx2-port/vendor/remoteplay/kotlin-stdlib-2.2.10.jar` | B | All binary dependencies are omitted; native dependency rights/closure remain separate where category C. |
| `aether-armsx2-port/vendor/remoteplay/libjingle_peerconnection_so.so` | C | All binary dependencies are omitted; native dependency rights/closure remain separate where category C. |
| `aether-armsx2-port/vendor/remoteplay/webrtc-1.3.10.jar` | B | All binary dependencies are omitted; native dependency rights/closure remain separate where category C. |

## Entire private scopes omitted

- `aether-armsx2-port/inputs/`: Legacy APKs, decoded code/resources and rollback inputs; C/D.
- `aether-armsx2-port/build/`: Generated private artifacts, credentials, caches and binary dependencies; excluded in full.
- `aether-armsx2-port/out/`: Hybrid/test APKs and signing sidecars; excluded in full.
- `aether-armsx2-port/donor/`: Donor APK, native binaries, artwork and bundle resources; no binary distribution in this export.
- `aether-armsx2-port/reports/`: Raw logs, screenshots, disassembly and private build/device evidence; excluded in full.
- `armsx2-analysis/`: No decompiled tree, APK, resource or disassembly is copied.
- `armsx2-custom/`: No wholesale source-tree copy; only already-vendored verified A/B snapshots are used.
- `backups/`: Private archives may contain signing material and legacy inputs; excluded in full.
- `.git/ (all original repositories)`: No private object database, config, reflogs, remotes or commit history is copied.

## External dependencies intentionally absent

- ARMSX2 core native libraries and their native dependencies; no executable runtime is bundled.
- Kotlin stdlib 2.2.10 JAR, Stream WebRTC 1.3.10 AAR/JAR, native libjingle_peerconnection_so.so, and JSON-java test JAR. The three active public suites do not require them.
- Complete Android host, manifest/resources, legacy Preference ABI and Android UI test fakes.
- Adapted backend output and its private patch generator.
- BIOS/game dumps, cover artwork, skin ZIPs, fallback vectors, avatars and screenshots.

## Files still needing permission or provenance review

Legacy APK/code/resources/native host; copied D-pad vector geometry; Remote Play Smali method bodies; held mapping/translation inputs and generated outputs; avatar/skin/cover artwork; native WebRTC and other binary dependency closure; reconstructed ABI/test stubs.

NetherSX2-patch licensing does not resolve rights over AetherSX2. Rights over names/logos are separate from source licenses. See PUBLICATION-AUDIT.md.

## Later source updates

The exclusion tables above record the initial audit snapshot. Later source additions are individually reviewed and recorded in PUBLICATION-MANIFEST.json. The private controller hook/build scripts, controller Default PNGs and their provenance payload, private reports, native binaries, signing material and backups remain omitted. Existing remote README visual assets are separately listed in .github/assets/ASSET-REVIEW.json; their rights are not cleared by the source allowlist.
