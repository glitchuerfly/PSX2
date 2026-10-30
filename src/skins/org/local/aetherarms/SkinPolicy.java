// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.net.URLEncoder;
import java.util.*;

/** Filename compatibility with ARMSX2 community/iOS PNG packs and geometry policy. */
public final class SkinPolicy {
    public static final String REPO="bagasromadon/ARMSX2-CustomControllerSkins";
    public static final String RAW="https://raw.githubusercontent.com/"+REPO+"/main/";
    public static final Set<String> KEYS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "cross","circle","square","triangle","l1","l2","l3","r1","r2","r3",
        "start","select","up","right","down","left","analog_base","analog_stick",
        "analog_base_left","analog_base_right","analog_stick_left","analog_stick_right",
        "macro1","macro2","macro3","macro4")));
    public static String keyForFilename(String name) {
        String path=name.replace('\\','/');
        if(path.startsWith("/")||path.indexOf(':')>=0) return null;
        for(String part:path.split("/")) if(part.equals("..")) return null;
        String n=path.substring(path.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
        if(n.startsWith("._")||!n.endsWith(".png")) return null;
        n=n.substring(0,n.length()-4);
        if(n.startsWith("ic_controller_")) n=n.substring(14);
        if(n.endsWith("_button")) n=n.substring(0,n.length()-7);
        if(n.matches("m[1-4]")) n="macro"+n.substring(1);
        if(n.matches("macro_[1-4]")) n="macro"+n.substring(6);
        return KEYS.contains(n)?n:null;
    }
    public static String url(String path,String folder,String suffix) {
        if(path==null||!path.startsWith(folder+"/")||!path.toLowerCase(Locale.ROOT).endsWith(suffix))
            throw new IllegalArgumentException("Invalid repository path");
        StringBuilder out=new StringBuilder(RAW);
        for(String part:path.split("/",-1)) {
            if(part.isEmpty()||part.equals(".")||part.equals("..")||part.indexOf('\\')>=0||part.indexOf(':')>=0)
                throw new IllegalArgumentException("Invalid repository path");
            if(out.length()>RAW.length()) out.append('/');
            try {out.append(URLEncoder.encode(part,"UTF-8").replace("+","%20"));}
            catch(java.io.UnsupportedEncodingException impossible) {throw new AssertionError(impossible);}
        }
        return out.toString();
    }
    public static float fitScale(float width,float height,float imageWidth,float imageHeight) {
        if(width<=0||height<=0||imageWidth<=0||imageHeight<=0) return 0;
        return Math.min(width/imageWidth,height/imageHeight);
    }
}
