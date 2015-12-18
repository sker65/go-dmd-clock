package com.rinke.solutions.pinball.widget;

public class RectTool extends DrawTool {

	public RectTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton >0 ) {
			dmd.copyLastBuffer();
			drawRect(x1,y1,x,y);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.addUndoBuffer();
		drawRect(x1,y1,x,y);
		return true;
	}

	private void drawRect(int x1, int y1, int x2, int y2) {
		for( int x=x1; x < x2; x++) {
			for(int y = y1; y<y2; y++) dmd.setPixel(x, y, actualColor);
		}
	}

}
