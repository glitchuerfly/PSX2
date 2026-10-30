// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.util.*;
/** A shared physical motor stays on until every mapped pad/motor has released it. */
public final class MotorMixer {
    private final Map<String,Map<String,Integer>> values=new HashMap<>();
    public synchronized int set(String motor,String source,int value){
        Map<String,Integer> inputs=values.computeIfAbsent(motor,k->new HashMap<>());
        if(value<=0)inputs.remove(source);else inputs.put(source,Math.min(255,value));
        int max=0;for(int v:inputs.values())max=Math.max(max,v);
        if(inputs.isEmpty())values.remove(motor);return max;
    }
    public synchronized void clear(){values.clear();}
}
