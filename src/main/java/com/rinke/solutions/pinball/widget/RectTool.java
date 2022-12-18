package com.rinke.solutions.pinball.widget;

import com.rinke.solutions.pinball.Constants;

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
	
	private enum dir { topdown , bottomup, rightleft, leftright };

	private void drawRect(int x1, int y1, int x2, int y2) {
		if (toolSize == 1) {
			int tmp;
			if( x2 < x1 ) { tmp = x2; x2=x1; x1=tmp; }
			if( y2 < y1 ) { tmp = y2; y2=y1; y1=tmp; }
			for( int x=x1; x <= x2; x++) {
				for(int y = y1; y<=y2; y++) dmd.setPixel(x, y, actualColor);
			}
		} else { // gradient draw
			int tmp;
			if( x2 < x1 ) { if (y2 < y1) direction = dir.bottomup; else direction = dir.rightleft; } else { if (y2 < y1) direction = dir.leftright; else direction = dir.topdown;}
			if( x2 < x1 ) { tmp = x2; x2=x1; x1=tmp; }
			if( y2 < y1 ) { tmp = y2; y2=y1; y1=tmp; }
			int width = x2-x1;
			int	height = y2-y1;
			
			dir direction;
			int fragmentSize;
			int segmentColor;

			int drawMask = dmd.getDrawMask();
			int colorOffset = 1; // replace mode
			if(drawMask == 0xF8) colorOffset = 4; // 4 planes colmask drawing
			if(drawMask == 0xE0) colorOffset = 16; // 16 planes colmask drawing

			if (direction == dir.topdown || direction == dir.bottomup) {
				fragmentSize = height / toolSize;
				for( int x=x1; x <= x2; x++) {
					for (int t = 0; t < toolSize-1; t++) {
						if (direction == dir.topdown)
							segmentColor = actualColor - (t * colorOffset);
						else 
							segmentColor = actualColor - (toolSize-1) + (t * colorOffset);
						if (segmentColor < 0) segmentColor = 0;
						for(int y = y1 + (t*fragmentSize); y < (y1 + ((t+1) * fragmentSize)); y++) dmd.setPixel(x, y, (segmentColor));
					}
					if (direction == dir.topdown)
						for(int y = y1 + ((toolSize-1)*fragmentSize); y < (y1 + height); y++) dmd.setPixel(x, y, (actualColor - ((toolSize-1) * colorOffset )));
					else
						for(int y = y1 + ((toolSize-1)*fragmentSize); y < (y1 + height); y++) dmd.setPixel(x, y, actualColor);
				}
			} else {
				fragmentSize = width / toolSize;
				for( int y=y1; y <= y2; y++) {
					for (int t = 0; t < toolSize; t++) {
						if (direction == dir.leftright)
							segmentColor = actualColor - (t * colorOffset);
						else 
							segmentColor = actualColor - (toolSize-1) + (t * colorOffset);
						if (segmentColor < 0) segmentColor = 0;
						for(int x = x1 + (t*fragmentSize); x < (x1 + ((t+1) * fragmentSize)); x++) dmd.setPixel(x, y, (segmentColor));
					}
					if (direction == dir.leftright)
						for(int x = x1 + ((toolSize-1)*fragmentSize); x < (x1 + width); x++) dmd.setPixel(x, y, (actualColor - ((toolSize-1) * colorOffset)));
					else
						for(int x = x1 + ((toolSize-1)*fragmentSize); x < (x1 + width); x++) dmd.setPixel(x, y, actualColor);
				}
			}
		}
	}

}
