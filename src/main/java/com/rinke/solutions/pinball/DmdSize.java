package com.rinke.solutions.pinball;

public enum DmdSize {
	Size128x32("128x32",128,32), Size192x64("192x64",192,64), Size128x16("128x16",128,16);

	public final String label;
	public final int width;
	public final int height;
	public final int planeSize;

	private DmdSize(String label, int width, int height) {
		this.label = label;
		this.width = width;
		this.height = height;
		planeSize = (width/8)*height;
	}

	public static DmdSize fromOrdinal(int n) {
		for( DmdSize s : values()) {
			if( s.ordinal() == n ) return s;
		}
		return null;
	}

}
