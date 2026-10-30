<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Build status

**The complete hybrid APK cannot be built from this repository.**

This is a selected source export. It is not a Gradle application project, an APK patcher, a native core distribution, or a complete GPL Corresponding Source delivery.

## Supported validation

Run python3 tests/run-public-checks.py with Python 3 and a JDK.

The runner compiles five included policy classes and three unchanged test classes, then executes:

1. AudioFocusChecks: audio-focus ownership and pause/audio ordering.
2. RemotePadChecks: input buffering, session ownership and disconnect release.
3. SkinChecks: safe archive paths, limits, image filtering and skin policy; community archive fixture deliberately skipped.

On Termux, the runner uses PREFIX/lib for child-process library lookup when PREFIX is available. It creates and removes its temporary class files inside this export. It performs no ADB/device interaction, network access or APK build.

Seven additional test files are archived under tests/reference. Their prerequisites are documented in tests/README.md and are not claimed to pass in this export.

## Deliberately absent prerequisites

- Android application manifest, host resources, decoded classes and Smali patches.
- Legacy libemucore.so and the host's preferred-form native source.
- ARMSX2 core binaries and complete native source/build dependency closure.
- GraphicsSchema.java and PadRoutes.java, with their held configuration inputs.
- DpadGeometry.java and the copied legacy vector geometry.
- Legacy Preference ABI stub and Android UI test fakes.
- Private Remote Play generator/adapted output, Kotlin/WebRTC binaries and complete runtime classpath.
- Artwork, community fixtures, signing material and application data.

Retained Java/Kotlin packages do not make these omitted prerequisites available.

## Private build versus public export

The private commit f0276fa built and passed its existing host checks before export, and its APK was installed separately. Those results belong to the private project. They do not establish that this public source collection builds an APK.

The public export's own results are recorded in VALIDATION-REPORT.json. Runtime appearance, games, networking and native execution are not tested here.

Any future APK release needs component permissions, complete matching source/build materials, license notices, and a suitable GPL source-access arrangement. A link to this partial export would not meet that need by itself.
