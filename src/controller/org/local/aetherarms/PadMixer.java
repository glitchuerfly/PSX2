// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.util.*;

/** Merge touchscreen/physical/macro sources so one source's release cannot drop another's hold. */
public final class PadMixer {
    public interface Output{void value(int pad,String name,float value);}
    private final Map<String,Map<String,Float>> values=new HashMap<>();
    private final Map<String,Float> applied=new HashMap<>();
    private final Map<Integer,Float> pressure=new HashMap<>();
    private final Output output;
    public PadMixer(Output output){this.output=output;}
    public synchronized void set(int pad,String name,String source,float value){
        if(pad<0||pad>7||name==null)return;
        String key=pad+"/"+name;Map<String,Float> sources=values.computeIfAbsent(key,k->new HashMap<>());
        float v=Float.isFinite(value)?Math.max(0,Math.min(1,value)):0;
        if(v==0)sources.remove(source);else sources.put(source,v);
        apply(key);if(sources.isEmpty())values.remove(key);
    }
    public synchronized void pressure(int pad,float amount){pressure.put(pad,Math.max(0,Math.min(1,amount)));for(String key:new ArrayList<>(values.keySet()))if(key.startsWith(pad+"/"))apply(key);}
    private void apply(String key){
        int split=key.indexOf('/'),pad=Integer.parseInt(key.substring(0,split));String name=key.substring(split+1);
        float v=0;for(float n:values.getOrDefault(key,Collections.emptyMap()).values())v=Math.max(v,n);
        if(Arrays.asList("Up","Right","Down","Left","Triangle","Circle","Cross","Square","L1","L2","R1","R2").contains(name))v*=pressure.getOrDefault(pad,1f);
        if(Math.abs(v-applied.getOrDefault(key,0f))>0.00001f){applied.put(key,v);output.value(pad,name,v);}
    }
    public synchronized void release(){for(String key:new ArrayList<>(applied.keySet()))if(applied.get(key)!=0){int s=key.indexOf('/');output.value(Integer.parseInt(key.substring(0,s)),key.substring(s+1),0);}values.clear();applied.clear();pressure.clear();}
}
