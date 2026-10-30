// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Map;

/** Small in-game acknowledgement. The pinned donor has no public JNI OSD-message API. */
public final class ControllerLoadNotice {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static long generation;
    private static boolean loaded,shown;
    private static WeakReference<Activity> owner=new WeakReference<>(null);
    private static TextView notice;
    private ControllerLoadNotice(){}
    public static synchronized long session(){return generation;}
    public static synchronized void beginSession(){generation++;loaded=false;shown=false;hide();}
    public static synchronized void endSession(){generation++;loaded=false;shown=false;hide();}
    public static synchronized void applied(long token,boolean success){if(token==generation)loaded=success;}
    public static void attach(Activity activity){owner=new WeakReference<>(activity);}
    public static void hide(){MAIN.post(ControllerLoadNotice::remove);}
    private static void remove(){
        if(notice!=null){notice.animate().cancel();if(notice.getParent() instanceof ViewGroup)((ViewGroup)notice.getParent()).removeView(notice);notice=null;}
    }
    public static void presenting(){
        final long token=session();
        MAIN.post(()->{
            Activity activity=owner.get();
            if(activity==null||activity.isFinishing()||activity.isDestroyed()||!Bridge.controllerNoticeVisible()||!SkinStore.ready())return;
            synchronized(ControllerLoadNotice.class){if(token!=generation||!loaded||shown)return;shown=true;}
            Map<String,?> settings=activity.getSharedPreferences(activity.getPackageName()+"_preferences",0).getAll();
            Object enabled=settings.get("EmuCore/GS/OsdShowMessages");
            if(enabled!=null&&!ControllerSettings.truth(enabled))return;
            View root=activity.getWindow().getDecorView();if(!(root instanceof FrameLayout))return;
            remove();
            float density=activity.getResources().getDisplayMetrics().density;
            float scale=Math.max(.5f,ControllerSettings.number(settings.get("EmuCore/GS/osd_scale"),100,10,500)*.0065f);
            TextView text=new TextView(activity);notice=text;
            boolean indonesian=activity.getResources().getConfiguration().getLocales().get(0).getLanguage().matches("id|in");
            text.setText(indonesian?"Konfigurasi controller dimuat":"Controller configuration loaded");
            text.setTextColor(Color.WHITE);text.setTextSize(TypedValue.COMPLEX_UNIT_PX,18*density*scale);
            int pad=Math.max(1,Math.round(8*density*scale));text.setPadding(pad,pad,pad,pad);
            text.setIncludeFontPadding(false);text.setClickable(false);text.setFocusable(false);
            text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            GradientDrawable bg=new GradientDrawable();bg.setColor(0xff212121);bg.setCornerRadius(5*density*scale);bg.setStroke(1,0xff484848);text.setBackground(bg);
            FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
            params.topMargin=Math.round(10*density*scale);
            WindowInsets insets=root.getRootWindowInsets();
            if(insets!=null&&android.os.Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null)params.topMargin+=insets.getDisplayCutout().getSafeInsetTop();
            text.setMaxWidth(Math.max(1,root.getWidth()-pad*4));
            ((FrameLayout)root).addView(text,params);
            text.setAlpha(0);text.animate().alpha(1).setDuration(150).start();
            MAIN.postDelayed(()->{
                if(notice!=text)return;
                text.animate().alpha(0).setDuration(250).withEndAction(()->{if(notice==text)remove();}).start();
            },2500);
        });
    }
}
