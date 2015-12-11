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
		setColors(colors);
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
				+ ", index=" + index 
				+ ", type=" + type + ", colors=" + Arrays.toString(colors) + "]";
	}

	public static Palette getDefaultPalette() {
		return new Palette(defaultColors());
	}

	public static RGB[] defaultColors() {
		RGB[] rgb = new RGB[16];
		rgb[0] = new RGB(0x00, 0x00, 0x00); // black
		rgb[1] = new RGB(0x7F, 0x00, 0x00); // dark red
		rgb[2] = new RGB(0xFF, 0x00, 0x00); // red
		rgb[3] = new RGB(0xFF, 0x00, 0xFF); // pink
		rgb[4] = new RGB(0x00, 0x7F, 0x7F); // teal
		rgb[5] = new RGB(0x00, 0x7F, 0x00); // green
		rgb[6] = new RGB(0x00, 0xFF, 0x00); // bright green
		rgb[7] = new RGB(0x00, 0xFF, 0xFF); // turquoise
		rgb[8] = new RGB(0x00, 0x00, 0x7F); // dark blue
		rgb[9] = new RGB(0x7F, 0x00, 0x7F); // violet
		rgb[10] = new RGB(0x00, 0x00, 0xFF); // blue
		rgb[11] = new RGB(0x3F, 0x3F, 0x3F); // gray 25%
		rgb[12] = new RGB(0x7F, 0x7F, 0x7F); // gray 50%
		rgb[13] = new RGB(0x7F, 0x7F, 0x00); // dark yellow
		rgb[14] = new RGB(0xFF, 0xFF, 0x00); // yellow
		rgb[15] = new RGB(0xFF, 0xFF, 0xFF); // white
		return rgb;
	}

	public void setColors(RGB[] colors) {
		this.colors = new RGB[colors.length];
		this.numberOfColors = colors.length;
		this.colors = Arrays.copyOf(colors, colors.length);
	}

}
