// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.content.Context;
import android.media.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import java.util.WeakHashMap;

/** Android focus requests/callbacks run on main; never call Bridge while holding a policy lock. */
public final class GameAudioFocus {
    private static final AudioFocusPolicy policy=new AudioFocusPolicy();
    private static final WeakHashMap<Activity,ViewTreeObserver.OnWindowFocusChangeListener> windows=new WeakHashMap<>();
    private static AudioManager manager;
    private static Handler handler;
    private static AudioFocusRequest request;
    private GameAudioFocus(){}
    public static void initialize(Context context){
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        handler=new Handler(Looper.getMainLooper());
    }
    public static boolean pauseRequired(){return policy.pauseRequired();}
    public static void userPlay(){policy.userPlay();retry();}
    public static void wanted(boolean wanted){long token=policy.want(wanted);if(token!=0&&handler!=null)handler.post(()->request(token));}
    public static void retry(){long token=policy.retry();if(token!=0&&handler!=null)handler.post(()->request(token));}
    private static void request(long token){
        if(!policy.current(token))return;
        if(request!=null){manager.abandonAudioFocusRequest(request);request=null;}
        if(!policy.wanted())return;
        if(manager==null){if(policy.result(token,0))Bridge.onAudioFocusChanged();return;}
        AudioFocusRequest next=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(change->{
                if(policy.changed(token,change)){
                    Log.i("AetherArmsBridge","AUDIO_FOCUS change="+change+" blocked="+policy.blocked()+" pause="+policy.pauseRequired());
                    Bridge.onAudioFocusChanged();
                }
            },handler).build();
        request=next;
        int result;
        try{result=manager.requestAudioFocus(next);}
        catch(RuntimeException failure){Log.w("AetherArmsBridge","Audio focus request failed",failure);result=AudioManager.AUDIOFOCUS_REQUEST_FAILED;}
        if(policy.result(token,result)){
            Log.i("AetherArmsBridge","AUDIO_FOCUS request="+result+" blocked="+policy.blocked()+" pause="+policy.pauseRequired());
            Bridge.onAudioFocusChanged();
        }
    }
    // Attach to the framework window, not a legacy Activity lifecycle override.
    public static void observe(Activity activity){
        if(windows.containsKey(activity))return;
        ViewTreeObserver.OnWindowFocusChangeListener listener=focused->{if(focused)retry();};
        activity.getWindow().getDecorView().getViewTreeObserver().addOnWindowFocusChangeListener(listener);
        windows.put(activity,listener);
    }
    public static void forget(Activity activity){
        ViewTreeObserver.OnWindowFocusChangeListener listener=windows.remove(activity);
        if(listener!=null){ViewTreeObserver observer=activity.getWindow().getDecorView().getViewTreeObserver();if(observer.isAlive())observer.removeOnWindowFocusChangeListener(listener);}
    }
}
