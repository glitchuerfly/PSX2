# PSX2 Background Gallery — separate candidate 15280

This library-only UI extends the existing sidebar **Change Background** entry.
It is a separate source checkpoint/APK from branding-only candidate15279 and
from the installed Stage 2 BIOS APK15278. No new APK is installed automatically.
BIOS migration, core/JNI, library metadata/SAF, covers, controls and Remote Play
behavior are unchanged. It never applies artwork to gameplay or in-game menus.

| Choice | Behavior | Source/rights |
| --- | --- | --- |
| Default (Follow App Theme) | Use the existing app theme surface; follow system/light/dark preferences | Original integration |
| Classic Blue | Original angular blue vector backdrop inspired by OPL's palette, with no copied OPL/Sony assets | app-assets/backgrounds/classic_blue.xml, GPL-3.0-or-later |
| Blue Waves (ARMSX2) | Listed but disabled until artwork distribution rights can be verified; no substitute/fake preview | Official-source asset located; not bundled/recreated |
| Custom Image | User selects a readable image with SAF; bounded decoded preview and read permission persisted only after Apply | User-chosen content, never bundled or uploaded |

There is no Dark Minimal preset; app Dark Theme covers that purpose.

## Draft, preview and persistence

The gallery uses a separate private Activity with app language and light/dark/
system theme context selected before views are created. Previewing a preset or
selecting an image changes only an in-memory draft. Cancel/Back discard it.
Only Apply persists mode/URI in the versioned psx2_library_background_v1 store.
A failed disk commit attempts restoration of the previous in-memory selection;
a newly acquired URI grant is released on commit failure. Existing grants are
not revoked, since other features may share them.

Images are opened read-only and bounded to 16MiB encoded input; decode dimensions
are sampled to at most2048 pixels per edge. IO and decode run off the main thread.
Obsolete preview callbacks are rejected; closing the Activity cancels queued work
and disposes its preview. Images are not copied into another storage directory.
Apply is unavailable while decoding or on an unreadable/invalid selection.
A theme-colored scrim keeps library text readable in both light and dark themes.

An already imported legacy background.jpg in this app's own data root is honored
when no gallery choice exists. Selecting Default never deletes that file. The
new package does not read the old app's storage; any eventual old background
transfer is part of the separate user-approved migration plan.

## Blue Waves investigation

Existing source checkout: ../armsx2-custom at ba9d8460a56889180b999c52131c48fb5b4ab77a.
Real files found:

- platforms/android/app/src/main/res/drawable-nodpi/library_bg_xmb.jpg
- platforms/android/app/src/main/java/com/armsx2/ui/home/HomeScreen.kt
- platforms/android/app/src/main/java/com/armsx2/ui/home/LibraryBackground.kt
- platforms/android/app/src/main/java/com/armsx2/ui/home/LibraryWaveBackground.kt
- COPYING.GPLv3

HomeScreen references the JPEG as its still backdrop under XmbGlView. The other
wave implementation is a procedural fallback; it is not evidence of the JPEG's
artwork authorship. The root GPL text is present, but no asset-specific copyright,
author/source or explicit artwork grant was established in the inspected files
and available shallow history. This is conservatively category C pending scope/
provenance verification, not a conclusion that upstream distribution is unlawful.
No JPEG or adapted copy has been included, and the same image is not recreated.
Hashes and exact source evidence are in config/background-gallery-provenance.json.

If adequate permission/provenance becomes available, use the exact upstream asset
with required notices; review enabling the Blue Waves option separately. Do not
replace the disabled option with invented metadata or claim a root-source license
automatically proves every third-party artwork's rights.

## Validation and limits

Host tests exercise the production draft/commit/cancel policy, store failure,
restart, valid URI schemes and the conditional Blue Waves gate. Static checks
cover private Activity/SAF grants, read bounds, locale/theme initialization,
MainActivity high-register result hooks and unchanged Stage 2 BIOS sources.
The APK is built/checked for manifest, new classes/resources, original native
hashes, signer, package and retained migrated metadata/BIOS routes. The complete
existing host suite is run. These do not prove UI runtime or provider behavior.

Device acceptance remains pending: preview each allowed preset, Cancel/reopen,
Apply/restart, custom image/provider revocation/oversize/cancel, rotation, and
system/light/dark theme changes. Confirm covers/grid navigation and no game/menu
appearance change. Installation requires explicit approval. There is no push,
release or migration beyond the already authorized stages.
