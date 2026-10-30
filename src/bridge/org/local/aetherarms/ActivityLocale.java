// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

/** Match Aether's UI language before Activity resources/themes are initialized. */
public final class ActivityLocale {
    private ActivityLocale(){}
    public static Context wrap(Context base){
        String language=base.getSharedPreferences(base.getPackageName()+"_preferences",0).getString("UI/Language","none");
        if(language==null||language.isEmpty()||language.equals("none"))return base;
        LocaleList locales=LocaleList.forLanguageTags(language);
        if(locales.isEmpty())return base;
        // Override only locale so orientation, density and other device changes still propagate.
        Configuration configuration=new Configuration();
        configuration.setLocales(locales);
        return base.createConfigurationContext(configuration);
    }
}
