// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.util.*;
/** Macro combos and frame-counted turbo, anchored at the first pressed frame. */
public final class MacroController {
    public interface Output{void value(int pad,int macro,String name,float value);}
    private static final class Macro {int pad,index,frequency;long start;boolean held,on;Set<String> names=new LinkedHashSet<>();}
    private final Map<String,Macro> macros=new HashMap<>();private final Output output;
    public MacroController(Map<String,?> settings,Output output){this.output=output;
        for(int p=0;p<8;p++)for(int n=0;n<4;n++){
            Macro m=new Macro();m.pad=p;m.index=n;
            String prefix="Pad"+(p+1)+"/Macro"+(n+1);
            m.frequency=ControllerSettings.integer(settings.get(prefix+"Frequency"),0,0,100000);
            for(String list:ControllerSettings.bindings(settings.get(prefix+"Binds")))for(String name:list.split("[& ,]+"))if(!name.isEmpty())m.names.add(name);
            macros.put(p+"/"+n,m);
        }
    }
    public void set(int pad,int index,boolean down,long frame){Macro m=macros.get(pad+"/"+index);if(m==null||m.held==down)return;m.held=down;m.start=frame;apply(m,down);}
    public void frame(long frame){for(Macro m:macros.values())if(m.held)apply(m,m.frequency==0||((Math.max(0,frame-m.start)/m.frequency)&1)==0);}
    private void apply(Macro m,boolean on){if(m.on==on)return;m.on=on;for(String name:m.names)output.value(m.pad,m.index,name,on?1:0);}
    public void release(){for(Macro m:macros.values()){m.held=false;apply(m,false);}}
}
