package com.rinke.solutions.pinball.widget;

import java.util.ArrayList;

import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LassoSelectTool extends DrawTool {

	private DMDWidget dmdWidget;
	private static class Point {
		public Point(int x2, int y2) {
			this.x=x2; this.y=y2;
		}
		public static Point from(int x, int y) {
			return new Point(x,y);
		}
		public int x;
		public int y;
	}
	ArrayList<Point> polygon = new ArrayList<>();

	public LassoSelectTool(int actualColor) {
		super(actualColor);
	}
	
	@Override
	public boolean mouseMove(int x, int y) {
		return false;
	}

	@Override
	public boolean mouseDown(int x, int y) {
		return false;
	}

	@Override
	public boolean mouseUp(int x, int y) {
		polygon.add( Point.from(x,y) );
		// check if complete
		Point start = polygon.get(0);
		if( start.x == x && start.y == y ) {
			// polygon complete
			// calculate mask ...
			return true;
		}
		return false;
	}
	
	public void setDmdWidget(DMDWidget dmdWidget) {
		assert dmdWidget!=null;
		this.dmdWidget = dmdWidget;
	}
	
}
