// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Aether's effective controller layer; no Android or native calls on the rumble path. */
public final class RumbleSettings {
    public final boolean enabled;
    public final int port, throttleMs;
    public RumbleSettings(Map<String,?> values) {
        enabled=truth(values.get("TouchscreenController/EnableVibration"));
        port=integer(values.get("TouchscreenController/PortIndex"),0,0,1);
        throttleMs=integer(values.get("AndroidInputSource/VibrationThrottle"),10,0,1000);
    }
    private static boolean truth(Object value) {
        String text=String.valueOf(value);
        return text.equalsIgnoreCase("true")||text.equals("1")||text.equalsIgnoreCase("yes");
    }
    private static int integer(Object value,int fallback,int low,int high) {
        try { return Math.max(low,Math.min(high,Integer.parseInt(String.valueOf(value)))); }
        catch(NumberFormatException failure) { return fallback; }
    }
    public static Map<String,Object> effective(Map<String,?> global,Map<String,?> layer) {
        Map<String,Object> values=new HashMap<>();values.putAll(global);values.putAll(layer);return values;
    }
    public static RumbleSettings load(File root,int crc,Map<String,?> global) throws IOException {
        Map<String,String> game=Collections.emptyMap(),layer=Collections.emptyMap();
        if(crc!=0) {
            File file=new File(root,"gamesettings/"+String.format(Locale.ROOT,"%08X.ini",crc));
            if(file.isFile()) game=read(file);
        }
        // Same priority as EmulationActivity.L: per-game controller settings,
        // otherwise the game's named input profile, otherwise global. Missing keys inherit global.
        if(truth(game.get("Pad/UseGameSettingsForController"))) layer=game;
        else {
            String name=game.get("Pad/InputProfileName");
            if(name!=null&&!name.isEmpty()) {
                File directory=new File(root,"inputprofiles").getCanonicalFile();
                File profile=new File(directory,name+".ini").getCanonicalFile();
                if(!directory.equals(profile.getParentFile())) throw new IOException("Invalid input profile path");
                if(profile.isFile()) layer=read(profile);
            }
        }
        return new RumbleSettings(effective(global,layer));
    }
    private static Map<String,String> read(File file) throws IOException {
        try(Reader reader=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)) {
            return GraphicsSettingsModel.readIni(reader);
        }
    }
}
