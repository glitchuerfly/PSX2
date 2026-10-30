// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

/** Shared visual geometry, independent of Android for host regression checks. */
public final class TouchFeedback {
    private TouchFeedback() {}
    public static final float STICK_SIZE=.6f;
    public static float toArtwork(float coordinate,float extent,float feedback){return (coordinate-extent*.5f)/feedback+extent*.5f;}
    public static boolean targetContains(float width,float height,float density,float x,float y){
        if(width<=0||height<=0)return false;
        float feedback=pressScale(width,height,density);
        x=toArtwork(x,width,feedback);y=toArtwork(y,height,feedback);
        return x>=0&&y>=0&&x<width&&y<height;
    }
    public static float pressScale(float width,float height,float density) {
        float longest=Math.max(width,height);
        if(longest<=0||density<=0)return 1;
        // Original Aether extends a 50 dp face button by 10 dp on each side.
        // Use one scale on both axes so rectangular custom art stays proportional.
        return 1+20*density/longest;
    }
}
