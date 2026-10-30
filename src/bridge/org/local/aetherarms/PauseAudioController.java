// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

/** Orders the donor's audio flags independently of deduplicated VM pause commands.
 * Menu pauses keep the device warm (silence on underrun). Background/surface loss
 * releases it. Suppression stays set until the native resume callback lands. */
public final class PauseAudioController {
    public interface Output {
        void setSuppressed(boolean suppressed);
        void setBackgrounded(boolean backgrounded);
    }
    private final Output output;
    private Boolean suppressed,backgrounded;
    public PauseAudioController(Output output) { this.output=output; }
    public void reset() { suppressed=null;backgrounded=null; }
    public void update(boolean foregroundReady,boolean menuPaused,boolean vmPaused) {
        // Also arm on a background/surface return while the VM is still paused.
        // Clearing background must reopen output BEFORE queueing the VM resume.
        boolean keepWarm=foregroundReady && (menuPaused || vmPaused);
        // Suppress first: returning to a still-open menu must start the stream warm,
        // rather than briefly applying an ordinary audio pause before suppression.
        if (suppressed==null || suppressed.booleanValue()!=keepWarm) {
            output.setSuppressed(keepWarm);
            suppressed=keepWarm;
        }
        boolean background=!foregroundReady;
        if (backgrounded==null || backgrounded.booleanValue()!=background) {
            output.setBackgrounded(background);
            backgrounded=background;
        }
    }
}
