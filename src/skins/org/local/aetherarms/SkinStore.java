// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.View;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class SkinStore {
    public static final ExecutorService IO=Executors.newSingleThreadExecutor(r -> new Thread(r,"ControllerSkins"));
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static Context app;
    private static volatile String active="";
    private static volatile Map<String,Bitmap> images=Collections.emptyMap();
    private static volatile int generation;
    private static volatile Map<String,DpadMask> dpadMasks=Collections.emptyMap();
    private static final Map<View,Boolean> views=new WeakHashMap<>();
    private static final Map<SkinPreference,Boolean> preferences=new WeakHashMap<>();
    public static final class Skin {
        public final String id,name;
        public Skin(String id,String name){this.id=id;this.name=name;}
    }
    public static synchronized void initialize(Context context) {
        if(app!=null)return;
        app=context.getApplicationContext();
        active=app.getSharedPreferences("aetherarms_skins",0).getString("active","");
        android.content.SharedPreferences controllerPrefs=app.getSharedPreferences(app.getPackageName()+"_preferences",0);
        if(!controllerPrefs.contains("TouchscreenController/CustomSkin"))controllerPrefs.edit().putString("TouchscreenController/CustomSkin",active.isEmpty()?"default":active).commit();
        IO.execute(() -> {
            File[] files=root().listFiles();
            if(files!=null)for(File f:files)if(f.isDirectory()&&f.getName().matches("skin_[a-f0-9]{32}\\.tmp"))
                try{removeTree(f);}catch(IOException ignored){}
        });
        loadSelected();
    }
    public static void register(View view) {initialize(view.getContext());views.put(view,true);}
    public static void register(SkinPreference pref) {initialize(pref.i);preferences.put(pref,true);}
    private static File root(){File f=new File(app.getFilesDir(),"controllerskins");f.mkdirs();return f;}
    private static boolean localId(String id){return id!=null&&id.matches("skin_[a-f0-9]{32}");}
    public static String active(){return active;}
    public static String label(Context context,String id) {
        if(id==null||id.isEmpty())return SkinText.get(context,"default_name");
        if(id.equals("builtin:nethersx2"))return "NetherSX2";
        if(id.equals("builtin:nethersx2_old"))return "NetherSX2 Old";
        if(!localId(id))return SkinText.get(context,"default_name");
        Properties properties=new Properties();
        try(InputStream in=new FileInputStream(new File(root(),id+"/skin.properties"))){properties.load(in);return properties.getProperty("name",SkinText.get(context,"custom_name"));}
        catch(IOException ignored){return SkinText.get(context,"custom_name");}
    }
    public static List<Skin> list(Context context) {
        List<Skin> out=new ArrayList<>();
        out.add(new Skin("",SkinText.get(context,"default_name")));
        out.add(new Skin("builtin:nethersx2","NetherSX2"));
        out.add(new Skin("builtin:nethersx2_old","NetherSX2 Old"));
        File[] files=root().listFiles();
        if(files!=null)for(File f:files)if(localId(f.getName())&&f.isDirectory())out.add(new Skin(f.getName(),label(context,f.getName())));
        return out;
    }
    public static void select(String id) {
        if(!(id.isEmpty()||id.equals("builtin:nethersx2")||id.equals("builtin:nethersx2_old")||localId(id)&&new File(root(),id).isDirectory()))return;
        active=id;
        ControllerPreferences.saveSkin(id);
        loadSelected();
        refresh();
    }
    public static void applyControllerSkin(Object saved){
        String id=ControllerPreferences.normalizeSkin(saved);
        if(!(id.isEmpty()||id.equals("builtin:nethersx2")||id.equals("builtin:nethersx2_old")||localId(id)&&new File(root(),id).isDirectory()))id="";
        if(!active.equals(id)){active=id;loadSelected();refresh();}
    }
    public static boolean isLocal(String id){return localId(id);}
    public static void delete(String id) throws IOException {
        if(!localId(id))throw new IOException("Skin bawaan tidak dapat dihapus");
        if(active.equals(id))select("");
        removeTree(new File(root(),id));
    }
    public static void removeTree(File directory)throws IOException {
        File[] files=directory.listFiles();
        if(files!=null)for(File f:files){if(f.isDirectory())removeTree(f);else if(!f.delete())throw new IOException("Cannot delete skin file");}
        if(directory.exists()&&!directory.delete())throw new IOException("Cannot delete skin directory");
    }
    public static String install(InputStream stream,String name)throws IOException {
        String id="skin_"+UUID.randomUUID().toString().replace("-","");
        File staging=new File(root(),id+".tmp");boolean committed=false;
        try {
            SkinArchive.extract(new BoundedStream(stream,32L*1024*1024),staging,png -> {
                Bitmap b=decode(() -> new FileInputStream(png));
                if(b==null)return false;
                b.recycle();return true;
            });
            Properties properties=new Properties();
            String title=name==null?"":name.trim();if(title.isEmpty())title=SkinText.get(app,"custom_name");
            properties.setProperty("name",title.substring(0,Math.min(title.length(),100)));
            try(OutputStream out=new FileOutputStream(new File(staging,"skin.properties"))){properties.store(out,"Controller skin");}
            if(!staging.renameTo(new File(root(),id)))throw new IOException("Cannot save imported skin");
            committed=true;return id;
        } finally {if(!committed)removeTree(staging);}
    }
    public static class BoundedStream extends FilterInputStream {
        private long left;
        public BoundedStream(InputStream in,long limit){super(in);left=limit;}
        private void used(int n)throws IOException{if(n>0&&(left-=n)<0)throw new IOException("Download melebihi batas ukuran");}
        @Override public int read()throws IOException{int b=super.read();if(b!=-1)used(1);return b;}
        @Override public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,len);used(n);return n;}
    }
    interface Open {InputStream get()throws IOException;}
    static Bitmap decode(Open source)throws IOException {
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        try(InputStream in=source.get()){BitmapFactory.decodeStream(in,null,bounds);}
        if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>8192||bounds.outHeight>8192)return null;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>512)options.inSampleSize*=2;
        try(InputStream in=source.get()){return BitmapFactory.decodeStream(in,null,options);}
    }
    private static synchronized void loadSelected() {
        final int request=++generation;final String selected=active;
        images=Collections.emptyMap();dpadMasks=Collections.emptyMap();
        IO.execute(() -> {
            Map<String,Bitmap> loaded=new HashMap<>();
            Map<String,DpadMask> masks=new HashMap<>();
            if(!selected.isEmpty())for(String key:SkinPolicy.KEYS)try {
                Bitmap b;
                if(selected.startsWith("builtin:")){
                    String folder=selected.substring(8);
                    if(!folder.equals("nethersx2")&&!folder.equals("nethersx2_old"))continue;
                    String filename="ic_controller_"+key+(key.startsWith("analog_")?"":"_button")+".png";
                    b=decode(() -> app.getAssets().open("controller_skins/"+folder+"/"+filename));
                }else if(localId(selected)){
                    File f=new File(root(),selected+"/"+key+".png");
                    b=f.isFile()?decode(() -> new FileInputStream(f)):null;
                }else continue;
                if(b!=null){
                    loaded.put(key,b);
                    if(key.equals("up")||key.equals("right")||key.equals("down")||key.equals("left")){
                        int w=b.getWidth(),h=b.getHeight();int[] pixels=new int[w*h];
                        b.getPixels(pixels,0,w,0,0,w,h);masks.put(key,new DpadMask(w,h,pixels));
                    }
                }
            }catch(IOException|RuntimeException ignored){}
            MAIN.post(() -> {if(request!=generation)return;images=Collections.unmodifiableMap(loaded);dpadMasks=Collections.unmodifiableMap(masks);refresh();});
        });
    }
    public static Bitmap bitmap(String key){return images.get(key);}
    public static DpadMask dpadMask(String key){return dpadMasks.get(key);}
    private static void refresh() {
        if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(SkinStore::refresh);return;}
        for(View v:new ArrayList<>(views.keySet()))if(v!=null)v.invalidate();
        for(SkinPreference p:new ArrayList<>(preferences.keySet()))if(p!=null)p.r();
    }
}
