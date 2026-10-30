// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.net.Uri;
import java.io.*;

/** Own framework Activity handles SAF results without adding legacy Activity lifecycle overrides. */
public final class SkinImportActivity extends Activity {
    private Uri pendingUri;
    private String pendingName;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(ActivityLocale.wrap(base));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);SkinStore.initialize(this);
        if(state!=null&&state.getString("skin.uri")!=null){
            importUri(Uri.parse(state.getString("skin.uri")),state.getString("skin.name",SkinText.get(this,"custom_name")));
        }else if(state==null){
            Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            picker.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});
            startActivityForResult(picker,41);
        }
    }
    @Override public void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=41||result!=RESULT_OK||data==null||data.getData()==null){finish();return;}
        Uri uri=data.getData();String label=SkinText.get(this,"custom_name");
        try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst())label=cursor.getString(0).replaceFirst("(?i)\\.zip$","");
        }catch(Exception ignored){}
        importUri(uri,label);
    }
    private void importUri(Uri uri,String name) {
        pendingUri=uri;pendingName=name;
        SkinUi.work(this,SkinText.get(this,"importing"),()->{
            InputStream stream=getContentResolver().openInputStream(uri);
            if(stream==null)throw new IOException("Cannot open skin ZIP");
            return SkinStore.install(stream,name);
        },this::finish);
    }
    @Override public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        if(pendingUri!=null){state.putString("skin.uri",pendingUri.toString());state.putString("skin.name",pendingName);}
    }
}
