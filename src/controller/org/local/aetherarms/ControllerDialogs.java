// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import java.lang.reflect.*;
import java.util.*;

/** Profile dialogs belong to one attached controller page, never its replacement. Main thread only. */
public final class ControllerDialogs {
    private static final WeakHashMap<Object,Boolean> refreshing=new WeakHashMap<>();
    private static final WeakHashMap<Object,Set<Dialog>> dialogs=new WeakHashMap<>();
    private ControllerDialogs(){}
    private static Object call(Object owner,String name)throws ReflectiveOperationException{return owner.getClass().getMethod(name).invoke(owner);}
    private static View view(Object owner)throws ReflectiveOperationException{return (View)call(owner,"getView");}
    private static boolean live(Object owner,View expected)throws ReflectiveOperationException{
        Activity activity=(Activity)call(owner,"getActivity");View current=view(owner);
        return activity!=null&&!activity.isFinishing()&&!activity.isDestroyed()&&call(owner,"getContext")!=null
            &&Boolean.TRUE.equals(call(owner,"isAdded"))&&current!=null&&current.isAttachedToWindow()&&(expected==null||current==expected);
    }
    public static boolean current(Object owner){
        if(owner==null||refreshing.containsKey(owner))return false;
        try{return live(owner,null);}catch(ReflectiveOperationException e){throw new IllegalStateException("Aether controller dialog ABI",e);}
    }
    public static void show(Dialog dialog,Object owner){
        if(!current(owner))return;
        try{
            View page=view(owner);
            View.OnAttachStateChangeListener attachment=new View.OnAttachStateChangeListener(){
                public void onViewAttachedToWindow(View v){}
                public void onViewDetachedFromWindow(View v){dialog.dismiss();}
            };
            dialogs.computeIfAbsent(owner,k->new HashSet<>()).add(dialog);
            page.addOnAttachStateChangeListener(attachment);
            dialog.setOnDismissListener(d->{
                page.removeOnAttachStateChangeListener(attachment);
                Set<Dialog> active=dialogs.get(owner);if(active!=null){active.remove(dialog);if(active.isEmpty())dialogs.remove(owner);}
            });
            dialog.show();
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether controller dialog ABI",e);}
    }
    /** Finish the switch click before replacing its page. Block old-page callbacks immediately. */
    public static void refresh(Object owner){
        if(!current(owner))return;
        try{
            View page=view(owner);Object callback=owner.getClass().getField("o0").get(owner);
            if(callback==null)return;
            refreshing.put(owner,true);
            Set<Dialog> active=dialogs.get(owner);if(active!=null)for(Dialog dialog:new ArrayList<>(active))dialog.dismiss();
            page.post(()->{
                try{if(live(owner,page))callback.getClass().getMethod("a").invoke(callback);}
                catch(ReflectiveOperationException e){throw new IllegalStateException("Aether controller refresh ABI",e);}
                // The old owner stays invalid while FragmentManager commits its replacement.
                // Weak keys release this marker when the detached owner is collected.
            });
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Aether controller refresh ABI",e);}
    }
}
