package com.rinke.solutions.pinball.animation;

import java.util.Arrays;


public class Plane {
    public byte marker;
    public byte[] plane;
    public Plane(byte marker, byte[] plane) {
        super();
        this.marker = marker;
        this.plane = Arrays.copyOf(plane, plane.length);
    }
    @Override
    public String toString() {
        return "Plane [marker=" + marker + ", plane=byte[" + plane.length + "]]";
    }
}
