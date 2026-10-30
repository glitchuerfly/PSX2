// SPDX-License-Identifier: GPL-3.0-or-later
package org.local.aetherarms;

/** Cached PNG silhouette: solid pixels and enclosed transparent interiors are tappable. */
public final class DpadMask {
    private final int width,height;
    private final byte[] state;
    public DpadMask(int width,int height,int[] argb){
        if(width<=0||height<=0||argb.length!=width*height)throw new IllegalArgumentException("Invalid mask dimensions");
        this.width=width;this.height=height;state=new byte[argb.length];
        for(int i=0;i<argb.length;i++)if((argb[i]>>>24)>=32)state[i]=1;
        int[] queue=new int[state.length];int end=0;
        for(int x=0;x<width;x++){end=enqueue(x,queue,end);end=enqueue((height-1)*width+x,queue,end);}
        for(int y=0;y<height;y++){end=enqueue(y*width,queue,end);end=enqueue(y*width+width-1,queue,end);}
        for(int head=0;head<end;head++){
            int p=queue[head],x=p%width,y=p/width;
            if(x>0)end=enqueue(p-1,queue,end);if(x+1<width)end=enqueue(p+1,queue,end);
            if(y>0)end=enqueue(p-width,queue,end);if(y+1<height)end=enqueue(p+width,queue,end);
        }
    }
    private int enqueue(int p,int[] queue,int end){if(state[p]==0){state[p]=2;queue[end++]=p;}return end;}
    public boolean hit(float cellWidth,float cellHeight,float x,float y){
        float scale=SkinPolicy.fitScale(cellWidth,cellHeight,width,height);if(scale<=0)return false;
        float px=(x-(cellWidth-width*scale)*.5f)/scale,py=(y-(cellHeight-height*scale)*.5f)/scale;
        if(!Float.isFinite(px)||!Float.isFinite(py)||px<0||py<0||px>=width||py>=height)return false;
        return state[(int)py*width+(int)px]!=2;
    }
}
