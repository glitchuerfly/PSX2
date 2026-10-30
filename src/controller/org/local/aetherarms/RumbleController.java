// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import java.util.Collections;

/** Serialize game rumble, coalesce throttled changes and cancel immediately on disable/pause. */
public final class RumbleController {
    public interface Output { void vibrate(int amplitude); void cancel(); }
    public interface Scheduler {
        long now(); void postDelayed(Runnable task,long delayMs); void remove(Runnable task);
    }
    private final Output output;
    private final Scheduler scheduler;
    private RumbleSettings settings=new RumbleSettings(Collections.emptyMap());
    private boolean active,buzzing,pending,hasSent;
    private int amplitude;
    private long lastSent,generation;
    private Runnable pendingTask;
    private synchronized void flush(long token) {
        // A removed Handler task may already be running and waiting for this lock.
        // It must not flush a newer burst after configure/pause/cancel.
        if(!pending||token!=generation)return;
        pending=false;pendingTask=null;
        if(active&&settings.enabled&&amplitude>0)deliver();
    }
    public RumbleController(Output output,Scheduler scheduler) { this.output=output;this.scheduler=scheduler; }
    public synchronized void configure(RumbleSettings next) {
        cancel();settings=next;
    }
    public synchronized void setActive(boolean value) {
        active=value;if(!value)cancel();
    }
    public synchronized void motors(int port,int large,int small) {
        // Another player's zero/rumble must not cancel or drive the touchscreen player's motor.
        if(port!=settings.port)return;
        if(!active||!settings.enabled) { cancel();return; }
        amplitude=Math.max(Math.max(0,Math.min(255,large)),Math.max(0,Math.min(255,small)));
        if(amplitude==0) { cancel();return; }
        long wait=hasSent?settings.throttleMs-(scheduler.now()-lastSent):0;
        if(wait<=0) {
            if(pending)scheduler.remove(pendingTask);
            generation++;
            pending=false;deliver();
        } else if(!pending) {
            pending=true;final long token=++generation;
            pendingTask=()->flush(token);scheduler.postDelayed(pendingTask,wait);
        }
    }
    private void deliver() {
        output.vibrate(amplitude);buzzing=true;hasSent=true;lastSent=scheduler.now();
    }
    private void cancel() {
        if(pending)scheduler.remove(pendingTask);
        generation++;pendingTask=null;
        pending=false;amplitude=0;hasSent=false;
        if(buzzing) { buzzing=false;output.cancel(); }
    }
}
