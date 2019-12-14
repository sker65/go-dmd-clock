package com.rinke.solutions.pinball.widget;

public class FloodFillTool extends DrawTool {
	
	public FloodFillTool(int actualColor) {
		super(actualColor);
	}
	
	int depth = 0;

	@Override
	public boolean mouseUp(int x, int y) {
		int oldColor = dmd.getPixelWithoutMask(x, y);
		if( oldColor != actualColor ) {
			dmd.addUndoBuffer();
			depth = 0;
			if (toolSize > 1) {
				replaceColor(oldColor);
			} else {
				fill( oldColor, x, y);
			}
			return true;
		}
		return false;
	}

	private void replaceColor(int oldColor) {
		for( int x=0; x <= dmd.getWidth(); x++) {
			for(int y = 0; y<=dmd.getHeight(); y++) {
				if (dmd.getPixelWithoutMask(x, y) == oldColor) {
					dmd.setPixel(x, y, actualColor);
				}
			}
		}
	}

	private void fill(int oldColor, int x, int y) {
		depth++;
		if( depth > 10000 ) {
			throw new RuntimeException("fill to deep");
		}
		if( dmd.getPixelWithoutMask(x,y) == oldColor ) {
			dmd.setPixel(x, y, this.actualColor);
			if( dmd.getPixelWithoutMask(x,y) != oldColor ) { // double check because of col masking, only decend if its really changing color
				if( x>0 ) fill( oldColor, x-1, y);
				if( x<dmd.getWidth()-1) fill(oldColor, x+1,y);
				if( y> 0 ) fill(oldColor,x,y-1);
				if( y< dmd.getHeight()-1) fill(oldColor,x,y+1);
			}
		}
		depth--;
	}

}
