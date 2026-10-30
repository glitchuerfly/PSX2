// SPDX-License-Identifier: GPL-3.0-or-later
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.local.aetherarms.*;

/** Host-only regression checks. No Android app launches or UI automation. */
public final class ModelChecks {
    private static void equal(Object actual,Object expected) {
        if(!Objects.equals(actual,expected)) throw new AssertionError(actual+" != "+expected);
    }
    private static void audioPauseChecks() {
        List<String> calls=new ArrayList<>();
        PauseAudioController policy=new PauseAudioController(new PauseAudioController.Output() {
            public void setSuppressed(boolean value) { calls.add("suppress="+value); }
            public void setBackgrounded(boolean value) { calls.add("background="+value); }
        });
        policy.update(true,false,false); // A new foreground VM resets both native flags.
        equal(calls,Arrays.asList("suppress=false","background=false"));calls.clear();
        policy.update(true,true,true); // Menu opens: keep audio alive BEFORE the VM pause.
        equal(calls,Arrays.asList("suppress=true"));calls.clear();
        for(int i=0;i<50;i++) policy.update(true,true,true); // Observer ticks / long pause.
        equal(calls,Collections.emptyList());
        policy.update(true,false,true); // Resume requested, but native pause still in flight.
        equal(calls,Collections.emptyList());
        policy.update(true,true,false); // Menu reopens before an earlier resume callback.
        equal(calls,Collections.emptyList());
        policy.update(true,false,false); // Native resume ACK: now suppression may be cleared.
        equal(calls,Arrays.asList("suppress=false"));calls.clear();
        policy.update(true,true,true);calls.clear();
        policy.update(false,true,true); // Home/screen off/surface loss while menu stays open.
        equal(calls,Arrays.asList("suppress=false","background=true"));calls.clear();
        policy.update(false,true,true);equal(calls,Collections.emptyList());
        policy.update(true,true,true); // Return to menu: suppression BEFORE clearing background.
        equal(calls,Arrays.asList("suppress=true","background=false"));calls.clear();
        policy.update(false,true,true);calls.clear();
        policy.update(true,false,true); // Foreground resume from a real audio pause: suppression must be armed first.
        equal(calls,Arrays.asList("suppress=true","background=false"));calls.clear();
        policy.update(true,false,true);equal(calls,Collections.emptyList()); // still awaiting resume ACK
        policy.update(true,false,false);equal(calls,Arrays.asList("suppress=false"));calls.clear();
        policy.update(true,true,true);calls.clear();
        policy.update(false,true,true); // Stop/reset releases the audio device even from a menu.
        equal(calls,Arrays.asList("suppress=false","background=true"));calls.clear();
        policy.reset();policy.update(true,false,false);
        equal(calls,Arrays.asList("suppress=false","background=false"));
        calls.clear();
        // Recents return without a menu: warm output before the CPU-thread VM resume.
        policy.update(false,false,true);equal(calls,Arrays.asList("background=true"));calls.clear();
        policy.update(true,false,true);equal(calls,Arrays.asList("suppress=true","background=false"));calls.clear();
        policy.update(false,false,true);equal(calls,Arrays.asList("suppress=false","background=true"));calls.clear();
        policy.update(false,false,false);equal(calls,Collections.emptyList()); // late resume ACK cannot warm in background
        policy.update(true,false,true);equal(calls,Arrays.asList("suppress=true","background=false"));calls.clear();
        for(int i=0;i<50;i++)policy.update(true,false,true);
        equal(calls,Collections.emptyList());
        policy.update(true,false,false);equal(calls,Arrays.asList("suppress=false"));
    }
    private static void coverSelectionChecks() throws Exception {
        Path temp=Files.createTempDirectory("cover-policy-");
        try {
            File legacy=temp.resolve("manual.png").toFile(),selected=temp.resolve("selected.jpg").toFile(),other=temp.resolve("other.png").toFile();
            Files.write(legacy.toPath(),new byte[]{1});Files.write(selected.toPath(),new byte[]{2});Files.write(other.toPath(),new byte[]{3});
            equal(CoverPolicy.selectExisting(true,legacy,selected,other),legacy);
            equal(CoverPolicy.selectExisting(false,legacy,selected,other),selected);
            Files.delete(selected.toPath());
            equal(CoverPolicy.selectExisting(false,legacy,selected,other),other);
            Files.write(selected.toPath(),new byte[0]);
            equal(CoverPolicy.selectExisting(false,legacy,selected,other),other);
            Files.delete(other.toPath());
            equal(CoverPolicy.selectExisting(false,legacy,selected,other),legacy);
            Files.delete(legacy.toPath());
            equal(CoverPolicy.selectExisting(true,legacy,selected,other),null);
            equal(CoverPolicy.selectExisting(false,null,null,null),null);
        } finally {
            try(java.util.stream.Stream<Path> files=Files.walk(temp)){
                files.sorted(Comparator.reverseOrder()).forEach(file->{try{Files.delete(file);}catch(IOException e){throw new UncheckedIOException(e);}});
            }
        }
    }
    public static void main(String[] args) throws Exception {
        audioPauseChecks();
        coverSelectionChecks();
        Map<String,Object> renderer=new HashMap<>();
        equal(GraphicsSettingsModel.translate(renderer,true).get("EmuCore/GS/Renderer"),"12");
        equal(GraphicsSettingsModel.translate(renderer,false).containsKey("EmuCore/GS/Renderer"),false);
        renderer.put("EmuCore/GS/Renderer","-1");
        equal(GraphicsSettingsModel.translate(renderer,true).get("EmuCore/GS/Renderer"),"12");
        equal(GraphicsSettingsModel.translate(renderer,false).get("EmuCore/GS/Renderer"),"12");
        for (String api:Arrays.asList("12","13","14")) {
            renderer.put("EmuCore/GS/Renderer",api);
            equal(GraphicsSettingsModel.translate(renderer,false).get("EmuCore/GS/Renderer"),api);
        }
        renderer.put("EmuCore/GS/Renderer","3"); // Desktop-only API cannot reach the Android core.
        equal(GraphicsSettingsModel.translate(renderer,true).get("EmuCore/GS/Renderer"),"12");
        equal(GraphicsSettingsModel.translate(renderer,false).containsKey("EmuCore/GS/Renderer"),false);
        Map<String,Object> global=new HashMap<>();
        global.put("EmuCore/GS/upscale_multiplier","2.000000");
        global.put("EmuCore/GS/mipmap_hw","0");global.put("EmuCore/GS/UserHacks_TriFilter","2");
        global.put("EmuCore/GS/linear_present_mode","2");global.put("EmuCore/GS/CASMode","1");
        global.put("EmuCore/GS/CASSharpness",65);global.put("EmuCore/GS/fxaa",false);
        global.put("EmuCore/GS/osd_scale",75f);global.put("EmuCore/GS/CRCHackLevel","3");
        Map<String,String> mapped=GraphicsSettingsModel.translate(global,true);
        equal(mapped.get("EmuCore/GS/upscale_multiplier"),"2.0");
        equal(mapped.get("EmuCore/GS/hw_mipmap"),"false");
        equal(mapped.get("EmuCore/GS/TriFilter"),"2");
        equal(mapped.get("EmuCore/GS/CASSharpness"),"65");
        equal(mapped.get("EmuCore/GS/linear_present_mode"),"2");
        equal(mapped.get("EmuCore/GS/OsdScale"),"48.75");
        Map<String,Object> osd=new HashMap<>();
        equal(GraphicsSettingsModel.translate(osd,true).get("EmuCore/GS/OsdScale"),"65.0");
        equal(GraphicsSettingsModel.translate(osd,false).containsKey("EmuCore/GS/OsdScale"),false);
        osd.put("EmuCore/GS/osd_scale",100f);
        equal(GraphicsSettingsModel.translate(osd,false).get("EmuCore/GS/OsdScale"),"65.0");
        osd.put("EmuCore/GS/osd_scale",200f);
        equal(GraphicsSettingsModel.translate(osd,false).get("EmuCore/GS/OsdScale"),"130.0");
        osd.put("EmuCore/GS/osd_scale","NaN");
        equal(GraphicsSettingsModel.translate(osd,true).get("EmuCore/GS/OsdScale"),"65.0");
        osd.put("EmuCore/GS/OsdScale",80f); // Native units take priority, without a second conversion.
        osd.put("EmuCore/GS/hw_mipmap",false);
        equal(GraphicsSettingsModel.translate(osd,false).get("EmuCore/GS/OsdScale"),"80.0");
        equal(GraphicsSettingsModel.translate(osd,false).get("EmuCore/GS/hw_mipmap"),"false");
        equal(mapped.containsKey("EmuCore/GS/mipmap_hw"),false);
        equal(mapped.containsKey("EmuCore/GS/CRCHackLevel"),false);
        equal(mapped.get("EmuCore/GS/OsdMessagesPos"),"1");
        equal(mapped.get("EmuCore/GS/FrameLimitEnable"),"true");
        Map<String,String> profile=GraphicsSettingsModel.readIni(new StringReader(
            "[EmuCore/GS]\nupscale_multiplier = 3.0\nUserHacks_AutoFlush = true\nfxaa = true\nCropLeft = 2\n[EmuCore/Speedhacks]\nEECycleRate = -1\n"));
        Map<String,String> perGame=GraphicsSettingsModel.translate(profile,false);
        equal(perGame.get("EmuCore/GS/upscale_multiplier"),"3.0");
        equal(perGame.get("EmuCore/GS/UserHacks_AutoFlushLevel"),"2");
        equal(perGame.get("EmuCore/GS/CropLeft"),"2");
        equal(perGame.get("EmuCore/Speedhacks/EECycleRate"),"-1");
        equal(perGame.containsKey("EmuCore/GS/CASMode"),false); // inherit global, never stamp defaults into a profile.
        equal(GraphicsSettingsModel.readIni(new StringReader(GraphicsSettingsModel.writeIni(perGame))),perGame);
        global.remove("EmuCore/GS/mipmap_hw");
        equal(GraphicsSettingsModel.translate(global,true).get("EmuCore/GS/hw_mipmap"),"true"); // reset restores Auto.
        global.put("EmuCore/GS/upscale_multiplier","NaN");
        equal(GraphicsSettingsModel.translate(global,true).get("EmuCore/GS/upscale_multiplier"),"1.0");
        equal(CoverPolicy.url("SLUS_20999",false),"https://raw.githubusercontent.com/xlenore/ps2-covers/main/covers/default/SLUS-20999.jpg");
        equal(CoverPolicy.url("SLUS-20999",true),"https://raw.githubusercontent.com/xlenore/ps2-covers/main/covers/3d/SLUS-20999.png");
        equal(CoverPolicy.serial("../../cover"),"");
        equal(CoverPolicy.serial("https://evil.test/a"),"");
        if(CoverPolicy.relativeFile("SCUS-97177",true).equals(CoverPolicy.relativeFile("SCUS-97177",false))) throw new AssertionError("Styles must have distinct offline files");
        Path temp=Files.createTempDirectory("aether-profile-check-");
        try {
            Path dir=Files.createDirectories(temp.resolve("gamesettings"));
            Path legacy=dir.resolve("5AE01D98.ini"),nativeFile=dir.resolve("SCUS-97177_5AE01D98.ini");
            String source="[EmuCore/GS]\nupscale_multiplier = 2\nmipmap_hw = 0\nosd_scale = 100\nRenderer = -1\n";
            Files.write(legacy,source.getBytes(StandardCharsets.UTF_8));
            String oldNative="[EmuCore/GS]\nupscale_multiplier = 8\n[Custom]\nkeep = yes\n";
            Files.write(nativeFile,oldNative.getBytes(StandardCharsets.UTF_8));
            equal(ProfileMigrator.prepare(temp.toFile(),"SCUS-97177",0x5AE01D98),true);
            Map<String,String> converted=GraphicsSettingsModel.readIni(Files.newBufferedReader(nativeFile));
            equal(converted.get("EmuCore/GS/upscale_multiplier"),"2.0");
            equal(converted.get("EmuCore/GS/hw_mipmap"),"false");equal(converted.get("Custom/keep"),"yes");
            equal(converted.get("EmuCore/GS/OsdScale"),"65.0");
            equal(converted.get("EmuCore/GS/Renderer"),"12");
            equal(new String(Files.readAllBytes(legacy),StandardCharsets.UTF_8),source); // legacy UI input unchanged.
            equal(ProfileMigrator.prepare(temp.toFile(),"SCUS-97177",0x5AE01D98),false);
            Files.write(legacy,"[EmuCore/GS]\nfilter = 2\n".getBytes(StandardCharsets.UTF_8));
            ProfileMigrator.prepare(temp.toFile(),"SCUS-97177",0x5AE01D98);
            equal(GraphicsSettingsModel.readIni(Files.newBufferedReader(nativeFile)).containsKey("EmuCore/GS/upscale_multiplier"),false);
            equal(GraphicsSettingsModel.readIni(Files.newBufferedReader(nativeFile)).containsKey("EmuCore/GS/OsdScale"),false);
            equal(GraphicsSettingsModel.readIni(Files.newBufferedReader(nativeFile)).containsKey("EmuCore/GS/Renderer"),false);
            Files.delete(legacy);ProfileMigrator.prepare(temp.toFile(),"SCUS-97177",0x5AE01D98);
            equal(new String(Files.readAllBytes(nativeFile),StandardCharsets.UTF_8),oldNative); // restore pre-existing native profile.
        } finally {
            try(java.util.stream.Stream<Path> files=Files.walk(temp)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {try {Files.delete(path);}catch(IOException e){throw new UncheckedIOException(e);}});
            }
        }
        System.out.println("PASS: audio menu/resume-ACK/background/surface/reset ordering, graphics defaults/types/aliases, retired Auto conversion/manual renderer and inheritance, compact OSD sizing/native units, per-game inheritance/INI roundtrip, reset, invalid values, 2D/3D cover URLs and safe filenames.");
    }
}
