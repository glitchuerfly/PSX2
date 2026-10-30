// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

/** Focus ownership is independent of user/menu pause and Activity/Surface visibility. */
public final class AudioFocusPolicy {
    // Android AudioManager constants; kept here so transitions can run on the host.
    public static final int GRANTED=1,DELAYED=2,GAIN=1,LOSS=-1,TRANSIENT=-2,DUCK=-3;
    private enum State { INACTIVE,REQUESTING,HELD,WAITING,LOST,FAILED }
    private State state=State.INACTIVE;
    private boolean wanted,pauseRequired;
    private long generation;
    public synchronized long want(boolean next){
        if(wanted==next)return 0;
        wanted=next;pauseRequired=false;state=next?State.REQUESTING:State.INACTIVE;return ++generation;
    }
    public synchronized long retry(){
        if(!wanted||(state!=State.FAILED&&state!=State.LOST))return 0;
        state=State.REQUESTING;return ++generation;
    }
    public synchronized boolean current(long token){return token==generation;}
    public synchronized boolean wanted(){return wanted;}
    public synchronized boolean blocked(){return !wanted||state!=State.HELD;}
    // Permission for sound is not a prerequisite for launching/continuing a game.
    // Only an actual platform focus-loss event can request an automatic pause.
    public synchronized boolean pauseRequired(){return wanted&&pauseRequired;}
    public synchronized void userPlay(){pauseRequired=false;}
    public synchronized boolean result(long token,int result){
        if(!current(token)||!wanted||state!=State.REQUESTING)return false;
        state=result==GRANTED?State.HELD:result==DELAYED?State.WAITING:State.FAILED;
        if(result==GRANTED)pauseRequired=false;return true;
    }
    public synchronized boolean changed(long token,int change){
        if(!current(token)||!wanted)return false;
        switch(change){
            case GAIN:state=State.HELD;pauseRequired=false;return true;
            case LOSS:pauseRequired|=state==State.HELD;state=State.LOST;return true;
            case TRANSIENT:pauseRequired|=state==State.HELD;state=State.WAITING;return true;
            // Notifications may be ducked by Android; never alter the user's volume.
            case DUCK:return false;
            default:return false;
        }
    }
}
