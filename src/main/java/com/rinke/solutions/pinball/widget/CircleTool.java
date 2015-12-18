package com.rinke.solutions.pinball.widget;

public class CircleTool extends DrawTool {

	public CircleTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton >0 ) {
			dmd.copyLastBuffer();
			int r = Math.abs(x1-x)*Math.abs(x1-x) + Math.abs(y1-y)*Math.abs(y1-y);
			circle(x1,y1,(int) Math.sqrt(r),actualColor);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.addUndoBuffer();
		circle(x1,y1,1, actualColor);
		return true;
	}

	public void circle(int x,int y,int radius,int color) {
	    int discriminant = (5 - radius<<2)>>2 ;
	    int i = 0, j = radius ;
	    while (i<=j) {
	        putpixel(x+i,y-j,color) ;
	        putpixel(x+j,y-i,color) ;
	        putpixel(x+i,y+j,color) ;
	        putpixel(x+j,y+i,color) ;
	        putpixel(x-i,y-j,color) ;
	        putpixel(x-j,y-i,color) ;
	        putpixel(x-i,y+j,color) ;
	        putpixel(x-j,y+i,color) ;
	        i++ ;
	        if (discriminant<0) {                
	            discriminant += (i<<1) + 1 ;
	        } else {
	            j-- ;
	            discriminant += (1 + i - j)<<1 ;
	        }
	    }
	}

	private void putpixel(int x, int y, int color) {
		dmd.setPixel(x, y, color);
	}
	
		
}
