// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.ref.WeakReference;

/** One per-game configuration acknowledgement, rendered alongside core patch messages. */
public final class ControllerLoadNotice {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static long generation;
    private static boolean loaded,shown;
    private static WeakReference<Activity> owner=new WeakReference<>(null);
    private ControllerLoadNotice(){}
    public static synchronized long session(){return generation;}
    public static synchronized void beginSession(){generation++;loaded=false;shown=false;}
    public static synchronized void endSession(){generation++;loaded=false;shown=false;}
    public static synchronized void applied(long token,boolean success){if(token==generation)loaded=success;}
    public static void attach(Activity activity){owner=new WeakReference<>(activity);}
    /** Core rendering/window visibility owns already queued messages; there is no Android overlay. */
    public static void hide(){}
    public static void presenting(){
        final long token;
        synchronized(ControllerLoadNotice.class){if(!loaded||shown)return;token=generation;}
        MAIN.post(()->{
            Activity activity=owner.get();
            if(activity==null||activity.isFinishing()||activity.isDestroyed()||!Bridge.controllerNoticeVisible()||!SkinStore.ready())return;
            synchronized(ControllerLoadNotice.class){if(token!=generation||!loaded||shown)return;shown=true;}
            boolean indonesian=activity.getResources().getConfiguration().getLocales().get(0).getLanguage().matches("id|in");
            try{
                if(!CoreOsd.post("\uf11b  "+(indonesian?"Konfigurasi controller dimuat":"Controller configuration loaded")))
                    Log.w("AetherArmsBridge","Controller OSD binding unavailable; no Android overlay fallback");
            }catch(LinkageError failure){Log.e("AetherArmsBridge","Controller OSD adapter could not load",failure);}
        });
    }
}
