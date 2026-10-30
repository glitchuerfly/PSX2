// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.content.*;
import android.view.*;
import java.lang.reflect.*;

/** Open the original Aether controller screen with the selected game's existing CRC data store. */
public final class ControllerProperties {
    private ControllerProperties(){}
    private static Object field(Object object,String name)throws ReflectiveOperationException{return object.getClass().getField(name).get(object);}
    public static void menu(Activity activity,Menu menu,Object entry){
        int id=activity.getResources().getIdentifier("game_list_entry_menu_controller_properties","id",activity.getPackageName());
        MenuItem item=menu.findItem(id);if(item==null)return;
        try{
            int crc=(Integer)entry.getClass().getMethod("getCRC").invoke(entry);
            String path=(String)entry.getClass().getMethod("getPath").invoke(entry);
            item.setEnabled(crc!=0&&path!=null&&!path.isEmpty());
            item.setOnMenuItemClickListener(clicked->{
                Intent intent=new Intent().setClassName(activity,"xyz.aethersx2.android.ControllerSettingsActivity").putExtra("path",path);
                activity.startActivity(intent);return true;
            });
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether game metadata ABI",e);}
    }
    public static void attach(Activity activity,Object fragment){
        String path=activity.getIntent().getStringExtra("path");if(path==null||path.isEmpty())return;
        Object entry=Bridge.hostCall("getGameListEntry",new Class<?>[]{String.class},path);
        if(entry==null)throw new IllegalArgumentException("Game entry is unavailable");
        try{
            int crc=(Integer)entry.getClass().getMethod("getCRC").invoke(entry);
            if(crc==0)throw new IllegalArgumentException("Game CRC is unavailable");
            String profile=(String)Bridge.hostCall("getGameSettingsPath",new Class<?>[]{int.class},crc);
            Object store=Class.forName("l6.v4").getConstructor(String.class).newInstance(profile);
            fragment.getClass().getField("l0").set(fragment,store);
            int label=activity.getResources().getIdentifier("controller_properties","string",activity.getPackageName());
            String title=(String)entry.getClass().getMethod("getTitle").invoke(entry);
            activity.setTitle(activity.getString(label)+" · "+title);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether per-game controller ABI",e);}
    }
    /** When a per-game override is off, show inherited values without allowing this screen
     * to edit global configuration accidentally. Aether's own switch enables editing. */
    public static void guard(Object preferenceFragment,Object controllerFragment){
        try{
            Activity activity=(Activity)preferenceFragment.getClass().getMethod("getActivity").invoke(preferenceFragment);
            ControllerLayoutAccess.installActions(preferenceFragment,controllerFragment);
            if(activity==null||!activity.getClass().getName().equals("xyz.aethersx2.android.ControllerSettingsActivity")||!activity.getIntent().hasExtra("path"))return;
            Object store=field(controllerFragment,"l0");if(store==null)return;
            boolean enabled=(Boolean)store.getClass().getMethod("a",String.class,boolean.class).invoke(store,"Pad/UseGameSettingsForController",false);
            if(enabled)return;
            Object screen=preferenceFragment.getClass().getMethod("z").invoke(preferenceFragment);
            disableExceptSwitch(screen);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether preference guard ABI",e);}
    }
    private static void disableExceptSwitch(Object pref)throws ReflectiveOperationException{
        Class<?> group=Class.forName("androidx.preference.PreferenceGroup");
        if(group.isInstance(pref)){
            int count=(Integer)group.getMethod("c0").invoke(pref);
            for(int i=0;i<count;i++)disableExceptSwitch(group.getMethod("b0",int.class).invoke(pref,i));
        }else{
            String key=(String)pref.getClass().getField("u").get(pref);
            if(!"Pad/UseGameSettingsForController".equals(key))pref.getClass().getMethod("J",boolean.class).invoke(pref,false);
        }
    }
}
