// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.util.*;

/** Atomic artwork publication; a pending custom skin must never expose Default. */
public final class SkinLoadState<T> {
    public static final class Request {
        public final String id;
        private final long generation;
        private Request(String id,long generation){this.id=id;this.generation=generation;}
    }
    public static final class Snapshot<T> {
        public final String id;
        public final boolean ready;
        public final T value;
        private Snapshot(String id,boolean ready,T value){this.id=id;this.ready=ready;this.value=value;}
    }
    private final T defaults;
    private final int limit;
    private final LinkedHashMap<String,T> cache=new LinkedHashMap<>(4,.75f,true);
    private long generation,selectionRevision;
    private Snapshot<T> current;
    public SkinLoadState(T defaults,int limit){
        if(defaults==null||limit<1)throw new IllegalArgumentException();
        this.defaults=defaults;this.limit=limit;current=new Snapshot<>("",true,defaults);
    }
    /** Order selection messages when worker results are queued behind UI configuration. */
    public synchronized long requestSelection(){return ++selectionRevision;}
    public synchronized boolean selectionCurrent(long revision){return revision==selectionRevision;}
    public synchronized Request select(String id){
        Objects.requireNonNull(id);
        if(current.id.equals(id))return null;
        generation++;
        T value=id.isEmpty()?defaults:cache.get(id);
        current=new Snapshot<>(id,value!=null,value);
        return value==null?new Request(id,generation):null;
    }
    public synchronized boolean current(Request request){return request.generation==generation&&request.id.equals(current.id);}
    public synchronized boolean complete(Request request,T value){
        Objects.requireNonNull(value);
        cache.put(request.id,value);
        while(cache.size()>limit)cache.remove(cache.keySet().iterator().next());
        if(!current(request))return false;
        current=new Snapshot<>(request.id,true,value);return true;
    }
    public synchronized Snapshot<T> snapshot(){return current;}
}
