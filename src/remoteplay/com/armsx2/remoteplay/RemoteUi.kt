// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout

/** Clear action surfaces and neutral colors, scoped to Remote Play. */
internal object RemoteUi {
    fun dp(context:Context,value:Int)=(value*context.resources.displayMetrics.density).toInt()
    fun color(context:Context,name:String):Int = context.getColor(context.resources.getIdentifier("remote_play_"+name,"color",context.packageName))
    private fun shape(context:Context,darkOverlay:Boolean=false)=GradientDrawable().apply{
        setColor(if(darkOverlay)0xff242424.toInt() else color(context,"surface"))
        cornerRadius=dp(context,8).toFloat()
        setStroke(dp(context,1).coerceAtLeast(1),if(darkOverlay)0xff9e9e9e.toInt() else color(context,"outline"))
    }
    fun panel(context:Context)=shape(context)
    fun margins(context:Context)=LinearLayout.LayoutParams(-1,-2).apply{
        setMargins(dp(context,20),dp(context,6),dp(context,20),dp(context,6))
    }
    fun button(context:Context,darkOverlay:Boolean=false)=Button(context).apply{
        isAllCaps=false;transformationMethod=null;letterSpacing=0f
        gravity=Gravity.START or Gravity.CENTER_VERTICAL
        setTypeface(null,Typeface.NORMAL);setTextSize(16f)
        minWidth=0;minimumWidth=0;minHeight=dp(context,48);minimumHeight=dp(context,48)
        setPadding(dp(context,16),dp(context,12),dp(context,16),dp(context,12))
        val foreground=if(darkOverlay)Color.WHITE else color(context,"foreground")
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(if(darkOverlay)0xff777777.toInt() else color(context,"disabled"),foreground)))
        backgroundTintList=null;stateListAnimator=null
        background=RippleDrawable(ColorStateList.valueOf(if(darkOverlay)0x33ffffff else color(context,"ripple")),shape(context,darkOverlay),null)
    }
    fun dialogBuilder(context:Context)=AlertDialog.Builder(context,context.resources.getIdentifier("RemotePlayDialogTheme","style",context.packageName))
    fun styleDialog(dialog:AlertDialog){
        val foreground=color(dialog.context,"foreground")
        for(which in intArrayOf(AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL))dialog.getButton(which)?.setTextColor(foreground)
    }
}

internal fun AlertDialog.Builder.showMonochrome():AlertDialog {
    val dialog=create()
    dialog.setOnShowListener{RemoteUi.styleDialog(dialog)}
    dialog.show()
    return dialog
}
