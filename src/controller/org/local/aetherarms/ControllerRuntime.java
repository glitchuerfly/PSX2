// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.os.*;
import android.util.Log;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import kr.co.iefriends.pcsx2.NativeApp;

/** Connect legacy controller values, descriptor bindings and output routes to the donor. */
public final class ControllerRuntime {
    private static final String TAG="AetherArmsBridge";
    private static final String[] NAMES={"Up","Right","Down","Left","Triangle","Circle","Cross","Square","Select","Start","L1","L2","R1","R2","L3","R3","Analog","LUp","LRight","LDown","LLeft","RUp","RRight","RDown","RLeft"};
    private static final int[] CODES={19,22,20,21,100,97,96,99,109,108,102,104,103,105,106,107,200,110,111,112,113,120,121,122,123};
    private static final Map<String,Integer> keys=new HashMap<>();
    private static final Map<Integer,String> bindNames=new HashMap<>();
    private static final Map<Integer,Map<Integer,String>> portBinds=new HashMap<>();
    private static long generation;
    private static Map<String,Object> values=Collections.emptyMap();
    private static final Map<String,String> defaults=new LinkedHashMap<>(),types=new HashMap<>();
    private static final Set<String> targets=new HashSet<>();
    private static final Map<Integer,Map<Integer,String>> usbNames=new HashMap<>();
    private static final Map<String,Float> relative=new HashMap<>();
    private static final Map<String,Set<String>> macroSources=new HashMap<>();
    private static MacroController macroController;
    private static ControllerBindings bindings;
    private static boolean active,initialized;
    private static volatile boolean remotePlayer2;
    private static int width,height;
    private static float pointerX=.5f,pointerY=.5f,lastX=Float.NaN,lastY=Float.NaN;
    private static Handler handler;
    private static final PadMixer mixer=new PadMixer(ControllerRuntime::output);
    private static void output(int pad,String name,float value){
        if(pad==1&&remotePlayer2)return;
        if(name.equals("Pressure")){mixer.pressure(pad,value>0?ControllerSettings.number(values.get("Pad"+(pad+1)+"/PressureModifier"),.5f,0,1):1);return;}
        Integer key=PadRoutes.key(String.valueOf(values.getOrDefault("Pad"+(pad+1)+"/Type",pad==0?"DualShock2":"None")),name);
        if(key!=null)NativeApp.setPadButtonForPort(pad,key,Math.max(1,Math.min(32767,Math.round(value*32767))),value>0);
    }
    private static Runnable tick;
    private static void scheduleFrame(){final long token=generation;tick=()->frame(token);handler.postDelayed(tick,8);}
    private static long lastFrame;
    private ControllerRuntime(){}
    private static String text(Object object,String method) throws ReflectiveOperationException {return (String)object.getClass().getMethod(method).invoke(object);}
    private static int integer(Object object,String method) throws ReflectiveOperationException {return (Integer)object.getClass().getMethod(method).invoke(object);}
    private static void put(String key,String type,String value){defaults.put(key,value);types.put(key,type);}
    public static synchronized void initialize(){
        if(initialized)return;
        handler=new Handler(Looper.getMainLooper());
        for(int i=0;i<NAMES.length;i++)keys.put(NAMES[i],CODES[i]);
        Object[] binds=(Object[])Bridge.hostCall("getPadBinds",new Class<?>[]{String.class,boolean.class},"DualShock2",false);
        try{for(Object bind:binds)ControllerBindings.addPadInput(bindNames,integer(bind,"getBindIndex"),text(bind,"getName"),integer(bind,"getType"));}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Aether controller binding ABI",e);}
        put("Pad/MultitapPort1","bool","false");put("Pad/MultitapPort2","bool","false");
        put("Pad/PointerXScale","float","8.0");put("Pad/PointerYScale","float","8.0");
        // The actual legacy SettingInfo defaults are the same values shown by the Aether UI.
        Object[] settings=(Object[])Bridge.hostCall("getPadSettings",new Class<?>[]{String.class},"DualShock2");
        for(int p=1;p<=8;p++){
            put("Pad"+p+"/Type","string",p==1?"DualShock2":"None");
            try{for(Object setting:settings){int kind=integer(setting,"getType");String name=text(setting,"getName");
                put("Pad"+p+"/"+name,kind==0?"bool":kind==1||kind==2?"int":kind==3?"float":"string",text(setting,"getDefaultValue"));}
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether controller settings ABI",e);}
            put("Pad"+p+"/LargeMotorScale","float","1.0");put("Pad"+p+"/SmallMotorScale","float","1.0");
            for(String name:bindNames.values())targets.add("Pad"+p+"/"+name);
            for(int macro=1;macro<=4;macro++)targets.add("Pad"+p+"/Macro"+macro);
        }
        initialized=true;
    }
    private static final java.util.concurrent.ExecutorService remoteConfig=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"rp-pad2");t.setDaemon(true);return t;});
    private static final RemotePadController remotePad=new RemotePadController(new RemotePadController.Output(){
        public void button(int key,int range,boolean down){NativeApp.setPadButtonForPort(1,key,range,down);}
        public void configure(boolean on,Runnable done){
            remotePlayer2=on;NativeApp.sRemotePlayer2=on;NativeApp.sRemotePlayer2Rumble=0;
            remoteConfig.execute(()->{
                String type;
                synchronized(ControllerRuntime.class){type=on?"DualShock2":String.valueOf(values.getOrDefault("Pad2/Type","None"));}
                // This native base layer is disposable; never write Aether's profile files.
                NativeApp.setSetting("Pad2","Type","string",type);NativeApp.commitSettings();done.run();
            });
        }
    });
    public static long remoteStart(){return remotePad.start();}
    public static void remoteStop(long token){remotePad.stop(token);}
    public static void remoteButton(long token,int key,int range,boolean down){remotePad.button(token,key,range,down);}
    public static void reapplyRemote(){remotePad.reapply();}
    public static synchronized void refresh(File root,int crc,Map<String,?> global){
        initialize();release();
        try{values=ControllerSettings.effective(root,crc,global);}catch(IOException e){Log.e(TAG,"Controller profile read failed; using global controller settings",e);values=new LinkedHashMap<>();values.putAll(global);}
        for(int p=1;p<=8;p++){
            String kind=String.valueOf(values.getOrDefault("Pad"+p+"/Type",p==1?"DualShock2":"None"));
            Object[] info=(Object[])Bridge.hostCall("getPadSettings",new Class<?>[]{String.class},kind);
            if(info!=null)try{for(Object setting:info){int code=integer(setting,"getType");put("Pad"+p+"/"+text(setting,"getName"),code==0?"bool":code==1||code==2?"int":code==3?"float":"string",text(setting,"getDefaultValue"));}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        for(Map.Entry<String,String> e:ControllerSettings.nativeValues(values,defaults,types).entrySet()){
            String key=e.getKey();int split=key.lastIndexOf('/');NativeApp.setSetting(key.substring(0,split),key.substring(split+1),types.getOrDefault(key,"string"),remotePlayer2&&key.equals("Pad2/Type")?"DualShock2":e.getValue());
        }
        portBinds.clear();Set<String> all=new HashSet<>(targets);
        for(int p=0;p<8;p++){
            String type=String.valueOf(values.getOrDefault("Pad"+(p+1)+"/Type",p==0?"DualShock2":"None"));
            Object[] info=(Object[])Bridge.hostCall("getPadBinds",new Class<?>[]{String.class,boolean.class},type,false);
            Map<Integer,String> names=new HashMap<>();
            if(info!=null)try{for(Object bind:info){String name=text(bind,"getName");ControllerBindings.addPadInput(names,integer(bind,"getBindIndex"),name,integer(bind,"getType"));}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            for(String name:names.values())all.add("Pad"+(p+1)+"/"+name);
            portBinds.put(p,names);
        }
        for(String key:values.keySet())if(key.startsWith("Hotkeys/"))all.add(key);
        usbNames.clear();
        for(int p=0;p<2;p++){
            String section="USB"+(p+1),type=String.valueOf(values.getOrDefault(section+"/Type","None"));
            NativeApp.setSetting(section,"Type","string",type);
            int subtype=ControllerSettings.integer(values.get(section+"/"+type+"_subtype"),0,0,100);
            NativeApp.setSetting(section,type+"_subtype","int",Integer.toString(subtype));
            Map<Integer,String> names=new HashMap<>();
            if(!type.equals("None")){
                Object[] info=(Object[])Bridge.hostCall("getUSBDeviceBinds",new Class<?>[]{String.class,int.class,boolean.class},type,subtype,false);
                if(info!=null)try{for(Object bind:info){String name=text(bind,"getName");names.put(integer(bind,"getBindIndex"),name);all.add(section+"/"+type+"_"+name);}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            }
            usbNames.put(p,names);
            for(Map.Entry<String,Object> e:values.entrySet())if(e.getKey().startsWith(section+"/")&&!e.getKey().equals(section+"/Type")&&!(e.getValue() instanceof Collection)){
                String key=e.getKey().substring(section.length()+1);
                if(!names.containsValue(key.replace(type+"_","")))NativeApp.setSetting(section,key,"string",String.valueOf(e.getValue()));
            }
        }
        bindings=new ControllerBindings(values,all,ControllerRuntime::target);
        macroController=new MacroController(values,(pad,index,name,value)->mixer.set(pad,name,"macro"+index,value));
        ControllerDevices.configure(values);
        if(active)scheduleFrame();
        SkinStore.applyControllerSkin(values.get("TouchscreenController/CustomSkin"));
    }
    private static synchronized boolean current(long token){return generation==token&&active;}
    public static synchronized Map<String,Object> snapshot(){return new LinkedHashMap<>(values);}
    public static synchronized void setActive(boolean next){
        if(!initialized)return;
        if(active==next)return;active=next;
        if(!next){release();ControllerDevices.setActive(false);}else{lastFrame=NativeApp.getPresentedFrameCount();scheduleFrame();ControllerDevices.setActive(true);}
    }
    private static void release(){
        generation++;if(handler!=null&&tick!=null)handler.removeCallbacks(tick);
        if(bindings!=null)bindings.release();if(macroController!=null)macroController.release();mixer.release();relative.clear();macroSources.clear();lastX=lastY=Float.NaN;
        if(initialized){for(int p=0;p<2;p++)if(gun(p))NativeApp.usbLightgunButton(p,13,false);}
    }
    public static synchronized void setPad(int pad,int bind,float value){
        String name=portBinds.getOrDefault(pad,bindNames).get(bind);if(name==null||pad<0||pad>7)return;
        if(!active&&value!=0)return;
        mixer.set(pad,name,"touch",value);
    }
    public static synchronized void button(int device,int code,boolean down){if(bindings!=null&&(active||!down))bindings.button(ControllerDevices.descriptor(device),code,down);}
    public static synchronized void axis(int device,int axis,float value){if(bindings!=null&&active)bindings.axis(ControllerDevices.descriptor(device),axis,value);}
    public static synchronized boolean hasButton(int device,int button){return bindings!=null&&bindings.hasButton(ControllerDevices.descriptor(device),button);}
    private static void target(String target,float value){
        if(target.startsWith("Pad")){
            int slash=target.indexOf('/'),pad=Integer.parseInt(target.substring(3,slash))-1;String name=target.substring(slash+1);
            if(name.matches("Macro[1-4]")){macroInput(pad,Integer.parseInt(name.substring(5))-1,"physical",value>0);return;}
            mixer.set(pad,name,"physical",value);
        }else if(target.startsWith("USB")){
            int slash=target.indexOf('/'),pad=Integer.parseInt(target.substring(3,slash))-1;String type=String.valueOf(values.getOrDefault("USB"+(pad+1)+"/Type","None"));
            String name=target.substring(slash+1);if(name.startsWith(type+"_"))name=name.substring(type.length()+1);
            usb(pad,name,value);
        }else if(target.startsWith("Hotkeys/")){boolean down=value>0;final long token=generation;handler.post(()->{if(current(token))Bridge.controllerHotkey(target.substring(8),down);});}
    }
    public static synchronized void macro(int pad,int macro,boolean down){macroInput(pad,macro,"touch",down);}
    private static void macroInput(int pad,int macro,String source,boolean down){
        if(pad<0||pad>7||macro<0||macro>3||macroController==null)return;
        String id=pad+"/"+macro;Set<String> inputs=macroSources.computeIfAbsent(id,k->new HashSet<>());
        if(active&&down)inputs.add(source);else inputs.remove(source);
        macroController.set(pad,macro,!inputs.isEmpty(),NativeApp.getPresentedFrameCount());
    }
    private static synchronized void frame(long token){
        if(!active||token!=generation)return;
        long frame=NativeApp.getPresentedFrameCount();
        if(macroController!=null)macroController.frame(frame);
        float dx=relative.getOrDefault("RelativeRight",0f)-relative.getOrDefault("RelativeLeft",0f);
        float dy=relative.getOrDefault("RelativeDown",0f)-relative.getOrDefault("RelativeUp",0f);
        if((dx!=0||dy!=0)&&frame!=lastFrame){
            float steps=Math.min(10,Math.max(0,frame-lastFrame));
            pointerX=Math.max(0,Math.min(1,pointerX+dx*steps*ControllerSettings.number(values.get("Pad/PointerXScale"),8,.25f,50)/600));
            pointerY=Math.max(0,Math.min(1,pointerY+dy*steps*ControllerSettings.number(values.get("Pad/PointerYScale"),8,.25f,50)/600));
            NativeApp.usbLightgunAim(pointerX,pointerY);
        }
        // Relative pointer bindings are pulses, not a permanent held direction.
        if(bindings!=null){for(String c:new String[]{"X+","X-","Y+","Y-"})bindings.component("Pointer-0/"+c,0);}
        lastFrame=frame;scheduleFrame();
    }
    public static synchronized void surface(int w,int h){width=w;height=h;}
    private static boolean gun(int port){return String.valueOf(values.getOrDefault("USB"+(port+1)+"/Type","None")).toLowerCase(Locale.ROOT).contains("guncon");}
    public static synchronized void pointer(int state,float x,float y){
        if(!active&&state!=0)return;
        if(!Float.isFinite(x)||!Float.isFinite(y)||width<=0||height<=0)return;
        pointerX=Math.max(0,Math.min(1,x/width));pointerY=Math.max(0,Math.min(1,y/height));NativeApp.usbLightgunAim(pointerX,pointerY);
        if(bindings!=null){
            if(Float.isFinite(lastX)&&state<0){
                float dx=(x-lastX)*ControllerSettings.number(values.get("Pad/PointerXScale"),8,.25f,50)/width;
                float dy=(y-lastY)*ControllerSettings.number(values.get("Pad/PointerYScale"),8,.25f,50)/height;
                bindings.component("Pointer-0/X+",Math.max(0,dx));bindings.component("Pointer-0/X-",Math.max(0,-dx));
                bindings.component("Pointer-0/Y+",Math.max(0,dy));bindings.component("Pointer-0/Y-",Math.max(0,-dy));
            }
            if(state>=0)bindings.component("Pointer-0/LeftButton",state>0?1:0);
        }
        if(state>=0)for(int p=0;p<2;p++)if(gun(p)&&!values.containsKey("USB"+(p+1)+"/"+values.get("USB"+(p+1)+"/Type")+"_Trigger"))NativeApp.usbLightgunButton(p,13,state>0);
        lastX=state==0?Float.NaN:x;lastY=state==0?Float.NaN:y;
    }
    public static synchronized void sensor(float x,float y,float z){if(bindings==null||!active)return;float[] a={x,y,z};String[] axes={"X","Y","Z"};for(int i=0;i<3;i++){float v=Float.isFinite(a[i])?Math.max(-1,Math.min(1,a[i]/9.80665f)):0;bindings.component("Sensor/"+axes[i]+"+",Math.max(0,v));bindings.component("Sensor/"+axes[i]+"-",Math.max(0,-v));}}
    public static synchronized void usb(int port,int bind,float value){String name=usbNames.getOrDefault(port,Collections.emptyMap()).get(bind);if(name!=null)usb(port,name,active?value:0);}
    private static void usb(int port,String name,float value){
        if(name.startsWith("Relative")){relative.put(name,value);return;}
        Map<String,Integer> gunBinds=new HashMap<>();String[] names={"C","B","A","Up","Right","Down","Left","Trigger","Select","Start","ShootOffscreen","Recalibrate"};int[] codes={1,2,3,4,5,6,7,13,14,15,16,17};for(int i=0;i<names.length;i++)gunBinds.put(names[i],codes[i]);
        Integer key=gunBinds.get(name);if(gun(port)&&key!=null)NativeApp.usbLightgunButton(port,key,value>.0001f);
        else {Integer padKey=keys.get(name);if(padKey!=null)NativeApp.setPadButtonForPort(port,padKey,Math.max(1,Math.round(Math.max(0,Math.min(1,value))*32767)),value>.0001f);}
    }
}
