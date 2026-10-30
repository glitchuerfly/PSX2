// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Build;
import android.view.*;

/** Keep the existing pause fragment, with a full-height header inside the display's safe area. */
public final class PauseMenuHeader {
    private PauseMenuHeader(){}
    public static void attach(Activity activity,Dialog dialog,View root){
        if(dialog==null||activity.isFinishing()||activity.isDestroyed())return;
        final int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view,insets)->{
            int l,t,r,b;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                l=safe.left;t=safe.top;r=safe.right;b=safe.bottom;
            }else{
                l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom();
                if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){
                    DisplayCutout cutout=insets.getDisplayCutout();
                    l=Math.max(l,cutout.getSafeInsetLeft());t=Math.max(t,cutout.getSafeInsetTop());
                    r=Math.max(r,cutout.getSafeInsetRight());b=Math.max(b,cutout.getSafeInsetBottom());
                }
            }
            view.setPadding(left+l,top+t,right+r,bottom+b);
            return insets;
        });
        // onViewCreated runs before DialogFragment.onStart/show. Set the window
        // size and immersive flags now, so the first visible frame is already full size.
        Window window=dialog.getWindow();if(window==null)return;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(Build.VERSION.SDK_INT>=28){
            WindowManager.LayoutParams attrs=window.getAttributes();
            attrs.layoutInDisplayCutoutMode=Build.VERSION.SDK_INT>=30
                ?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                :WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attrs);
        }
        window.setStatusBarColor(Color.TRANSPARENT);window.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29){window.setStatusBarContrastEnforced(false);window.setNavigationBarContrastEnforced(false);}
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN
            |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        if(Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false);WindowInsetsController control=window.getInsetsController();
            if(control!=null){control.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);control.hide(WindowInsets.Type.systemBars());}
        }
        root.requestApplyInsets();
    }
}
