package com.rinke.solutions.pinball.view.model;

public class Rect {
	public final int x1,y1,x2,y2;
	public Rect(int x1, int y1, int x2, int y2) {
		super();
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}
	
	public boolean inSelection(int x, int y) {
		return x>=x1 && x< x2 && y>=y1 && y<y2;
	}
	
	public boolean isOnSelectionMark(int x, int y) {
		return x == this.x1 || y == this.y1 || x == this.x2-1 || y == this.y2-1;
	}
	
	public static boolean selected( Rect sel, int x, int y) {
		return sel==null||sel.inSelection(x, y);
	}
	@Override
	public String toString() {
		return String.format("Rect [x1=%s, y1=%s, x2=%s, y2=%s]", x1, y1, x2, y2);
	}
}