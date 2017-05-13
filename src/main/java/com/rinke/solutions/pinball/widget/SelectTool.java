package com.rinke.solutions.pinball.widget;

import com.rinke.solutions.pinball.view.model.Rect;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectTool extends DrawTool {

	private DMDWidget dmdWidget;
	private int x2, y2, x3, y3;
	private int move;

	public SelectTool(int actualColor, DMDWidget dmdWidget) {
		super(actualColor);
		this.dmdWidget = dmdWidget;
	}

	@Override
	public boolean mouseMove(int x, int y) {
		if( pressedButton >0 ) {
			dmdWidget.setCapture(true);
			if( move == 1) {
				int xo = x-x1;
				int yo = y-y1;
				check();
				drawRect(x2+xo,y2+yo,x3+xo,y3+yo);
			} else if( move == 2 ) {
				x2=x;
				check();
				drawRect(x2,y2,x3,y3);
			} else if( move == 3 ) {
				x3 = x;
				check();
				drawRect(x2,y2,x3,y3);
			} else if( move == 4 ) {
				y2=y;	
				check();
				drawRect(x2,y2,x3,y3);
			} else if( move == 5 ) {
				y3=y;
				check();
				drawRect(x2,y2,x3,y3);
			} else if( move == 0 ){
				x3 = x;
				y3 = y;
				check();
				drawRect(x2,y2,x3,y3);
			}
			return true;
		}
		return false;
	}

	private void check() {
		if( x2<0 ) x2=0;
		if( x3 > dmd.getWidth() ) x3 = dmd.getWidth();
		if( x3<x2 ) x3=x2;
		if( y2<0 ) y2=0;
		if( y3 > dmd.getHeight()) y3 = dmd.getHeight(); 
		if( y3<y2) y3=y2;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		move = -1;
		log.debug("DOWN x:"+x+" y:"+y+" x2:"+x2+" y2:"+y2+" x3:"+x3+" y3:"+y3);
		// move edges
		if( dmdWidget.isMouseOnAreaMark()) {
			if( x == x2 ) move = 2;
			else if( x == x3-1 ) move = 3;
			else if( y == y2 ) move = 4;
			else if( y == y3-1 ) move = 5;
		} else if(x>x2 && x < x3 && y>y2 && y<y3) { 
			move = 1;
		} else {
			move = 0;
			x2 = x1; y2 = y1;
		}
		//log.debug("move: "+move);
		
		return true;
	}

	private void drawRect(int x1, int y1, int x2, int y2) {
		if( x2>=x1 && y2>=y1 )
			dmdWidget.setSelection(x1, y1, x2-x1, y2-y1);
	}

	@Override
	public boolean mouseUp(int x, int y) {
		log.debug("move: "+move);
		dmdWidget.setCapture(false);
		if( move == 1 ) {
			int xo = x-x1;
			int yo = y-y1;
			x2 += xo; x3 += xo;
			y2 += yo; y3 += yo;
			check();
			drawRect(x2,y2,x3,y3);
		}
		log.debug("UP  x:"+x+" y:"+y+" x2:"+x2+" y2:"+y2+" x3:"+x3+" y3:"+y3+" w:"+(x3-x2)+" h:"+(y3-y2));
		return true;
	}
	
	public Rect getSelection() {
		return new Rect(x2, y2, x3, y3);
	}

	public void setSelection(int x, int y, int width, int height) {
		log.info("set selection: x={}, y={}, w={}, h={}", x,y,width,height);
		x2 = x; x3 = x2+width;
		y2 = y; y3 = y2+height;
		check();
		drawRect(x2,y2,x3,y3);
	}

	
}
