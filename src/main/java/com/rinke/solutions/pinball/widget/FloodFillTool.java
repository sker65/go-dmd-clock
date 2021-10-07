package com.rinke.solutions.pinball.widget;

public class FloodFillTool extends DrawTool {
	
	public FloodFillTool(int actualColor) {
		super(actualColor);
	}
	
	int depth = 0;
	int maxDepth= 0;

	@Override
	public boolean mouseUp(int x, int y) {
		if (dmd.getDrawMask() != 1) {
			int oldColor = dmd.getPixelWithoutMask(x, y);
			if( oldColor != actualColor ) {
				dmd.addUndoBuffer();
				depth = 0;
				maxDepth= 0;
				if (toolSize > 2) {
					replaceColor(oldColor);
				} else {
					fill( oldColor, x, y);
				}
				System.out.println(maxDepth);
				return true;
			}
		} else {
			int oldColor = dmd.getPixelWithoutMask(x, y);
			int oldMask = dmd.getMaskPixel(x, y);
			if( oldMask != actualColor ) {
				dmd.addUndoBuffer();
				depth = 0;
				fillMask( oldColor, oldMask, x, y);
				return true;
			}
		}
		return false;
	}

	private void replaceColor(int oldColor) {
		for( int x=0; x < dmd.getWidth(); x++) {
			for(int y = 0; y < dmd.getHeight(); y++) {
				if (dmd.getPixelWithoutMask(x, y) == oldColor) {
					dmd.setPixel(x, y, actualColor);
				}
			}
		}
	}
	
    private static final int[] dx = { -1,  0, 0, 1 };
    private static final int[] dy = {  0, -1, 1, 0 };

    private boolean isSafe(int x, int y) {
    	return x >= 0 && x < dmd.getWidth() && y>=0 && y < dmd.getHeight();
    }
    
	private void fill(int oldColor, int x, int y) {
		depth++;
		if( depth > maxDepth) maxDepth = depth;
		if( depth > dmd.getWidth()*dmd.getHeight() ) {
			throw new RuntimeException("fill to deep: "+depth);
		}
		if( dmd.getPixelWithoutMask(x,y) == oldColor ) {
			dmd.setPixel(x, y, this.actualColor);
			if( dmd.getPixelWithoutMask(x,y) != oldColor ) { // double check because of col masking, only decend if its really changing color
				for(int k = 0; k < dx.length; k++) {
					if( isSafe(x+dx[k], y+dy[k] ) ) fill(oldColor, x+dx[k], y+dy[k] );
				}
			}
		}
		depth--;
	}

	private void fillMask(int oldColor, int oldMask, int x, int y) {
		depth++;
		if( depth > dmd.getWidth()*dmd.getHeight() ) {
			throw new RuntimeException("fill to deep");
		}
		if( dmd.getPixelWithoutMask(x,y) == oldColor &&  dmd.getMaskPixel(x,y) == oldMask ) {
			dmd.setPixel(x, y, this.actualColor);
			if( dmd.getMaskPixel(x,y) != oldMask ) { // double check because of col masking, only decend if its really changing color
				for(int k = 0; k < dx.length; k++) {
					if( isSafe(x+dx[k], y+dy[k] ) ) fillMask( oldColor, oldMask, x+dx[k], y+dy[k] );
				}
			}
		}
		depth--;
	}

}
