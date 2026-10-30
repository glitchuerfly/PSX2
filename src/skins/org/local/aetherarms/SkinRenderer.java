// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.view.View;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.*;

/** Artwork only; all original Aether touch dispatch, bindings and editor state remain in charge. */
public final class SkinRenderer {
    private static final WeakHashMap<View,String> ids=new WeakHashMap<>();
    private static final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private static String id(View view) {
        String name=ids.get(view);
        if(name==null){
            try{name=view.getResources().getResourceEntryName(view.getId());}catch(Exception ignored){name="";}
            ids.put(view,name);SkinStore.register(view);
        }
        return name;
    }
    private static String buttonKey(String name) {
        if(!name.startsWith("controller_button_"))return null;
        String key=name.substring(18);
        if(key.length()==2&&key.charAt(0)=='m'&&key.charAt(1)>='1'&&key.charAt(1)<='4')key="macro"+key.substring(1);
        return SkinPolicy.KEYS.contains(key)?key:null;
    }
    public static void button(View view,Canvas canvas,Drawable original,boolean pressed) {
        String key=buttonKey(id(view));
        if(!SkinStore.ready())return;
        Bitmap bitmap=key==null?null:SkinStore.bitmap(key);
        if(bitmap==null&&key!=null&&key.length()==2&&(key.charAt(0)=='l'||key.charAt(0)=='r')&&(key.charAt(1)=='1'||key.charAt(1)=='2')){
            shoulder(view,canvas,key,pressed);return;
        }
        draw(canvas,bitmap,original,0,0,view.getWidth(),view.getHeight(),pressed,view.getResources().getDisplayMetrics().density);
    }
    public static void axis(View view,Canvas canvas,Drawable base,Drawable stick,int dx,int dy) {
        String side=id(view).endsWith("right")?"right":"left";
        if(!SkinStore.ready())return;
        int save=canvas.save();AnalogTouch.translate(view,canvas);
        Bitmap b=SkinStore.bitmap("analog_base_"+side),s=SkinStore.bitmap("analog_stick_"+side);
        if(b==null)b=SkinStore.bitmap("analog_base");if(s==null)s=SkinStore.bitmap("analog_stick");
        float x=view.getPaddingLeft(),y=view.getPaddingTop();
        float w=view.getWidth()-x-view.getPaddingRight(),h=view.getHeight()-y-view.getPaddingBottom();
        draw(canvas,b,base,x,y,w,h,false,1);
        float sw=w*TouchFeedback.STICK_SIZE,sh=h*TouchFeedback.STICK_SIZE;
        draw(canvas,s,stick,view.getWidth()*.5f+dx-sw*.5f,view.getHeight()*.5f+dy-sh*.5f,sw,sh,false,1);
        canvas.restoreToCount(save);
    }
    public static void dpad(View view,Canvas canvas,int direction,Drawable original,boolean pressed,int x,int y,int w,int h) {
        id(view);
        if(!SkinStore.ready())return;
        draw(canvas,SkinStore.bitmap(DpadGeometry.key(direction)),original,x,y,w,h,pressed,view.getResources().getDisplayMetrics().density);
    }
    private static void draw(Canvas canvas,Bitmap bitmap,Drawable original,float x,float y,float width,float height,boolean pressed,float viewDensity) {
        if(width<=0||height<=0)return;
        float iw=bitmap!=null?bitmap.getWidth():original==null?0:original.getIntrinsicWidth();
        float ih=bitmap!=null?bitmap.getHeight():original==null?0:original.getIntrinsicHeight();
        if(iw<=0||ih<=0)return;
        float scale=SkinPolicy.fitScale(width,height,iw,ih);
        float w=iw*scale,h=ih*scale,cx=x+width*.5f,cy=y+height*.5f;
        int save=canvas.save();
        if(pressed){float feedback=TouchFeedback.pressScale(width,height,viewDensity);canvas.scale(feedback,feedback,cx,cy);}
        if(bitmap!=null){
            canvas.translate(cx-w*.5f,cy-h*.5f);canvas.scale(scale,scale);
            paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(bitmap,0,0,paint);
        }else{
            original.setBounds(Math.round(cx-w*.5f),Math.round(cy-h*.5f),Math.round(cx+w*.5f),Math.round(cy+h*.5f));
            original.draw(canvas);
        }
        canvas.restoreToCount(save);
    }
    private static void shoulder(View view,Canvas canvas,String key,boolean pressed) {
        float density=view.getResources().getDisplayMetrics().density;
        float w=view.getWidth(),h=view.getHeight(),stroke=1.8f*density;
        int save=canvas.save();if(pressed){float feedback=TouchFeedback.pressScale(w,h,density);canvas.scale(feedback,feedback,w*.5f,h*.5f);}
        paint.setColor(0xffeeeeee);paint.setAlpha(255);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);
        canvas.drawRoundRect(stroke,stroke,w-stroke,h-stroke,9*density,9*density,paint);
        paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(h*.62f,w*.36f));
        canvas.drawText(key.toUpperCase(Locale.ROOT),w*.5f,h*.5f-(paint.ascent()+paint.descent())*.5f,paint);
        canvas.restoreToCount(save);
    }
}
