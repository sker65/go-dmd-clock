package com.rinke.solutions.pinball;

public enum DeviceMode {

	PinMame_RGB, PinMame_Mono, WPC, Stern, Gottlieb, DataEast, WhiteStar, WPC95;

	public static DeviceMode forOrdinal(int i) {
		for( DeviceMode m : values()) {
			if( m.ordinal() == i ) return m;
		}
		return null;
	}
}