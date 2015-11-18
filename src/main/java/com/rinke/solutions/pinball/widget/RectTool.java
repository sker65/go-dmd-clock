package com.rinke.solutions.pinball.widget;

public class RectTool extends DrawTool {

	@Override
	public void mouseMove(int x, int y) {
		if( pressedButton >0 ) {
			dmd.copyTemp();
			drawRect(x1,y1,x,y);
		}
	}

	@Override
	public void mouseUp(int x, int y) {

	}

	@Override
	public void mouseDown(int x, int y) {
		dmd.createTemp();
		drawRect(x1,y1,x,y);
	}

	private void drawRect(int x1, int y1, int x2, int y2) {
		for( int x=x1; x < x2; x++) {
			for(int y = y1; y<y2; y++) dmd.setPixel(x, y, actualColor);
		}
	}

}
