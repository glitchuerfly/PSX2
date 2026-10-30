// SPDX-License-Identifier: GPL-3.0-or-later
import java.util.*;
import org.local.aetherarms.RemotePadController;
public final class RemotePadChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        List<String> sent=new ArrayList<>();List<Runnable> commits=new ArrayList<>();
        RemotePadController pad=new RemotePadController(new RemotePadController.Output(){
            public void button(int key,int range,boolean down){sent.add(key+":"+range+":"+down);}
            public void configure(boolean on,Runnable ready){commits.add(ready);}
        });
        long first=pad.start();pad.button(first,19,0,true);pad.button(first,111,16000,true);
        check(sent.isEmpty(),"input waits for controller insertion, without being dropped");
        commits.get(0).run();check(sent.size()>2&&sent.subList(sent.size()-2,sent.size()).equals(Arrays.asList("19:0:true","111:16000:true")),"held input flushed after native commit");
        pad.button(first,19,0,false);pad.stop(first);
        check(sent.get(sent.size()-1).equals("111:0:false"),"last held axis released on disconnect");
        int size=sent.size();pad.button(first,19,0,true);commits.get(0).run();check(sent.size()==size,"old input and late insertion ignored");
        long second=pad.start();pad.button(second,96,0,true);
        pad.stop(first);check(commits.size()==3,"old-session cleanup cannot configure a new controller");
        commits.get(2).run();check(sent.get(sent.size()-1).equals("96:0:true"),"new session input unaffected");
        size=sent.size();pad.reapply();check(sent.size()==size+1&&sent.get(size).equals("96:0:true"),"native settings refresh reapplies remote holds");
        pad.stop(second);long third=pad.start();pad.button(third,104,9000,true);pad.stop(third);
        size=sent.size();commits.get(commits.size()-2).run();check(sent.size()==size,"cancel before insertion cannot restore input");
        long fourth=pad.start();pad.button(fourth,105,99999,true);commits.get(commits.size()-1).run();
        check(sent.get(sent.size()-1).equals("105:32767:true"),"native pressure range is bounded");
        System.out.println("PASS: Remote Play pending controller input, disconnect release, stale-session isolation and settings refresh.");
    }
}
