// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Effective Aether controller settings and typed native defaults; never flatten into CRC inputs. */
public final class ControllerSettings {
    public static final String UNBOUND="__AETHER_UNBOUND__";
    public static boolean emptyBindingOverride(String key,Set<?> bindings){return bindings!=null&&bindings.isEmpty()&&(key.matches("Pad[1-8]/.*")||key.matches("USB[12]/.*")||key.startsWith("Hotkeys/"));}
    public static Set<String> visibleBindings(Set<String> bindings){if(bindings==null||!bindings.contains(UNBOUND))return bindings;Set<String> visible=new LinkedHashSet<>(bindings);visible.remove(UNBOUND);return visible;}
    public static boolean owns(String key) {
        return key.startsWith("Pad/")||key.matches("Pad[1-8]/.*")||key.startsWith("TouchscreenController/")
            ||key.startsWith("AndroidInputSource/")||key.startsWith("InputSources/")||key.startsWith("Hotkeys/")||key.matches("USB[12]/.*")
            ||key.equals("EmuCore/InputProfileName");
    }
    public static boolean truth(Object value) {return "true".equalsIgnoreCase(String.valueOf(value))||"1".equals(String.valueOf(value))||"yes".equalsIgnoreCase(String.valueOf(value));}
    public static int integer(Object raw,int fallback,int min,int max) {
        if(raw instanceof Number){double value=((Number)raw).doubleValue();if(Double.isFinite(value))return (int)Math.max(min,Math.min(max,value));}
        try{return Math.max(min,Math.min(max,Integer.parseInt(String.valueOf(raw))));}catch(RuntimeException e){return fallback;}
    }
    public static float number(Object raw,float fallback,float min,float max) {
        try{float v=Float.parseFloat(String.valueOf(raw));return Float.isFinite(v)?Math.max(min,Math.min(max,v)):fallback;}catch(RuntimeException e){return fallback;}
    }
    public static List<String> bindings(Object raw) {
        List<String> out=new ArrayList<>();
        if(raw instanceof Collection){for(Object item:(Collection<?>)raw)if(item!=null&&!UNBOUND.equals(String.valueOf(item))&&!String.valueOf(item).trim().isEmpty())out.add(String.valueOf(item).trim());}
        else if(raw!=null)for(String value:String.valueOf(raw).split("\\n"))if(!value.trim().isEmpty()&&!value.trim().equals(UNBOUND))out.add(value.trim());
        return out;
    }
    public static Map<String,Object> read(File file) throws IOException {
        Map<String,Object> out=new LinkedHashMap<>();if(!file.isFile())return out;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))){
            String section="",line;
            while((line=reader.readLine())!=null){
                line=line.replace("\ufeff","").trim();if(line.isEmpty()||line.startsWith(";")||line.startsWith("#"))continue;
                if(line.startsWith("[")&&line.endsWith("]")){section=line.substring(1,line.length()-1).trim();continue;}
                int split=line.indexOf('=');if(section.isEmpty()||split<=0)continue;
                String key=section+"/"+line.substring(0,split).trim(),value=line.substring(split+1).trim();
                if(value.length()>=2&&value.startsWith("\"")&&value.endsWith("\""))value=value.substring(1,value.length()-1);
                Object old=out.get(key);
                if(old==null)out.put(key,value);
                else {LinkedHashSet<String> all=new LinkedHashSet<>(bindings(old));all.add(value);out.put(key,all);}
            }
        }return out;
    }
    /** Selected on-disk controller layer; null means global settings. */
    public static File profileFile(File root,int crc) throws IOException {
        File gameFile=new File(root,"gamesettings/"+String.format(Locale.ROOT,"%08X.ini",crc));
        Map<String,Object> game=crc==0?Collections.emptyMap():read(gameFile);
        if(truth(game.get("Pad/UseGameSettingsForController")))return gameFile;
        String name=String.valueOf(game.getOrDefault("Pad/InputProfileName",""));
        if(name.isEmpty())return null;
        File directory=new File(root,"inputprofiles").getCanonicalFile(),profile=new File(directory,name+".ini").getCanonicalFile();
        if(!directory.equals(profile.getParentFile()))throw new IOException("Invalid controller input profile path");
        return profile;
    }
    public static Map<String,Object> effective(File root,int crc,Map<String,?> global) throws IOException {
        File profile=profileFile(root,crc);
        Map<String,Object> layer=profile==null?Collections.emptyMap():read(profile);
        Map<String,Object> out=new LinkedHashMap<>();
        for(Map.Entry<String,?> e:global.entrySet())if(owns(e.getKey()))out.put(e.getKey(),e.getValue());
        for(Map.Entry<String,Object> e:layer.entrySet())if(owns(e.getKey()))out.put(e.getKey(),e.getValue());
        return out;
    }
    public static boolean explicitPhoneMotor(Map<String,?> values){
        for(int p=1;p<=8;p++)for(String motor:new String[]{"LargeMotor","SmallMotor"})for(String binding:bindings(values.get("Pad"+p+"/"+motor)))if(binding.startsWith("__DEVICE_VIBRATOR__/Vibrator"))return true;
        return false;
    }
    /** Same orientation values as EmulationActivity.R; UI overrides are independent of Pad's switch. */
    public static int editorOrientation(Map<String,?> global,Map<String,?> game) {
        String key="UI/EmulationActivityOrientation";
        Object value=game.get(key);if(value==null)value=global.get(key);if(value==null)value="landscape";
        switch(String.valueOf(value)) {
            case "portrait": return 12; // SCREEN_ORIENTATION_USER_PORTRAIT
            case "landscape": return 11; // SCREEN_ORIENTATION_USER_LANDSCAPE
            case "sensor": return 4; // SCREEN_ORIENTATION_SENSOR
            default: return -1; // unspecified / legacy unknown value
        }
    }
    public static Map<String,String> nativeValues(Map<String,?> effective,Map<String,String> defaults,Map<String,String> types) {
        Map<String,String> result=new TreeMap<>();
        for(Map.Entry<String,String> e:defaults.entrySet()){
            String key=e.getKey(),fallback=e.getValue(),type=types.getOrDefault(key,"string");Object raw=effective.getOrDefault(key,null);
            if(raw==null)raw=fallback;
            if(type.equals("bool"))result.put(key,truth(raw)?"true":"false");
            else if(type.equals("int"))result.put(key,Integer.toString(integer(raw,integer(fallback,0,Integer.MIN_VALUE,Integer.MAX_VALUE),Integer.MIN_VALUE,Integer.MAX_VALUE)));
            else if(type.equals("float"))result.put(key,Float.toString(number(raw,number(fallback,0,-Float.MAX_VALUE,Float.MAX_VALUE),-Float.MAX_VALUE,Float.MAX_VALUE)));
            else result.put(key,String.valueOf(raw));
        }
        // Aether controller-profile selection is resolved by Java. The donor's differently
        // named input layer must not override that resolution with an older named profile.
        result.put("EmuCore/InputProfileName","");
        return result;
    }
}
