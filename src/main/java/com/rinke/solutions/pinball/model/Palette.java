package com.rinke.solutions.pinball.model;

import org.eclipse.swt.graphics.RGB;

public class Palette {
    public String name;
    public int numberOfColors;
    public int index;
    public RGB[] colors;

    public Palette(RGB[] colors, int index, String name) {
        super();
        this.numberOfColors = colors.length;
        this.index = index;
        this.colors = colors;
        this.name = name;
    }
    
}
