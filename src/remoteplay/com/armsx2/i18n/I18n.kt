// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.i18n
import com.armsx2.remoteplay.RemoteText
/** Keep upstream transport messages, using the caller's Aether language. */
object I18n { fun get(key:String)=RemoteText.get(key) }
