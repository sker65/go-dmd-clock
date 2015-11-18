package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

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

	public static Palette getDefaultPalette() {
		return new Palette(defaultColors());
	}
	
    private static RGB newRGB(int rgb) {
        return new RGB(rgb >> 16, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
	
    public static RGB[] defaultColors() {
    	RGB[] rgb = new RGB[16];
        rgb[0] = newRGB(0x00);
        rgb[1] = newRGB(0x6f0000);
        rgb[4] = newRGB(0xca0000);
        rgb[15] = newRGB(0xff0000);

        rgb[2] = newRGB(0x008000);
        rgb[3] = newRGB(0x808000);
        rgb[5] = newRGB(0x000080);
        rgb[6] = newRGB(0x800080);
        rgb[7] = newRGB(0xC0C0C0);
        rgb[8] = newRGB(0x808080);
        rgb[9] = newRGB(0x00FF00);
        rgb[10] = newRGB(0xFFFF00);
        rgb[11] = newRGB(0x0000FF);
        rgb[12] = newRGB(0xFF00FF);
        rgb[13] = newRGB(0x00FFFF);
        rgb[14] = newRGB(0xFFFFFF);
        return rgb;
    }

    
}
