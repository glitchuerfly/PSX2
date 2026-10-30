// SPDX-License-Identifier: GPL-3.0-or-later
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.local.aetherarms.*;

public final class SkinChecks {
    static void equal(Object actual,Object expected){if(!Objects.equals(actual,expected))throw new AssertionError(actual+" != "+expected);}
    static byte[] zip(Map<String,byte[]> files)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream archive=new ZipOutputStream(out)){
            for(Map.Entry<String,byte[]> item:files.entrySet()){archive.putNextEntry(new ZipEntry(item.getKey()));archive.write(item.getValue());archive.closeEntry();}
        }return out.toByteArray();
    }
    static boolean png(File file)throws IOException{
        byte[] magic=new byte[8];try(InputStream in=new FileInputStream(file)){if(in.read(magic)!=8)return false;}
        return Arrays.equals(magic,new byte[]{(byte)137,80,78,71,13,10,26,10});
    }
    public static void main(String[] args)throws Exception{
        equal(SkinPolicy.keyForFilename("nested/ic_controller_R1_button.png"),"r1");
        equal(SkinPolicy.keyForFilename("Cross.PNG"),"cross");
        equal(SkinPolicy.keyForFilename("L2.png"),"l2");
        equal(SkinPolicy.keyForFilename("ic_controller_analog_stick_left.png"),"analog_stick_left");
        equal(SkinPolicy.keyForFilename("analog_stick_right.png"),"analog_stick_right");
        equal(SkinPolicy.keyForFilename("M1.png"),"macro1");
        equal(SkinPolicy.keyForFilename("macro_4.png"),"macro4");
        for(String name:Arrays.asList("../r1.png","C:\\r1.png","/r1.png","__MACOSX/._r1.png","r1.jpg","unknown.png"))equal(SkinPolicy.keyForFilename(name),null);
        equal(SkinPolicy.url("Skins/God Of War 2.zip","Skins",".zip"),SkinPolicy.RAW+"Skins/God%20Of%20War%202.zip");
        for(String path:Arrays.asList("Skins/../file.zip","https://other/file.zip","Skins//file.zip","Skins/C:\\file.zip")){
            boolean rejected=false;try{SkinPolicy.url(path,"Skins",".zip");}catch(IllegalArgumentException e){rejected=true;}equal(rejected,true);
        }
        float fit=SkinPolicy.fitScale(70,42,512,256);equal(fit*512,70f);equal(fit*256,35f);
        float tall=SkinPolicy.fitScale(70,42,128,256);equal(tall*128,21f);equal(tall*256,42f);
        equal(SkinPolicy.fitScale(0,42,512,256),0f);
        Path root=Files.createTempDirectory("skin-model-");
        byte[] image=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
        try{
            Map<String,byte[]> files=new LinkedHashMap<>();
            for(String name:Arrays.asList("folder/ic_controller_r1_button.png","L2.png","Cross.png","analog_stick_left.png","analog_stick_right.png","M1.png","../r3.png","__MACOSX/._r1.png"))files.put(name,image);
            files.put("nested/square.png",new byte[]{1,2,3});files.put("layout.json","{}".getBytes("UTF-8"));
            Path valid=root.resolve("valid");
            int count=SkinArchive.extract(new ByteArrayInputStream(zip(files)),valid.toFile(),SkinChecks::png);
            equal(count,6);equal(Files.exists(root.resolve("r3.png")),false);equal(Files.exists(valid.resolve("square.png")),false);
            equal(Files.exists(valid.resolve("analog_stick_left.png")),true);equal(Files.exists(valid.resolve("analog_stick_right.png")),true);
            equal(Files.exists(valid.resolve("layout.json")),false); // A skin cannot overwrite the Aether touch layout.
            files.clear();files.put("layout.json","{}".getBytes("UTF-8"));
            boolean rejected=false;try{SkinArchive.extract(new ByteArrayInputStream(zip(files)),root.resolve("empty").toFile(),SkinChecks::png);}catch(IOException e){rejected=true;}equal(rejected,true);
            files.clear();files.put("big-unknown.bin",new byte[8*1024*1024+1]);
            rejected=false;try{SkinArchive.extract(new ByteArrayInputStream(zip(files)),root.resolve("bomb").toFile(),SkinChecks::png);}catch(IOException e){rejected=true;}equal(rejected,true);
            Path fixture=Paths.get(args[0]);
            if(Files.isRegularFile(fixture)){
                try(InputStream in=Files.newInputStream(fixture)){
                    int live=SkinArchive.extract(in,root.resolve("live").toFile(),SkinChecks::png);
                    if(live<16)throw new AssertionError("Community fixture missing core controls: "+live);
                    System.out.println("PASS: live community ZIP compatibility ("+live+" usable PNG slots); not installed on device.");
                }
            }
        }finally{
            try(java.util.stream.Stream<Path> walk=Files.walk(root)){walk.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new UncheckedIOException(e);}});}
        }
        System.out.println("PASS: skin aliases, distinct analog sides, safe repository URLs, proportional fit, ZIP paths/limits/invalid PNG filtering and layout preservation.");
    }
}
