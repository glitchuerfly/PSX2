// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.os.*;
import android.util.Log;
import java.util.*;

/** Reuse Aether's exact descriptor registry, including its ordered vibrator arrays. */
public final class ControllerDevices {
    private static final Map<String,RumbleController> motors=new HashMap<>();
    private static final MotorMixer mixer=new MotorMixer();
    private static String[] descriptors=new String[0];
    private static Object[] devices=new Object[0];
    private static Map<String,Object> settings=Collections.emptyMap();
    private static boolean active;
    private static final Handler handler=new Handler(Looper.getMainLooper());
    private ControllerDevices(){}
    public static synchronized String descriptor(int index){return index>=0&&index<descriptors.length?descriptors[index]:"Keyboard";}
    public static synchronized void update(Object[] input){
        clear();devices=input==null?new Object[0]:input.clone();descriptors=new String[devices.length];
        for(int i=0;i<devices.length;i++)try{descriptors[i]=(String)devices[i].getClass().getField("descriptor").get(devices[i]);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Aether input registry ABI",e);}
        build();
    }
    public static synchronized void configure(Map<String,Object> values){clear();settings=new LinkedHashMap<>(values);build();}
    private static void clear(){for(RumbleController c:motors.values())c.setActive(false);motors.clear();mixer.clear();}
    private static void build(){
        for(int d=0;d<devices.length;d++){
            boolean phone="__DEVICE_VIBRATOR__".equals(descriptors[d]);
            try{
                Vibrator[] vibrators=(Vibrator[])devices[d].getClass().getField("vibrators").get(devices[d]);
                if(vibrators==null)continue;
                for(int i=0;i<vibrators.length;i++){
                    Vibrator vibrator=vibrators[i];if(vibrator==null)continue;
                    RumbleController controller=new RumbleController(new RumbleController.Output(){
                        public void vibrate(int amplitude){try{if(vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createOneShot(3000,amplitude));}catch(RuntimeException e){Log.w("AetherArmsBridge","Controller motor unavailable",e);}}
                        public void cancel(){try{vibrator.cancel();}catch(RuntimeException e){Log.w("AetherArmsBridge","Controller motor cancel failed",e);}}
                    },new RumbleController.Scheduler(){
                        public long now(){return SystemClock.uptimeMillis();}public void postDelayed(Runnable r,long delay){handler.postDelayed(r,delay);}public void remove(Runnable r){handler.removeCallbacks(r);}
                    });
                    Map<String,Object> config=new HashMap<>();config.put("TouchscreenController/EnableVibration",!phone||ControllerSettings.truth(settings.get("TouchscreenController/EnableVibration")));config.put("AndroidInputSource/VibrationThrottle",settings.getOrDefault("AndroidInputSource/VibrationThrottle",10));
                    controller.configure(new RumbleSettings(config));controller.setActive(active);motors.put(descriptors[d]+"/Vibrator"+i,controller);
                }
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether vibrator ABI",e);}
        }
    }
    public static synchronized void setActive(boolean value){active=value;for(RumbleController c:motors.values())c.setActive(value);if(!value)mixer.clear();}
    public static synchronized void rumble(int pad,int large,int small){
        int[] intensities={large,small};String[] names={"LargeMotor","SmallMotor"};
        for(int m=0;m<2;m++)for(String binding:ControllerSettings.bindings(settings.get("Pad"+(pad+1)+"/"+names[m]))){
            RumbleController output=motors.get(binding);if(output!=null)output.motors(0,mixer.set(binding,pad+"/"+names[m],active?intensities[m]:0),0);
        }
    }
}
