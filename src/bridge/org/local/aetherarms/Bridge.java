// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.*;
import kr.co.iefriends.pcsx2.NativeApp;

/** Experimental local port: legacy UI/host services, ARMSX2 VM. No legacy VM fallback. */
public final class Bridge {
    private static final String TAG = "AetherArmsBridge";
    private static volatile Context app;
    private static volatile boolean ready, paused, surfaceReady, stopping, resetting;
    private static volatile long sessionStart;
    private static int limiterMode;
    private static boolean fastForwardHeld;
    // Menu pause and Activity/surface suspension are independent pause reasons.
    private static boolean activityResumed, menuPaused, lifecycleRegistered, sessionActive;
    private static Boolean appliedPause;
    private static final PauseAudioController audioPause=new PauseAudioController(new PauseAudioController.Output() {
        @Override public void setSuppressed(boolean value) { NativeApp.setOutputPauseSuppressed(value); }
        @Override public void setBackgrounded(boolean value) { NativeApp.setAppBackgrounded(value); }
    });
    private static WeakReference<Activity> emulationActivity = new WeakReference<>(null);
    private static volatile String gamePath = "", gameSerial = "";
    private static volatile int gameCRC, controllerCRC;
    private static Class<?> host;
    private static File dataRoot;
    private static final Set<String> warnings = Collections.synchronizedSet(new HashSet<String>());
    public static Context context() { return app; }
    static Object hostCall(String name, Class<?>[] types, Object... args) {
        try {
            if (host == null) host = Class.forName("xyz.aethersx2.android.NativeLibrary");
            Method m = host.getDeclaredMethod(name, types); m.setAccessible(true);
            return m.invoke(null, args);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Legacy host callback: " + name, e); }
    }
    static Object hostCall(String name) { return hostCall(name, new Class<?>[0]); }
    public static void error(String text) {
        Log.e(TAG, text);
        hostCall("reportErrorAsync", new Class<?>[]{String.class, String.class}, "ARMSX2", text);
    }
    private static void unsupported(String feature) {
        if (warnings.add(feature)) Log.w(TAG, "Adapter belum mendukung: " + feature);
    }
    public static synchronized void prepareLaunchDefaults(Context context) {
        SharedPreferences state=context.getSharedPreferences("aetherarms_adapter",Context.MODE_PRIVATE);
        SharedPreferences settings=context.getSharedPreferences(context.getPackageName()+"_preferences",Context.MODE_PRIVATE);
        Map<String,?> current=settings.getAll();
        SharedPreferences.Editor editor=settings.edit();
        if (!state.getBoolean("landscape_auto_v1",false)) {
            Object orientation=current.get("UI/EmulationActivityOrientation");
            if (orientation==null||"unspecified".equals(String.valueOf(orientation)))
                editor.putString("UI/EmulationActivityOrientation","landscape");
        }
        // Remove saved Auto on every start; explicit OpenGL/Vulkan/Software picks are retained.
        Object renderer=current.get("EmuCore/GS/Renderer");
        if (renderer==null||"-1".equals(String.valueOf(renderer)))
            editor.putString("EmuCore/GS/Renderer","12");
        if (!state.getBoolean("fullscreen_manual_v1",false)) editor.putBoolean("UI/ExpandToCutout",true);
        if (editor.commit()) state.edit().putBoolean("landscape_auto_v1",true)
            .putBoolean("fullscreen_manual_v1",true).commit();
    }
    public static void applyEmulationFullscreen(Activity activity) {
        boolean expand=(Boolean)hostCall("getBooleanSettingValue",
            new Class<?>[]{String.class,String.class,boolean.class},"UI","ExpandToCutout",true);
        EmulationFullscreen.apply(activity,expand);
    }
    public static void resetLaunchDefaults(Context context) {
        context.getSharedPreferences(context.getPackageName()+"_preferences",Context.MODE_PRIVATE)
            .edit().putString("UI/EmulationActivityOrientation","landscape")
            .putString("EmuCore/GS/Renderer","12").putBoolean("UI/ExpandToCutout",true).commit();
    }
    public static synchronized void initialize(Context context) {
        if (ready) return;
        app = context.getApplicationContext();
        RemotePlayMenu.initialize(app);
        SkinStore.initialize(app);
        ControllerRumble.initialize(app);
        GameAudioFocus.initialize(app);
        if (!lifecycleRegistered) {
            ((Application)app).registerActivityLifecycleCallbacks(new EmulationLifecycleCallbacks());
            lifecycleRegistered = true;
        }
        dataRoot = new File((String)hostCall("getDataDirectory"));
        try {
            copyAssets(app.getAssets(), "armsx2_resources", new File(dataRoot, "resources"));
            NativeApp.load();
            org.libsdl.app.SDL.setupJNI();
            org.libsdl.app.SDL.initialize();
            if (context instanceof Activity) org.libsdl.app.SDL.setContext((Activity)context);
            NativeApp.initialize(dataRoot.getAbsolutePath(), new File(dataRoot, "bios").getAbsolutePath(), Build.VERSION.SDK_INT);
            ready = true;
            ControllerRuntime.initialize();
            syncSettings();
            Log.i(TAG, "CORE_READY version=" + NativeApp.getBuildVersion() + " root=" + dataRoot);
        } catch (IOException e) { throw new IllegalStateException("Tidak bisa menyiapkan resource ARMSX2", e); }
    }
    private static void copyAssets(AssetManager assets, String path, File dest) throws IOException {
        String[] children = assets.list(path);
        if (children != null && children.length != 0) {
            if (!dest.isDirectory() && !dest.mkdirs()) throw new IOException("mkdir " + dest);
            for (String child : children) copyAssets(assets, path + "/" + child, new File(dest, child));
        } else {
            if (dest.isFile()) return;
            File parent = dest.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("mkdir " + parent);
            File temp = new File(dest.getPath() + ".tmp");
            try (InputStream in = assets.open(path); OutputStream out = new FileOutputStream(temp)) {
                byte[] b = new byte[65536]; int n;
                while ((n = in.read(b)) != -1) out.write(b, 0, n);
            }
            if (!temp.renameTo(dest)) throw new IOException("rename " + dest);
        }
    }
    private static SharedPreferences prefs() { return app.getSharedPreferences(app.getPackageName() + "_preferences", Context.MODE_PRIVATE); }
    /** Resolve boot identity without waiting for the native first-frame observer. */
    private static int launchControllerCRC(String path,String serial,int listed){
        String saved=app.getSharedPreferences("aetherarms_controller_identity",0).getString(path,"");
        int remembered=0;
        if(saved.startsWith(serial+"\n"))try{remembered=(int)Long.parseLong(saved.substring(serial.length()+1),16);}catch(NumberFormatException ignored){}
        return ControllerSettings.launchCRC(dataRoot,serial,listed,remembered);
    }
    public static File controllerProfile(Activity activity) throws IOException {
        String path=activity.getIntent().getStringExtra("bootPath");
        int crc;
        synchronized(Bridge.class){crc=sessionActive&&path!=null&&path.equals(gamePath)?controllerCRC:0;}
        if(crc==0&&path!=null&&!path.isEmpty()){
            Object entry=hostCall("getGameListEntry",new Class<?>[]{String.class},path);
            String serial="";int listed=0;
            if(entry!=null)try{
                listed=(Integer)entry.getClass().getMethod("getCRC").invoke(entry);
                serial=(String)entry.getClass().getMethod("getSerial").invoke(entry);
            }catch(ReflectiveOperationException failure){throw new IOException("Launch game identity ABI",failure);}
            crc=launchControllerCRC(path,serial,listed);
        }
        return ControllerSettings.profileFile(dataRoot,crc);
    }
    public static synchronized boolean controllerNoticeVisible(){
        return sessionActive&&activityResumed&&surfaceReady&&!menuPaused&&!stopping&&!paused;
    }
    public static void syncSettings() {
        if (!ready) return;
        ControllerRumble.refresh(dataRoot,controllerCRC,prefs().getAll());
        GraphicsConfig.pushGlobal(prefs());
        long noticeSession=ControllerLoadNotice.session();
        boolean controllerLoaded=ControllerRuntime.refresh(dataRoot,controllerCRC,prefs().getAll());
        NativeApp.setSetting("SPU2/Output", "Backend", "string", "Oboe");
        // Use the donor's compatibility output to avoid AAudio MMAP/idle-reclaim
        // behavior on Home/Recents returns; volume and mute remain user settings.
        NativeApp.setSetting("SPU2/Output", "AndroidOpenSLES", "bool", "true");
        // Legacy achievement objects and configuration do not match the new core's JSON API.
        NativeApp.setSetting("Achievements", "Enabled", "bool", "false");
        // The core's API-not-Automatic popup belongs to its settings-warning group.
        NativeApp.setSetting("EmuCore","WarnAboutUnsafeSettings","bool","false");
        NativeApp.commitSettings();
        boolean gameController=false;
        try{gameController=ControllerSettings.gameConfigEnabled(dataRoot,controllerCRC);}catch(IOException failure){Log.w(TAG,"Cannot read controller configuration status",failure);}
        ControllerLoadNotice.applied(noticeSession,controllerLoaded&&gameController);
        ControllerRuntime.reapplyRemote();
        NativeApp.usbApplyPorts();
        NativeApp.setFreeSoftwareNotice(false);
    }
    public static String getScmVersion() { return ready ? "ARMSX2 " + NativeApp.getBuildVersion() : "ARMSX2 adapter (initializing)"; }
    public static String getFullScmVersion() { return getScmVersion() + " (Aether UI, experimental local bridge)"; }
    public static void runVMThread(Context activity, String path, String state) {
        initialize(activity);
        if (activity instanceof Activity) org.libsdl.app.SDL.setContext((Activity)activity);
        ControllerLoadNotice.beginSession();
        gamePath = path == null ? "" : path;
        gameSerial = ""; gameCRC = 0; controllerCRC = 0;
        if (!gamePath.isEmpty()) {
            Object entry = hostCall("getGameListEntry",new Class<?>[]{String.class},gamePath);
            if(entry != null) try {
                gameSerial = (String)entry.getClass().getMethod("getSerial").invoke(entry);
                gameCRC = (Integer)entry.getClass().getMethod("getCRC").invoke(entry);
                GraphicsConfig.prepareProfile(dataRoot,gameSerial,gameCRC);
            } catch(ReflectiveOperationException failure) { Log.w(TAG,"Cannot resolve pre-boot profile",failure); }
        }
        controllerCRC=gamePath.isEmpty()?0:launchControllerCRC(gamePath,gameSerial,gameCRC); // Ready before VM boot, retained through BIOS CRC zero.
        stopping = false;
        synchronized (Bridge.class) {
            limiterMode = 0; fastForwardHeld = false;
            menuPaused = false; appliedPause = null;
            audioPause.reset();sessionActive=true;
            paused = !activityResumed || !surfaceReady || GameAudioFocus.pauseRequired();
        }
        try {
        sessionStart = SystemClock.elapsedRealtime();
        hostCall("onVMStarting");
        do {
            resetting = false;
            updateAudioPausePolicy();
            syncSettings();
            synchronized (Bridge.class) { appliedPause = null; }
            final String bootState = state;
            Thread observer = new Thread(() -> observeBoot(bootState), "ArmsxBootObserver");
            observer.setDaemon(true); observer.start();
            try {
                Log.i(TAG, "CORE_RUN path=" + gamePath);
                boolean initialized = NativeApp.runVMThread(gamePath);
                String bootError = NativeApp.getLastBootError();
                Log.i(TAG, "CORE_EXIT returned=" + initialized + " error=" + bootError);
                if (!initialized || (bootError != null && !bootError.isEmpty())) error("Boot ARMSX2 gagal: " + bootError);
            } finally {
                observer.interrupt();
                ControllerRumble.setActive(false);
                ControllerRuntime.setActive(false);
                synchronized (Bridge.class) { limiterMode = 0; fastForwardHeld = false; }
            }
            state = null;
        } while (resetting && !stopping);
        } finally {
            ControllerLoadNotice.endSession();
            synchronized(Bridge.class){sessionStart=0;sessionActive=false;updateAudioPausePolicy();}
        }
    }
    private static void observeBoot(String state) {
        String previous = "", bootPath=gamePath;
        boolean started = false, identityRemembered=false;
        try {
            while (!stopping && !Thread.currentThread().isInterrupted()) {
                // A pause can arrive before the native VM exists during boot.
                // Reconcile once it is active instead of losing that request.
                reconcilePauseState();
                if (NativeApp.hasActiveVM() && NativeApp.getPresentedFrameCount() > 0) {
                    String serial = NativeApp.getGameSerial(), crcString = NativeApp.getGameCRC();
                    String title = NativeApp.getPauseGameTitle();
                    String identity = serial + ":" + crcString + ":" + title;
                    int crc = 0;
                    try { crc = (int)Long.parseLong(crcString.replace("0x", ""), 16); } catch (Exception ignored) { }
                    if (!identity.equals(previous)) {
                        gameSerial = serial; gameCRC = crc;
                        if(crc!=0)controllerCRC=crc;
                        if(crc!=0&&!identityRemembered){
                            app.getSharedPreferences("aetherarms_controller_identity",0).edit().putString(bootPath,serial+"\n"+String.format(Locale.ROOT,"%08X",crc)).apply();
                            identityRemembered=true;
                        }
                        ControllerRumble.refresh(dataRoot,controllerCRC,prefs().getAll());
                        if (GraphicsConfig.prepareProfile(dataRoot,serial,crc)) {
                            NativeApp.reloadGameSettingsLayer();
                        }
                        syncSettings();
                        hostCall("onGameChanged", new Class<?>[]{String.class, String.class, String.class, int.class},
                            gamePath, serial, title, crc);
                        Log.i(TAG, "CORE_PRESENTING serial=" + serial + " crc=" + crcString + " title=" + title);
                        previous = identity;
                    }
                    if (!started) { hostCall("onVMStarted"); started = true; }
                    if(crc!=0)ControllerLoadNotice.presenting();
                    // ELF CRC becomes available after the first BIOS frames; wait for it
                    // before restoring a game state instead of treating CRC zero as final.
                    if (state != null && !state.isEmpty() && crc != 0) {
                        if (NativeApp.loadStateFromFile(state) != 1) error("State tidak dapat dimuat oleh core ARMSX2.");
                        state = null;
                    }
                }
                Thread.sleep(300);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (RuntimeException e) { Log.e(TAG, "Boot observer", e); }
    }
    public static void changeSurface(Surface surface, int width, int height, float refresh) {
        surfaceReady = surface != null;
        ControllerRuntime.surface(width,height);
        if (surface == null) {
            releaseFastForward();
            reconcilePauseState();
            NativeApp.onNativeSurfaceDestroyed();
        } else {
            NativeApp.setDisplayRefreshRate(refresh);
            NativeApp.onNativeSurfaceChanged(surface, width, height);
            reconcilePauseState();
        }
    }
    public static boolean hasValidRenderSurface() { return surfaceReady; }
    public static synchronized void onEmulationPaused(Activity activity) {
        Activity owner = emulationActivity.get();
        if (owner != null && owner != activity) return;
        emulationActivity = new WeakReference<>(activity);
        activityResumed = false;
        ControllerLoadNotice.hide();
        releaseFastForward();
        reconcilePauseState();
        Log.i(TAG, "CORE_ACTIVITY background");
    }
    public static synchronized void onEmulationResumed(Activity activity) {
        if(ready&&NativeApp.hasActiveVM())reloadGameSettings();
        emulationActivity = new WeakReference<>(activity);
        activityResumed = true;
        ControllerLoadNotice.attach(activity);
        reconcilePauseState();
        GameAudioFocus.retry();
        Log.i(TAG, "CORE_ACTIVITY foreground");
    }
    private static void updateFocusWanted(){GameAudioFocus.wanted(ready&&sessionActive&&activityResumed&&surfaceReady&&!stopping&&!resetting);}
    public static synchronized void onAudioFocusChanged(){reconcilePauseState();}
    private static synchronized void updateAudioPausePolicy() {
        updateFocusWanted();
        boolean foreground=ready&&sessionActive&&activityResumed&&surfaceReady&&!stopping&&!resetting&&!GameAudioFocus.pauseRequired();
        ControllerRumble.setActive(foreground&&!paused&&!menuPaused);
        ControllerRuntime.setActive(foreground&&!paused&&!menuPaused);
        if (ready) audioPause.update(foreground,menuPaused,paused);
    }
    private static synchronized void reconcilePauseState() {
        updateFocusWanted();
        boolean wanted = menuPaused || !activityResumed || !surfaceReady || GameAudioFocus.pauseRequired();
        // Report the requested pause immediately, even before the native callback.
        // This keeps opening a pause menu during boot from losing the menu pause.
        if (wanted) paused = true;
        // Audio background changes matter even if the VM is already menu-paused.
        // Apply these before the deduplication and BEFORE queueing a native pause.
        updateAudioPausePolicy();
        if (!ready || stopping || resetting || !NativeApp.hasActiveVM()) return;
        if (appliedPause != null && appliedPause.booleanValue() == wanted) return;
        appliedPause = wanted;
        if (wanted) { releaseFastForward(); NativeApp.pause(); }
        else NativeApp.resume();
    }
    public static synchronized void onPaused(boolean value) {
        paused = value;
        if (value) releaseFastForward();
        // Native boot/resume may race a background or missing-surface event.
        // Retry the requested pause on the observer's next pass in that case.
        if (!value && (menuPaused || !activityResumed || !surfaceReady || GameAudioFocus.pauseRequired())) {
            paused = true;
            appliedPause = null;
        }
        // resume() queues work: clear menu suppression only after this callback,
        // so a queued pause cannot race an early stream release.
        updateAudioPausePolicy();
        Log.i(TAG, "CORE_PAUSED=" + value);
    }
    public static synchronized void pauseVM(boolean pause) {
        menuPaused = pause;
        if(!pause)GameAudioFocus.userPlay();
        reconcilePauseState();
    }
    public static boolean isVMPaused() { return paused; }
    public static void stopVMThreadLoop(boolean saveResume) {
        RemotePlayMenu.stopSession();
        releaseFastForward();
        stopping = true;
        updateAudioPausePolicy();
        if (saveResume && NativeApp.hasActiveVM() && !NativeApp.saveStateToSlot(-1)) error("Tidak dapat menyimpan resume state ARMSX2.");
        NativeApp.shutdown();
    }
    public static void resetVM() {
        RemotePlayMenu.stopSession();
        releaseFastForward(); resetting = true;
        updateAudioPausePolicy();
        NativeApp.shutdown();
    }
    public static void applySettings() { reloadGameSettings(); }
    public static void reloadGameSettings() {
        if (NativeApp.hasActiveVM()) {
            GraphicsConfig.prepareProfile(dataRoot,gameSerial,gameCRC);
            NativeApp.reloadGameSettingsLayer();
        }
        // Reload the game layer BEFORE commit so changes apply immediately.
        syncSettings();
    }
    public static void reloadPatches() { NativeApp.reloadPatches(); }
    public static void changeDisc(String path) { if (!NativeApp.changeDisc(path)) error("Gagal mengganti disc."); else gamePath = path; }
    public static synchronized void saveStateSlot(int slot) { if (!NativeApp.saveStateToSlot(slot)) error("Gagal menyimpan state; core mungkin belum siap atau memory card sibuk."); }
    public static void loadStateSlot(int slot) { if (!NativeApp.loadStateFromSlot(slot)) error("State tidak dapat dimuat oleh core ARMSX2."); }
    public static synchronized void waitForSaveStateFlush() { /* ARMSX2 saveStateToSlot uses synchronous zip_on_thread=false. */ }
    public static Bitmap readSaveStateScreenshot(String path) {
        byte[] bytes = NativeApp.getSaveStateImage(path);
        return bytes == null ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    // Legacy "toggle" is not the donor's direct mode setter (0 normal, 1 turbo,
    // 2 slow motion, 3 unlimited). Keep non-touch callers working as toggles.
    public static synchronized void toggleLimiterMode(int mode) {
        if (mode < 0 || mode > 3 || !ready || !NativeApp.hasActiveVM()) return;
        fastForwardHeld = false;
        applyLimiterMode(limiterMode == mode ? 0 : mode, "toggle");
    }
    public static synchronized void setFastForwardHeld(int mode, boolean down) {
        if (mode != 1 && mode != 3) return;
        if (down) {
            if (!ready || paused || stopping || !NativeApp.hasActiveVM()) return;
            if (fastForwardHeld && limiterMode == mode) return;
            fastForwardHeld = true;
            applyLimiterMode(mode, "hold");
        } else if (fastForwardHeld && limiterMode == mode) {
            fastForwardHeld = false;
            applyLimiterMode(0, "release");
        }
    }
    private static synchronized void releaseFastForward() {
        if (!fastForwardHeld && limiterMode != 1 && limiterMode != 3) return;
        fastForwardHeld = false;
        applyLimiterMode(0, "lifecycle");
    }
    private static void applyLimiterMode(int mode, String source) {
        limiterMode = mode;
        if (ready && NativeApp.hasActiveVM()) NativeApp.speedhackLimitermode(mode);
        Log.i(TAG, "LIMITER mode=" + mode + " source=" + source);
    }
    public static void toggleSoftwareRenderer() {
        if (NativeApp.isHardwareRenderer()) NativeApp.renderSoftware();
        else if ("14".equals(String.valueOf(prefs().getAll().get("EmuCore/GS/Renderer")))) NativeApp.renderVulkan();
        else NativeApp.renderOpenGL();
    }
    public static void setDisplayAlignment(int alignment) {
        NativeApp.setPortraitRenderTop(alignment == 0); NativeApp.setLandscapeRenderTop(alignment == 0);
        if (alignment == 2) unsupported("alignment kanan/bawah");
    }
    public static long getCurrentSessionTime() { return sessionStart == 0 ? 0 : (SystemClock.elapsedRealtime() - sessionStart) / 1000; }
    public static void saveSingleFrameGSDump() { NativeApp.captureGsDump(1); }
    public static void setPadValue(int pad,int bind,float value){ControllerRuntime.setPad(pad,bind,value);}
    public static void handleControllerButtonEvent(int device,int code,boolean down){ControllerRuntime.button(device,code,down);}
    public static void handleControllerAxisEvent(int device,int axis,float value){ControllerRuntime.axis(device,axis,value);}
    public static boolean hasAnyBindingsForControllerButton(int device,int button){return ControllerRuntime.hasButton(device,button);}
    public static void handleAccelerometerUpdate(float x,float y,float z){ControllerRuntime.sensor(x,y,z);}
    public static void handlePointerEvent(int state,float x,float y){
        Activity owner=emulationActivity.get();
        if(owner!=null){int id=owner.getResources().getIdentifier("fullscreen_content","id",owner.getPackageName());android.view.View view=owner.findViewById(id);if(view!=null&&view.getWidth()>0&&view.getHeight()>0)ControllerRuntime.surface(view.getWidth(),view.getHeight());}
        ControllerRuntime.pointer(state,x,y);
    }
    public static void setPadMacroState(int pad,int macro,boolean pressed){ControllerRuntime.macro(pad,macro,pressed);}
    public static void setUSBDeviceBind(int port,int bind,float value){ControllerRuntime.usb(port,bind,value);}
    public static void controllerHotkey(String name,boolean down){
        if(name.equals("ToggleTurbo")){setFastForwardHeld(1,down);return;}
        if(name.equals("ToggleFrameLimit")){setFastForwardHeld(3,down);return;}
        if(!down)return;
        Activity activity=emulationActivity.get();
        switch(name){
            case "QuickSave":saveStateSlot(0);break;
            case "QuickLoad":loadStateSlot(0);break;
            case "ToggleSlowMotion":toggleLimiterMode(2);break;
            case "TogglePause":if(activity!=null)activity.onBackPressed();break;
            case "ToggleSoftwareRendering":toggleSoftwareRenderer();break;
            default:unsupported("controller hotkey "+name);
        }
    }
    public static boolean cheevosLogin(String user, String password) { error("RetroAchievements belum dihubungkan ke UI Aether pada adapter ini."); return false; }
    public static void cheevosLogout() { unsupported("RetroAchievements"); }
    public static void onRumble(int pad, int large, int small) {
        // Native calls back without checking JNI exceptions; keep failures inside Java.
        try { ControllerRumble.motors(pad,large,small);ControllerDevices.rumble(pad,large,small); }
        catch(Throwable failure) { Log.w(TAG,"Controller rumble callback failed",failure); }
    }
    public static void playSound(String path) { hostCall("playSoundAsync", new Class<?>[]{String.class}, path); }
}
