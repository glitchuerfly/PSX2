// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.*;
import android.content.*;
import android.view.*;
import java.lang.reflect.*;
import java.util.*;

/** Reuse Aether's real touchscreen editor with a game data store, without starting a VM. */
public final class ControllerLayoutAccess {
    private static final WeakHashMap<View,Object> editors=new WeakHashMap<>();
    private ControllerLayoutAccess(){}
    public static void bind(View view,Object profile){editors.put(view,profile);}
    /** Install the launch/retained-CRC profile before any layout/visibility getters run. */
    public static void resolveActivity(Activity activity){
        try{
            java.io.File file=Bridge.controllerProfile(activity);
            Object profile=file==null?null:Class.forName("l6.v4").getConstructor(String.class).newInstance(file.getPath());
            activity.getClass().getField("G").set(activity,profile);
        }catch(java.io.IOException failure){
            android.util.Log.e("AetherArmsBridge","Cannot resolve launch controller configuration",failure);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Aether launch controller profile ABI",failure);}
    }
    /** Called on the UI thread after host views exist, before its VM thread starts. */
    public static void prepareLaunch(Activity activity){
        resolveActivity(activity);
        try{activity.getClass().getMethod("D").invoke(activity);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Aether initial controller layout ABI",failure);}
    }
    /** O() may reuse the existing view type; it must still apply changed visibility. */
    public static void refreshActivity(Activity activity){
        try{
            View view=(View)activity.getClass().getField("Q").get(activity);
            if(view!=null){
                view.getClass().getMethod("E").invoke(view);
                view.getClass().getMethod("x").invoke(view);
                SkinStore.controllerConfigured(view);
            }
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Aether controller refresh ABI",failure);}
    }
    public static Object profile(View view){
        if(editors.containsKey(view))return editors.get(view);
        try{Object owner=view.getClass().getField("i").get(view);return owner==null?null:owner.getClass().getField("G").get(owner);}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static Object value(View view,String key,Object fallback){
        Object data=profile(view);
        try{if(data!=null&&(Boolean)data.getClass().getMethod("k",String.class).invoke(data,key)){
            String name=fallback instanceof Boolean?"a":fallback instanceof Integer?"c":"b";
            Class<?> type=fallback instanceof Boolean?boolean.class:fallback instanceof Integer?int.class:float.class;
            return data.getClass().getMethod(name,String.class,type).invoke(data,key,fallback);
        }}catch(ReflectiveOperationException e){throw new IllegalStateException("Aether layout settings ABI",e);}
        Object saved=view.getContext().getSharedPreferences(view.getContext().getPackageName()+"_preferences",0).getAll().get(key);return saved==null?fallback:saved;
    }
    public static String getString(View view,String key,String fallback){
        if(!editors.containsKey(view))try{
            Object owner=view.getClass().getField("i").get(view);
            if(owner!=null)return (String)owner.getClass().getMethod("I",String.class,String.class).invoke(owner,key,fallback);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Aether controller skin layer ABI",failure);}
        Object data=profile(view);
        try{if(data!=null&&(Boolean)data.getClass().getMethod("k",String.class).invoke(data,key))
            return (String)data.getClass().getMethod("d",String.class,String.class).invoke(data,key,fallback);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Aether editor skin layer ABI",failure);}
        return view.getContext().getSharedPreferences(view.getContext().getPackageName()+"_preferences",0).getString(key,fallback);
    }
    public static int getInt(View view,String key,int fallback){return ControllerSettings.integer(value(view,key,fallback),fallback,Integer.MIN_VALUE,Integer.MAX_VALUE);}
    public static float getFloat(View view,String key,float fallback){return ControllerSettings.number(value(view,key,fallback),fallback,-Float.MAX_VALUE,Float.MAX_VALUE);}
    public static boolean getBool(View view,String key,boolean fallback){return ControllerSettings.truth(value(view,key,fallback));}
    public static void pause(View view,boolean paused){if(!editors.containsKey(view))Bridge.pauseVM(paused);}
    public static void finish(View view){if(editors.containsKey(view)&&view.getContext() instanceof ControllerLayoutActivity)((Activity)view.getContext()).finish();}
    public static void installActions(Object prefFragment,Object controllerFragment){
        try{
            Activity activity=(Activity)prefFragment.getClass().getMethod("getActivity").invoke(prefFragment);
            if(activity==null||!activity.getClass().getName().equals("xyz.aethersx2.android.ControllerSettingsActivity"))return;
            Object screen=prefFragment.getClass().getMethod("z").invoke(prefFragment);
            Class<?> group=Class.forName("androidx.preference.PreferenceGroup"),click=Class.forName("androidx.preference.Preference$e");
            String[] actions={"AddRemoveButtons","EditPositions","EditScale","ToggleButtons","ResetLayout"};
            for(String action:actions){
                Object row=group.getMethod("a0",CharSequence.class).invoke(screen,"TouchscreenController/"+action);if(row==null)continue;
                row.getClass().getMethod("J",boolean.class).invoke(row,true);
                Object listener=Proxy.newProxyInstance(click.getClassLoader(),new Class<?>[]{click},(proxy,method,args)->{
                    if(method.getName().equals("b")){
                        Intent intent=new Intent(activity,ControllerLayoutActivity.class).putExtra("action",action).putExtra("skin.ui_locales",activity.getResources().getConfiguration().getLocales().toLanguageTags());
                        boolean perGame=(Boolean)controllerFragment.getClass().getMethod("H").invoke(controllerFragment);
                        Object store=controllerFragment.getClass().getField("l0").get(controllerFragment);
                        if(store!=null){
                            String path=(String)store.getClass().getField("a").get(store);
                            intent.putExtra("controller.game_profile",path);
                            if(perGame)intent.putExtra("controller.profile",path);
                        }
                        ControllerPreferences.editingLayout(activity);activity.startActivity(intent);return true;
                    }
                    if(method.getName().equals("toString"))return "Controller layout action";
                    if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                    if(method.getName().equals("equals"))return proxy==args[0];
                    return null;
                });
                row.getClass().getField("o").set(row,listener);
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether layout preference ABI",e);}
    }
}
