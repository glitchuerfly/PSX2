// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay
import android.app.*
import android.content.*
import android.media.*
import android.os.*
import android.graphics.Color
import android.view.*
import android.widget.*
import org.json.JSONObject
import org.webrtc.*
import org.local.aetherarms.ActivityLocale
import org.local.aetherarms.EmulationFullscreen

/** Viewer owns only its stream/input/audio, never a second local VM. */
class RemoteViewerActivity:Activity(){
    private var session:ViewerSession?=null
    private var input:ViewerInput?=null
    private var renderer:SurfaceViewRenderer?=null
    private lateinit var pad:RemotePadView
    private lateinit var status:TextView
    private lateinit var leave:Button
    private lateinit var reply:LinearLayout
    private lateinit var stats:TextView
    private var replyCode=""
    private var lastDevice=-1
    private var resumed=false
    private var focusAllowsAudio=true
    private var connected=false
    private var ended=false
    private val main=Handler(Looper.getMainLooper())
    private var audioFocus:AudioFocusRequest?=null
    private var focusGeneration=0
    private val hide:Runnable=object:Runnable{override fun run(){if(pad.holding()){main.postDelayed(this,1000);return};if(RemotePlayPrefs.touchControls(this@RemoteViewerActivity)==RevealMode.TAP){pad.clear();pad.visibility=View.INVISIBLE};if(RemotePlayPrefs.exitButton(this@RemoteViewerActivity)==RevealMode.TAP)leave.visibility=View.INVISIBLE}}
    private val sample=object:Runnable{override fun run(){
        session?.sampleStats{line->main.post{if(!ended)stats.text=line}}
        if(!ended&&resumed)main.postDelayed(this,1000)
    }}
    override fun attachBaseContext(base:Context){super.attachBaseContext(RemoteTheme.wrap(base))}
    override fun onCreate(saved:Bundle?){
        super.onCreate(saved);RemoteRuntime.initialize(this);RemoteText.use(this)
        EmulationFullscreen.apply(this,true);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root=FrameLayout(this).apply{setBackgroundColor(Color.BLACK)}
        val screen=SurfaceViewRenderer(this).also{renderer=it}
        root.addView(screen,FrameLayout.LayoutParams(-1,-1))
        pad=RemotePadView(this){state->input?.setTouch(state)}
        root.addView(pad,FrameLayout.LayoutParams(-1,-1))
        status=TextView(this).apply{setTextColor(Color.WHITE);setTextSize(14f);gravity=Gravity.CENTER;setPadding(dp(20),dp(8),dp(20),dp(8));setBackgroundColor(0xaa000000.toInt());text=RemoteText.get("remoteplay.invite.preparing")}
        root.addView(status,FrameLayout.LayoutParams(-1,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(52)})
        leave=RemoteUi.button(this,true).apply{text=RemoteText.get("remoteplay.viewer.leave");setOnClickListener{askLeave()}}
        root.addView(leave,FrameLayout.LayoutParams(-2,dp(48),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        stats=TextView(this).apply{setTextColor(Color.WHITE);setTextSize(11f);setBackgroundColor(0xaa000000.toInt());setPadding(dp(8),dp(4),dp(8),dp(4));visibility=if(RemotePlayPrefs.streamStats(this@RemoteViewerActivity))View.VISIBLE else View.GONE}
        root.addView(stats,FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.START))
        reply=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(12),dp(20),dp(12));setBackgroundColor(0xdd181818.toInt());visibility=View.GONE}
        root.addView(reply,FrameLayout.LayoutParams(-1,-2,Gravity.CENTER))
        root.setOnTouchListener{_,e->if(e.actionMasked==MotionEvent.ACTION_DOWN)reveal();true}
        setContentView(root)
        val mode=runCatching{when{
            intent.hasExtra("lobby")->RemotePlayLobby.normalize(intent.getStringExtra("lobby").orEmpty())?.let{ViewerMode.Lobby(it)}
            intent.hasExtra("offer")->ViewerMode.Invite(JSONObject(intent.getStringExtra("offer").orEmpty()))
            intent.hasExtra("host")->ViewerMode.Wifi(intent.getStringExtra("host").orEmpty(),intent.getIntExtra("port",RemotePlayProtocol.PORT))
            else->null
        }}.getOrNull()
        if(mode==null){finish();return}
        try{
            RemotePlayRtc.initialize(this)
            screen.init(RemotePlayRtc.eglBase.eglBaseContext,null)
            screen.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            screen.setEnableHardwareScaler(true)
            session=ViewerSession(this,mode,null,{lastDevice},
                {interrupted->if(interrupted){input?.releaseAll();pad.clear()};status.text=if(interrupted)RemoteText.get("remoteplay.viewer.lost") else "";status.visibility=if(interrupted)View.VISIBLE else View.GONE},
                {paused->status.text=RemoteText.get("remoteplay.viewer.hostPaused");status.visibility=if(paused)View.VISIBLE else View.GONE},
                {code->showReply(code)},
                {connected=true;status.visibility=View.GONE;reply.visibility=View.GONE;reveal()},
                {status.visibility=View.VISIBLE;status.text=RemoteText.get("remoteplay.viewer.waitingAccept")},
                {message->onEnded(message)})
            input=ViewerInput{session?.sendPad(it)}
            session?.showOn(screen)
            session?.start()
        }catch(failure:RuntimeException){onEnded(RemoteText.get("remoteplay.error.connect"))}
        reveal()
    }
    private fun showReply(code:String){
        replyCode=code;reply.removeAllViews();reply.visibility=View.VISIBLE
        reply.addView(TextView(this).apply{text=RemoteText.get("remoteplay.invite.step1");setTextColor(Color.WHITE)})
        reply.addView(TextView(this).apply{text=code;setTextIsSelectable(true);maxLines=3;setTextColor(Color.WHITE)})
        val buttons=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        buttons.addView(RemoteUi.button(this,true).apply{text=RemoteText.get("remoteplay.code.copy");setOnClickListener{getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Remote Play",replyCode))}})
        buttons.addView(RemoteUi.button(this,true).apply{text=RemoteText.get("remoteplay.code.share");setOnClickListener{startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,RemoteText.format("remoteplay.reply.share",replyCode)),text))}})
        for(index in 0 until buttons.childCount)buttons.getChildAt(index).layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)}
        reply.addView(buttons,LinearLayout.LayoutParams(-1,-2))
    }
    private fun reveal(){
        if(!::pad.isInitialized)return
        pad.visibility=if(connected&&RemotePlayPrefs.touchControls(this)!=RevealMode.OFF)View.VISIBLE else View.INVISIBLE
        leave.visibility=if(RemotePlayPrefs.exitButton(this)!=RevealMode.OFF)View.VISIBLE else View.INVISIBLE
        main.removeCallbacks(hide);main.postDelayed(hide,15_000)
    }
    internal fun touchActivity(){reveal()}
    private fun askLeave(){
        input?.releaseAll();pad.clear()
        RemoteUi.dialogBuilder(this).setMessage(RemoteText.get("remoteplay.viewer.leaveQuestion"))
            .setPositiveButton(RemoteText.get("remoteplay.viewer.leave")){_,_->finish()}.setNegativeButton(android.R.string.cancel,null).showMonochrome()
    }
    private fun onEnded(message:String?){
        if(ended)return
        ended=true;main.removeCallbacks(sample);main.removeCallbacks(hide)
        input?.releaseAll();if(::pad.isInitialized)pad.clear()
        abandonFocus()
        if(isFinishing||isDestroyed)return
        if(message==null)finish()else RemoteUi.dialogBuilder(this).setMessage(message).setCancelable(false).setPositiveButton(android.R.string.ok){_,_->finish()}.showMonochrome()
    }
    override fun onResume(){
        super.onResume();resumed=true;EmulationFullscreen.apply(this,true)
        focusAllowsAudio=true;requestFocus();session?.setForeground(true);session?.audioForeground(focusAllowsAudio)
        if(RemotePlayPrefs.streamStats(this))main.post(sample)
    }
    override fun onPause(){
        resumed=false;main.removeCallbacks(sample);main.removeCallbacks(hide)
        input?.releaseAll();if(::pad.isInitialized)pad.clear()
        session?.setForeground(false);session?.audioForeground(false);abandonFocus();super.onPause()
    }
    override fun onDestroy(){ended=true;session?.end(null);session=null;renderer?.release();renderer=null;main.removeCallbacksAndMessages(null);abandonFocus();super.onDestroy()}
    override fun onWindowFocusChanged(has:Boolean){super.onWindowFocusChanged(has);if(has)EmulationFullscreen.apply(this,true)else{input?.releaseAll();if(::pad.isInitialized)pad.clear()}}
    override fun onBackPressed(){askLeave()}
    override fun dispatchKeyEvent(event:KeyEvent):Boolean{
        if(event.keyCode==KeyEvent.KEYCODE_BACK||event.keyCode==KeyEvent.KEYCODE_BUTTON_MODE){if(event.action==KeyEvent.ACTION_UP)askLeave();return true}
        if(event.isFromSource(InputDevice.SOURCE_GAMEPAD)||event.isFromSource(InputDevice.SOURCE_DPAD)||event.isFromSource(InputDevice.SOURCE_JOYSTICK)){
            lastDevice=event.deviceId
            if(input?.onKey(event)==true){if(RemotePlayPrefs.touchControls(this)==RevealMode.TAP){pad.clear();pad.visibility=View.INVISIBLE};return true}
        }
        return super.dispatchKeyEvent(event)
    }
    override fun dispatchGenericMotionEvent(event:MotionEvent):Boolean{
        if(event.isFromSource(InputDevice.SOURCE_JOYSTICK)){lastDevice=event.deviceId;if(input?.onMotion(event)==true)return true}
        return super.dispatchGenericMotionEvent(event)
    }
    private fun requestFocus(){
        if(ended||audioFocus!=null)return
        val token=++focusGeneration
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener({change->if(token==focusGeneration&&resumed&&!ended){focusAllowsAudio=change==AudioManager.AUDIOFOCUS_GAIN;session?.audioForeground(focusAllowsAudio);if(!focusAllowsAudio){input?.releaseAll();pad.clear()}}},main).build()
        audioFocus=request
        runCatching{getSystemService(AudioManager::class.java).requestAudioFocus(request)}
    }
    private fun abandonFocus(){focusGeneration++;audioFocus?.let{runCatching{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)}};audioFocus=null}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
