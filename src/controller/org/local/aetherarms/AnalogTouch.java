// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import java.util.WeakHashMap;

/** Gameplay-only hooks; the legacy dispatcher retains pointer ownership and release/cancel. */
public final class AnalogTouch {
    private static final WeakHashMap<View,AnalogGesture> gestures=new WeakHashMap<>();
    private static final String KEY="TouchscreenController/AnalogMode";
    private AnalogTouch() {}
    private static boolean dynamic(View view) {
        Context context=view.getContext();
        for(int depth=0;depth<16&&context!=null;depth++) {
            if(context.getClass().getName().equals("xyz.aethersx2.android.EmulationActivity")) {
                try {
                    // I reads the Activity's effective per-game/input-profile controller
                    // layer with global fallback, just like all existing touchscreen options.
                    String mode=(String)context.getClass().getMethod("I",String.class,String.class).invoke(context,KEY,"dynamic");
                    return AnalogGesture.isDynamic(mode);
                } catch(ReflectiveOperationException failure) { break; }
            }
            if(!(context instanceof ContextWrapper))break;
            Context next=((ContextWrapper)context).getBaseContext();if(next==context)break;context=next;
        }
        Object mode=view.getContext().getSharedPreferences(view.getContext().getPackageName()+"_preferences",Context.MODE_PRIVATE).getAll().get(KEY);
        return AnalogGesture.isDynamic(mode==null?null:String.valueOf(mode));
    }
    public static boolean canStart(View view,MotionEvent event,int index,Rect hit) {
        int action=event.getActionMasked();
        if((action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_POINTER_DOWN)||event.getActionIndex()!=index)return false;
        float sx=view.getScaleX(),sy=view.getScaleY();
        if(sx<=0||sy<=0)return false;
        // Use the exact integer coordinates/center used by Aether's existing dispatcher.
        float x=((int)event.getX(index)-hit.left)/sx-view.getWidth()/2;
        float y=((int)event.getY(index)-hit.top)/sy-view.getHeight()/2;
        AnalogGesture gesture=gestures.get(view);
        if(gesture==null){gesture=new AnalogGesture();gestures.put(view,gesture);}
        boolean accepted=gesture.start(action,event.getActionIndex(),index,x,y,
            view.getWidth()-view.getPaddingLeft()-view.getPaddingRight(),
            view.getHeight()-view.getPaddingTop()-view.getPaddingBottom(),dynamic(view));
        if(accepted)redraw(view);
        return accepted;
    }
    public static float dragX(View view,float x){AnalogGesture g=gestures.get(view);return g==null?x:g.dragX(x);}
    public static float dragY(View view,float y){AnalogGesture g=gestures.get(view);if(g!=null)redraw(view);return g==null?y:g.dragY(y);}
    public static void translate(View view,Canvas canvas){
        AnalogGesture g=gestures.get(view);if(g!=null)canvas.translate(g.offsetX(),g.offsetY());
    }
    public static void release(View view){
        AnalogGesture g=gestures.get(view);if(g==null)return;
        g.release();redraw(view);
    }
    private static void redraw(View view){
        view.invalidate();
        // Dynamic art may extend outside the old view rectangle. Repaint its
        // unclipped parent so no old base/knob pixels remain after moving/releasing.
        ViewParent parent=view.getParent();if(parent instanceof View)((View)parent).invalidate();
    }
}
