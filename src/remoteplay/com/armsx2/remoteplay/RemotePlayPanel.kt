// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay
import android.app.*
import android.content.*
import android.os.*
import android.view.*
import android.widget.*

/** Content owned by the actual Aether settings/pause Fragment, with no extra window. */
internal class RemotePlayPanel(private val activity:Activity,private val page:Int):ContextWrapper(activity){
    private val layoutInflater get()=activity.layoutInflater
    private var active=false
    private val dialogs=mutableListOf<Dialog>()
    private lateinit var rows:LinearLayout
    private lateinit var hosts:LinearLayout
    private lateinit var status:TextView
    private lateinit var invite:TextView
    private lateinit var hostWifi:View
    private lateinit var hostOnline:View
    private lateinit var stop:View
    private lateinit var reply:View
    private lateinit var copy:View
    private lateinit var share:View
    private lateinit var scan:View
    private var found=emptyList<RemotePlayHostInfo>()
    private var browser:HostBrowser?=null
    private val main=Handler(Looper.getMainLooper())
    private var shown=""
    private val refresh=object:Runnable{override fun run(){update();main.postDelayed(this,500)}}
    private val stopScan=Runnable{
        browser?.stop();browser=null
        if(::scan.isInitialized)actionTitle(scan,RemoteText.get("remoteplay.join.scan"))
        if(::hosts.isInitialized&&found.isEmpty()){hosts.removeAllViews();hosts.addView(text(RemoteText.local("No hosts found on this Wi-Fi.","Tidak ada host ditemukan di Wi-Fi ini."),14))}
    }
    fun create():View {
        // Native PreferenceFragmentCompat does this before inflating any preference rows.
        org.local.aetherarms.RemotePreferenceTheme.prepare(activity)
        RemoteRuntime.initialize(activity);RemoteText.use(activity)
        rows=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        // Leave both surfaces transparent: the native Settings/pause window owns its background.
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(rows)}
        if(page==0){
        section(RemoteText.get("remoteplay.title"))
        note(RemoteText.local("Join a friend's game as player 2 over Wi-Fi or an invite code. To host, start your game, open the pause menu, then tap the Remote Play icon.","Gabung ke game teman sebagai pemain 2 melalui Wi-Fi atau kode undangan. Untuk host, mulai game, buka pause menu, lalu tekan ikon Remote Play."))
        row("remoteplay.join.code","remoteplay.join.codeHint"){input("remoteplay.join.codeField"){joinCode(it)}}
        scan=row("remoteplay.join.scan","remoteplay.join.scanHint"){
            if(browser!=null){main.removeCallbacks(stopScan);stopScan.run();return@row}
            found=emptyList()
            browser=HostBrowser(this){list->found=list;showHosts(list)}.also{it.start()}
            actionTitle(scan,RemoteText.get("remoteplay.join.scanStop"))
            showHosts(emptyList());main.removeCallbacks(stopScan);main.postDelayed(stopScan,60_000)
        }
        row("remoteplay.join.manual",null){input("remoteplay.join.address"){text->
            val parsed=RemoteAddress.parse(text)
            if(parsed==null)error(RemoteText.get("remoteplay.error.connect"))else launch("host",parsed.first,parsed.second)
        }}
        hosts=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};rows.addView(hosts)
        section(RemoteText.get("remoteplay.screen.title"),separated=true)
        choice("remoteplay.screen.touch",{RemotePlayPrefs.touchControls(this)}){RemotePlayPrefs.setTouchControls(this,it)}
        choice("remoteplay.screen.exit",{RemotePlayPrefs.exitButton(this)}){RemotePlayPrefs.setExitButton(this,it)}
        toggle("remoteplay.screen.stats","remoteplay.screen.statsDesc",RemotePlayPrefs.streamStats(this)){RemotePlayPrefs.setStreamStats(this,it)}
        toggle("remoteplay.screen.qos","remoteplay.screen.qosDesc",RemotePlayPrefs.wifiQos(this)){RemotePlayPrefs.setWifiQos(this,it)}
        }else{

        status=note("")
        hostWifi=row("remoteplay.host.wifi",null){guard{RemotePlayHost.startWifi(this)}}
        hostOnline=row("remoteplay.host.invite",null){guard{RemotePlayHost.createInvite(this)}}
        invite=note("");invite.setTextIsSelectable(true)
        copy=row("remoteplay.code.copy",null){if(invite.text.isNotBlank()){
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Remote Play",invite.text))
        }}
        share=row("remoteplay.code.share",null){if(invite.text.isNotBlank())startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,invite.text.toString()),RemoteText.get("remoteplay.code.share")))}
        reply=row("remoteplay.invite.step2",null){input("remoteplay.invite.step2"){if(!RemotePlayHost.acceptReply(it))error(RemoteText.get("remoteplay.join.badCode"))}}
        stop=row("remoteplay.host.stop",null){RemotePlayHost.stop();update()}
        }
        update()
        return scroll
    }
    fun resume(){active=true;RemoteText.use(activity);main.removeCallbacks(refresh);main.post(refresh)}
    fun pause(){
        active=false;main.removeCallbacks(refresh);main.removeCallbacks(stopScan);stopScan.run()
        dialogs.toList().forEach{it.dismiss()};dialogs.clear()
    }
    private fun guard(action:()->Unit){runCatching(action).onFailure{error(RemoteText.get("remoteplay.host.failed"))};update()}
    private fun update(){
        if(!::status.isInitialized)return
        val s=RemotePlayHost.status.value
        val game=RemotePlayCore.runningGame()!=null
        actionEnabled(hostWifi,game&&(s is RemotePlayHost.Status.Off||s is RemotePlayHost.Status.Failed))
        actionEnabled(hostOnline,hostWifi.isEnabled||s is RemotePlayHost.Status.Waiting)
        stop.visibility=if(s is RemotePlayHost.Status.Off)View.GONE else View.VISIBLE
        val code=(s as? RemotePlayHost.Status.Inviting)?.let{it.lobbyCode?.let(RemotePlayLobby::display)?:it.code}.orEmpty()
        invite.text=code
        invite.visibility=if(code.isBlank())View.GONE else View.VISIBLE
        copy.visibility=invite.visibility;share.visibility=invite.visibility
        reply.visibility=if((s as? RemotePlayHost.Status.Inviting)?.code!=null)View.VISIBLE else View.GONE
        val text=when(s){
            RemotePlayHost.Status.Off->if(game)RemoteText.get("remoteplay.host.explain")else RemoteText.get("remoteplay.host.noGame")
            is RemotePlayHost.Status.Waiting->RemoteText.get("remoteplay.host.waiting")+s.address?.let{"\n"+RemoteText.format("remoteplay.host.address",it)}.orEmpty()
            is RemotePlayHost.Status.Inviting->RemoteText.get(if(code.isEmpty())"remoteplay.invite.preparing" else "remoteplay.invite.lobbyWaiting")
            is RemotePlayHost.Status.Connecting->RemoteText.format("remoteplay.host.connecting",s.viewer)
            is RemotePlayHost.Status.Asking->RemoteText.format("remoteplay.request.title",s.viewer)
            is RemotePlayHost.Status.Playing->RemoteText.format("remoteplay.host.playing",s.viewer)
            is RemotePlayHost.Status.Failed->s.message
        }
        if(text!=shown){shown=text;status.text=text}
    }
    private fun joinCode(code:String){
        RemotePlayLobby.normalize(code)?.let{launch("lobby",it);return}
        val offer=RemotePlayProtocol.decodeCode(code)
        if(offer?.optString("t")=="offer")launch("offer",offer.toString())else error(RemoteText.get("remoteplay.join.badCode"))
    }
    private fun launch(key:String,value:String,port:Int=RemotePlayProtocol.PORT){
        if(RemotePlayCore.runningGame()!=null){error(RemoteText.local("Close the current game before joining a friend.","Tutup game yang sedang berjalan sebelum bergabung ke teman."));return}
        startActivity(Intent(this,RemoteViewerActivity::class.java).putExtra(key,value).putExtra("port",port))
    }
    private fun showHosts(list:List<RemotePlayHostInfo>){
        if(!active||activity.isFinishing||activity.isDestroyed)return
        hosts.removeAllViews()
        if(list.isEmpty())hosts.addView(text(RemoteText.get("remoteplay.join.searching"),14))
        for(host in list){
            val view=preference(host.name,host.game,hosts)
            action(view){launch("host",host.address,host.port)}
        }
    }
    private fun input(key:String,accept:(String)->Unit){
        val edit=EditText(this).apply{hint=RemoteText.get(key);setSingleLine(key=="remoteplay.join.address")}
        dialogs.add(RemoteUi.dialogBuilder(activity).setTitle(RemoteText.get(key)).setView(edit)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok){_,_->accept(edit.text.toString().trim())}.showMonochrome())
    }
    private fun error(message:String){if(active)dialogs.add(RemoteUi.dialogBuilder(activity).setMessage(message).setPositiveButton(android.R.string.ok,null).showMonochrome())}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun text(label:String,size:Int)=TextView(this).apply{text=label;setTextSize(size.toFloat());setTextColor(RemoteUi.color(this@RemotePlayPanel,if(size>=16)"foreground" else "secondary"));setPadding(dp(20),dp(12),dp(20),dp(12))}
    private fun note(label:String)=text(label,14).also{rows.addView(it)}
    private fun section(label:String,separated:Boolean=false){
        if(separated)divider()
        val category=layoutInflater.inflate(resources.getIdentifier("preference_category_material","layout",packageName),rows,false)
        category.findViewById<View>(resources.getIdentifier("icon_frame","id",packageName))?.visibility=View.GONE
        category.findViewById<TextView>(android.R.id.title).text=label
        category.findViewById<View>(android.R.id.summary).visibility=View.GONE
        rows.addView(category)
    }
    private fun actionTitle(view:View,label:String){view.findViewById<TextView>(android.R.id.title).text=label}
    private fun actionEnabled(view:View,enabled:Boolean){
        view.isEnabled=enabled
        if(view is ViewGroup)for(index in 0 until view.childCount)actionEnabled(view.getChildAt(index),enabled)
    }
    private fun action(view:View,callback:()->Unit){
        view.setOnClickListener{if(active&&view.isEnabled)callback()}
        view.accessibilityDelegate=object:View.AccessibilityDelegate(){
            override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo){
                super.onInitializeAccessibilityNodeInfo(host,info)
                info.className="android.widget.Button"
            }
        }
    }
    private fun row(title:String,summary:String?,callback:()->Unit):View {
        val view=preference(RemoteText.get(title),summary?.let{RemoteText.get(it)}.orEmpty())
        action(view,callback)
        return view
    }
    /** All actions/preferences share Aether's title, smaller summary and native ripple. */
    private fun preference(title:String,description:String,parent:LinearLayout=rows):View {
        val view=layoutInflater.inflate(resources.getIdentifier("preference_material","layout",packageName),parent,false)
        view.findViewById<View>(resources.getIdentifier("icon_frame","id",packageName))?.visibility=View.GONE
        view.findViewById<TextView>(android.R.id.title).apply{
            text=title;setSingleLine(false);ellipsize=null
        }
        view.findViewById<TextView>(android.R.id.summary).apply{
            text=description;visibility=if(description.isBlank())View.GONE else View.VISIBLE
        }
        view.isFocusable=true;view.isClickable=true
        parent.addView(view,LinearLayout.LayoutParams(-1,-2))
        return view
    }
    /** A single category separator, rather than a border/divider on each row. */
    private fun divider(){
        val divider=View(this)
        val attr=android.util.TypedValue()
        if(theme.resolveAttribute(android.R.attr.listDivider,attr,true)){
            divider.background=if(attr.resourceId!=0)getDrawable(attr.resourceId)else android.graphics.drawable.ColorDrawable(attr.data)
        }
        divider.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        rows.addView(divider,LinearLayout.LayoutParams(-1,divider.background?.intrinsicHeight?.coerceAtLeast(1)?:1))
    }
    private fun choice(key:String,current:()->RevealMode,save:(RevealMode)->Unit){
        val description=RemoteText.get(key+"Summary")
        val view=preference(RemoteText.get(key),description)
        view.setOnClickListener{
            val modes=RevealMode.entries
            val dialog=org.local.aetherarms.RemotePreferenceDialogs.choice(activity,RemoteText.get(key),
                modes.map{RemoteText.get("remoteplay.screen."+it.key)}.toTypedArray(),modes.indexOf(current())){d,index->
                    save(modes[index]);d.dismiss()
                }
            dialogs.add(dialog)

        }
    }
    private fun toggle(key:String,description:String,on:Boolean,save:(Boolean)->Unit){
        val view=preference(RemoteText.get(key),RemoteText.get(description))
        val frame=view.findViewById<LinearLayout>(android.R.id.widget_frame)
        val widget=layoutInflater.inflate(resources.getIdentifier("preference_widget_switch_compat","layout",packageName),frame,false) as CompoundButton
        widget.isChecked=on;widget.contentDescription=RemoteText.get(key)
        // Keep Aether SwitchCompat's own blue activated tint and disabled states.
        widget.setOnCheckedChangeListener{_,checked->save(checked)}
        frame.addView(widget)
        view.setOnClickListener{widget.isChecked=!widget.isChecked}
        view.accessibilityDelegate=object:View.AccessibilityDelegate(){
            override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo){
                super.onInitializeAccessibilityNodeInfo(host,info)
                info.className="android.widget.Switch";info.isCheckable=true;info.isChecked=widget.isChecked
            }
        }
    }
}

/** Strict address handling without any network side effects, also used by host-only tests. */
internal object RemoteAddress {
    fun parse(text:String):Pair<String,Int>? {
        val value=text.trim();if(value.length !in 1..253||value.any{it.isWhitespace()||it=='/'||it=='\\'})return null
        val parts=value.split(':');if(parts.size>2)return null
        val host=parts[0];if(host.isEmpty()||!host.all{it.isLetterOrDigit()||it=='.'||it=='-'})return null
        val port=if(parts.size==2)parts[1].toIntOrNull()?:return null else RemotePlayProtocol.PORT
        return if(port in 1..65535)host to port else null
    }
}
