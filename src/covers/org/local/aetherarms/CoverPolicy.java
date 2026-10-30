// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.util.Locale;
import java.io.File;
/** Pure cover selection policy; filenames and URLs never contain arbitrary game paths. */
public final class CoverPolicy {
    public static String serial(String value) {
        if(value==null) return "";
        String clean=value.trim().toUpperCase(Locale.ROOT).replace('_','-');
        return clean.matches("[A-Z0-9]{4,8}-[A-Z0-9]{3,8}")?clean:"";
    }
    public static String relativeFile(String serial,boolean threeD) {
        String clean=serial(serial);
        if(clean.isEmpty()) throw new IllegalArgumentException("Invalid disc serial");
        return (threeD?"3d/":"2d/")+clean+(threeD?".png":".jpg");
    }
    private static boolean usable(File file){return file!=null&&file.isFile()&&file.length()>0;}
    public static File selectExisting(boolean manual,File legacy,File selected,File other){
        if(manual&&usable(legacy))return legacy;
        if(usable(selected))return selected;
        if(usable(other))return other;
        return usable(legacy)?legacy:null;
    }
    public static String url(String serial,boolean threeD) {
        return "https://raw.githubusercontent.com/xlenore/ps2-covers/main/covers/"+
            relativeFile(serial,threeD).replace("2d/","default/");
    }
}
