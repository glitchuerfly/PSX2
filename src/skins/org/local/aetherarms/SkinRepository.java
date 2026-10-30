// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.content.Context;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Same manifest/tree/ZIP endpoints as upstream ARMSX2 SkinRepo, no third-party mirror. */
public final class SkinRepository {
    public static final class Remote {
        public final String name,file,preview,author;
        Remote(String n,String f,String p,String a){name=n;file=f;preview=p;author=a;}
    }
    static byte[] get(String url,int limit)throws IOException {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(15000);connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent","ARMSX2-Hybrid-Skins/1");
        try {
            if(connection.getResponseCode()!=200)throw new IOException("Server HTTP "+connection.getResponseCode());
            if(connection.getContentLengthLong()>limit)throw new IOException("Unduhan terlalu besar");
            try(InputStream in=new SkinStore.BoundedStream(connection.getInputStream(),limit);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toByteArray();
            }
        }finally{connection.disconnect();}
    }
    public static List<Remote> catalog(Context ctx)throws Exception {
        File cached=new File(ctx.getCacheDir(),"controller-skin-manifest.json");
        byte[] body;
        try{body=get(SkinPolicy.RAW+"manifest.json",1024*1024);}
        catch(IOException offline){if(cached.isFile())body=java.nio.file.Files.readAllBytes(cached.toPath());else return tree();}
        List<Remote> list=new ArrayList<>();
        try {
            JSONArray skins=new JSONObject(new String(body,StandardCharsets.UTF_8)).getJSONArray("skins");
            for(int i=0;i<skins.length();i++){
                JSONObject skin=skins.optJSONObject(i);if(skin==null)continue;
                String file=skin.optString("file",""),preview=skin.optString("preview","");
                try{SkinPolicy.url(file,"Skins",".zip");}catch(IllegalArgumentException invalid){continue;}
                if(!preview.isEmpty())try{SkinPolicy.url(preview,"Previews",".png");}catch(IllegalArgumentException invalid){preview="";}
                list.add(new Remote(skin.optString("name",file),file,preview,skin.optString("author","community")));
            }
        }catch(JSONException invalid){return tree();}
        if(list.isEmpty())return tree();
        try(FileOutputStream out=new FileOutputStream(cached)){out.write(body);}
        return list;
    }
    private static List<Remote> tree()throws Exception {
        JSONArray tree=new JSONObject(new String(get("https://api.github.com/repos/"+SkinPolicy.REPO+"/git/trees/main?recursive=1",4*1024*1024),StandardCharsets.UTF_8)).getJSONArray("tree");
        List<Remote> out=new ArrayList<>();
        for(int i=0;i<tree.length();i++){
            JSONObject item=tree.getJSONObject(i);String path=item.optString("path","");
            if(!item.optString("type").equals("blob")||item.optLong("size",0)<1024)continue;
            try{SkinPolicy.url(path,"Skins",".zip");}catch(IllegalArgumentException invalid){continue;}
            String name=path.substring(path.lastIndexOf('/')+1,path.length()-4);
            out.add(new Remote(name,path,"","community"));
        }
        if(out.isEmpty())throw new IOException("Katalog skin belum tersedia");return out;
    }
    public static String install(Remote skin)throws Exception {
        byte[] archive=get(SkinPolicy.url(skin.file,"Skins",".zip"),32*1024*1024);
        return SkinStore.install(new ByteArrayInputStream(archive),skin.name);
    }
    public static File preview(Context ctx,Remote skin)throws Exception {
        if(skin.preview.isEmpty())return null;
        String url=SkinPolicy.url(skin.preview,"Previews",".png");
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));StringBuilder key=new StringBuilder();for(byte b:hash)key.append(String.format(Locale.ROOT,"%02x",b));
        File f=new File(ctx.getCacheDir(),"skin-preview-"+key+".png");
        if(f.isFile()&&f.length()>0)return f;
        byte[] body=get(url,4*1024*1024);
        try(FileOutputStream out=new FileOutputStream(f)){out.write(body);}return f;
    }
}
