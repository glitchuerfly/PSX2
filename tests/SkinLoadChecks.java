// SPDX-License-Identifier: GPL-3.0-or-later
import org.local.aetherarms.SkinLoadState;

/** Execute the actual cache state used by SkinStore with out-of-order decoder completions. */
public final class SkinLoadChecks {
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        Object defaults=new Object(),a=new Object(),b=new Object(),c=new Object();
        SkinLoadState<Object> skins=new SkinLoadState<>(defaults,2);
        check(skins.snapshot().ready&&skins.snapshot().value==defaults,"Default should be immediate");
        SkinLoadState.Request first=skins.select("game-A");
        check(first!=null&&!skins.snapshot().ready&&skins.snapshot().value==null,"Cold custom skin exposed Default art");
        check(skins.select("game-A")==null,"Repeated configuration scheduled duplicate decoding");
        check(skins.complete(first,a)&&skins.snapshot().value==a,"Decoded artwork was not published atomically");
        SkinLoadState.Request second=skins.select("game-B");
        check(!skins.snapshot().ready&&skins.snapshot().value==null,"New game exposed the previous skin");
        check(skins.select("game-A")==null&&skins.snapshot().ready&&skins.snapshot().value==a,"Warm game skin was not instant");
        check(!skins.complete(second,b)&&skins.snapshot().value==a,"Late decode replaced the current game skin");
        check(skins.select("game-B")==null&&skins.snapshot().value==b,"Completed stale result was not reusable in cache");
        check(skins.select("")==null&&skins.snapshot().value==defaults,"Explicit Default should never wait for I/O");
        check(!skins.complete(second,b)&&skins.snapshot().value==defaults,"Late callback resurrected a custom skin after Default");
        SkinLoadState<Object> racing=new SkinLoadState<>(defaults,2);
        SkinLoadState.Request oldA=racing.select("A"),oldB=racing.select("B"),newA=racing.select("A");
        check(!racing.complete(oldA,a)&&!racing.snapshot().ready,"A-B-A accepted an obsolete generation");
        check(!racing.complete(oldB,b)&&!racing.snapshot().ready,"Wrong game became visible while loading");
        check(racing.complete(newA,c)&&racing.snapshot().value==c,"Current A-B-A generation was rejected");
        SkinLoadState<Object> bounded=new SkinLoadState<>(defaults,1);
        check(bounded.complete(bounded.select("A"),a),"A decode");
        check(bounded.complete(bounded.select("B"),b),"B decode");
        check(bounded.select("A")!=null&&!bounded.snapshot().ready,"Evicted skin unexpectedly retained unbounded cache");
        // Reproduce the real worker/UI ordering: old Default is enqueued, game skin
        // is applied by the layout, then the old queue entry finally executes.
        SkinLoadState<Object> ordered=new SkinLoadState<>(defaults,2);
        long oldGlobal=ordered.requestSelection();
        long gameLayout=ordered.requestSelection();
        check(ordered.selectionCurrent(gameLayout),"Current layout selection rejected");
        ordered.complete(ordered.select("game"),a);
        if(ordered.selectionCurrent(oldGlobal))ordered.select("");
        check(ordered.snapshot().ready&&ordered.snapshot().value==a,"Queued global Default overrode visible game skin");
        long queuedReload=ordered.requestSelection();
        long sameLayout=ordered.requestSelection(); // same skin still invalidates old selections
        check(ordered.select("game")==null,"Same layout should reuse its artwork");
        check(!ordered.selectionCurrent(queuedReload)&&ordered.selectionCurrent(sameLayout),"Same-id layout did not cancel stale selection");
        long queuedGame=ordered.requestSelection();
        long userDefault=ordered.requestSelection();
        ordered.select("");
        check(!ordered.selectionCurrent(queuedGame)&&ordered.selectionCurrent(userDefault)&&ordered.snapshot().value==defaults,"Explicit Default was undone by a queued game update");
        // Native CRC zero has no profile. Gameplay rendering must retain the known
        // launch CRC, exactly as ControllerSettings.effective does for input.
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("skin-controller-scope-");
        java.nio.file.Path games=root.resolve("gamesettings");java.nio.file.Files.createDirectories(games);
        java.nio.file.Path profile=games.resolve("CA857E71.ini");
        try{
            java.nio.file.Files.write(profile,("[Pad]\nUseGameSettingsForController = true\n[TouchscreenController]\nCustomSkin = game-skin\n[TouchscreenController/analog_sticks]\nToggleFastForwardLandscapeVisible = true\nMacro1LandscapeVisible = false\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.util.Map<String,Object> global=new java.util.HashMap<>();global.put("TouchscreenController/CustomSkin","default");
            check("default".equals(org.local.aetherarms.ControllerSettings.effective(root.toFile(),0,global).get("TouchscreenController/CustomSkin")),"Zero-CRC fixture did not reproduce fallback");
            Object effective=org.local.aetherarms.ControllerSettings.effective(root.toFile(),(int)0xCA857E71L,global).get("TouchscreenController/CustomSkin");
            check("game-skin".equals(effective),"Known game CRC lost its skin during boot");
            check(org.local.aetherarms.ControllerSettings.profileFile(root.toFile(),0)==null,"CRC zero must have no controller profile");
            check(profile.toFile().equals(org.local.aetherarms.ControllerSettings.profileFile(root.toFile(),(int)0xCA857E71L)),"First layout selected a different profile from core input");
            java.util.Map<String,Object> layout=org.local.aetherarms.ControllerSettings.effective(root.toFile(),(int)0xCA857E71L,global);
            check(org.local.aetherarms.ControllerSettings.truth(layout.get("TouchscreenController/analog_sticks/ToggleFastForwardLandscapeVisible")),"Added fast-forward button missing at boot");
            check(!org.local.aetherarms.ControllerSettings.truth(layout.get("TouchscreenController/analog_sticks/Macro1LandscapeVisible")),"Removed macro button reappeared at boot");

            check(org.local.aetherarms.ControllerSettings.gameConfigEnabled(root.toFile(),(int)0xCA857E71L),"Enabled per-game configuration lost its notification");
            check(!org.local.aetherarms.ControllerSettings.gameConfigEnabled(root.toFile(),0),"Unknown/BIOS CRC produced configuration notice");
            check(!org.local.aetherarms.ControllerSettings.gameConfigEnabled(root.toFile(),0x12345678),"Game with no configuration produced notice");
            java.nio.file.Path nativeProfile=games.resolve("SLPM-66848_CA857E71.ini");
            java.nio.file.Files.write(nativeProfile,new byte[0]);
            check(org.local.aetherarms.ControllerSettings.launchCRC(root.toFile(),"SLPM-66848",0,0)==(int)0xCA857E71L,"Known controller profile was deferred until first game frame");
            check(org.local.aetherarms.ControllerSettings.launchCRC(root.toFile(),"OTHER-SERIAL",0,0)==0,"Different game inherited this profile");
            check(org.local.aetherarms.ControllerSettings.launchCRC(root.toFile(),"SLPM-66848",0,0x76543210)==0x76543210,"Remembered boot identity was ignored");
            check(org.local.aetherarms.ControllerSettings.launchCRC(root.toFile(),"SLPM-66848",0x12345678,0x76543210)==0x12345678,"Current game-list identity lost priority");
            java.nio.file.Path other=games.resolve("12345678.ini"),nativeOther=games.resolve("SLPM-66848_12345678.ini");
            java.nio.file.Files.write(other,new byte[0]);java.nio.file.Files.write(nativeOther,new byte[0]);
            check(org.local.aetherarms.ControllerSettings.launchCRC(root.toFile(),"SLPM-66848",0,0)==0,"Ambiguous multi-ELF profile was guessed");
            java.nio.file.Files.delete(other);java.nio.file.Files.delete(nativeOther);java.nio.file.Files.delete(nativeProfile);
            java.nio.file.Files.write(profile,"[Pad]\nUseGameSettingsForController = false\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            global.put("Pad/UseGameSettingsForController",true); // global metadata must not enable this game
            check(!org.local.aetherarms.ControllerSettings.gameConfigEnabled(root.toFile(),(int)0xCA857E71L),"Disabled per-game configuration still produced notice");

        }finally{java.nio.file.Files.deleteIfExists(profile);java.nio.file.Files.deleteIfExists(games);java.nio.file.Files.deleteIfExists(root);}
        System.out.println("PASS: no Default/previous-skin flash, warm cache, stale image and selection callbacks, same-id invalidation, preboot identity/visibility, per-game notice OFF/default, explicit Default and bounded cache.");
    }
}
