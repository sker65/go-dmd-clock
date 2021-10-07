package com.rinke.solutions.pinball.widget;

import java.util.ArrayDeque;
import java.util.Queue;

public class FloodFillTool extends DrawTool {

	public FloodFillTool(int actualColor) {
		super(actualColor);
	}

	int depth = 0;
	int maxDepth = 0;

	@Override
	public boolean mouseUp(int x, int y) {
		if (dmd.getDrawMask() != 1) {
			int oldColor = dmd.getPixelWithoutMask(x, y);
			if (oldColor != actualColor) {
				dmd.addUndoBuffer();
				depth = 0;
				maxDepth = 0;
				if (toolSize > 2) {
					replaceColor(oldColor);
				} else {
					fill(oldColor, x, y);
				}
				// System.out.println(maxDepth);
				return true;
			}
		} else {
			int oldColor = dmd.getPixelWithoutMask(x, y);
			int oldMask = dmd.getMaskPixel(x, y);
			if (oldMask != actualColor) {
				dmd.addUndoBuffer();
				depth = 0;
				fillMask(oldColor, oldMask, x, y);
				return true;
			}
		}
		return false;
	}

	private void replaceColor(int oldColor) {
		for (int x = 0; x < dmd.getWidth(); x++) {
			for (int y = 0; y < dmd.getHeight(); y++) {
				if (dmd.getPixelWithoutMask(x, y) == oldColor) {
					dmd.setPixel(x, y, actualColor);
				}
			}
		}
	}

	private static final int[] dx = { -1, 0, 0, 1 };
	private static final int[] dy = { 0, -1, 1, 0 };

	private boolean isSafe(Point p) {
		return p.x >= 0 && p.x < dmd.getWidth() && p.y >= 0 && p.y < dmd.getHeight();
	}

	static class Point {
		public int x;
		public int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Point other = (Point) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return String.format("Point [x=%s, y=%s]", x, y);
		}
	}

	private void fill(int oldColor, int x, int y) {
		Queue<Point> q = new ArrayDeque<>();
		Point p = new Point(x, y);
		if (!q.contains(p))
			q.add(p);
		if (dmd.getPixelWithoutMask(x, y) == this.actualColor)
			return;
		while (!q.isEmpty()) {
			p = q.poll();
			dmd.setPixel(p.x, p.y, this.actualColor);
			if (dmd.getPixelWithoutMask(p.x, p.y) != oldColor) {
				// double check because of col masking, only decend if its
				// really changing color
				for (int k = 0; k < dx.length; k++) {
					Point p1 = new Point(p.x + dx[k], p.y + dy[k]);
					if (isSafe(p1) && !q.contains(p1) && dmd.getPixelWithoutMask(p1.x, p1.y) == oldColor)
						q.add(p1);
				}
			}
		}
	}

	private void fillMask(int oldColor, int oldMask, int x, int y) {
		Queue<Point> q = new ArrayDeque<>();
		Point p = new Point(x, y);
		if (!q.contains(p))
			q.add(p);
		while (!q.isEmpty()) {
			p = q.poll();
			if (dmd.getPixelWithoutMask(p.x, p.y) == oldColor && dmd.getMaskPixel(p.x, p.y) == oldMask) {
				dmd.setPixel(p.x, p.y, this.actualColor);
				for (int k = 0; k < dx.length; k++) {
					Point p1 = new Point(p.x + dx[k], p.y + dy[k]);
					if (isSafe(p1) && !q.contains(p1) && dmd.getPixelWithoutMask(p1.x, p1.y) == oldColor && dmd.getMaskPixel(p1.x, p1.y) == oldMask)
						q.add(p1);
				}
			}
		}
	}

}
