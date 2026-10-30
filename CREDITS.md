<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Credits and copyright scope

> Local asset review update: .github/assets now contains screenshots and a user-requested app_icon.png outside the audited source allowlist. Their redistribution rights remain unverified; they are not covered by the source GPL declaration. This staging folder is not publication-cleared and no ZIP is produced. See .github/assets/ASSET-REVIEW.json and VALIDATION-REPORT.json.

## Hybrid integration

The private project uses the credit “patches by glitch” for local hybrid work. Included adapter source contains new integration and adaptations developed for that project. This credit does not assert ownership over ARMSX2, SDL, AetherSX2, NetherSX2, WebRTC, Kotlin, skins, or game artwork.

Adapter GPL-3.0-or-later scope is preserved verbatim in LICENSES/ADAPTER-LICENSE.txt. Source SPDX headers are retained. PUBLICATION-MANIFEST.json records source paths and available commit/change dates. Documentation describes changes relative to upstream; copied source is not changed by this export.

AI-assisted creation or modification does not remove obligations attached to third-party material.

## ARMSX2 and PCSX2

ARMSX2 is based on PCSX2. Included vendor source is copied from the pinned ARMSX2 snapshot `ba9d8460a56889180b999c52131c48fb5b4ab77a`; original copyright and SPDX headers, including “2026 ARMSX2 Contributors” where present, remain intact.

- Source: https://github.com/ARMSX2/ARMSX2
- Snapshot: https://github.com/ARMSX2/ARMSX2/tree/ba9d8460a56889180b999c52131c48fb5b4ab77a
- GPL text: LICENSES/ARMSX2-COPYING.GPLv3
- Source inventories: vendor/provenance.json and vendor/remoteplay/provenance.json

Upstream files are source snapshots, not claimed as original hybrid code.

## SDL

The 11 Java helpers in vendor/upstream-java/org/libsdl/app retain the SDL zlib license and original source content. LICENSES/SDL-LICENSE.txt retains Sam Lantinga's copyright notice and redistribution conditions.

## Referenced dependencies, not bundled binaries

- Kotlin standard library 2.2.10: Apache-2.0; LICENSES/LICENSE-kotlin.txt.
- Stream WebRTC Android 1.3.10: wrapper licensing is recorded in LICENSES/LICENSE-stream.txt.
- WebRTC: BSD notice in LICENSES/LICENSE-webrtc.txt; native/transitive dependency review remains separate.

No Kotlin/WebRTC JAR, AAR, or native library is distributed in this folder. These license texts are retained for source/dependency context and do not imply every transitive binary obligation has been resolved.

## Legacy lineage and artwork

AetherSX2 and NetherSX2 are named to describe the private project's lineage. Their decoded application code, Smali, resources, host binary, and artwork are not redistributed here. NetherSX2-patch's Unlicense does not relicense AetherSX2.

The cover service and skin catalog are referenced by source code, but no game covers, skins, avatar, screenshots, BIOS, or game data are bundled. Availability in a public repository is not itself an artwork redistribution grant.

## Names and logos

This is an unofficial integration source archive. No trademark permission, endorsement, or right to redistribute third-party logos is asserted.

## Controller startup update

The later hybrid source update adds original controller-store selection, configuration acknowledgement and artwork-loading state/regression logic under the retained GPL-3.0-or-later declarations. ControllerBindingIcons contains resource identifiers and original label rendering; the upstream Default artwork is not included. These source changes do not grant rights to legacy app assets or existing README screenshots/icon.
