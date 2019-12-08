package com.rinke.solutions.pinball.widget;

import com.rinke.solutions.pinball.Constants;

public class MagicTool extends DrawTool {

	public MagicTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton > 0 ) {
			dmd.setDrawMask(0b11111000);
			dmd.setPixel(x, y, actualColor);
			dmd.setDrawMask(Constants.DEFAULT_DRAW_MASK);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.setDrawMask(0b11111000);
		dmd.addUndoBuffer();
		dmd.setPixel(x, y, actualColor);
		dmd.setDrawMask(Constants.DEFAULT_DRAW_MASK);
		return true;
	}
}
