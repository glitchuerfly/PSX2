// SPDX-License-Identifier: GPL-3.0-or-later
import org.local.aetherarms.*;
import java.util.*;

public final class AudioFocusChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void equal(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        AudioFocusPolicy focus=new AudioFocusPolicy();check(focus.blocked(),"Idle focus was playable");
        long first=focus.want(true);check(first!=0&&focus.blocked(),"Initial request was not pending");
        check(!focus.pauseRequired(),"Focus request pending froze game boot");
        check(focus.result(first,AudioFocusPolicy.GRANTED),"Initial grant ignored");check(!focus.blocked(),"Granted focus remained blocked");
        List<String> calls=new ArrayList<>();
        PauseAudioController audio=new PauseAudioController(new PauseAudioController.Output(){
            public void setSuppressed(boolean v){calls.add("suppress="+v);}
            public void setBackgrounded(boolean v){calls.add("background="+v);}
        });
        audio.update(!focus.pauseRequired(),false,false);calls.clear();
        focus.changed(first,AudioFocusPolicy.TRANSIENT);check(focus.blocked()&&focus.pauseRequired(),"Actual call focus loss did not request pause");
        audio.update(!focus.pauseRequired(),false,true);equal(calls,Arrays.asList("background=true"));calls.clear();
        // A native resume ACK during the call must not authorize output.
        audio.update(!focus.pauseRequired(),false,false);equal(calls,Collections.emptyList());
        for(int i=0;i<50;i++)equal(focus.want(true),0L);equal(focus.retry(),0L); // Wait for call; never steal focus.
        focus.changed(first,AudioFocusPolicy.GAIN);check(!focus.blocked(),"Call-end gain did not restore playback");
        audio.update(!focus.pauseRequired(),false,true);equal(calls,Arrays.asList("suppress=true","background=false"));calls.clear();
        audio.update(!focus.pauseRequired(),false,false);equal(calls,Arrays.asList("suppress=false"));calls.clear();
        audio.update(true,true,true);calls.clear();focus.changed(first,AudioFocusPolicy.TRANSIENT);
        audio.update(!focus.pauseRequired(),true,true);equal(calls,Arrays.asList("suppress=false","background=true"));calls.clear();
        focus.changed(first,AudioFocusPolicy.GAIN);
        audio.update(!focus.pauseRequired(),true,true);equal(calls,Arrays.asList("suppress=true","background=false"));calls.clear();
        focus.changed(first,AudioFocusPolicy.DUCK);check(!focus.blocked(),"Notification ducking stopped the game");
        long background=focus.want(false);check(background!=0&&focus.blocked(),"Background retained focus authorization");
        check(!focus.changed(first,AudioFocusPolicy.GAIN),"Late abandoned call gain revived output");
        equal(focus.retry(),0L);long back=focus.want(true);
        focus.result(back,AudioFocusPolicy.DELAYED);check(focus.blocked(),"Delayed focus ownership incorrectly granted");
        for(int i=0;i<100;i++){
            equal(focus.want(true),0L);
            check(!focus.pauseRequired(),"Repeated delayed-focus observer ticks kept the game blank");
        }
        focus.changed(back,AudioFocusPolicy.TRANSIENT);
        check(!focus.pauseRequired(),"Loss for an ungranted delayed request paused boot");
        equal(focus.retry(),0L);focus.changed(back,AudioFocusPolicy.GAIN);check(!focus.blocked(),"Delayed gain failed");
        focus.changed(back,AudioFocusPolicy.LOSS);check(focus.blocked(),"Permanent loss did not pause");
        for(int i=0;i<50;i++)equal(focus.want(true),0L); // Observer ticks must not seize another app's focus.
        long retry=focus.retry();check(retry!=0,"Explicit foreground/resume could not retry permanent loss");
        check(!focus.changed(back,AudioFocusPolicy.GAIN),"Old request callback affected retry");
        focus.result(retry,0);check(focus.blocked()&&focus.pauseRequired(),"Retry without gain cleared an actual interruption");
        focus.userPlay();check(!focus.pauseRequired()&&focus.blocked(),"User Resume either stayed paused or claimed audio ownership");
        focus.changed(retry,AudioFocusPolicy.LOSS);
        check(!focus.pauseRequired(),"Duplicate loss re-paused explicit user Resume before a gain");
        long second=focus.retry();focus.result(second,AudioFocusPolicy.GRANTED);
        check(!focus.blocked(),"Foreground retry failed to acquire focus");
        check(!focus.changed(retry,AudioFocusPolicy.LOSS),"Stale loss paused a new request");
        long stopped=focus.want(false);check(stopped!=0&&focus.blocked(),"Stop left output authorized");
        check(!focus.changed(second,AudioFocusPolicy.GAIN),"Stopped session resumed from old callback");
        long failedBoot=focus.want(true);focus.result(failedBoot,0);
        check(focus.blocked()&&!focus.pauseRequired(),"Failed focus request prevented game boot");
        focus.want(false);long newGame=focus.want(true);focus.changed(newGame,AudioFocusPolicy.GAIN);
        check(!focus.result(newGame,AudioFocusPolicy.DELAYED)&&!focus.blocked(),"Delayed request return overwrote earlier gain callback");
        focus.want(false);focus.want(true);focus.want(false);
        check(focus.blocked()&&!focus.result(newGame,AudioFocusPolicy.GRANTED),"Rapid stop/restart accepted stale result");
        System.out.println("PASS: call focus loss/gain, delayed/failed requests never freeze boot, explicit Resume while waiting, no focus stealing, permanent loss/explicit retry, stale callbacks, stop/background, menu pause and warm-before-resume audio ordering.");
    }
}
