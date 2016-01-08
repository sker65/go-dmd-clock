package com.rinke.solutions.pinball.model;

public enum PlaneNumber {
	two(2), four(4), TW(12);
	
	PlaneNumber(int i) {
		this.numberOfPlanes = i;
	}
	
	public int numberOfPlanes;

	public static PlaneNumber valueOf(int i) {
		PlaneNumber[] values = values();
		for (int j = 0; j < values.length; j++) {
			if( values[j].numberOfPlanes == i ) return values[j];
		}
		return null;
	}
}
