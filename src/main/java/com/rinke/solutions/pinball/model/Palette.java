package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.graphics.RGB;

public class Palette implements Model {
    public String name;
    public int numberOfColors;
    public int index;
    public RGB[] colors;
    public PaletteType type;

    public Palette(RGB[] colors, int index, String name) {
    	this(colors, index, name, PaletteType.NORMAL);
    }
    
    public Palette(RGB[] colors, int index, String name, PaletteType type) {
        this(colors);
        this.index = index;
        this.name = name;
        this.type = type;
    }

    public Palette(RGB[] colors) {
        this.colors = new RGB[colors.length];
        this.numberOfColors = colors.length;
        this.colors = Arrays.copyOf(colors, colors.length);
    }

    public void writeTo(DataOutputStream os) throws IOException {
		os.writeShort(index);
		os.writeShort(numberOfColors);
		os.writeByte(type.ordinal());
		for (int i = 0; i < colors.length; i++) {
			os.writeByte(colors[i].red);
			os.writeByte(colors[i].green);
			os.writeByte(colors[i].blue);
		}
	}

	@Override
	public String toString() {
		return "Palette [name=" + name + ", numberOfColors=" + numberOfColors
				+ ", index=" + index + ", colors=" + Arrays.toString(colors)
				+ ", type=" + type + "]";
	}
    
}
