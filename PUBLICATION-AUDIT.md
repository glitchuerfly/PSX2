<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Publication audit — selected source export

> Local asset review update: .github/assets now contains screenshots and a user-requested app_icon.png outside the audited source allowlist. Their redistribution rights remain unverified; they are not covered by the source GPL declaration. This staging folder is not publication-cleared and no ZIP is produced. See .github/assets/ASSET-REVIEW.json and VALIDATION-REPORT.json.

Prepared on 2026-10-30 from private project commit f0276fa2804f9174492b69f7cebbcf4c338bcc86.

**This is a partial source collection for publication review. It is not a legal clearance, an APK release, or complete Corresponding Source for the hybrid APK. User approval is required before any upload.**

## Selection

The export contains 131 unchanged copies of audited files: 3 category A and 128 category B. This includes 51 adapter Java files, 8 hybrid Kotlin files, one diagnostics source, 10 Java tests, 52 vendor source/translation files, two vendor provenance records, and seven license/declaration files.

New documentation, the public test runner, provenance mapping, validation report, and checksum inventory are created only in staging. They are not presented as recovered application source.

Every selected original has a path, audit category, license basis, source hash, and available last-change commit/date in PUBLICATION-MANIFEST.json. All 59 tracked private files not selected are listed with reasons in docs/EXCLUDED-FILES.md.

The included GPL source retains its notices. Vendor content is unchanged, its hashes are checked, original copyright headers are preserved, and full relevant license texts are included. CREDITS.md and CHANGELOG.md identify the hybrid integration and export changes rather than claiming upstream work as original.

## Relationship to the earlier workspace audit

The earlier audit covered aether-armsx2-port/, armsx2-custom/, armsx2-analysis/, and backups/. At that snapshot there were 42,805 physical files: A 52, B 8,020, C 33,849, D 884. These are historical workspace counts, not current staging counts or a claim that every line was manually reviewed.

Of 190 tracked hybrid files, the earlier audit classified A 15, B 144, C 31. All 31 C files remain excluded. Twelve optional A files and sixteen B files are also omitted. Two of those B files are conservatively held because their preferred inputs remain C:

- src/org/local/aetherarms/GraphicsSchema.java derives from config/graphics-mapping.json.
- src/org/local/aetherarms/PadRoutes.java derives from config/controller-pad-routes.json.

A GPL header on generated output is not used to bypass unresolved provenance of its input. Those inputs and outputs need review before inclusion. The earlier audit is not silently rewritten to call them cleared.

## Evidence determining exclusions

The following are references to private workspace evidence, not files bundled in this export:

- ADAPTER-LICENSE.txt explicitly excludes relicensing the original AetherSX2/NetherSX2 APK, Smali, native library, and artwork. Its complete original text is included under LICENSES.
- inputs/legacy/decoded/assets/3rdparty.html contains CC BY-NC-ND wording and incomplete license placeholders. It does not establish an authentic complete copyright/license chain for each legacy file.
- build.py copies the decoded legacy tree and patches the legacy native host. That script also contains signing-password arguments and is not copied.
- reports/legacy-host-patch.json records modifications to the legacy host while official core binaries remain unchanged.
- DpadGeometry.java copies legacy vector geometry; its source header does not establish a grant for the underlying artwork.
- RemoteTabsFragment.smali includes a method body identical to a legacy settings method. New class names do not establish independent authorship.
- app-assets/about/profile.jpg has user-supplied provenance, but no verified artwork redistribution grant.
- A private backup was confirmed to contain the local signing key. All backups and original Git databases are excluded.

No legacy decoded file, resource, artwork, library, APK, build log, signing material, backup, or private Git database is copied. References to host class/resource names in the independently selected adapter source do not include those implementations.

## License scope

- Adapter Java/Kotlin and project tests: GPL-3.0-or-later where granted by retained headers or the original adapter declaration.
- Diagnostics: GPL-3.0-or-later under the adapter declaration.
- ARMSX2 vendor source/translations: the upstream GPL grant and individual source notices apply; many headers specify GPL-3.0+.
- SDL Java helpers: zlib, with the full original notice.
- New staging documentation and runner: GPL-3.0-or-later.
- Apache/BSD dependency texts are preserved for referenced Kotlin/Stream/WebRTC components. Their binaries are absent, and these texts are not a complete native dependency clearance.

The root GPL text does not replace third-party licenses or grant rights to excluded legacy components. AI-assisted work retains obligations attached to reused material.

## GPL and any future APK

This export is insufficient by itself as Corresponding Source for an APK that combines the private host, adapters, core, and their dependencies.

Before conveying such an APK, establish the applicable scope of the combined work, obtain compatible rights for every component, and arrange access to the complete matching source and required interface/build/install materials. JNI or dynamic linking does not automatically establish an exemption.

The preferred native host source is not recovered by the available Smali/disassembly. A source homepage or a link to this partial collection does not resolve that absence.

Some ordinary system libraries and general build tools may fall outside the source delivery requirement; that must be assessed from the actual work and GPL definitions, rather than assuming every tool or Android SDK file must be bundled.

Any requirement for installation information must be assessed under the relevant GPL provisions. Do not publish a private signing key as a shortcut.

A patcher can reduce distribution of legacy binaries, but its included code/data still requires rights. Distributing a patched APK needs its own analysis. NetherSX2-patch's Unlicense applies to material actually covered by that grant; it does not grant rights over all AetherSX2 material.

Primary references already used in the workspace audit:

- GPL text: [LICENSE](LICENSE).
- ARMSX2 license: https://github.com/ARMSX2/ARMSX2/blob/master/COPYING.GPLv3
- CC BY-NC-ND: https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode.en
- NetherSX2-patch license: https://github.com/Trixarian/NetherSX2-patch/blob/main/LICENSE
- Practical compliance guidance: https://softwarefreedom.org/resources/2014/SFLC-Guide_to_GPL_Compliance_2d_ed.html

## External assets and dependencies

No game covers, controller skins, avatar, logos, screenshots, BIOS, game dumps, native libraries, JARs, or AARs are included. URLs in downloader source describe service endpoints, not ownership of the material served.

The absent runtime dependencies and reference-only test prerequisites are listed in docs/BUILD-STATUS.md and tests/README.md. Source, binary redistribution, artwork, and name/logo rights are separate questions.

## Validation and limits

VALIDATION-REPORT.json records the selected-file/hash checks, package/layout checks, vendor provenance checks, secret-pattern scan, forbidden-file checks, Markdown links, host test outcome, and original-workspace integrity comparison. SHA256SUMS covers every final file except itself.

The three active host suites pass. They do not establish Android UI behavior, gameplay, native runtime compatibility, networking, or legal compliance. Seven other test sources are reference-only.

Security scanning looks for identified patterns and credential assignments; it is not a proof that every possible secret or rights issue can be detected automatically. No actual credential values are copied into documentation as audit examples.

The archive is created only after validation succeeds. No Git initialization, private history import, push, release, upload, or phone modification is part of this export.

## Review decision

The selected source is prepared for human publication review on the evidence described above. No known C/D file is admitted to the selected export and no credential finding is accepted.

Approval to upload has not been given. Whole-APK redistribution, mixed GPL/legacy scope, reconstructed interfaces, artwork, and trademarks still need separate permission/provenance or legal review where applicable. This report does not certify that a future distribution is legal.
