// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;
import java.util.*;

/** The existing Aether layouts, skins and editor gestures, available before boot. */
public final class ControllerLayoutActivity extends Activity {
    private Object controller,store;
    private boolean expandToCutout=true;
    private Map<String,Object> settings;
    private Object call(String name,Class<?>[] types,Object...args)throws ReflectiveOperationException{return controller.getClass().getMethod(name,types).invoke(controller,args);}
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(ActivityLocale.wrap(base));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        settings=new LinkedHashMap<>(getSharedPreferences(getPackageName()+"_preferences",0).getAll());
        try{
            String gameProfile=getIntent().getStringExtra("controller.game_profile");
            Map<String,Object> game=gameProfile==null?Collections.emptyMap():ControllerSettings.read(new java.io.File(gameProfile));
            expandToCutout=ControllerSettings.truth(game.getOrDefault("UI/ExpandToCutout",settings.getOrDefault("UI/ExpandToCutout",true)));
            applyFullscreen();
            setRequestedOrientation(ControllerSettings.editorOrientation(settings,game));
            String profile=getIntent().getStringExtra("controller.profile");
            if(profile!=null){store=Class.forName("l6.v4").getConstructor(String.class).newInstance(profile);settings.putAll(ControllerSettings.read(new java.io.File(profile)));}
            Class<?> type=Class.forName("xyz.aethersx2.android.k"),emu=Class.forName("xyz.aethersx2.android.EmulationActivity");
            controller=type.getConstructor(Context.class).newInstance(this);
            View view=(View)controller;view.setBackgroundColor(0xff18151f);ControllerLayoutAccess.bind(view,store);setContentView(view);
            String kind=String.valueOf(settings.getOrDefault("TouchscreenController/View","analog_stick"));
            int port=ControllerSettings.integer(settings.get("TouchscreenController/PortIndex"),0,0,1);
            if(kind.equals("lightgun")){
                String device=String.valueOf(settings.getOrDefault("USB"+(port+1)+"/Type","guncon2"));
                int subtype=ControllerSettings.integer(settings.get("USB"+(port+1)+"/"+device+"_subtype"),0,0,100);
                call("p",new Class<?>[]{emu,int.class,String.class,int.class,String.class,boolean.class,boolean.class,boolean.class},null,port,device,subtype,kind,false,false,false);
            }else call("o",new Class<?>[]{emu,int.class,String.class,String.class,boolean.class,boolean.class,boolean.class},null,port,String.valueOf(settings.getOrDefault("Pad"+(port+1)+"/Type","DualShock2")),kind,false,false,false);
            // Keep the editor visible even when the configured gameplay auto-hide is enabled.
            type.getField("B").setInt(controller,0);call("b",new Class<?>[0]);
            SkinStore.initialize(this);SkinStore.applyControllerSkin(settings.get("TouchscreenController/CustomSkin"));
            // Wait for layout, then synchronize the real widget's Portrait/Landscape namespace.
            // Rotation can arrive after onCreate; onConfigurationChanged keeps that namespace current.
            view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
                public boolean onPreDraw(){
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    if(isFinishing())return true;
                    try{call("E",new Class<?>[0]);openAction();}
                    catch(ReflectiveOperationException e){throw new IllegalStateException("Aether editor ABI",e);}
                    return false;
                }
            });
        }catch(Exception e){throw new IllegalStateException("Cannot initialize Aether controller editor",e);}
    }
    private void applyFullscreen(){EmulationFullscreen.apply(this,expandToCutout);}
    @Override protected void onResume(){super.onResume();applyFullscreen();}
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);if(focused)applyFullscreen();
    }
    @Override public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);applyFullscreen();
        if(controller!=null)try{call("E",new Class<?>[0]);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Aether editor orientation ABI",e);}
    }
    private void openAction()throws ReflectiveOperationException{
        String action=getIntent().getStringExtra("action");
        if("EditPositions".equals(action)||"EditScale".equals(action)){call("C",new Class<?>[]{int.class},action.equals("EditScale")?3:2);return;}
        if("ResetLayout".equals(action)){
            int title=getResources().getIdentifier("touchscreen_reset_layout","string",getPackageName());
            new AlertDialog.Builder(this).setTitle(title).setPositiveButton(android.R.string.ok,(d,w)->{try{call("y",new Class<?>[0]);finish();}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}).setNegativeButton(android.R.string.cancel,(d,w)->finish()).setOnCancelListener(d->finish()).show();return;
        }
        if("AddRemoveButtons".equals(action)){
            Object builder=call("c",new Class<?>[]{Context.class},this);Dialog dialog=(Dialog)builder.getClass().getMethod("a").invoke(builder);dialog.setOnDismissListener(d->finish());dialog.show();return;
        }
        if("ToggleButtons".equals(action)){
            List<?> buttons=(List<?>)controller.getClass().getField("o").get(controller);
            String[] names=new String[buttons.size()];boolean[] checked=new boolean[buttons.size()];
            for(int i=0;i<buttons.size();i++){View v=(View)buttons.get(i);CharSequence label=v.getContentDescription();names[i]=label==null?(String)v.getClass().getMethod("getConfigName").invoke(v):label.toString();checked[i]=v.getClass().getField("s").getBoolean(v);}
            int title=getResources().getIdentifier("settings_touchscreen_controller_toggle_buttons","string",getPackageName());
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setMultiChoiceItems(names,checked,(d,index,value)->{
                try{Object button=buttons.get(index);button.getClass().getMethod("setToggle",boolean.class).invoke(button,value);
                    // FF/Turbo deliberately remain HOLD even when selected in this legacy list.
                    boolean saved=button.getClass().getField("s").getBoolean(button);
                    String config=(String)button.getClass().getMethod("getConfigName").invoke(button),kind=(String)controller.getClass().getField("m").get(controller);
                    String key="TouchscreenController/"+kind+"/"+config+"Toggle";
                    if(store==null)getSharedPreferences(getPackageName()+"_preferences",0).edit().putBoolean(key,saved).apply();
                    else store.getClass().getMethod("f",String.class,boolean.class).invoke(store,key,saved);
                }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            }).setPositiveButton(android.R.string.ok,(d,w)->finish()).setOnCancelListener(d->finish()).create();dialog.setOnDismissListener(d->finish());dialog.show();return;
        }
        finish();
    }
}
