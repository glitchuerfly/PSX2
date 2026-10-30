// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.content.Context;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.util.*;

/** Android output only; game rumble never changes the separate touchscreen press-haptic option. */
public final class ControllerRumble {
    private static RumbleController controller;
    private ControllerRumble(){}
    public static synchronized void initialize(Context app) {
        if(controller!=null)return;
        final Vibrator vibrator=(Vibrator)app.getSystemService(Context.VIBRATOR_SERVICE);
        final Handler handler=new Handler(Looper.getMainLooper());
        controller=new RumbleController(new RumbleController.Output() {
            public void vibrate(int amplitude) {
                try {
                    // Match the donor's bounded one-shot: callbacks arrive on motor changes,
                    // not every frame. Motor scales were already applied by the native pad.
                    if(vibrator!=null&&vibrator.hasVibrator())
                        vibrator.vibrate(VibrationEffect.createOneShot(3000,amplitude));
                } catch(RuntimeException failure) { Log.w("AetherArmsBridge","Game vibration unavailable",failure); }
            }
            public void cancel() {
                try { if(vibrator!=null)vibrator.cancel(); }
                catch(RuntimeException failure) { Log.w("AetherArmsBridge","Cannot cancel game vibration",failure); }
            }
        },new RumbleController.Scheduler() {
            public long now() { return SystemClock.uptimeMillis(); }
            public void postDelayed(Runnable task,long delay) { handler.postDelayed(task,delay); }
            public void remove(Runnable task) { handler.removeCallbacks(task); }
        });
    }
    public static synchronized void refresh(File root,int crc,Map<String,?> global) {
        if(controller==null)return;
        try {
            Map<String,Object> effective=ControllerSettings.effective(root,crc,global);
            // An explicit phone-motor binding uses the shared device router. Avoid
            // running the independent touchscreen fallback against the same motor.
            if(ControllerSettings.explicitPhoneMotor(effective))effective.put("TouchscreenController/EnableVibration",false);
            controller.configure(new RumbleSettings(effective));
        }
        catch(IOException|RuntimeException failure) {
            // Do not start vibration when the effective settings cannot be read safely.
            controller.configure(new RumbleSettings(Collections.emptyMap()));
            Log.w("AetherArmsBridge","Cannot read controller vibration settings",failure);
        }
    }
    public static synchronized void setActive(boolean active) { if(controller!=null)controller.setActive(active); }
    public static synchronized void motors(int port,int large,int small) { if(controller!=null)controller.motors(port,large,small); }
}
