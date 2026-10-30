// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.*
import kotlin.math.*

/** Aether's original pad artwork, with per-finger ownership and neutral-on-cancel input. */
internal class RemotePadView(context:Context,private val publish:(RemotePlayProtocol.PadState)->Unit):View(context){
    private data class Key(val name:String,val bit:Int,val x:Float,val y:Float,val shoulder:Boolean=false)
    private val keys=listOf(
        Key("up",0,.11f,.46f),Key("right",1,.16f,.60f),Key("down",2,.11f,.74f),Key("left",3,.06f,.60f),
        Key("select",4,.45f,.87f),Key("start",5,.55f,.87f),Key("l3",6,.37f,.87f),Key("r3",7,.63f,.87f),
        Key("triangle",8,.89f,.46f),Key("circle",9,.94f,.60f),Key("cross",10,.89f,.74f),Key("square",11,.84f,.60f),
        Key("l1",12,.08f,.28f,true),Key("r1",13,.92f,.28f,true),Key("l2",14,.22f,.28f,true),Key("r2",15,.78f,.28f,true))
    private val owners=mutableMapOf<Int,Int>()
    private val positions=mutableMapOf<Int,PointF>()
    private val anchors=mutableMapOf<Int,PointF>()
    private val icons=mutableMapOf<String,Drawable>()
    private var last=RemotePlayProtocol.PadState()
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init{
        isFocusable=false
        for(name in keys.map{it.name}+listOf("analog_base","analog_stick")){
            val id=resources.getIdentifier("ic_controller_"+name+(if(name.startsWith("analog_"))"" else "_button"),"drawable",context.packageName)
            if(id!=0)icons[name]=context.getDrawable(id)!!.mutate()
        }
    }
    private fun size()=min(50*resources.displayMetrics.density,min(width/12f,height/6f))
    private fun point(key:Key)=PointF(width*key.x,height*key.y)
    private fun center(stick:Int)=PointF(width*(if(stick==16).28f else .72f),height*.72f)
    override fun onDraw(canvas:Canvas){
        val s=size()
        for((i,key)in keys.withIndex()){
            val p=point(key);val held=if(i<14)last.buttons and (1 shl key.bit)!=0 else if(i==14)last.l2>0 else last.r2>0;val scale=if(held)1.4f else 1f
            draw(canvas,key.name,p.x,p.y,s*scale,if(key.shoulder)s*.65f*scale else s*scale,if(held)255 else 160)
        }
        for(stick in 16..17){
            val owner=owners.entries.firstOrNull{it.value==stick}?.key
            val p=owner?.let{anchors[it]}?:center(stick)
            draw(canvas,"analog_base",p.x,p.y,s*1.9f,s*1.9f,150)
            val xy=owner?.let{positions[it]}?:PointF(p.x,p.y)
            var dx=xy.x-p.x;var dy=xy.y-p.y;val length=hypot(dx,dy);val radius=s*.65f
            if(length>radius){dx*=radius/length;dy*=radius/length}
            draw(canvas,"analog_stick",p.x+dx,p.y+dy,s*.85f,s*.85f,200)
        }
    }
    private fun draw(canvas:Canvas,name:String,x:Float,y:Float,w:Float,h:Float,alpha:Int){
        val icon=icons[name]?:return;icon.alpha=alpha;icon.setBounds((x-w/2).toInt(),(y-h/2).toInt(),(x+w/2).toInt(),(y+h/2).toInt());icon.draw(canvas)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean{
        val action=event.actionMasked
        if(action==MotionEvent.ACTION_CANCEL){clear();return true}
        val index=event.actionIndex;val id=event.getPointerId(index)
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            val x=event.getX(index);val y=event.getY(index);val s=size()
            val selected=keys.indices.filter{val p=point(keys[it]);abs(x-p.x)<s*.7f&&abs(y-p.y)<s*.7f}.minByOrNull{val p=point(keys[it]);hypot(x-p.x,y-p.y)}
                ?: (16..17).firstOrNull{val p=center(it);hypot(x-p.x,y-p.y)<s*(if(context.getSharedPreferences(context.packageName+"_preferences",0).getString("TouchscreenController/AnalogMode","dynamic")=="static").425f else .95f)&&!owners.containsValue(it)}
            if(selected!=null){
                owners[id]=selected;positions[id]=PointF(x,y)
                if(selected>=16){val dynamic=context.getSharedPreferences(context.packageName+"_preferences",0).getString("TouchscreenController/AnalogMode","dynamic")!="static";anchors[id]=if(dynamic)PointF(x,y)else center(selected)}
            }
            (context as? RemoteViewerActivity)?.touchActivity()
        }
        for(i in 0 until event.pointerCount){val pid=event.getPointerId(i);if(owners.containsKey(pid))positions[pid]=PointF(event.getX(i),event.getY(i))}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){owners.remove(id);positions.remove(id);anchors.remove(id)}
        send();invalidate();return true
    }
    fun holding()=owners.isNotEmpty()
    fun clear(){owners.clear();positions.clear();anchors.clear();send();invalidate()}
    override fun onDetachedFromWindow(){clear();super.onDetachedFromWindow()}
    override fun onWindowFocusChanged(has:Boolean){if(!has)clear();super.onWindowFocusChanged(has)}
    private fun send(){
        var bits=0;var l2=0;var r2=0;val axes=intArrayOf(128,128,128,128)
        for((id,key)in owners){
            if(key<4){
                val p=positions[id]?:continue;val x=(p.x-width*.11f)/(width*.05f);val y=(p.y-height*.60f)/(height*.14f)
                if(hypot(x,y)<1.8f){if(x>.35f)bits=bits or RemotePlayProtocol.PAD_RIGHT;if(x<-.35f)bits=bits or RemotePlayProtocol.PAD_LEFT;if(y>.35f)bits=bits or RemotePlayProtocol.PAD_DOWN;if(y<-.35f)bits=bits or RemotePlayProtocol.PAD_UP}
            }else if(key<14){val p=positions[id]?:continue;val c=point(keys[key]);if(hypot(p.x-c.x,p.y-c.y)<size())bits=bits or (1 shl keys[key].bit)}
            else if(key==14)l2=255 else if(key==15)r2=255
            else{
                val p=positions[id]?:continue;val a=anchors[id]?:continue
                val radius=size()*.65f;var x=(p.x-a.x)/radius;var y=(p.y-a.y)/radius;val length=hypot(x,y)
                if(length>1){x/=length;y/=length}
                val offset=(key-16)*2;axes[offset]=RemotePlayProtocol.axisByte(x);axes[offset+1]=RemotePlayProtocol.axisByte(y)
            }
        }
        val state=RemotePlayProtocol.PadState(bits,axes[0],axes[1],axes[2],axes[3],l2,r2)
        if(last!=state){last=state;publish(state)}
    }
}
