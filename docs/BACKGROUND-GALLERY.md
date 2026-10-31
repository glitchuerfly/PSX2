<!-- SPDX-License-Identifier: GPL-3.0-or-later -->
# PSX2 Background Gallery — private build 15284

This document describes the current private app. The newer Gallery implementation
is not included in this historical partial source snapshot, and this repository
cannot build the complete APK. The existing Change Background sidebar entry opens
a compact native dialog over Game Library, with preview and standard Cancel/Apply.

| Choice | Current behavior |
| --- | --- |
| Default (Follow App Theme) | Uses the existing Follow System/Light/Dark theme surface. |
| Classic Plasma | Original procedural blue shading, with broad soft variation and a static fallback. |
| Custom Image | Bounded read-only SAF image preview; user images are not bundled or uploaded. |

Classic Plasma was previously displayed as Classic Blue. The internal saved
CLASSIC_BLUE identifier is retained, so existing choices do not reset.
There is no separate Dark Minimal option; the app theme covers that use case.
Blue Waves artwork is not bundled and is not an offered choice.

## Classic Plasma

The original Android renderer uses a reusable48x48 bitmap with a30FPS target.
Build15284 broadens primary wavelengths1.75times around the same field center,
reduces secondary detail frequency from1.9to1.35 and its weight from14%to4%.
The blue palette and temporal rate remain unchanged. Library cover sizes/layout
and the existing lifecycle gates are preserved. Animation runs only in the visible,
focused, resumed library and stops on background/focus loss/detach/destroy; the
Gallery uses a still preview. A static fallback remains available.

Reference research distinguished OPL0.9.2's procedural Perlin plasma from the
modern default theme, which selects a static PNG. Relevant upstream functions are
src/gui.c:guiCalcPerlin/guiDrawBGPlasma, src/themes.c:initBackground and
src/renderman.c:rmEndFrame. PSX2's independently written scalar-value renderer
does not copy OPL code, gradient/permutation tables, artwork or community themes.
OPL reference: https://github.com/ps2homebrew/Open-PS2-Loader/tree/da8e3c24651f2f36ce6ce42ee928d8a103f3debd

## Custom Image and persistence

The first Custom Image selection immediately opens the system image picker if
no image URI exists. Once an image is selected, switching to another preset and
back reuses the draft image without launching the picker again. A previously
applied image is also reused when the dialog is reopened. Choose Image explicitly
opens the picker to replace it; cancelling replacement keeps the prior image.

Only Apply persists the chosen mode/URI and acquires a required persistable read
grant. Cancel/Back discard the draft. Images are opened read-only, limited to16MiB
encoded input, and sampled to at most2048pixels per edge. Worker cancellation and
stale-result guards keep previews bounded. No user image is copied or deleted.
Existing saved preferences and the app's legacy background.jpg handling are kept.
A revoked/unreadable image shows an error and the Choose Image action.

The app does not automatically read another package's private files or SAF grants.
Any old-package data transfer remains subject to the separate migration plan.

## Scope and verification

Private host tests exercise the production field/clock and draft/picker policy;
Android lifecycle simulations are explicitly host-only. Private APK validation
confirms package/signature, icon/fallback resources, native equality and unchanged
cover/library resources. Installing15284 does not establish full UI/performance
acceptance. Actual picker behavior, rotation, visual smoothness, GPU/memory/frame
pacing and gameplay remain device-test concerns, not claims made by this export.

The earlier Blue Waves review located an ARMSX2 JPEG but did not establish its
asset-specific provenance/license scope. That image is not copied or recreated.
Existing license, provenance, audit and asset-review records are preserved.
