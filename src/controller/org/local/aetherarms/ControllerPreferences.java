// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.content.*;
import android.app.Activity;
import java.util.WeakHashMap;
import java.lang.reflect.*;

/** Additional controls use Aether's existing PreferenceDataStore when editing a game. */
public final class ControllerPreferences {
    private static final String SKIN="TouchscreenController/CustomSkin";
    private static Object skinStore;
    private static Context preferenceContext;
    private static final WeakHashMap<Activity,Boolean> editedLayouts=new WeakHashMap<>();
    public static void editingLayout(Activity activity){editedLayouts.put(activity,true);}
    public static void onResumed(Activity activity){
        if(editedLayouts.remove(activity)==null)return;
        try{
            Object fragment=activity.getClass().getField("E").get(activity);
            Object data=fragment.getClass().getField("l0").get(fragment);
            if(data!=null)data.getClass().getMethod("p").invoke(data);
            if(!activity.isFinishing()&&!activity.isDestroyed())activity.recreate();
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot refresh edited controller layout",e);}
    }
    private ControllerPreferences(){}
    private static Context appContext(){return Bridge.context()!=null?Bridge.context():preferenceContext;}
    private static Object globalValue(String key){Context app=appContext();return app==null?null:app.getSharedPreferences(app.getPackageName()+"_preferences",0).getAll().get(key);}
    public static boolean inherit(Object store,String key){
        if(!ControllerSettings.owns(key)||key.equals("Pad/InputProfileName")||key.equals("Pad/UseGameSettingsForController")||key.equals("Pad/GameSettingsInitialized")||key.equals("EmuCore/InputProfileName")||globalValue(key)==null)return false;
        try{return !(Boolean)store.getClass().getMethod("k",String.class).invoke(store,key);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Aether inherited controller setting ABI",e);}
    }
    public static boolean inheritedBool(String key,boolean fallback){Object value=globalValue(key);return value==null?fallback:ControllerSettings.truth(value);}
    public static int inheritedInt(String key,int fallback){return ControllerSettings.integer(globalValue(key),fallback,Integer.MIN_VALUE,Integer.MAX_VALUE);}
    public static float inheritedFloat(String key,float fallback){return ControllerSettings.number(globalValue(key),fallback,-Float.MAX_VALUE,Float.MAX_VALUE);}
    public static String inheritedString(String key,String fallback){Object value=globalValue(key);return value==null?fallback:String.valueOf(value);}
    public static java.util.Set<String> inheritedSet(String key,java.util.Set<String> fallback){Object value=globalValue(key);return value==null?fallback:new java.util.LinkedHashSet<>(ControllerSettings.bindings(value));}
    public static void reset(Context context){
        context.getSharedPreferences(context.getPackageName()+"_preferences",Context.MODE_PRIVATE).edit()
            .putString("TouchscreenController/AnalogMode","dynamic").putString(SKIN,"default")
            .putBoolean("TouchscreenController/EnableVibration",false).putInt("AndroidInputSource/VibrationThrottle",10)
            .putFloat("Pad/PointerXScale",8).putFloat("Pad/PointerYScale",8).commit();
        context.getSharedPreferences("aetherarms_skins",0).edit().putString("active","").apply();
        SkinStore.applyControllerSkin("default");
    }
    private static Object store(Object preference)throws ReflectiveOperationException{return preference.getClass().getMethod("m").invoke(preference);}
    private static String global(){
        Context app=Bridge.context()!=null?Bridge.context():preferenceContext;if(app==null)return "";
        String saved=app.getSharedPreferences(app.getPackageName()+"_preferences",0).getString(SKIN,null);
        return saved==null?app.getSharedPreferences("aetherarms_skins",0).getString("active",""):saved;
    }
    public static String normalizeSkin(Object value){String id=value==null?global():String.valueOf(value);return id.equals("default")?"":id;}
    public static String selectedSkin(Object preference){
        try{preferenceContext=((Context)preference.getClass().getField("i").get(preference)).getApplicationContext();Object data=store(preference);String selected=global();
            if(data!=null){if(data.getClass().getName().equals("l6.v4"))data.getClass().getMethod("p").invoke(data);selected=(String)data.getClass().getMethod("d",String.class,String.class).invoke(data,SKIN,selected);}
            return normalizeSkin(selected);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether skin data store ABI",e);}
    }
    public static void beginSkin(Object preference){
        try{skinStore=store(preference);SkinStore.applyControllerSkin(selectedSkin(preference));}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public static void saveSkin(String id){
        String saved=id.isEmpty()?"default":id;
        try{
            if(skinStore!=null)skinStore.getClass().getMethod("i",String.class,String.class).invoke(skinStore,SKIN,saved);
            else {
                Context app=Bridge.context()!=null?Bridge.context():preferenceContext;app.getSharedPreferences(app.getPackageName()+"_preferences",0).edit().putString(SKIN,saved).apply();
                app.getSharedPreferences("aetherarms_skins",0).edit().putString("active",id).apply();
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot save per-game skin",e);}
    }
}
