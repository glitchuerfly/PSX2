<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# Public test scope

The Java test sources are unchanged copies of audit-B files. run-public-checks.py is a new GPL-3.0-or-later runner for this layout.

Runnable with the included policies and a JDK:

- AudioFocusChecks.java
- RemotePadChecks.java
- SkinChecks.java — its missing community ZIP argument points to an intentionally nonexistent temporary path, so no external artwork fixture is read.

Reference-only sources retain their original tests but are not executed:

| File | Missing prerequisites |
| --- | --- |
| ControllerChecks.java | Complete graphics/model dependency chain |
| ControllerDialogChecks.java | Held Android lifecycle UI fakes and Android API |
| DpadChecks.java | Held DpadGeometry |
| FullControllerChecks.java | Held PadRoutes and complete model dependency chain |
| ModelChecks.java | Held GraphicsSchema and complete configuration model |
| RemotePlayChecks.java | Compiled adapted Kotlin backend and external dependencies |
| TouchChecks.java | Held DpadGeometry |

The private full-suite Python runners are not copied because they require the private layout, decoded resources, and excluded build artifacts.

Checks do not validate Android rendering, app lifecycle on a phone, a complete emulator build, native execution, network connectivity, gameplay, or licensing legality.
