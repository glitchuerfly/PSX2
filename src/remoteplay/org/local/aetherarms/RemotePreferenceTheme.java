// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.content.Context;
import android.util.TypedValue;

/** Mirrors the retained PreferenceFragmentCompat.onCreate theme initialization. */
public final class RemotePreferenceTheme {
    private RemotePreferenceTheme(){}

    public static void prepare(Context context){
        int attribute=context.getResources().getIdentifier("preferenceTheme","attr",context.getPackageName());
        TypedValue selected=new TypedValue();
        context.getTheme().resolveAttribute(attribute,selected,true);
        int overlay=selected.resourceId;
        if(overlay==0)overlay=context.getResources().getIdentifier("PreferenceThemeOverlay","style",context.getPackageName());
        if(overlay==0)throw new IllegalStateException("Missing Aether preference theme overlay");
        // Fill missing preference attributes without replacing the Activity's colors/window theme.
        context.getTheme().applyStyle(overlay,false);
    }
}
