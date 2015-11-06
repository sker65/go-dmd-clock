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

    public Palette(RGB[] colors, int index, String name) {
        super();
        this.numberOfColors = colors.length;
        this.index = index;
        this.colors = colors;
        this.name = name;
    }

	@Override
	public String toString() {
		return "Palette [name=" + name + ", numberOfColors=" + numberOfColors
				+ ", index=" + index + ", colors=" + Arrays.toString(colors)
				+ "]";
	}

	public void writeTo(DataOutputStream os) throws IOException {
		os.writeUTF(name);
		os.writeShort(numberOfColors);
		os.writeShort(index);
		for (int i = 0; i < colors.length; i++) {
			os.writeByte(colors[i].red);
			os.writeByte(colors[i].green);
			os.writeByte(colors[i].blue);
		}
	}
    
    
}
