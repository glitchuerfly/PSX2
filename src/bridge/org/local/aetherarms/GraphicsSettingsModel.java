// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import java.io.*;
import java.util.*;

/** Platform-independent conversion and INI handling, shared by runtime and host checks. */
public final class GraphicsSettingsModel {
    public static final class Rule {
        public final String source, target, type, fallback, transform;
        public final boolean supported;
        public Rule(String s,String t,String kind,String value,String x,boolean ok) {
            source=s;target=t;type=kind;fallback=value;transform=x;supported=ok;
        }
    }
    public static boolean isGraphics(String key) {
        return key.startsWith("EmuCore/GS/") || key.equals("EmuCore/EnableNoInterlacingPatches");
    }
    public static String normalize(Rule rule,Object raw) {
        return normalize(rule,raw,false);
    }
    private static String normalize(Rule rule,Object raw,boolean nativeValue) {
        String value=String.valueOf(raw).trim();
        // A canonical target key already uses native units and must not be converted again.
        String transform=nativeValue ? "identity" : rule.transform;
        if (transform.equals("mipmap")) return Integer.parseInt(value)==0 ? "false" : "true";
        if (transform.equals("messages")) return truth(value) ? "1" : "0";
        if (transform.equals("autoflush")) return truth(value) ? "2" : "0";
        if (rule.type.equals("bool")) return truth(value) ? "true" : "false";
        if (rule.type.equals("int")) {
            int number=Integer.parseInt(value);
            if (transform.equals("renderer")) {
                if (number==-1) number=12; // Retired Auto values in global and legacy per-game profiles.
                if (number!=12 && number!=13 && number!=14)
                    throw new IllegalArgumentException("Unsupported Android renderer: "+value);
            }
            return Integer.toString(number);
        }
        if (rule.type.equals("float")) {
            float number=Float.parseFloat(value);
            if (Float.isNaN(number)||Float.isInfinite(number)) throw new IllegalArgumentException("Non-finite value");
            // Keep the legacy UI percentage relative to the donor's compact handheld baseline.
            if (transform.equals("osd_scale")) number=(float)(number*0.65d);
            return Float.toString(number);
        }
        return value;
    }
    private static boolean truth(String value) {
        if (value.equalsIgnoreCase("true")||value.equals("1")||value.equalsIgnoreCase("yes")) return true;
        if (value.equalsIgnoreCase("false")||value.equals("0")||value.equalsIgnoreCase("no")) return false;
        throw new IllegalArgumentException("Invalid boolean: "+value);
    }
    public static Map<String,String> translate(Map<String,?> source,boolean defaults) {
        Map<String,String> output=new TreeMap<>();
        Set<String> known=new HashSet<>();
        for (Rule rule:GraphicsSchema.RULES) {
            known.add(rule.source);
            if (!rule.supported) continue;
            boolean nativeValue=!rule.source.equals(rule.target) && source.get(rule.target)!=null;
            Object raw=source.get(nativeValue ? rule.target : rule.source);
            if (raw==null && defaults) raw=rule.fallback;
            if (raw==null || raw instanceof Set) continue;
            try { output.put(rule.target,normalize(rule,raw,nativeValue)); }
            catch (IllegalArgumentException invalid) {
                if (defaults && rule.fallback!=null) output.put(rule.target,normalize(rule,rule.fallback));
            }
        }
        // Preserve non-graphics game settings, with their native section/key structure.
        for (Map.Entry<String,?> item:source.entrySet()) {
            String key=item.getKey(); Object value=item.getValue();
            if (known.contains(key)||isGraphics(key)||value==null||value instanceof Set) continue;
            if (key.lastIndexOf('/')>0) output.put(key,String.valueOf(value));
        }
        return output;
    }
    public static Map<String,String> readIni(Reader input) throws IOException {
        Map<String,String> out=new LinkedHashMap<>();
        BufferedReader reader=new BufferedReader(input); String section="",line;
        while ((line=reader.readLine())!=null) {
            line=line.replace("\ufeff","").trim();
            if (line.isEmpty()||line.startsWith(";")||line.startsWith("#")) continue;
            if (line.startsWith("[")&&line.endsWith("]")) { section=line.substring(1,line.length()-1).trim();continue; }
            int split=line.indexOf('=');
            if (split<=0||section.isEmpty()) continue;
            String value=line.substring(split+1).trim();
            if (value.length()>=2 && value.startsWith("\"") && value.endsWith("\"")) value=value.substring(1,value.length()-1);
            out.put(section+"/"+line.substring(0,split).trim(),value);
        }
        return out;
    }
    public static String writeIni(Map<String,String> map) {
        StringBuilder out=new StringBuilder("; AetherArms managed graphics translation v1\n"); String section="";
        for (Map.Entry<String,String> entry:new TreeMap<>(map).entrySet()) {
            int split=entry.getKey().lastIndexOf('/');
            if (split<=0) continue;
            String next=entry.getKey().substring(0,split),key=entry.getKey().substring(split+1),value=entry.getValue();
            if ((next+key+value).indexOf('\n')>=0 || (next+key+value).indexOf('\r')>=0 || next.indexOf(']')>=0) continue;
            if (!next.equals(section)) { section=next;out.append('\n').append('[').append(section).append("]\n"); }
            out.append(key).append(" = ").append(value).append('\n');
        }
        return out.toString();
    }
}
