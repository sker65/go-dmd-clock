package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    
    public Frame(int delay,int w, int h) {
        this.delay = delay;
        width = w;
        height = h;
   }
    
    public Frame(int w, int h, byte[] plane1, byte[] plane2) {
        width = w;
        height = h;
        planes.add(new Plane((byte)0, plane1));
        planes.add(new Plane((byte)1, plane2));
    }
    
    public int width;
    public int height;
    public int delay;
    public List<Plane> planes = new ArrayList<>();
    
    @Override
    public String toString() {
        return "Frame [delay=" + delay + ", planes=" + planes + "]";
    }

    public void setPixel(int x, int y) {
        if( x>=0 && x< width && y>=0 && y<height) {
            int bytesPerRow = width / 8;
            //System.out.println("setPixel("+x+","+y+")");
            int offset = y*bytesPerRow + x / 8;
            planes.get(0).plane[offset] &= ~ (128>>x%8);
            planes.get(1).plane[offset] &= ~ (128>>x%8);
        }
    }
    
}