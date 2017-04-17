package com.rinke.solutions.pinball;

public enum DmdSize {
	Size128x32("128x32"), Size192x64("192x64"), Size128x16("128x16");

	public final String label;
	
	private DmdSize(String label) {
		this.label = label;
	}

	public static DmdSize fromOrdinal(int n) {
		for( DmdSize s : values()) {
			if( s.ordinal() == n ) return s;
		}
		return null;
	}

}
