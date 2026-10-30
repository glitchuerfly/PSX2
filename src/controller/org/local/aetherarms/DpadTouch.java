// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/** Match hits to artwork expanded to its press-feedback size; retain the dispatcher's pointer ownership. */
public final class DpadTouch {
    private DpadTouch(){}
    private static int direct(View view,int x,int y){
        int w=view.getWidth()/3,h=view.getHeight()/3;if(w<=0||h<=0)return 0;
        float feedback=TouchFeedback.pressScale(w,h,view.getResources().getDisplayMetrics().density);int result=0;
        for(int d=0;d<4;d++){
            float lx=TouchFeedback.toArtwork(x-DpadGeometry.column(d)*w,w,feedback),ly=TouchFeedback.toArtwork(y-DpadGeometry.row(d)*h,h,feedback);
            if(lx<0||ly<0||lx>=w||ly>=h)continue;
            DpadMask skin=SkinStore.dpadMask(DpadGeometry.key(d));
            boolean hit=skin!=null?skin.hit(w,h,lx,ly):DpadGeometry.defaultHit(d,w,h,lx,ly);
            if(hit)result|=1<<d;
        }
        return result;
    }
    public static boolean canStart(View view,MotionEvent event,int index,Rect hit){
        return DpadGeometry.freshDown(event.getActionMasked(),event.getActionIndex(),index)&&hit(view,event,index,hit);
    }
    public static boolean hit(View view,MotionEvent event,int index,Rect hit){
        float sx=view.getScaleX(),sy=view.getScaleY();if(sx<=0||sy<=0)return false;
        int x=(int)(((int)event.getX(index)-hit.left)/sx),y=(int)(((int)event.getY(index)-hit.top)/sy);
        return direct(view,x,y)!=0;
    }
    public static int mask(View view,int x,int y,boolean pressed){
        return pressed?DpadGeometry.heldMask(view.getWidth(),view.getHeight(),x,y,direct(view,x,y)):0;
    }
}
