// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay
import android.app.Activity
import android.content.Context
import android.view.View
import java.util.WeakHashMap

/** ABI boundary for the original, obfuscated AndroidX Fragment classes. Main-thread only. */
object RemotePages {
    private val panels=WeakHashMap<Any,RemotePlayPanel>()
    @JvmStatic fun create(owner:Any,activity:Activity,page:Int):View {
        panels.remove(owner)?.pause()
        return RemotePlayPanel(activity,page).also{panels[owner]=it}.create()
    }
    @JvmStatic fun resume(owner:Any){panels[owner]?.resume()}
    @JvmStatic fun pause(owner:Any){panels[owner]?.pause()}
    @JvmStatic fun destroy(owner:Any){panels.remove(owner)?.pause()}
    @JvmStatic fun tabTitle(context:Context,page:Int):String {
        RemoteText.use(context)
        return if(page==0)RemoteText.get("remoteplay.title")else context.getString(context.resources.getIdentifier("remote_play_host_title","string",context.packageName))
    }
}
