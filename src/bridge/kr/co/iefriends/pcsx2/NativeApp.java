// SPDX-License-Identifier: GPL-3.0-or-later
package kr.co.iefriends.pcsx2;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.view.Surface;
import java.io.File;
import org.local.aetherarms.Bridge;

/** JNI contract for the pinned ARMSX2 2.9.1 donor core. */
public final class NativeApp {
    public static void load() {
        long pageSize = 4096;
        try { pageSize = Os.sysconf(OsConstants._SC_PAGESIZE); } catch (Exception ignored) { }
        System.loadLibrary(pageSize >= 16384 ? "emucore_16k" : "emucore_4k");
    }
    public static native void initialize(String root, String bios, int api);
    public static native String getBuildVersion();
    public static native boolean runVMThread(String path);
    public static native String getLastBootError();
    public static native void shutdown();
    public static native boolean hasActiveVM();
    public static native void pause();
    public static native void resume();
    public static native void setOutputPauseSuppressed(boolean suppressed);
    public static native void setAppBackgrounded(boolean backgrounded);
    public static native void onNativeSurfaceCreated();
    public static native void onNativeSurfaceChanged(Surface surface, int width, int height);
    public static native void onNativeSurfaceDestroyed();
    public static native void setDisplayRefreshRate(float hz);
    public static native void setSetting(String section, String key, String type, String value);
    public static native void commitSettings();
    public static native void setFreeSoftwareNotice(boolean show);
    public static native void reloadGameSettingsLayer();
    public static native int reloadPatches();
    public static native void usbLightgunAim(float x,float y);
    public static native void usbLightgunButton(int port,int bind,boolean pressed);
    public static native void usbApplyPorts();
    public static native void setPadButtonForPort(int port, int key, int range, boolean down);
    public static native void setPortraitRenderTop(boolean top);
    public static native void setLandscapeRenderTop(boolean top);
    public static native void speedhackLimitermode(int mode);
    public static native void renderSoftware();
    public static native void renderOpenGL();
    public static native void renderVulkan();
    public static native boolean isHardwareRenderer();
    public static native boolean saveStateToSlot(int slot);
    public static native boolean loadStateFromSlot(int slot);
    public static native int loadStateFromFile(String path);
    public static native byte[] getSaveStateImage(String path);
    public static native boolean changeDisc(String path);
    public static native void captureGsDump(int frames);
    public static native String getGameTitle(String path);
    public static native String getGameSerial();
    public static native String getPauseGameTitle();
    public static native String getGameCRC();
    public static native int getPresentedFrameCount();

    public static volatile boolean sRemotePlayer2;
    public static volatile int sRemotePlayer2Rumble;
    public static native boolean remotePlayStart(int maxWidth,int maxHeight);
    public static native void remotePlayStop();
    public static native int remotePlayPollVideo(java.nio.ByteBuffer buffer,int[] info,int timeoutMs);
    public static native int remotePlayPollAudio(java.nio.ByteBuffer buffer,int capFrames,int timeoutMs);
    public static native boolean remotePlayCounters(int[] out);
    public static native long remotePlayImportFrame(int slot,int texture);
    public static native void remotePlayDestroyImage(long image);
    public static native void remotePlayReleaseFrame(int slot,int seq);
    public static native void remotePlaySetHardware(boolean enabled);
    public static native boolean remotePlayPaused();
    public static native String remotePlayUpnpPrepare();
    public static native String remotePlayUpnpMap(int internalPort,int externalHint,int leaseSeconds);
    public static native void remotePlayUpnpUnmap(int externalPort);

    public static void vmSetPaused(boolean paused) { Bridge.onPaused(paused); }
    public static void onPadRumble(int pad, int large, int small) {
        if(pad==1&&sRemotePlayer2){sRemotePlayer2Rumble=(Math.max(0,Math.min(255,large))<<8)|Math.max(0,Math.min(255,small));return;}
        Bridge.onRumble(pad, large, small);
    }
    public static void playSound(String path) { Bridge.playSound(path); }
    public static void onAutoTuneCommitted(String serial, String section, String key, String value) {
        android.util.Log.i("AetherArmsBridge", "Core auto-tune: " + serial + " " + section + "/" + key + "=" + value);
    }
    public static String[] findSiblingChds(String uri) { return ContentChdFiles.findSiblings(Bridge.context().getContentResolver(), Uri.parse(uri)); }
    public static int openContentUri(String uri) {
        try {
            ParcelFileDescriptor fd = Bridge.context().getContentResolver().openFileDescriptor(Uri.parse(uri), "r");
            return fd == null ? -1 : fd.detachFd();
        } catch (Exception e) { android.util.Log.e("AetherArmsBridge", "openContentUri", e); return -1; }
    }
    public static boolean createDirectoryPath(String path) {
        File f = new File(path); return f.isDirectory() || f.mkdirs();
    }
    public static boolean createFilePath(String path) {
        try {
            File f = new File(path), parent = f.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return false;
            return f.isFile() || f.createNewFile();
        } catch (Exception e) { return false; }
    }
}
