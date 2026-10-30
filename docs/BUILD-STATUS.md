<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Build status

**The complete hybrid APK cannot be built from this repository.**

This is a selected source export. It is not a Gradle application project, an APK patcher, a native core distribution, or a complete GPL Corresponding Source delivery.

## Supported validation

Run python3 tests/run-public-checks.py with Python 3 and a JDK.

The runner compiles seven included policy classes and four unchanged test classes, then executes:

1. AudioFocusChecks: audio-focus ownership and pause/audio ordering.
2. RemotePadChecks: input buffering, session ownership and disconnect release.
3. SkinChecks: safe archive paths, limits, image filtering and skin policy; community archive fixture deliberately skipped.
4. SkinLoadChecks: actual artwork cache/selection state, stale callbacks and retained-CRC profile/visibility resolution.

On Termux, the runner uses PREFIX/lib for child-process library lookup when PREFIX is available. It creates and removes its temporary class files inside this export. It performs no ADB/device interaction, network access or APK build.

Seven additional Java test files are archived under tests/reference. NativeOsdAbiChecks.cpp is an additional reference-only native ABI check under tests/. Their prerequisites are documented in tests/README.md and are not claimed to pass in this export.

## Deliberately absent prerequisites

- Android application manifest, host resources, decoded classes and Smali patches.
- Legacy libemucore.so and the host's preferred-form native source.
- ARMSX2 core binaries and complete native source/build dependency closure.
- GraphicsSchema.java and PadRoutes.java, with their held configuration inputs.
- DpadGeometry.java and the copied legacy vector geometry.
- Legacy Preference ABI stub and Android UI test fakes.
- Private Remote Play generator/adapted output, Kotlin/WebRTC binaries and complete runtime classpath.
- Runtime artwork, Default controller PNGs, community fixtures, signing material and application data. Existing README visual assets are outside this runtime source/dependency export.

Retained Java/Kotlin packages do not make these omitted prerequisites available.

## Private build versus public export

The private checkpoint 6786e1679de14239d274f909eacec5faaef4890b built and passed its full host checks, and its APK was installed separately. Those results belong to the private project. They do not establish that this public source collection builds an APK.

The public export's own results are recorded in VALIDATION-REPORT.json. Runtime appearance, games, networking and native execution are not tested here.

Any future APK release needs component permissions, complete matching source/build materials, license notices, and a suitable GPL source-access arrangement. A link to this partial export would not meet that need by itself.

## Native OSD and launcher tool sources

native_osd.py, native/core_osd.cpp, config/native-osd.json, check_alignment.py and tests/NativeOsdAbiChecks.cpp are included unchanged from the private project as preferred-form source. The native helper needs an Android/Termux clang toolchain, NDK 26.1 headers/sysroot, the pinned donor libc++ and core files, and the original private layout or equivalent explicit setup. Those binaries/toolchain are not bundled. It cannot build the combined APK. Its private ABI check does not load a core or launch a VM.

app_icon.py additionally uses Pillow for deterministic image resizing and final APK pixel validation. Its canonical private app-assets input layout and the complete APK build pipeline are absent. The original user PNG is published only at .github/assets/app_icon.png under the recorded user authorization, without assigning a general artwork license.

The helper's NDK unwind archive and full toolchain notice are additionally pinned by hash. native/NOTICE.toolchain is copied into the private APK and exposed through its existing Third-Party Notices index. A final notice-only APK rebuild verified those bytes and the 15 icon variants; the JNI adapter bytes remained identical to the fully host-checked build.
