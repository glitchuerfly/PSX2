// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Observe the legacy Activity without overriding R8-finalized lifecycle methods. */
public final class EmulationLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private boolean isEmulation(Activity activity) {
        String name = activity.getClass().getName();
        return name.equals("xyz.aethersx2.android.EmulationActivity")
            || name.equals("xyz.aethersx2.android.CoreProbeActivity");
    }
    @Override public void onActivityResumed(Activity activity) {
        if (isEmulation(activity)) Bridge.onEmulationResumed(activity);
        else if(activity.getClass().getName().equals("xyz.aethersx2.android.ControllerSettingsActivity"))ControllerPreferences.onResumed(activity);
    }
    @Override public void onActivityPaused(Activity activity) {
        if (isEmulation(activity)) Bridge.onEmulationPaused(activity);
    }
    @Override public void onActivityStopped(Activity activity) {
        if (isEmulation(activity)) Bridge.onEmulationPaused(activity);
    }
    @Override public void onActivityDestroyed(Activity activity) {
        if (isEmulation(activity)){Bridge.onEmulationPaused(activity);GameAudioFocus.forget(activity);}
    }
    @Override public void onActivityCreated(Activity activity, Bundle state) {if(isEmulation(activity))GameAudioFocus.observe(activity); }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
}
