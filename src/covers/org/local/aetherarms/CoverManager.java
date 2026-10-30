// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Dual-style cover downloads and recycled-view-safe binding, with persistent offline files. */
public final class CoverManager {
    private static final String TAG="AetherArmsCovers", STYLE="covers.style3d";
    private static final Handler UI=new Handler(Looper.getMainLooper());
    private static final ExecutorService DOWNLOADS=Executors.newFixedThreadPool(2,r -> {
        Thread t=new Thread(r,"CoverDownload");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;
    });
    private static final ExecutorService IMAGES=Executors.newFixedThreadPool(2,r -> {
        Thread t=new Thread(r,"CoverDecode");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;
    });
    private static final Set<String> pending=ConcurrentHashMap.newKeySet();
    private static final Map<ImageView,Binding> views=new WeakHashMap<>(); // UI thread only.
    private static final LruCache<String,Bitmap> bitmaps=new LruCache<String,Bitmap>(12*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap image) { return image.getAllocationByteCount(); }
    };
    private static volatile Context app;
    private static final class Binding {
        final Object entry; final String serial,path,title,legacy; final boolean grid,pause;
        Binding(Object e,boolean g) throws ReflectiveOperationException {
            entry=e;grid=g;pause=false;
            serial=CoverPolicy.serial(string(e,"getSerial"));path=string(e,"getPath");
            title=string(e,"getTitle");legacy=string(e,"getCoverPath");
        }
        Binding(String p,String s,String l){
            entry=null;grid=false;pause=true;title="";
            path=p==null?"":p;serial=CoverPolicy.serial(s);legacy=l==null?"":l;
        }
    }
    private static String string(Object target,String name) throws ReflectiveOperationException {
        Object value=target.getClass().getMethod(name).invoke(target);return value==null?"":value.toString();
    }
    private static Context context(Context value) { if(app==null) app=value.getApplicationContext();return app; }
    private static SharedPreferences prefs() { return app.getSharedPreferences("aetherarms_covers",Context.MODE_PRIVATE); }
    private static boolean threeD() { return prefs().getBoolean(STYLE,false); }
    private static File folder() { return new File(app.getFilesDir(),"covers-auto"); }
    private static File imageFile(String serial,boolean style) { return new File(folder(),CoverPolicy.relativeFile(serial,style)); }
    private static int itemId(Context context) { return context.getResources().getIdentifier("action_download_covers","id",context.getPackageName()); }
    public static void configureMenu(Activity activity,Menu menu) {
        context(activity);MenuItem item=menu.findItem(itemId(activity));
        if(item!=null) { String label=threeD()?"Cover 3D":"Cover 2D";item.setTitle(label);item.setContentDescription(label+" — ketuk untuk mengganti"); }
    }
    public static boolean onMenuItem(Activity activity,MenuItem item) {
        context(activity);
        if(item.getItemId()!=itemId(activity)) return false;
        boolean style=!threeD();prefs().edit().putBoolean(STYLE,style).apply();
        String label=style?"Cover 3D":"Cover 2D";
        item.setTitle(label);item.setContentDescription(label+" — ketuk untuk mengganti");
        refresh(null);
        return true;
    }
    public static void onLibraryChanged(Context activity,Object[] entries) {
        context(activity);if(entries==null) return;
        for(Object entry:entries) try {
            String serial=CoverPolicy.serial(string(entry,"getSerial"));
            if(!serial.isEmpty()) { enqueue(serial,false);enqueue(serial,true); }
        } catch(ReflectiveOperationException failure) { Log.w(TAG,"Cover serial unavailable",failure); }
    }
    public static void markManual(Object entry,String path) {
        if(app==null) return;
        try { prefs().edit().putBoolean("manual."+hash(string(entry,"getPath")),path!=null&&!path.isEmpty()).apply(); }
        catch(ReflectiveOperationException failure) { Log.w(TAG,"Cannot record custom cover",failure); }
        UI.post(() -> refresh(null));
    }
    private static String hash(String value) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder();for(byte b:bytes) out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
        } catch(Exception impossible) { throw new IllegalStateException(impossible); }
    }
    public static void bind(Object entry,ImageView view,boolean grid) {
        context(view.getContext());
        if(Looper.myLooper()!=Looper.getMainLooper()) { UI.post(() -> bind(entry,view,grid));return; }
        final Binding binding;
        try { binding=new Binding(entry,grid); }
        catch(ReflectiveOperationException failure) { Log.e(TAG,"Cannot bind cover",failure);view.setImageDrawable(null);return; }
        bindImage(binding,view);
    }
    public static void bindPause(String path,String serial,String legacy,ImageView view){
        context(view.getContext());
        if(Looper.myLooper()!=Looper.getMainLooper()){UI.post(()->bindPause(path,serial,legacy,view));return;}
        bindImage(new Binding(path,serial,legacy),view);
    }
    private static void bindImage(final Binding binding,ImageView view){
        views.put(view,binding);view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        boolean style=threeD();
        File legacy=binding.legacy.isEmpty()?null:new File(binding.legacy);
        File selected=binding.serial.isEmpty()?null:imageFile(binding.serial,style);
        boolean manual=prefs().getBoolean("manual."+hash(binding.path),false);
        File other=binding.serial.isEmpty()?null:imageFile(binding.serial,!style);
        selected=CoverPolicy.selectExisting(manual,legacy,selected,other);
        String filename=selected!=null&&selected.isFile()?selected.getAbsolutePath():null;
        String cacheKey=filename==null?(binding.pause?"pause-disc":"placeholder:"+binding.title):filename+":"+selected.lastModified();
        Bitmap cached=bitmaps.get(cacheKey);
        if(cached!=null) view.setImageBitmap(cached);
        else {
            int placeholder=view.getResources().getIdentifier(binding.pause?"ic_media_cdrom_case":"cover_placeholder",binding.pause?"drawable":"mipmap",app.getPackageName());
            if(placeholder!=0) view.setImageResource(placeholder);else view.setImageDrawable(null);
            WeakReference<ImageView> target=new WeakReference<>(view);
            if(filename!=null||!binding.pause)IMAGES.execute(() -> {
                Bitmap image=null;
                if(filename!=null) {
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                    BitmapFactory.decodeFile(filename,options);options.inJustDecodeBounds=false;options.inSampleSize=1;
                    while(options.outWidth/options.inSampleSize>900||options.outHeight/options.inSampleSize>1200) options.inSampleSize*=2;
                    image=BitmapFactory.decodeFile(filename,options);
                } else try {
                    image=(Bitmap)binding.entry.getClass().getMethod("generateCover",Context.class,String.class).invoke(null,app,binding.title);
                } catch(ReflectiveOperationException failure) { Log.w(TAG,"Placeholder unavailable",failure); }
                if(image==null) return;
                bitmaps.put(cacheKey,image);final Bitmap decoded=image;
                UI.post(() -> { ImageView imageView=target.get();if(imageView!=null&&views.get(imageView)==binding) imageView.setImageBitmap(decoded); });
            });
        }
        if(!binding.serial.isEmpty()) { enqueue(binding.serial,false);enqueue(binding.serial,true); }
    }
    private static void enqueue(String serial,boolean style) {
        File target=imageFile(serial,style);if(target.isFile()&&target.length()>0) return;
        String key=(style?"3d.":"2d.")+serial;
        if(System.currentTimeMillis()<prefs().getLong("retry."+key,0)||!pending.add(key)) return;
        DOWNLOADS.execute(() -> {
            HttpURLConnection connection=null;File temporary=null;
            try {
                File parent=target.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs()) throw new IOException("cover directory");
                connection=(HttpURLConnection)new URL(CoverPolicy.url(serial,style)).openConnection();
                connection.setConnectTimeout(8000);connection.setReadTimeout(12000);
                connection.setRequestProperty("User-Agent","AetherArms-Local-Covers/1");
                int code=connection.getResponseCode();
                if(code!=200) {
                    prefs().edit().putLong("retry."+key,System.currentTimeMillis()+(code==404?86400000L:300000L)).apply();
                    return;
                }
                temporary=File.createTempFile("cover-",".part",parent);
                try(InputStream input=connection.getInputStream();OutputStream output=new FileOutputStream(temporary)) {
                    byte[] buffer=new byte[16384];int count,total=0;
                    while((count=input.read(buffer))!=-1) { total+=count;if(total>8*1024*1024) throw new IOException("cover too large");output.write(buffer,0,count); }
                }
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(temporary.getPath(),bounds);
                if(bounds.outWidth<=0||bounds.outHeight<=0) throw new IOException("invalid image");
                Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);temporary=null;
                prefs().edit().remove("retry."+key).apply();
                UI.post(() -> refresh(serial));
            } catch(IOException|RuntimeException failure) {
                prefs().edit().putLong("retry."+key,System.currentTimeMillis()+300000L).apply();
                Log.w(TAG,"Cover download failed: "+key+" "+failure.getClass().getSimpleName());
            } finally {
                if(temporary!=null) temporary.delete();if(connection!=null) connection.disconnect();pending.remove(key);
            }
        });
    }
    private static void refresh(String serial) {
        for(Map.Entry<ImageView,Binding> entry:new ArrayList<>(views.entrySet())) {
            ImageView view=entry.getKey();Binding bound=entry.getValue();
            // RecyclerView may reattach a cached holder without binding it again.
            // Refresh detached holders too; bind() replaces their request token so
            // a pending decode for the old style cannot overwrite the new image.
            if(view!=null&&bound!=null&&(serial==null||serial.equals(bound.serial))){
                if(bound.pause)bindPause(bound.path,bound.serial,bound.legacy,view);
                else bind(bound.entry,view,bound.grid);
            }
        }
    }
}
