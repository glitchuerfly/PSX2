// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.content.SharedPreferences;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import kr.co.iefriends.pcsx2.NativeApp;

/** Translate UI values and keep legacy per-game input separate from its native output. */
public final class GraphicsConfig {
    public static void pushGlobal(SharedPreferences prefs) {
        Map<String,?> raw=prefs.getAll();
        Map<String,String> settings=GraphicsSettingsModel.translate(raw,true);
        Map<String,String> types=new HashMap<>();
        for (GraphicsSettingsModel.Rule rule:GraphicsSchema.RULES) if(rule.supported) types.put(rule.target,rule.type);
        for (Map.Entry<String,String> entry:settings.entrySet()) {
            String key=entry.getKey();if(ControllerSettings.owns(key))continue;int split=key.lastIndexOf('/');
            Object original=raw.get(key);
            String type=types.get(key);
            if(type==null) type=original instanceof Boolean?"bool":original instanceof Integer||original instanceof Long?"int":original instanceof Float?"float":"string";
            NativeApp.setSetting(key.substring(0,split),key.substring(split+1),type,entry.getValue());
        }
        Log.i("AetherArmsBridge","GRAPHICS global upscale="+settings.get("EmuCore/GS/upscale_multiplier")+" filter="+settings.get("EmuCore/GS/linear_present_mode"));
    }
    public static boolean prepareProfile(File root,String serial,int crc) {
        try {
            boolean changed=ProfileMigrator.prepare(root,serial,crc);
            if(changed) Log.i("AetherArmsBridge","GRAPHICS per-game profile refreshed: "+serial+" "+String.format(Locale.ROOT,"%08X",crc));
            return changed;
        } catch(IOException|RuntimeException failure) {
            Log.e("AetherArmsBridge","Cannot translate per-game graphics",failure);return false;
        }
    }
}
