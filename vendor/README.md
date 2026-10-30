<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Vendor source snapshots

This directory contains verified source/translation snapshots, not binary dependencies.

- upstream-java: 13 unchanged Java files recorded in provenance.json; 11 are SDL helpers and two are ARMSX2 helpers.
- remoteplay: 19 unchanged Java/Kotlin backend files, 20 translation JSON files, and the original provenance.json.

All selected snapshot hashes match the original provenance entries. License texts are retained under ../LICENSES; their relocation is recorded in PUBLICATION-MANIFEST.json.

The original Remote Play provenance file is intentionally unchanged. It describes a larger private dependency set, including excluded JAR/SO artifacts and license files now located under LICENSES. Those entries are provenance references, not a statement that those artifacts are present here.

The source snapshots reference ARMSX2 runtime, Kotlin, WebRTC and Android APIs. A complete public runtime build is not provided. No dependency is fetched by the public test runner.
