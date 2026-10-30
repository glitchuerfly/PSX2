// SPDX-License-Identifier: GPL-3.0-or-later
import org.local.aetherarms.*;

public final class TouchChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void near(float value,float expected){check(Math.abs(value-expected)<.0001f,value+" != "+expected);}
    public static void main(String[] args){
        // 50 dp face buttons regain 10 dp expansion per side at any screen density.
        near(TouchFeedback.pressScale(50,50,1),1.4f);
        near(TouchFeedback.pressScale(150,150,3),1.4f);
        near(TouchFeedback.pressScale(100,100,1),1.2f);
        float shoulder=TouchFeedback.pressScale(70,42,1);
        near(70*shoulder,90);near((70*shoulder)/(42*shoulder),70f/42);
        near(TouchFeedback.pressScale(0,0,1),1);
        check(TouchFeedback.targetContains(50,50,1,-8,25),"Face button near miss not captured");
        check(TouchFeedback.targetContains(50,50,1,58,25),"Face button right enlargement missing");
        check(!TouchFeedback.targetContains(50,50,1,-11,25),"Face target exceeds feedback");
        check(!TouchFeedback.targetContains(50,50,1,61,25),"Face target exceeds feedback on right");
        check(TouchFeedback.targetContains(150,150,3,-24,75),"Density-scaled target rejected");
        check(!TouchFeedback.targetContains(150,150,3,-33,75),"Density-scaled target too wide");
        check(TouchFeedback.targetContains(70,42,1,35,-5),"Shoulder target height not expanded");
        check(!TouchFeedback.targetContains(70,42,1,35,-7),"Shoulder target distorts aspect");
        check(!TouchFeedback.targetContains(50,50,1,Float.NaN,25),"Invalid target coordinate accepted");
        near(TouchFeedback.toArtwork(25,50,1.4f),25);
        float tip=TouchFeedback.toArtwork(55,50,1.4f);
        check(DpadGeometry.defaultHit(0,50,50,25,tip),"Enlarged D-pad tip target rejected");
        check(!DpadGeometry.defaultHit(0,50,50,25,TouchFeedback.toArtwork(65,50,1.4f)),"D-pad target exceeds pressed tip");
        AnalogGesture left=new AnalogGesture(),right=new AnalogGesture();
        // A 110 px base has a 66 px center knob. Outer ring, corners and slide-in cannot capture it.
        check(!left.start(0,0,0,45,0,110,110),"Outer ring DOWN activated stick");
        check(!left.start(0,0,0,30,30,110,110),"Corner outside knob activated stick");
        check(!left.start(2,0,0,0,0,110,110),"MOVE from outside captured stick");
        check(!left.start(5,1,0,0,0,110,110),"Unchanged finger captured on another finger DOWN");
        check(!left.start(1,0,0,0,0,110,110),"UP activated stick");
        check(!left.start(3,0,0,0,0,110,110),"CANCEL activated stick");
        check(!left.start(6,0,0,0,0,110,110),"POINTER_UP activated stick");
        check(left.start(0,0,0,12,-9,110,110),"Center knob DOWN rejected");
        near(left.dragX(12),0);near(left.dragY(-9),0); // no initial jump even slightly off-center
        near(left.dragX(32),20);near(left.dragY(-24),-15);
        near(left.dragX(200),188); // owner can continue outside base; legacy core clamps output
        check(right.start(5,1,1,-10,5,110,110),"Second stick/finger rejected");
        near(right.dragX(-30),-20);near(right.dragY(25),20);
        near(left.dragX(32),20); // right-hand gesture never changes left-hand anchor
        check(left.start(0,0,0,-4,2,110,110),"Next gesture rejected");
        near(left.dragX(-4),0);near(left.dragY(2),0); // fresh neutral after prior release/cancel
        check(!left.start(0,0,0,Float.NaN,0,110,110),"Invalid coordinates accepted");
        check(!left.start(0,0,0,0,0,0,0),"Zero-size control accepted");
        check(AnalogGesture.isDynamic(null),"Unset mode must use Dynamic");
        check(AnalogGesture.isDynamic("dynamic"),"Dynamic mode not recognized");
        check(AnalogGesture.isDynamic("invalid"),"Invalid mode must fall back to Dynamic");
        check(!AnalogGesture.isDynamic("static"),"Static mode not recognized");
        AnalogGesture floating=new AnalogGesture();
        check(floating.start(0,0,0,45,-5,110,110,true),"Dynamic DOWN on base ring rejected");
        near(floating.dragX(45),0);near(floating.dragY(-5),0); // Initial neutral; no output teleport.
        near(floating.offsetX(),45);near(floating.offsetY(),-5); // Base follows DOWN position.
        near(floating.dragX(65),20);near(floating.dragY(10),15);
        near(floating.offsetX(),45); // Base stays anchored during drag, not chasing every MOVE.
        check(!floating.start(2,0,0,0,0,110,110,true),"Dynamic MOVE captured idle/changed anchor");
        check(!floating.start(5,1,0,0,0,110,110,true),"Unchanged finger captured on another DOWN");
        near(floating.offsetX(),45);near(floating.dragX(65),20);
        check(!floating.start(0,0,0,50,50,110,110,true),"Dynamic corner beyond circular area accepted");
        check(!floating.start(0,0,0,56,0,110,110,true),"Dynamic outside base accepted");
        floating.release();near(floating.offsetX(),0);near(floating.offsetY(),0);
        near(floating.dragX(0),0);near(floating.dragY(0),0); // Release returns to saved position, no stale anchor.
        check(floating.start(5,1,1,-40,0,110,110,true),"Second-finger Dynamic DOWN rejected");
        near(floating.offsetX(),-40);near(left.offsetX(),0);near(right.offsetX(),0); // Independent views/static rendering.
        floating.release();
        check(floating.start(0,0,0,8,4,110,110,false),"Switch back to Static rejected");
        near(floating.offsetX(),0);near(floating.offsetY(),0);near(floating.dragX(8),0);
        check(!floating.start(0,0,0,45,0,110,110,false),"Static mode lost center-only capture");
        check(!floating.start(0,0,0,Float.POSITIVE_INFINITY,0,110,110,true),"Dynamic infinite input accepted");
        check(!floating.start(0,0,0,0,0,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,true),"Dynamic infinite bounds accepted");
        floating.release();floating.release();near(floating.offsetX(),0);
        System.out.println("PASS: original-size proportional press feedback and expanded targets, center-only DOWN, no slide-in/other-pointer capture, neutral anchor, continued drag, independent sticks and Dynamic/Static base anchors/release.");
    }
}
