// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.content.Context;

/** Resolve against the caller's UI resources; unqualified resources fall back to English. */
public final class SkinText {
    private SkinText() {}
    public static String get(Context context,String key,Object... args) {
        int id=context.getResources().getIdentifier("skin_"+key,"string",context.getPackageName());
        if(id==0)throw new IllegalArgumentException("Missing skin string: "+key);
        return args.length==0?context.getString(id):context.getString(id,args);
    }
}
