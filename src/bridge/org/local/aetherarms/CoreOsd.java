// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

/** JNI adapter into the pinned donor's own OSD queue, not an Android view. */
public final class CoreOsd {
    private CoreOsd(){}
    static { System.loadLibrary("hybridosd"); }
    public static native boolean post(String message);
}
