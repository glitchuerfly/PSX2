// SPDX-License-Identifier: GPL-3.0-or-later
import org.local.aetherarms.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pure host checks for settings ownership, routing and native motor callback ordering. */
public final class ControllerChecks {
    static void equal(Object actual,Object expected) {
        if(!Objects.equals(actual,expected))throw new AssertionError(actual+" != "+expected);
    }
    static final class Timer implements RumbleController.Scheduler {
        long time,due;Runnable task;
        public long now(){return time;}
        public void postDelayed(Runnable next,long delay){task=next;due=time+delay;}
        public void remove(Runnable old){if(task==old)task=null;}
        void advance(long ms){time+=ms;if(task!=null&&time>=due){Runnable r=task;task=null;r.run();}}
    }
    static RumbleSettings settings(boolean enabled,int port,int throttle) {
        Map<String,Object> values=new HashMap<>();
        values.put("TouchscreenController/EnableVibration",enabled);
        values.put("TouchscreenController/PortIndex",Integer.toString(port));
        values.put("AndroidInputSource/VibrationThrottle",throttle);
        return new RumbleSettings(values);
    }
    static void policyChecks() {
        List<String> output=new ArrayList<>();Timer timer=new Timer();
        RumbleController controller=new RumbleController(new RumbleController.Output(){
            public void vibrate(int value){output.add("vibrate="+value);}
            public void cancel(){output.add("cancel");}
        },timer);
        controller.setActive(true);
        controller.motors(0,255,255);equal(output,Collections.emptyList()); // Default OFF, regardless of game motors.
        controller.configure(settings(false,0,10));
        controller.motors(0,200,20);equal(output,Collections.emptyList()); // Explicit OFF.
        controller.configure(settings(true,0,100));
        controller.motors(1,255,255);equal(output,Collections.emptyList()); // Another player never buzzes phone.
        controller.motors(0,75,20);equal(output,Arrays.asList("vibrate=75"));output.clear();
        timer.advance(10);controller.motors(0,100,0);Runnable stale=timer.task;
        controller.motors(0,60,200);equal(output,Collections.emptyList());
        controller.motors(1,0,0); // Unrelated pad's stop must not cancel current player's vibration.
        timer.advance(89);equal(output,Collections.emptyList());timer.advance(1);
        equal(output,Arrays.asList("vibrate=200"));output.clear();
        timer.advance(1);controller.motors(0,128,0);equal(output,Collections.emptyList());
        controller.motors(0,0,0);equal(output,Arrays.asList("cancel"));output.clear();
        timer.advance(1000);equal(output,Collections.emptyList()); // Zero bypasses throttle and kills pending changes.
        controller.motors(0,300,-5);equal(output,Arrays.asList("vibrate=255"));output.clear();
        controller.motors(0,90,0);Runnable old=timer.task;
        controller.configure(settings(false,0,100));equal(output,Arrays.asList("cancel"));output.clear();
        old.run();controller.motors(0,255,255);timer.advance(1000);equal(output,Collections.emptyList());
        controller.configure(settings(true,1,100));
        controller.motors(0,255,255);equal(output,Collections.emptyList());
        controller.motors(1,30,80);equal(output,Arrays.asList("vibrate=80"));output.clear();
        controller.motors(1,60,0);Runnable late=timer.task;
        controller.setActive(false);equal(output,Arrays.asList("cancel"));output.clear();
        late.run();controller.motors(1,255,255);timer.advance(1000);equal(output,Collections.emptyList());
        controller.setActive(true);equal(output,Collections.emptyList()); // No stale rumble replay on return.
        controller.motors(1,90,0);equal(output,Arrays.asList("vibrate=90"));output.clear();
        controller.motors(1,50,0);Runnable next=timer.task;
        stale.run();late.run();old.run();equal(output,Collections.emptyList()); // Old running tasks cannot flush a new burst.
        equal(timer.task,next);timer.advance(100);equal(output,Arrays.asList("vibrate=50"));output.clear();
        controller.configure(settings(true,1,0));equal(output,Arrays.asList("cancel"));output.clear();
        controller.motors(1,1,0);controller.motors(1,2,0);
        equal(output,Arrays.asList("vibrate=1","vibrate=2"));
    }
    static void profileChecks() throws Exception {
        Path root=Files.createTempDirectory("controller-checks-");
        try {
            Files.createDirectories(root.resolve("gamesettings"));Files.createDirectories(root.resolve("inputprofiles"));
            Map<String,Object> global=new HashMap<>();
            global.put("TouchscreenController/EnableVibration",false);
            global.put("TouchscreenController/HapticFeedback",true); // Press haptics do not authorize game rumble.
            equal(RumbleSettings.load(root.toFile(),0,global).enabled,false);
            Path game=root.resolve("gamesettings/12345678.ini");
            Files.write(game,("[Pad]\nUseGameSettingsForController = false\n[TouchscreenController]\nEnableVibration = true\n").getBytes(StandardCharsets.UTF_8));
            equal(RumbleSettings.load(root.toFile(),0x12345678,global).enabled,false); // Disabled controller override ignored.
            Files.write(game,("[Pad]\nUseGameSettingsForController = true\n[TouchscreenController]\nEnableVibration = true\nPortIndex = 1\n[AndroidInputSource]\nVibrationThrottle = 250\n").getBytes(StandardCharsets.UTF_8));
            RumbleSettings result=RumbleSettings.load(root.toFile(),0x12345678,global);
            equal(result.enabled,true);equal(result.port,1);equal(result.throttleMs,250);
            global.put("TouchscreenController/EnableVibration",true);
            Files.write(game,("[Pad]\nUseGameSettingsForController = true\n[TouchscreenController]\nEnableVibration = false\n").getBytes(StandardCharsets.UTF_8));
            equal(RumbleSettings.load(root.toFile(),0x12345678,global).enabled,false); // Per-game OFF wins over global ON.
            Files.write(game,("[Pad]\nUseGameSettingsForController = true\n").getBytes(StandardCharsets.UTF_8));
            equal(RumbleSettings.load(root.toFile(),0x12345678,global).enabled,true); // Unset keys inherit, as Activity.F/H do.
            Files.write(root.resolve("inputprofiles/My pad.ini"),("[TouchscreenController]\nEnableVibration = false\nPortIndex = 1\n").getBytes(StandardCharsets.UTF_8));
            Files.write(game,("[Pad]\nInputProfileName = My pad\n").getBytes(StandardCharsets.UTF_8));
            result=RumbleSettings.load(root.toFile(),0x12345678,global);equal(result.enabled,false);equal(result.port,1);
            Files.write(game,("[Pad]\nUseGameSettingsForController = true\nInputProfileName = My pad\n[TouchscreenController]\nEnableVibration = true\n").getBytes(StandardCharsets.UTF_8));
            equal(RumbleSettings.load(root.toFile(),0x12345678,global).enabled,true); // Per-game controller wins over named profile.
            Files.write(game,("[Pad]\nInputProfileName = missing\n").getBytes(StandardCharsets.UTF_8));
            equal(RumbleSettings.load(root.toFile(),0x12345678,global).enabled,true);
            Files.write(game,("[Pad]\nInputProfileName = ../outside\n").getBytes(StandardCharsets.UTF_8));
            try {RumbleSettings.load(root.toFile(),0x12345678,global);throw new AssertionError("profile traversal accepted");}
            catch(java.io.IOException expected){}
            global.put("TouchscreenController/EnableVibration","garbage");
            equal(RumbleSettings.load(root.toFile(),0,global).enabled,false);
            global.put("AndroidInputSource/VibrationThrottle",9999);
            equal(RumbleSettings.load(root.toFile(),0,global).throttleMs,1000);
            global.put("AndroidInputSource/VibrationThrottle",-10);
            equal(RumbleSettings.load(root.toFile(),0,global).throttleMs,0);
            equal(global.get("TouchscreenController/HapticFeedback"),true); // No preference mutation.
        } finally {
            try(java.util.stream.Stream<Path> files=Files.walk(root)) {
                files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});
            }
        }
    }
    public static void main(String[] args) throws Exception {
        policyChecks();profileChecks();
        System.out.println("PASS: controller game-rumble OFF/default, press-haptic separation, profile/global inheritance, player routing, throttle and pause/disable cancellation including stale callbacks.");
    }
}
