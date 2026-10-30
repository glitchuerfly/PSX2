# PSX2 data migration plan — explicit export/import only

PSX2 uses `com.psx2.glitch`. The previous ARMSX2 Hybrid app uses
`com.armsx2.hybrid`. Android treats these as separate applications/UIDs,
even when signed by the same certificate. PSX2 is **not an in-place update**
to the old app and cannot assume access to its private data or SAF grants.
Both apps may coexist. Nothing is automatically copied or deleted.

This is a plan, not an implemented or executed migration. No migration button,
archive compatibility, private-setting transfer, or cross-package access is
claimed by the rebranding build. Installation and each export/import operation
require the user's separate approval. No root, unsafe filesystem permissions,
SELinux changes, run-as assumption, or direct access to old Android/data is used.

## Before export

1. Keep the old app installed with its original signing key and data.
2. Stop emulation normally and wait for save-state/card writes to finish before
   taking a consistent snapshot. Do not inspect/copy live writable memory cards.
3. Use an export operation **inside the old application UID**, writing to a
   user-chosen archive via SAF (e.g. Downloads/Documents). The existing Transfer
   Data feature is a candidate, not proof of complete archive coverage: review
   its actual export format and restore support before use.
4. If the existing exporter omits private SharedPreferences, skins, per-game
   files or sidecars, implement and review a separate opt-in exporter in a
   same-package/same-signer old-app compatibility update. That update needs
   independent build/install approval; it is not part of this branding change.
5. Inventory archive files, sizes, version, logical paths and SHA-256. Include
   only user-selected content and settings. Do not export signing keys, tokens,
   passwords or account sessions. Keep archives private; BIOS/firmware/cards/
   saves are user data, not GitHub assets.

## Data mapping and consent

| Content | Export/import policy |
| --- | --- |
| Imported BIOS | Optional explicit inclusion of user-owned managed BIOS and selected filename; validate hashes and real BIOS metadata before applying selection |
| BIOS sidecars | Preserve matching ROM1/ROM2/MEC/NVM files and names; never replace a newer sidecar without user review |
| Memory cards | Exact byte copies after VM shutdown; preserve format/names and slot mapping; destination collisions require explicit choice |
| Save/resume states | Preserve original filenames, serial/ELF CRC and core version; do not claim compatibility from filename alone; never overwrite a destination state implicitly |
| Global settings | Restore reviewed typed keys via the destination SharedPreferences API; translate the old default preference store to the new package's store, not by blind XML rename |
| Per-game/controller profiles | Preserve serial/CRC filenames and scope/inheritance; keep named profiles, layout, analog mode and skin selection distinct from global settings |
| Custom skins | Optional user-owned private files plus selection; respect each artwork license; do not download automatically during restore |
| Covers | Optional both 2D/3D caches and manual covers; keep style separation and user selections; no artwork publication |
| Library metadata cache | Rebuild for the new UID after SAF reauthorization; retain genuine identities, never mark inaccessible URIs valid |
| Library backgrounds | Optional old background.jpg and selected custom images when the user owns/chooses them; reauthorize any persisted URI; do not claim a gallery setting exists in old exports |
| Games/CHD/ISO | Excluded by default; keep original game files in place and grant the new app access through SAF |
| App signing/credentials | Never transferred or included in the archive |

The new app's managed external root is obtained through Android's own context
(`getExternalFilesDir`), normally `/sdcard/Android/data/com.psx2.glitch/files`.
The old root `/sdcard/Android/data/com.armsx2.hybrid/files` remains untouched.
Do not configure the new app to read/write the old private root.
Internal subdirectory names for BIOS, cards, states, profiles and covers remain
unchanged. Only reviewed path-valued settings may be mapped from exported logical
paths to the destination root; never globally replace strings in every INI.

## SAF authorization

Persisted URI permissions belong to the granting app UID. Copying a URI string
or using the same signing certificate does not transfer those grants.

- Ask the user to select each game root again with ACTION_OPEN_DOCUMENT_TREE.
- Persist only the granted read permission needed by the library, through the
  new app's ContentResolver. Validate actual FD access in the new application UID.
- A document's URI can differ between providers, roots or accounts. Rescan using
  the newly granted roots; match genuine serial/CRC without guessing a new path.
- Reauthorize standalone image/background/skin documents when required.
- Android may prohibit selecting Android/data through SAF; use an explicit
  export from the old app into a normal user-chosen document instead.

## Import safeguards and rollback

A future importer must preview archive contents and require confirmation before
writing. Validate version, logical paths, hashes, ZIP traversal/symlink hazards,
entry counts, expanded sizes and available space. Support cancellation and
transactional staging inside the new app's private storage. Do not promote a
partial archive into active settings. Preserve destination collisions by default
and take a destination snapshot before an approved replacement.

After import, verify file hashes, selected BIOS availability, all ten CHD
identities under newly granted SAF roots, cover styles, settings scopes and
actual boot/save/load with user permission. Keep the old app and export intact
until the user accepts the new app. Rollback means continuing to use the old app;
it never means uninstalling either app, clearing data or deleting original files.

The existing Stage 2 BIOS adapter and Stage 1 metadata/SAF code are not changed
by this plan. Their runtime acceptance is a separate checkpoint.

## Launcher shortcuts

Existing pinned shortcuts belong to com.armsx2.hybrid and stay intact. PSX2 cannot silently adopt or rewrite another package's shortcuts. After approved import and SAF authorization, recreate desired game shortcuts from PSX2; their explicit intents use the new application context, and their long labels use PSX2. Game cover thumbnails remain game artwork; the launcher supplies the new application icon/badge.
