// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.app.*;
import android.content.*;
import android.view.*;
import android.util.Log;

/** Entry points used by legacy Smali; runtime engine is compiled separately from Kotlin. */
public final class RemotePlayMenu {
    private RemotePlayMenu(){}
    public static boolean onNavigation(Activity activity,MenuItem item){
        int id=activity.getResources().getIdentifier("action_remote_play","id",activity.getPackageName());
        if(id==0||item.getItemId()!=id)return false;
        open(activity);return true;
    }
    public static void open(Activity activity){
        activity.startActivity(new Intent().setClassName(activity,"org.local.aetherarms.RemotePlaySettingsActivity"));
    }
    /** Bind the original pause Fragment so no Activity/background/orientation transition occurs. */
    public static void bindPause(Object fragment,View root){
        int id=root.getResources().getIdentifier("pause_remote_play","id",root.getContext().getPackageName());
        View button=root.findViewById(id);
        if(button!=null)button.setOnClickListener(v->{
            if(!root.isAttachedToWindow())return;
            try{fragment.getClass().getMethod("D",int.class,boolean.class).invoke(fragment,6,true);}
            catch(ReflectiveOperationException failure){Log.e("RemotePlay","Cannot open native pause settings",failure);}
        });
    }
    public static void initialize(Context context){call("initialize",new Class<?>[]{Context.class},context);}
    public static void stopSession(){call("stop",new Class<?>[0]);}
    private static void call(String name,Class<?>[] types,Object...args){
        try{Class.forName("com.armsx2.remoteplay.RemoteRuntime").getMethod(name,types).invoke(null,args);}
        catch(ReflectiveOperationException|LinkageError failure){Log.e("RemotePlay","Remote Play lifecycle hook: "+name,failure);}
    }
}
