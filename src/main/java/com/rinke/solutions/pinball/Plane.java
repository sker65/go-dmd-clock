package com.rinke.solutions.pinball;


public class Plane {
    public byte marker;
    public byte[] plane;
    public Plane(byte marker, byte[] plane) {
        super();
        this.marker = marker;
        this.plane = plane;
    }
    @Override
    public String toString() {
        return "Plane [marker=" + marker + ", plane=byte[" + plane.length + "]]";
    }
}
