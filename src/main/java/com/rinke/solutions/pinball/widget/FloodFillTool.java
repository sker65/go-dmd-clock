package com.rinke.solutions.pinball.widget;

public class FloodFillTool extends DrawTool {
	
	public FloodFillTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseUp(int x, int y) {
		int oldColor = dmd.getPixel(x, y);
		fill( oldColor, x, y);
		return true;
	}

	private void fill(int oldColor, int x, int y) {
		if( dmd.getPixel(x,y) == oldColor ) {
			dmd.setPixel(x, y, this.actualColor);
			if( x>0 ) fill( oldColor, x-1, y);
			if( x<dmd.getWidth()-1) fill(oldColor, x+1,y);
			if( y> 0 ) fill(oldColor,x,y-1);
			if( y< dmd.getHeight()-1) fill(oldColor,x,y+1);

		}
	}

}
