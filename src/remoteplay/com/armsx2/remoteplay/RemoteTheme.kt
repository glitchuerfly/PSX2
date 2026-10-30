// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.remoteplay
import android.content.Context
import android.content.res.Configuration
import org.local.aetherarms.ActivityLocale
/** Match Aether's explicit Light/Dark choice as well as its UI locale. */
internal object RemoteTheme {
    fun wrap(base:Context):Context {
        val localized=ActivityLocale.wrap(base)
        val selected=base.getSharedPreferences(base.packageName+"_preferences",0).getString("UI/Theme","follow_system")
        val mode=when(selected){"light"->Configuration.UI_MODE_NIGHT_NO;"dark"->Configuration.UI_MODE_NIGHT_YES;else->return localized}
        val delta=Configuration();delta.uiMode=mode
        return localized.createConfigurationContext(delta)
    }
}
