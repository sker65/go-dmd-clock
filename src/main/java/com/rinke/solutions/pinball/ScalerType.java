package com.rinke.solutions.pinball;

public enum ScalerType {
	
	NearPixel("NearPixel"), EPX("Epx");

	public final String label;

	private ScalerType(String label) {
		this.label = label;
	}

	public static ScalerType fromOrdinal(int n) {
		for( ScalerType s : values()) {
			if( s.ordinal() == n ) return s;
		}
		return null;
	}

}
