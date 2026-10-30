// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.util.*;

/** Session-owned port-2 input. Buffer while hot-plugging, reject old-session cleanup. */
public final class RemotePadController {
    public interface Output {
        void button(int key,int range,boolean down);
        /** Enqueue configuration in order; ready runs only after the native settings commit. */
        void configure(boolean on,Runnable ready);
    }
    private static final class Value {final int range;final boolean down;Value(int range,boolean down){this.range=range;this.down=down;}}
    private static final int[] KEYS={19,22,20,21,109,108,106,107,100,97,96,99,102,103,104,105,110,111,112,113,120,121,122,123,200};
    private final Output output;
    private final Map<Integer,Value> held=new LinkedHashMap<>();
    private long generation;
    private boolean active,ready;
    public RemotePadController(Output output){this.output=output;}
    public synchronized long start(){
        if(active)stop(generation);
        final long token=++generation;active=true;ready=false;held.clear();
        output.configure(true,()->ready(token));return token;
    }
    private synchronized void ready(long token){if(!active||generation!=token)return;for(int key:KEYS)output.button(key,0,false);ready=true;reapply();}
    public synchronized void button(long token,int key,int range,boolean down){
        if(!active||generation!=token)return;
        Value value=new Value(Math.max(0,Math.min(32767,range)),down);held.put(key,value);
        if(ready)output.button(key,value.range,value.down);
    }
    public synchronized void reapply(){if(active&&ready)for(Map.Entry<Integer,Value> e:held.entrySet())output.button(e.getKey(),e.getValue().range,e.getValue().down);}
    public synchronized void stop(long token){
        if(!active||generation!=token)return;
        for(Map.Entry<Integer,Value> e:held.entrySet())if(ready&&e.getValue().down)output.button(e.getKey(),0,false);
        active=false;ready=false;held.clear();generation++;
        output.configure(false,()->{});
    }
}
