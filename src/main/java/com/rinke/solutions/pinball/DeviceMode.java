package com.rinke.solutions.pinball;

public enum DeviceMode {

    PinMame, WPC, Stern, WhiteStar, Spike, DataEast, Gottlieb1, Gottlieb2, Gottlieb3, Capcom, AlvinG, Spooky, DE128x16, Inder, Sleic, HomePin;

	public static DeviceMode forOrdinal(int i) {
		for( DeviceMode m : values()) {
			if( m.ordinal() == i ) return m;
		}
		return null;
	}
}