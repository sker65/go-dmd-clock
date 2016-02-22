package com.rinke.solutions.pinball.widget;

import org.eclipse.swt.graphics.RGB;

public class SWTUtil {

	public static RGB toSwtRGB(com.rinke.solutions.pinball.model.RGB rgb) {
		return new RGB(rgb.red, rgb.green, rgb.blue);
	}

	public static com.rinke.solutions.pinball.model.RGB toModelRGB(RGB rgb) {
		return new com.rinke.solutions.pinball.model.RGB(rgb.red, rgb.green, rgb.blue);
	}


}
