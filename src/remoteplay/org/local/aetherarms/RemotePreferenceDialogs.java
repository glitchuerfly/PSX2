// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

/** The same retained AppCompat builder and single-choice list used by Aether preferences. */
public final class RemotePreferenceDialogs {
    private RemotePreferenceDialogs(){}
    public static Dialog choice(Context context,CharSequence title,CharSequence[] items,int checked,DialogInterface.OnClickListener listener){
        try {
            Class<?> type=Class.forName("androidx.appcompat.app.d$a");
            Object builder=type.getConstructor(Context.class).newInstance(context);
            Object params=type.getField("a").get(builder);
            params.getClass().getField("d").set(params,title);
            type.getMethod("i",CharSequence[].class,int.class,DialogInterface.OnClickListener.class).invoke(builder,items,checked,listener);
            type.getMethod("e",int.class,DialogInterface.OnClickListener.class).invoke(builder,android.R.string.cancel,null);
            Dialog dialog=(Dialog)type.getMethod("a").invoke(builder);
            dialog.show();return dialog;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Aether preference dialog ABI changed",failure);}
    }
}
