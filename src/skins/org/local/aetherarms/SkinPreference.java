// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.content.Context;
import android.util.AttributeSet;

/** Uses the actual legacy AndroidX Preference row, typography and theme. Obfuscated ABI is checked by build.py. */
public final class SkinPreference extends androidx.preference.Preference {
    public SkinPreference(Context context,AttributeSet attrs) {super(context,attrs);SkinStore.register(this);}
    @Override public CharSequence o(){return SkinText.get(i,"summary",SkinStore.label(i,ControllerPreferences.selectedSkin(this)));}
    @Override public void x(){ControllerPreferences.beginSkin(this);SkinUi.open(i);}
}
