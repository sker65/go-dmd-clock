package com.rinke.solutions.pinball.widget;

public class SetPixelTool extends DrawTool {

	public SetPixelTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton > 0 ) {
			int brushSize = 0;
			drawSquare(x,y,x+brushSize,y+brushSize);
			dmd.setPixel(x, y, actualColor);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		int brushSize = 0;
		dmd.addUndoBuffer();
		drawSquare(x,y,x+brushSize,y+brushSize);
		dmd.setPixel(x, y, actualColor);
		return true;
	}
	
	private void drawSquare(int x1, int y1, int x2, int y2) {
		int tmp;
		if( x2 < x1 ) { tmp = x2; x2=x1; x1=tmp; }
		if( y2 < y1 ) { tmp = y2; y2=y1; y1=tmp; }
		for( int x=x1; x <= x2; x++) {
			for(int y = y1; y<=y2; y++) dmd.setPixel(x, y, actualColor);
		}
	}
}
