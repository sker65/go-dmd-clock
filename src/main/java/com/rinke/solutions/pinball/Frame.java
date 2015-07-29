package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    
    public Frame(int delay) {
        this.delay = delay;
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
    
}