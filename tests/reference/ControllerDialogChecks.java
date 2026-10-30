// SPDX-License-Identifier: GPL-3.0-or-later
import android.app.*;
import android.view.View;
import org.local.aetherarms.ControllerDialogs;

/** Execute the actual dialog helper with host-only Android lifecycle/event-queue fakes. */
public final class ControllerDialogChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static final class Refresh {
        public int calls;
        public void a(){calls++;}
    }
    public static final class Owner {
        public Activity activity=new Activity();public View page=new View();public boolean added=true;
        public final Refresh o0=new Refresh();
        public Activity getActivity(){return activity;}
        public Object getContext(){return added?activity:null;}
        public View getView(){return page;}
        public boolean isAdded(){return added;}
    }
    public static void main(String[] args){
        Owner old=new Owner();Dialog warning=new Dialog();
        ControllerDialogs.show(warning,old);check(warning.showing,"Live page could not open confirmation");
        old.page.detach();old.added=false;
        check(!warning.showing&&old.page.listeners.isEmpty(),"Detached page retained its dialog/listener");
        // A cached Yes message can arrive after dismiss: it must never query profiles or show a toast.
        check(!ControllerDialogs.current(old),"Stale Yes still accepted its detached owner");
        Dialog stale=new Dialog();ControllerDialogs.show(stale,old);check(stale.shows==0,"Detached owner opened another dialog");
        Owner toggle=new Owner();Dialog pending=new Dialog();ControllerDialogs.show(pending,toggle);
        ControllerDialogs.refresh(toggle);ControllerDialogs.refresh(toggle);
        check(toggle.o0.calls==0,"Page replaced during switch click dispatch");
        check(!pending.showing&&toggle.page.listeners.isEmpty(),"Scope change retained old dialog");
        check(!ControllerDialogs.current(toggle),"Old page still accepted profile callbacks during refresh");
        Dialog between=new Dialog();ControllerDialogs.show(between,toggle);check(between.shows==0,"Profile dialog opened while switching scope");
        toggle.page.drain();check(toggle.o0.calls==1,"Refresh was missing or duplicated");
        check(!ControllerDialogs.current(toggle),"Old page revalidated before FragmentManager replacement committed");
        // A pending replacement must not refresh an already closed or replaced page.
        Owner closed=new Owner();View closedView=closed.page;ControllerDialogs.refresh(closed);closed.activity.destroyed=true;
        closedView.drain();check(closed.o0.calls==0,"Destroyed Activity refreshed");
        Owner replaced=new Owner();View previous=replaced.page;ControllerDialogs.refresh(replaced);replaced.page=new View();
        previous.drain();check(replaced.o0.calls==0,"Callback from previous view refreshed replacement view");
        Owner valid=new Owner();Dialog d=new Dialog();ControllerDialogs.show(d,valid);d.dismiss();
        check(valid.page.listeners.isEmpty()&&ControllerDialogs.current(valid),"Normal dismiss invalidated live page or leaked listener");
        check(!ControllerDialogs.current(null),"Null owner accepted");
        System.out.println("PASS: actual controller dialog helper rejects stale Yes, dismisses detached/scope-change dialogs, defers and deduplicates refresh, rejects destroyed/replaced views and releases listeners.");
    }
}
