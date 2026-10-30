// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Bounded ZIP extraction into canonical flat PNG names; caller publishes staging atomically. */
public final class SkinArchive {
    public interface Validator { boolean valid(File png) throws IOException; }
    public static int extract(InputStream input,File staging,Validator validator) throws IOException {
        if(!staging.isDirectory()&&!staging.mkdirs()) throw new IOException("Cannot create skin directory");
        Set<String> written=new HashSet<>();long total=0;int entries=0;
        byte[] buffer=new byte[16384];
        try(ZipInputStream zip=new ZipInputStream(input)) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if(++entries>512) throw new IOException("Too many ZIP entries");
                String key=entry.isDirectory()?null:SkinPolicy.keyForFilename(entry.getName());
                File output=key==null||written.contains(key)?null:new File(staging,key+".png");
                long bytes=0;
                try(OutputStream dest=output==null?new OutputStream(){public void write(int b){} public void write(byte[] b,int o,int n){}}:new FileOutputStream(output)) {
                    int count;
                    while((count=zip.read(buffer))!=-1) {
                        bytes+=count;total+=count;
                        if(bytes>8L*1024*1024||total>64L*1024*1024) throw new IOException("Skin ZIP exceeds size limit");
                        dest.write(buffer,0,count);
                    }
                }
                if(output!=null) {
                    if(validator.valid(output)) written.add(key);
                    else if(!output.delete()) throw new IOException("Cannot discard invalid PNG");
                }
                zip.closeEntry();
            }
        }
        if(written.isEmpty()) throw new IOException("ZIP tidak berisi gambar tombol PNG yang didukung");
        return written.size();
    }
}
