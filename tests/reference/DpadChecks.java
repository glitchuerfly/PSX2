// SPDX-License-Identifier: GPL-3.0-or-later
import org.local.aetherarms.*;

public final class DpadChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void equal(int value,int expected){check(value==expected,value+" != "+expected);}
    public static void main(String[] args){
        for(int d=0;d<4;d++){
            check(DpadGeometry.defaultHit(d,50,50,25,25),"Arrow interior rejected: "+d);
            check(!DpadGeometry.defaultHit(d,50,50,1,1),"Transparent corner hit: "+d);
        }
        check(!DpadGeometry.defaultHit(0,50,50,12,43),"Empty space beside top arrow tip hit");
        check(DpadGeometry.defaultHit(0,50,50,25,43),"Top arrow tip interior rejected");
        check(!DpadGeometry.defaultHit(0,100,50,12,15),"Aspect-fit letterbox activated arrow");
        check(DpadGeometry.defaultHit(0,100,50,50,15),"Aspect-fit arrow rejected");
        check(DpadGeometry.freshDown(0,0,0),"DOWN rejected");
        check(DpadGeometry.freshDown(5,1,1),"Action pointer DOWN rejected");
        check(!DpadGeometry.freshDown(5,1,0),"Other pointer activated idle D-pad");
        for(int action:new int[]{1,2,3,6})check(!DpadGeometry.freshDown(action,0,0),"Non-DOWN captured D-pad");
        // Existing owner can slide through the inner diagonal zones; outer blank corners are neutral.
        equal(DpadGeometry.heldMask(150,150,45,45,0),9);
        equal(DpadGeometry.heldMask(150,150,105,45,0),3);
        equal(DpadGeometry.heldMask(150,150,105,105,0),6);
        equal(DpadGeometry.heldMask(150,150,45,105,0),12);
        for(int[] p:new int[][]{{5,5},{145,5},{145,145},{5,145},{75,75},{50,3},{-20,75},{175,75},{75,-20},{75,175}})
            equal(DpadGeometry.heldMask(150,150,p[0],p[1],0),0);
        equal(DpadGeometry.heldMask(150,150,75,20,1),1);
        equal(DpadGeometry.heldMask(150,150,125,75,2),2);
        equal(DpadGeometry.heldMask(150,150,75,125,4),4);
        equal(DpadGeometry.heldMask(150,150,20,75,8),8);
        equal(DpadGeometry.heldMask(2,2,0,0,1),0);
        equal(DpadGeometry.heldMask(150,150,75,-2,1),1); // visible pressed art may extend past view
        equal(DpadGeometry.heldMask(150,150,75,-20,0),0);
        // Hollow outlined PNG: fill the enclosed center, leaving outside margins inactive.
        int[] ring=new int[25];
        for(int y=1;y<=3;y++)for(int x=1;x<=3;x++)if(x==1||x==3||y==1||y==3)ring[y*5+x]=0xffffffff;
        DpadMask mask=new DpadMask(5,5,ring);
        check(mask.hit(50,50,25,25),"Hollow button center not tappable");
        check(mask.hit(50,50,15,25),"PNG outline not tappable");
        check(!mask.hit(50,50,5,25),"Transparent PNG margin activated button");
        check(!mask.hit(50,50,-1,25),"Outside PNG activated button");
        check(!mask.hit(50,50,Float.NaN,25),"Invalid PNG coordinate accepted");
        check(!new DpadMask(5,5,new int[25]).hit(50,50,25,25),"Invisible image activated button");
        int[] rectangle=new int[15];rectangle[7]=0xffffffff;
        DpadMask wide=new DpadMask(5,3,rectangle);
        check(wide.hit(50,50,25,25),"Wide PNG center rejected");
        check(!wide.hit(50,50,25,2),"Wide PNG letterbox activated button");
        System.out.println("PASS: D-pad vector/PNG silhouettes, hollow interiors, transparent margins, DOWN ownership, neutral outside/corners, inner held diagonals and aspect-fit geometry.");
    }
}
