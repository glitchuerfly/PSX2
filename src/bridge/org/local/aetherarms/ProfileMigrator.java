// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Preserve legacy inputs and native backups while generating the file the new core loads. */
public final class ProfileMigrator {
    private static final String HEADER="; AetherArms managed graphics translation v1";
    public static synchronized boolean prepare(File root,String serial,int crc) throws IOException {
        if(crc==0||serial==null||serial.isEmpty()) return false;
        File directory=new File(root,"gamesettings");
        String hex=String.format(Locale.ROOT,"%08X",crc);
        File source=new File(directory,hex+".ini");
        File target=new File(directory,serial.replaceAll("[^A-Za-z0-9._-]","_")+"_"+hex+".ini");
        File backup=new File(directory,target.getName()+".pre-adapter.bak");
        if(!source.isFile()) {
            File original=backup.isFile()?backup:target;
            if(original.isFile()&&read(original).keySet().stream().anyMatch(ControllerSettings::owns)) {
                if(!backup.isFile())Files.copy(target.toPath(),backup.toPath(),StandardCopyOption.REPLACE_EXISTING);
                Map<String,String> retained=new TreeMap<>(read(backup));retained.keySet().removeIf(ControllerSettings::owns);
                byte[] content=GraphicsSettingsModel.writeIni(retained).getBytes(StandardCharsets.UTF_8);
                if(target.isFile()&&Arrays.equals(content,Files.readAllBytes(target.toPath())))return false;
                atomicWrite(target,content);return true;
            }
            if(target.isFile()&&isManaged(target)) {
                if(backup.isFile()) atomicWrite(target,Files.readAllBytes(backup.toPath()));
                else Files.deleteIfExists(target.toPath());
                return true;
            }
            return false;
        }
        if(!directory.isDirectory()&&!directory.mkdirs()) throw new IOException("mkdir gamesettings");
        if(target.isFile()&&!isManaged(target)) Files.copy(target.toPath(),backup.toPath(),StandardCopyOption.REPLACE_EXISTING);
        Map<String,String> values=new TreeMap<>();
        if(backup.isFile()) {
            values.putAll(read(backup));
            // Legacy UI owns the known graphics controls. Clear old per-game values
            // so an unset override really inherits the current global selection.
            for(GraphicsSettingsModel.Rule rule:GraphicsSchema.RULES) {
                values.remove(rule.source);values.remove(rule.target);
            }
        }
        values.putAll(GraphicsSettingsModel.translate(read(source),false));
        // Effective controller values are pushed as a complete typed base snapshot.
        // Remove the legacy controller layer so it cannot silently override global/profile choice.
        values.keySet().removeIf(ControllerSettings::owns);
        byte[] content=GraphicsSettingsModel.writeIni(values).getBytes(StandardCharsets.UTF_8);
        if(target.isFile()&&Arrays.equals(content,Files.readAllBytes(target.toPath()))) return false;
        atomicWrite(target,content);return true;
    }
    private static Map<String,String> read(File path) throws IOException {
        try(Reader reader=new InputStreamReader(new FileInputStream(path),StandardCharsets.UTF_8)) { return GraphicsSettingsModel.readIni(reader); }
    }
    private static boolean isManaged(File path) throws IOException {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(path),StandardCharsets.UTF_8))) { return HEADER.equals(reader.readLine()); }
    }
    private static void atomicWrite(File path,byte[] content) throws IOException {
        Path temporary=Files.createTempFile(path.getParentFile().toPath(),"profile-",".tmp");
        try {
            Files.write(temporary,content);
            try { Files.move(temporary,path.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException unsupported) { Files.move(temporary,path.toPath(),StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
