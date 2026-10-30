// SPDX-License-Identifier: GPL-3.0-or-later
import org.local.aetherarms.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class FullControllerChecks {
    static void eq(Object actual,Object expected){if(!Objects.equals(actual,expected))throw new AssertionError(actual+" != "+expected);}
    static void near(float actual,float expected){if(Math.abs(actual-expected)>.0001f)throw new AssertionError(actual+" != "+expected);}
    static void write(Path path,String value)throws Exception{Files.write(path,value.getBytes(StandardCharsets.UTF_8));}
    static void layers()throws Exception{
        Map<String,Object> orientationGlobal=new HashMap<>(),orientationGame=new HashMap<>();
        eq(ControllerSettings.editorOrientation(orientationGlobal,orientationGame),11);
        orientationGlobal.put("UI/EmulationActivityOrientation","portrait");
        eq(ControllerSettings.editorOrientation(orientationGlobal,orientationGame),12);
        orientationGame.put("UI/EmulationActivityOrientation","landscape");
        orientationGame.put("Pad/UseGameSettingsForController",false);
        eq(ControllerSettings.editorOrientation(orientationGlobal,orientationGame),11);
        orientationGame.put("UI/EmulationActivityOrientation","sensor");
        eq(ControllerSettings.editorOrientation(orientationGlobal,orientationGame),4);
        orientationGame.put("UI/EmulationActivityOrientation","unspecified");
        eq(ControllerSettings.editorOrientation(orientationGlobal,orientationGame),-1);
        orientationGame.put("UI/EmulationActivityOrientation","unknown");
        eq(ControllerSettings.editorOrientation(orientationGlobal,orientationGame),-1);
        eq(orientationGlobal.get("UI/EmulationActivityOrientation"),"portrait");
        Path root=Files.createTempDirectory("controllers-full-");
        try{
            Files.createDirectories(root.resolve("gamesettings"));Files.createDirectories(root.resolve("inputprofiles"));
            Map<String,Object> global=new HashMap<>();global.put("Pad1/AxisScale",1.1f);global.put("Pad/MultitapPort1",false);global.put("TouchscreenController/CustomSkin","builtin:nethersx2");global.put("EmuCore/GS/Renderer","14");
            global.put("Pad2/Cross",new LinkedHashSet<>(Arrays.asList("pad1/Button99","pad2/Button96")));
            Path game=root.resolve("gamesettings/12345678.ini");
            write(game,"[Pad]\nUseGameSettingsForController = false\n[Pad1]\nAxisScale = 0.5\n[TouchscreenController]\nCustomSkin = default\n");
            Map<String,Object> effective=ControllerSettings.effective(root.toFile(),0x12345678,global);
            eq(effective.get("Pad1/AxisScale"),1.1f);eq(effective.get("TouchscreenController/CustomSkin"),"builtin:nethersx2");eq(effective.containsKey("EmuCore/GS/Renderer"),false);
            write(game,"[Pad]\nUseGameSettingsForController = true\nMultitapPort1 = true\n[Pad1]\nAxisScale = 0.5\n[Pad2]\nCross = other/Button100\nCross = other/Button101\n[TouchscreenController]\nCustomSkin = default\nAnalogMode = static\n");
            effective=ControllerSettings.effective(root.toFile(),0x12345678,global);
            eq(effective.get("Pad1/AxisScale"),"0.5");eq(effective.get("TouchscreenController/CustomSkin"),"default");eq(effective.get("TouchscreenController/AnalogMode"),"static");
            eq(ControllerSettings.bindings(effective.get("Pad2/Cross")),Arrays.asList("other/Button100","other/Button101"));
            eq(global.get("Pad1/AxisScale"),1.1f); // No flattening or global mutation.
            eq(ControllerSettings.emptyBindingOverride("Pad1/Cross",Collections.emptySet()),true);eq(ControllerSettings.emptyBindingOverride("Pad1/Cross",null),false);
            eq(ControllerSettings.emptyBindingOverride("Paths/Games",Collections.emptySet()),false);
            eq(ControllerSettings.bindings(ControllerSettings.UNBOUND),Collections.emptyList());
            eq(ControllerSettings.visibleBindings(new LinkedHashSet<>(Arrays.asList(ControllerSettings.UNBOUND))),Collections.emptySet());
            write(root.resolve("inputprofiles/pad.ini"),"[Pad1]\nAxisScale = 1.7\n[TouchscreenController]\nEnableVibration = true\n");
            write(game,"[Pad]\nInputProfileName = pad\n[Pad1]\nAxisScale = 0.1\n");
            effective=ControllerSettings.effective(root.toFile(),0x12345678,global);
            eq(effective.get("Pad1/AxisScale"),"1.7");eq(effective.get("TouchscreenController/EnableVibration"),"true");eq(effective.get("TouchscreenController/CustomSkin"),"builtin:nethersx2");
            Map<String,String> defaults=new HashMap<>(),types=new HashMap<>();
            defaults.put("Pad1/AxisScale","1.33");types.put("Pad1/AxisScale","float");defaults.put("Pad/MultitapPort1","false");types.put("Pad/MultitapPort1","bool");defaults.put("Pad2/Type","None");types.put("Pad2/Type","string");
            Map<String,String> nativeValues=ControllerSettings.nativeValues(effective,defaults,types);eq(nativeValues.get("Pad1/AxisScale"),"1.7");eq(nativeValues.get("Pad2/Type"),"None");eq(nativeValues.get("EmuCore/InputProfileName"),"");
            nativeValues=ControllerSettings.nativeValues(Collections.emptyMap(),defaults,types);eq(nativeValues.get("Pad1/AxisScale"),"1.33");eq(nativeValues.get("Pad/MultitapPort1"),"false"); // Reset sends defaults, not merely present keys.
            Map<String,Object> bad=new HashMap<>();bad.put("Pad1/AxisScale",Float.NaN);eq(ControllerSettings.nativeValues(bad,defaults,types).get("Pad1/AxisScale"),"1.33");
            eq(ControllerSettings.integer(42.9f,1,0,100),42);
            eq(ControllerSettings.explicitPhoneMotor(global),false);global.put("Pad2/LargeMotor","__DEVICE_VIBRATOR__/Vibrator0");eq(ControllerSettings.explicitPhoneMotor(global),true);
            // The native game layer may no longer bypass controller selection with stale values.
            Path nativeFile=root.resolve("gamesettings/SLUS-00000_12345678.ini");
            write(nativeFile,"[Pad1]\nAxisScale = 99\n[EmuCore/GS]\nupscale_multiplier = 3\n[Custom]\nkeep = yes\n");
            eq(ProfileMigrator.prepare(root.toFile(),"SLUS-00000",0x12345678),true);
            Map<String,Object> nativeProfile=ControllerSettings.read(nativeFile.toFile());
            eq(nativeProfile.containsKey("Pad1/AxisScale"),false);eq(nativeProfile.get("Custom/keep"),"yes");
            eq(ControllerSettings.read(root.resolve("gamesettings/SLUS-00000_12345678.ini.pre-adapter.bak").toFile()).get("Pad1/AxisScale"),"99");
            Files.delete(game);eq(ProfileMigrator.prepare(root.toFile(),"SLUS-00000",0x12345678),true);
            nativeProfile=ControllerSettings.read(nativeFile.toFile());eq(nativeProfile.containsKey("Pad1/AxisScale"),false);eq(nativeProfile.get("EmuCore/GS/upscale_multiplier"),"3");
            eq(ProfileMigrator.prepare(root.toFile(),"SLUS-00000",0x12345678),false);
        }finally{try(java.util.stream.Stream<Path> files=Files.walk(root)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});}}
    }
    static void bindings(){
        Map<String,Object> config=new LinkedHashMap<>();config.put("Pad1/Cross",new LinkedHashSet<>(Arrays.asList("alpha/Button99","beta/Button96 & beta/Button102")));
        config.put("Pad2/Cross","beta/Button99");config.put("Pad8/LRight","gamma/+Axis0");config.put("Pad8/LLeft","gamma/-Axis0");
        config.put("Hotkeys/QuickSave","alpha/Button101");config.put("Pad3/Macro1","alpha/Button103");
        Map<String,Float> out=new HashMap<>();ControllerBindings engine=new ControllerBindings(config,config.keySet(),out::put);
        eq(engine.hasButton("alpha",99),true);eq(engine.hasButton("alpha",96),false);
        engine.button("beta",99,true);eq(out.get("Pad2/Cross"),1f);eq(out.containsKey("Pad1/Cross"),false);
        engine.button("beta",96,true);eq(out.containsKey("Pad1/Cross"),false);
        engine.button("beta",102,true);eq(out.get("Pad1/Cross"),1f);
        engine.button("alpha",99,true);engine.button("beta",102,false);eq(out.get("Pad1/Cross"),1f); // Other alternative still held.
        engine.button("alpha",99,false);eq(out.get("Pad1/Cross"),0f);
        engine.axis("gamma",0,.35f);near(out.get("Pad8/LRight"),.35f);
        engine.axis("gamma",0,-.8f);eq(out.get("Pad8/LRight"),0f);near(out.get("Pad8/LLeft"),.8f);
        engine.axis("gamma",0,Float.NaN);eq(out.get("Pad8/LLeft"),0f);
        engine.button("alpha",101,true);eq(out.get("Hotkeys/QuickSave"),1f);
        engine.button("alpha",103,true);eq(out.get("Pad3/Macro1"),1f);
        engine.release();for(float value:out.values())eq(value,0f);
        eq(engine.targetsForDevice("gamma"),new LinkedHashSet<>(Arrays.asList("Pad8/LRight","Pad8/LLeft")));
    }
    static void inputMetadata(){
        // Real DS2 metadata order: button indices followed by motors, both motors use 0.
        Map<Integer,String> names=new HashMap<>();
        String[] directions={"Up","Right","Down","Left"};
        for(int i=0;i<4;i++)ControllerBindings.addPadInput(names,i,directions[i],1);
        ControllerBindings.addPadInput(names,18,"LUp",3);
        ControllerBindings.addPadInput(names,0,"LargeMotor",4);
        ControllerBindings.addPadInput(names,0,"SmallMotor",4);
        ControllerBindings.addPadInput(names,0,"Device",7);
        eq(names.get(0),"Up");eq(names.get(18),"LUp");eq(names.size(),5);
        Map<Integer,Float> nativeInputs=new HashMap<>();
        PadMixer mixer=new PadMixer((pad,name,value)->nativeInputs.put(PadRoutes.key("DualShock2",name),value));
        for(int i=0;i<4;i++)mixer.set(0,names.get(i),"touch",i==0?1:0);
        eq(nativeInputs.get(19),1f); // UP reaches the donor's actual keycode, despite motor metadata.
        for(int i=0;i<4;i++)mixer.set(0,names.get(i),"touch",i==1?1:0);
        eq(nativeInputs.get(19),0f);eq(nativeInputs.get(22),1f);
        mixer.set(0,names.get(0),"physical",1);
        mixer.set(0,names.get(0),"touch",1);mixer.set(0,names.get(0),"touch",0);
        eq(nativeInputs.get(19),1f);mixer.release();eq(nativeInputs.get(19),0f);
        // Ignore motors even if they appear before the input entry; never expose them as inputs.
        names.clear();ControllerBindings.addPadInput(names,0,"SmallMotor",4);eq(names.isEmpty(),true);
        ControllerBindings.addPadInput(names,0,"Up",1);eq(names.get(0),"Up");
    }
    static void mixingAndMacros(){
        Map<String,Float> output=new HashMap<>();PadMixer mix=new PadMixer((pad,name,value)->output.put(pad+"/"+name,value));
        mix.set(0,"Cross","touch",1);mix.set(0,"Cross","physical",1);mix.set(0,"Cross","touch",0);eq(output.get("0/Cross"),1f);
        mix.pressure(0,.4f);near(output.get("0/Cross"),.4f);mix.pressure(0,1);eq(output.get("0/Cross"),1f);
        mix.set(1,"LRight","physical",.6f);near(output.get("1/LRight"),.6f);mix.release();eq(output.get("0/Cross"),0f);eq(output.get("1/LRight"),0f);
        MotorMixer motors=new MotorMixer();eq(motors.set("dev/Vibrator0","pad1/large",200),200);eq(motors.set("dev/Vibrator0","pad2/small",90),200);eq(motors.set("dev/Vibrator0","pad1/large",0),90);eq(motors.set("dev/Vibrator0","pad2/small",0),0);
        Map<String,Object> config=new HashMap<>();config.put("Pad1/Macro1Binds","Cross & Circle");config.put("Pad1/Macro1Frequency",2);config.put("Pad2/Macro2Binds","Square");
        MacroController macros=new MacroController(config,(pad,index,name,value)->mix.set(pad,name,"macro"+index,value));
        macros.set(0,0,true,101);eq(output.get("0/Cross"),1f);eq(output.get("0/Circle"),1f);
        macros.frame(102);eq(output.get("0/Cross"),1f);macros.frame(103);eq(output.get("0/Cross"),0f);macros.frame(105);eq(output.get("0/Cross"),1f);
        mix.set(0,"Cross","physical",1);macros.set(0,0,false,106);eq(output.get("0/Circle"),0f);eq(output.get("0/Cross"),1f);
        macros.set(1,1,true,1000);macros.frame(9999);eq(output.get("1/Square"),1f);macros.release();eq(output.get("1/Square"),0f);mix.release();
        eq(PadRoutes.key("DualShock2","Cross"),96);eq(PadRoutes.key("Guitar","Green"),100);eq(PadRoutes.key("None","Cross"),null);
    }
    public static void main(String[] args)throws Exception{layers();bindings();inputMetadata();mixingAndMacros();System.out.println("PASS: full controller layer/default reset, native-profile isolation/backups, skin/profile scope, repeated bindings/chords, eight-player remapping, shared input/motor arbitration, pressure and frame-counted macros.");}
}
