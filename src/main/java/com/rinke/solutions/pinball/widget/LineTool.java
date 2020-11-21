package com.rinke.solutions.pinball.widget;

public class LineTool extends DrawTool {

	public LineTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton >0 ) {
			dmd.copyLastBuffer();
			drawLine(x1,y1,x,y);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.addUndoBuffer();
		drawLine(x1,y1,x,y);
		return true;
	}


	public void drawLine(int x,int y,int x2, int y2) {
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
	    	if (toolSize==1) {
	    		dmd.setPixel(x, y, actualColor);
	    	} else {
		    	int j = i & (1 << (toolSize-2));
		    	if (j == (1 << (toolSize-2))) {
		    		dmd.setPixel(x, y, actualColor);
		    	}
	    	}
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
	
}
