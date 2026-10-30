// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

/** Adds the requested patch credit below the existing, selectable version information. */
public final class AppVersionContent {
    private AppVersionContent(){}
    private static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}

    public static AlertDialog.Builder attach(AlertDialog.Builder builder,CharSequence version){
        Context context=builder.getContext();
        LinearLayout content=new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context,24),dp(context,8),dp(context,24),dp(context,8));
        TextView info=new TextView(context);
        info.setTextSize(16);info.setText(version);info.setTextIsSelectable(true);
        content.addView(info,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout credit=new LinearLayout(context);
        credit.setOrientation(LinearLayout.HORIZONTAL);credit.setGravity(Gravity.CENTER_VERTICAL);
        ImageView avatar=new ImageView(context);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        avatar.setOutlineProvider(new ViewOutlineProvider(){
            @Override public void getOutline(View view,Outline outline){outline.setOval(0,0,view.getWidth(),view.getHeight());}
        });
        avatar.setClipToOutline(true);
        try(InputStream image=context.getAssets().open("about/profile.jpg")){
            avatar.setImageBitmap(BitmapFactory.decodeStream(image));
        }catch(IOException failure){Log.w("AppVersion","Cannot load bundled patch profile",failure);}
        credit.addView(avatar,new LinearLayout.LayoutParams(dp(context,52),dp(context,52)));
        TextView label=new TextView(context);
        label.setText("patches by glitch");label.setTextSize(16);
        LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(0,-2,1f);
        labelParams.setMarginStart(dp(context,16));credit.addView(label,labelParams);
        LinearLayout.LayoutParams creditParams=new LinearLayout.LayoutParams(-1,-2);
        creditParams.topMargin=dp(context,20);content.addView(credit,creditParams);

        ScrollView scroll=new ScrollView(context);scroll.addView(content);
        return builder.setView(scroll);
    }
}
