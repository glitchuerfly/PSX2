// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

/** A fresh DOWN anchors input neutrally; dynamic mode also relocates the visible base. */
public final class AnalogGesture {
    private float originX,originY;
    private boolean active,dynamic;
    public static boolean isDynamic(String mode) { return !"static".equals(mode); }
    // Retain the original center-only model for static mode and existing callers.
    public boolean start(int action,int actionIndex,int index,float x,float y,float width,float height) {
        return start(action,actionIndex,index,x,y,width,height,false);
    }
    public boolean start(int action,int actionIndex,int index,float x,float y,float width,float height,boolean floating) {
        if((action!=0&&action!=5)||actionIndex!=index)return false;
        float radius=Math.min(width,height)*(floating?1f:TouchFeedback.STICK_SIZE)*.5f;
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(radius)||radius<=0||x*x+y*y>radius*radius)return false;
        originX=x;originY=y;dynamic=floating;active=true;return true;
    }
    public float dragX(float x){return active?x-originX:x;}
    public float dragY(float y){return active?y-originY:y;}
    public float offsetX(){return active&&dynamic?originX:0;}
    public float offsetY(){return active&&dynamic?originY:0;}
    public void release(){active=false;dynamic=false;originX=originY=0;}
}
