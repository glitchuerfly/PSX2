// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;

/** Small dialogs from the Aether touchscreen preferences, with all IO off the UI thread. */
public final class SkinUi {
    private static Activity activity(Context context){
        for(int i=0;i<16;i++){if(context instanceof Activity)return (Activity)context;if(!(context instanceof ContextWrapper))break;context=((ContextWrapper)context).getBaseContext();}return null;
    }
    private static boolean alive(Activity a){return a!=null&&!a.isFinishing()&&!a.isDestroyed();}
    public interface Work {String run()throws Exception;}
    public static void work(Activity a,String title,Work work,Runnable after){
        ProgressDialog progress=new ProgressDialog(a);progress.setMessage(title);progress.setIndeterminate(true);progress.setCancelable(false);progress.show();
        SkinStore.IO.execute(() -> {
            String result=null;Exception failure=null;try{result=work.run();}catch(Exception problem){failure=problem;}
            final String done=result;final Exception error=failure;
            a.runOnUiThread(() -> {
                if(!alive(a))return;progress.dismiss();
                if(error!=null)new AlertDialog.Builder(a).setTitle(SkinText.get(a,"error_title")).setMessage(SkinText.get(a,"error_message")).setPositiveButton(android.R.string.ok,(d,w)->{if(after!=null)after.run();}).show();
                else {if(done!=null)SkinStore.select(done);if(after!=null)after.run();}
            });
        });
    }
    /** Reuse Aether's single-choice row, with one disabled divider before management actions. */
    private static final class ChooserAdapter extends BaseAdapter {
        private final Context context;
        private final List<SkinStore.Skin> skins;
        private final String[] actions;
        private final int selected,choiceLayout,actionLayout;
        ChooserAdapter(Context context,List<SkinStore.Skin> skins,int selected){
            this.context=context;this.skins=skins;this.selected=selected;
            actions=new String[]{SkinText.get(context,"download"),SkinText.get(context,"import_zip"),SkinText.get(context,"delete_custom")};
            choiceLayout=context.getResources().getIdentifier("select_dialog_singlechoice_material","layout",context.getPackageName());
            actionLayout=context.getResources().getIdentifier("select_dialog_item_material","layout",context.getPackageName());
        }
        @Override public int getCount(){return skins.size()+1+actions.length;}
        @Override public String getItem(int position){return position<skins.size()?skins.get(position).name:position==skins.size()?null:actions[position-skins.size()-1];}
        @Override public long getItemId(int position){return position;}
        @Override public int getViewTypeCount(){return 3;}
        @Override public int getItemViewType(int position){return position<skins.size()?0:position==skins.size()?1:2;}
        @Override public boolean areAllItemsEnabled(){return false;}
        @Override public boolean isEnabled(int position){return position!=skins.size();}
        @Override public View getView(int position,View recycled,ViewGroup parent){
            int type=getItemViewType(position);
            if(type==1){
                if(recycled!=null)return recycled;
                float density=context.getResources().getDisplayMetrics().density;
                FrameLayout separator=new FrameLayout(context);
                int inset=Math.round(24*density),space=Math.round(8*density);
                separator.setPaddingRelative(inset,space,inset,space);
                separator.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                View line=new View(context);
                android.content.res.TypedArray theme=context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});
                android.graphics.drawable.Drawable divider=theme.getDrawable(0);theme.recycle();
                if(divider!=null)line.setBackground(divider);else line.setBackgroundColor(0x33808080);
                separator.addView(line,new FrameLayout.LayoutParams(-1,Math.max(1,Math.round(density))));
                return separator;
            }
            TextView row=(TextView)(recycled!=null?recycled:LayoutInflater.from(context).inflate(type==0?choiceLayout:actionLayout,parent,false));
            row.setText(getItem(position));
            if(type==0)((CheckedTextView)row).setChecked(position==selected);
            return row;
        }
    }
    public static void open(Context context){
        Activity a=activity(context);if(!alive(a))return;SkinStore.initialize(a);
        List<SkinStore.Skin> skins=SkinStore.list(a);int selected=0;
        for(int i=0;i<skins.size();i++)if(skins.get(i).id.equals(SkinStore.active())){selected=i;break;}
        AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(SkinText.get(a,"title"));
        builder.setSingleChoiceItems(new ChooserAdapter(builder.getContext(),skins,selected),selected,(dialog,which)->{
            if(which==skins.size())return;
            dialog.dismiss();
            if(which<skins.size()){SkinStore.select(skins.get(which).id);return;}
            int action=which-skins.size()-1;
            if(action==0)browse(a);
            else if(action==1)a.startActivity(new Intent(a,SkinImportActivity.class).putExtra("skin.ui_locales",a.getResources().getConfiguration().getLocales().toLanguageTags()));
            else if(action==2)remove(a,skins);
        }).setNegativeButton(SkinText.get(a,"close"),null).show();
    }
    private static void remove(Activity a,List<SkinStore.Skin> skins){
        List<SkinStore.Skin> local=new ArrayList<>();for(SkinStore.Skin s:skins)if(SkinStore.isLocal(s.id))local.add(s);
        if(local.isEmpty()){new AlertDialog.Builder(a).setMessage(SkinText.get(a,"empty")).setPositiveButton(android.R.string.ok,null).show();return;}
        String[] names=new String[local.size()];for(int i=0;i<names.length;i++)names[i]=local.get(i).name;
        new AlertDialog.Builder(a).setTitle(SkinText.get(a,"delete_custom")).setItems(names,(d,which)->new AlertDialog.Builder(a).setMessage(SkinText.get(a,"delete_confirm",local.get(which).name)).setNegativeButton(android.R.string.cancel,null).setPositiveButton(SkinText.get(a,"delete"),(confirm,w)->work(a,SkinText.get(a,"deleting"),()->{SkinStore.delete(local.get(which).id);return null;},()->open(a))).show()).setNegativeButton(android.R.string.cancel,null).show();
    }
    private static void browse(Activity a){
        ProgressDialog progress=new ProgressDialog(a);progress.setMessage(SkinText.get(a,"loading"));progress.setCancelable(false);progress.show();
        SkinStore.IO.execute(()->{
            List<SkinRepository.Remote> loaded=null;Exception failure=null;
            try{loaded=SkinRepository.catalog(a.getApplicationContext());}catch(Exception e){failure=e;}
            final List<SkinRepository.Remote> skins=loaded;final Exception error=failure;
            a.runOnUiThread(()->{
                if(!alive(a))return;progress.dismiss();
                if(error!=null){new AlertDialog.Builder(a).setTitle(SkinText.get(a,"catalog_error")).setMessage(SkinText.get(a,"network_error")).setPositiveButton(SkinText.get(a,"retry"),(d,w)->browse(a)).setNegativeButton(SkinText.get(a,"close"),null).show();return;}
                String[] names=new String[skins.size()];for(int i=0;i<names.length;i++)names[i]=skins.get(i).name;
                new AlertDialog.Builder(a).setTitle(SkinText.get(a,"catalog_title")).setItems(names,(d,which)->preview(a,skins.get(which))).setNegativeButton(SkinText.get(a,"back"),(d,w)->open(a)).show();
            });
        });
    }
    private static void preview(Activity a,SkinRepository.Remote skin){
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);int padding=(int)(20*a.getResources().getDisplayMetrics().density);content.setPadding(padding,padding,padding,padding);
        TextView title=new TextView(a);title.setText(skin.name+"\n"+SkinText.get(a,"by",skin.author));content.addView(title);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        ImageView preview=new ImageView(a);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setAdjustViewBounds(true);
        preview.setMinimumHeight(padding*4);content.addView(preview,new LinearLayout.LayoutParams(-1,-2));
        TextView note=new TextView(a);note.setText(SkinText.get(a,"layout_note"));content.addView(note);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(SkinText.get(a,"preview_title")).setView(scroll).setPositiveButton(SkinText.get(a,"download_use"),(d,w)->work(a,SkinText.get(a,"installing"),()->SkinRepository.install(skin),null)).setNegativeButton(SkinText.get(a,"back"),(d,w)->browse(a)).create();dialog.show();
        SkinStore.IO.execute(()->{
            Bitmap image=null;try{File file=SkinRepository.preview(a.getApplicationContext(),skin);if(file!=null)image=SkinStore.decode(()->new java.io.FileInputStream(file));}catch(Exception ignored){}
            final Bitmap result=image;
            a.runOnUiThread(()->{if(alive(a)&&dialog.isShowing()){if(result!=null){preview.setMinimumHeight(0);preview.setImageBitmap(result);}else {content.removeView(preview);note.setText(SkinText.get(a,"preview_missing"));}}});
        });
    }
}
