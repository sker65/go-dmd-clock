package com.rinke.solutions.pinball.widget;

public class SetPixelTool extends DrawTool {

	@Override
	public void mouseMove(int x, int y) {
		if( pressedButton > 0 ) dmd.setPixel(x, y, actualColor);
	}

	@Override
	public void mouseUp(int x, int y) {
	}

	@Override
	public void mouseDown(int x, int y) {
		dmd.setPixel(x, y, actualColor);
	}
}
