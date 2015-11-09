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
    public boolean isDefault;

    public Palette(RGB[] colors, int index, String name, boolean isDefault) {
        super();
        this.numberOfColors = colors.length;
        this.index = index;
        this.colors = Arrays.copyOf(colors, colors.length);
        this.name = name;
        this.isDefault = isDefault;
    }

    public void writeTo(DataOutputStream os) throws IOException {
		os.writeShort(index);
		os.writeShort(numberOfColors);
		os.writeBoolean(isDefault);
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
				+ ", isDefault=" + isDefault + "]";
	}
    
    
}
