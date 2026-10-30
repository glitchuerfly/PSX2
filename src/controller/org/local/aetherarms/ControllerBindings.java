// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import java.util.*;

/** Aether descriptor/ButtonN and signed-axis bindings, alternatives and chords, for all eight pads. */
public final class ControllerBindings {
    /** Motor/device metadata has its own index namespace (motors reuse Up's index 0).
     * Only Button/Axis/HalfAxis entries can be addressed by setPadValue. */
    public static void addPadInput(Map<Integer,String> names,int index,String name,int type){
        if(index>=0&&name!=null&&(type==1||type==2||type==3))names.put(index,name);
    }
    public interface Output {void value(String target,float value);}
    private static final class Route {
        final String target;final List<String[]> alternatives=new ArrayList<>();float last;
        Route(String target,Object raw){this.target=target;for(String binding:ControllerSettings.bindings(raw)){
            String[] chord=binding.split("\\s*&\\s*");for(int i=0;i<chord.length;i++)chord[i]=chord[i].trim();alternatives.add(chord);
        }}
    }
    private final List<Route> routes=new ArrayList<>();
    private final Map<String,Float> state=new HashMap<>();
    private final Output output;
    public ControllerBindings(Map<String,?> values,Set<String> targets,Output output){
        this.output=output;
        for(Map.Entry<String,?> e:values.entrySet())if(targets.contains(e.getKey())){
            Route route=new Route(e.getKey(),e.getValue());if(!route.alternatives.isEmpty())routes.add(route);
        }
    }
    public boolean hasButton(String descriptor,int key){
        String component=descriptor+"/Button"+key;
        for(Route route:routes)for(String[] chord:route.alternatives)for(String part:chord)if(part.equals(component))return true;
        return false;
    }
    public Set<String> targetsForDevice(String descriptor){
        Set<String> result=new LinkedHashSet<>();
        for(Route route:routes)for(String[] chord:route.alternatives)for(String part:chord)if(part.startsWith(descriptor+"/"))result.add(route.target);
        return result;
    }
    public void button(String descriptor,int key,boolean down){component(descriptor+"/Button"+key,down?1:0);}
    public void axis(String descriptor,int axis,float raw){
        float value=Float.isFinite(raw)?Math.max(-1,Math.min(1,raw)):0;
        state.put(descriptor+"/+Axis"+axis,Math.max(0,value));state.put(descriptor+"/-Axis"+axis,Math.max(0,-value));
        state.put(descriptor+"/FullAxis"+axis,(value+1)*.5f);state.put(descriptor+"/~Axis"+axis,(1-value)*.5f);
        evaluate();
    }
    public void component(String component,float value){state.put(component,Float.isFinite(value)?Math.max(0,Math.min(1,value)):0);evaluate();}
    private void evaluate(){
        for(Route route:routes){float best=0;
            for(String[] chord:route.alternatives){float v=1;for(String part:chord)v=Math.min(v,state.getOrDefault(part,0f));best=Math.max(best,v);}
            if(Math.abs(best-route.last)>0.00001f){route.last=best;output.value(route.target,best);}
        }
    }
    public void release(){state.clear();for(Route route:routes)if(route.last!=0){route.last=0;output.value(route.target,0);}}
}
