package com.rinke.solutions.pinball.widget;

public class CircleTool extends DrawTool {

	private boolean filled;

	public CircleTool(int actualColor, boolean filled) {
		super(actualColor);
		this.filled = filled;
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton >0 ) {
			dmd.copyLastBuffer();
			int r = Math.abs(x1-x)*Math.abs(x1-x) + Math.abs(y1-y)*Math.abs(y1-y);
			circle1(x1,y1,(int) Math.sqrt(r),actualColor);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.addUndoBuffer();
		circle1(x1,y1,1, actualColor);
		return true;
	}

	public void filledCircle(int x,int y,int radius,int color) {
	    int discriminant = (5 - radius<<2)>>2 ;
	    int i = 0, j = radius ;
	    while (i<=j) {
	        line(x+i,y-j,x-i,y-j);
	        line(x+j,y-i,x-j,y-i);
	        line(x+i,y+j,x-i,y+j);
	        line(x-j,y+i,x+j,y+i);
	        i++ ;
	        if (discriminant<0) {                
	            discriminant += (i<<1) + 1 ;
	        } else {
	            j-- ;
	            discriminant += (1 + i - j)<<1 ;
	        }
	    }
	}

	public void line(int x,int y,int x2, int y2) {
	    int w = x2 - x ;
	    int h = y2 - y ;
	    int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
	    if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
	    if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
	    if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
	    int longest = Math.abs(w) ;
	    int shortest = Math.abs(h) ;
	    if (!(longest>shortest)) {
	        longest = Math.abs(h) ;
	        shortest = Math.abs(w) ;
	        if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
	        dx2 = 0 ;            
	    }
	    int numerator = longest >> 1 ;
	    for (int i=0;i<=longest;i++) {
	    	dmd.setPixel(x, y, actualColor);
	        numerator += shortest ;
	        if (!(numerator<longest)) {
	            numerator -= longest ;
	            x += dx1 ;
	            y += dy1 ;
	        } else {
	            x += dx2 ;
	            y += dy2 ;
	        }
	    }
	}

	public void circle1(int x,int y,int radius,int color) {
		if( filled ) circle(x, y, radius, color);
		else filledCircle(x, y, radius, color);
	}

	public void circle(int x,int y,int radius,int color) {
	    int discriminant = (5 - radius<<2)>>2 ;
	    int i = 0, j = radius ;
	    while (i<=j) {
	        putpixel(x+i,y-j,color);
	        putpixel(x+j,y-i,color);
	        putpixel(x+i,y+j,color);
	        putpixel(x+j,y+i,color);
	        putpixel(x-i,y-j,color);
	        putpixel(x-j,y-i,color);
	        putpixel(x-i,y+j,color);
	        putpixel(x-j,y+i,color);
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
