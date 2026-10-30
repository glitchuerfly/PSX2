// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

/** Keep the original button dispatch, with a stable target as large as its press feedback. */
public final class ButtonTouch {
    private static final Rect otherBounds=new Rect(); // UI-thread dispatch only
    private ButtonTouch(){}
    public static boolean hit(View view,Rect hit,int x,int y){
        if(!SkinStore.ready())return false;
        float sx=view.getScaleX(),sy=view.getScaleY();if(sx<=0||sy<=0)return false;
        float density=view.getResources().getDisplayMetrics().density;
        if(!TouchFeedback.targetContains(view.getWidth(),view.getHeight(),density,(x-hit.left)/sx,(y-hit.top)/sy))return false;
        if(hit.contains(x,y)||!(view.getParent() instanceof ViewGroup))return true;
        // Keep original hits first. Where only enlarged margins overlap, choose
        // the nearest button so one near miss cannot press two neighboring keys.
        ViewGroup parent=(ViewGroup)view.getParent();int ownIndex=parent.indexOfChild(view);
        float dx=x-hit.exactCenterX(),dy=y-hit.exactCenterY(),distance=dx*dx+dy*dy;
        for(int i=0;i<parent.getChildCount();i++){
            View other=parent.getChildAt(i);
            if(other==view||other.getClass()!=view.getClass()||other.getVisibility()!=View.VISIBLE)continue;
            float ox=other.getScaleX(),oy=other.getScaleY();if(ox<=0||oy<=0)continue;
            other.getHitRect(otherBounds);
            if(otherBounds.contains(x,y))return false;
            if(!TouchFeedback.targetContains(other.getWidth(),other.getHeight(),density,(x-otherBounds.left)/ox,(y-otherBounds.top)/oy))continue;
            dx=x-otherBounds.exactCenterX();dy=y-otherBounds.exactCenterY();float candidate=dx*dx+dy*dy;
            if(candidate<distance||(candidate==distance&&i<ownIndex))return false;
        }
        return true;
    }
}
