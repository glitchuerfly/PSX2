// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Locale;

/** Controller-port settings always show Default art, independent of gameplay skins. */
public final class ControllerBindingIcons {
    private ControllerBindingIcons(){}
    public static Drawable load(Context context,int resource){
        String name=context.getResources().getResourceEntryName(resource);
        if(name.startsWith("ic_controller_")){
            String key=name.substring(14).replace("_button","");
            if(key.equals("up")||key.equals("right")||key.equals("left"))key="dpad_"+key;
            else if(key.equals("down"))key="dpad_down";
            else if(key.equals("analog_base"))key="stick_base";
            else if(key.equals("analog_stick"))key="thumb";
            int id=context.getResources().getIdentifier("controller_default_"+key,"drawable",context.getPackageName());
            if(id!=0)return context.getDrawable(id);
            if(key.matches("m[1-4]")||key.equals("pressure_modifier")||key.equals("analog"))
                return new Label(key.equals("pressure_modifier")?"P":key.equals("analog")?"AN":key.toUpperCase(Locale.ROOT));
        }
        return context.getDrawable(resource);
    }
    private static final class Label extends Drawable {
        final String text;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Label(String text){this.text=text;}
        @Override public void draw(Canvas canvas){
            Rect b=getBounds();float size=Math.min(b.width(),b.height()),cx=b.exactCenterX(),cy=b.exactCenterY();
            paint.setColor(0xff303030);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(cx,cy,size*.46f,paint);
            paint.setColor(0xffeeeeee);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextSize(size*.36f);
            canvas.drawText(text,cx,cy-(paint.ascent()+paint.descent())*.5f,paint);
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        @Override public int getIntrinsicWidth(){return 40;}
        @Override public int getIntrinsicHeight(){return 40;}
    }
}
