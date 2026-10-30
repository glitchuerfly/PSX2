// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay
import android.app.*
import android.content.*
import android.os.*
import android.view.InputDevice
import java.lang.ref.WeakReference
import org.json.JSONObject
import org.local.aetherarms.ActivityLocale
import kr.co.iefriends.pcsx2.NativeApp

class RemoteState<T>(@Volatile var value:T)

object RemoteText {
    private var strings=mapOf<String,String>()
    fun use(context:Context){
        val app=ActivityLocale.wrap(context)
        val locale=app.resources.configuration.locales[0]
        val suffix=when(locale.language){"in"->"id";"ckb"->"ku";"zh"->if(locale.country in listOf("TW","HK","MO")||locale.script=="Hant")"zh-TW" else "zh-CN";"pt"->"pt-BR";else->locale.language}
        val result=mutableMapOf<String,String>()
        for(language in listOf("en",suffix))runCatching{
            val json=JSONObject(app.assets.open("remoteplay-i18n/$language.json").bufferedReader().use{it.readText()})
            for(key in json.keys())result[key]=json.getString(key)
        }
        strings=result
    }
    fun get(key:String)=strings[key]?:key
    fun format(key:String,vararg args:Any)=runCatching{String.format(get(key),*args)}.getOrElse{get(key)}
    fun local(english:String,indonesian:String):String = if(language() in listOf("id","in"))indonesian else english
    private fun language()=RemoteRuntime.context?.let{ActivityLocale.wrap(it).resources.configuration.locales[0].language}
}

/** Only Application callbacks observe legacy Activities; never add overrides to them. */
object RemoteRuntime : Application.ActivityLifecycleCallbacks {
    internal var context:Context?=null
    private var owner=WeakReference<Activity>(null)
    private var prompt:AlertDialog?=null
    private var pending:String?=null
    private val main=Handler(Looper.getMainLooper())
    private var motor:Vibrator?=null
    @JvmStatic fun initialize(context:Context){
        if(this.context!=null)return
        this.context=context.applicationContext
        RemoteText.use(context)
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }
    @JvmStatic fun stop(){val action=Runnable{pending=null;prompt?.dismiss();prompt=null;RemotePlayHost.stop()};if(Looper.myLooper()==Looper.getMainLooper())action.run()else main.post(action)}
    fun ask(viewer:String){pending=viewer;showPrompt()}
    fun answered(){pending=null;prompt?.dismiss();prompt=null}
    private fun showPrompt(){
        val activity=owner.get()?:return
        if(prompt!=null||activity.isFinishing||activity.isDestroyed)return
        val name=pending?:return
        RemoteText.use(activity)
        prompt=RemoteUi.dialogBuilder(activity)
            .setTitle(RemoteText.format("remoteplay.request.title",name))
            .setPositiveButton(RemoteText.get("remoteplay.request.accept")){_,_->RemotePlayHost.accept()}
            .setNegativeButton(RemoteText.get("remoteplay.request.decline")){_,_->RemotePlayHost.decline()}
            .setOnCancelListener{RemotePlayHost.decline()}.create()
        prompt?.show()
        prompt?.let{RemoteUi.styleDialog(it)}
    }
    fun rumble(deviceId:Int,large:Int,small:Int){
        val app=context?:return
        val candidate=if(deviceId>=0)InputDevice.getDevice(deviceId)?.vibrator else null
        val allowed=app.getSharedPreferences(app.packageName+"_preferences",0).getBoolean("TouchscreenController/EnableVibration",false)
        val target=candidate?.takeIf{it.hasVibrator()}?:if(allowed)app.getSystemService(Vibrator::class.java) else null
        if(large==0&&small==0){runCatching{motor?.cancel()};motor=null;return}
        if(target!==motor)runCatching{motor?.cancel()}
        motor=target
        runCatching{target?.vibrate(VibrationEffect.createOneShot(500,maxOf(large,small).coerceIn(1,255)))}
    }
    override fun onActivityResumed(activity:Activity){owner=WeakReference(activity);showPrompt()}
    override fun onActivityPaused(activity:Activity){if(owner.get()===activity){owner.clear();prompt?.dismiss();prompt=null}}
    override fun onActivityDestroyed(activity:Activity){if(owner.get()===activity)owner.clear()}
    override fun onActivityCreated(activity:Activity,state:Bundle?){}
    override fun onActivityStarted(activity:Activity){}
    override fun onActivityStopped(activity:Activity){}
    override fun onActivitySaveInstanceState(activity:Activity,state:Bundle){}
}
