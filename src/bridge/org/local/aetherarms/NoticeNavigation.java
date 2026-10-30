// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Keep the bundled notice index and its license links inside the existing dialog. */
public final class NoticeNavigation {
    private NoticeNavigation(){}
    public static void attach(WebView web,String initialUrl){
        if("file:///android_asset/3rdparty.html".equals(initialUrl))web.setWebViewClient(new WebViewClient());
    }
}
