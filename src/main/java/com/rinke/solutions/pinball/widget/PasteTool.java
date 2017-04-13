package com.rinke.solutions.pinball.widget;

public class PasteTool extends DrawTool {

	public PasteTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton >=0 ) {
			dmd.copyLastBuffer();
			dmd.setPixel(x, y, 15);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseUp(int x, int y) {
		return super.mouseUp(x, y);
	}

	@Override
	public boolean mouseDown(int x, int y) {
		return super.mouseDown(x, y);
	}

}
