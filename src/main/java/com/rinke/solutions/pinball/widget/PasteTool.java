package com.rinke.solutions.pinball.widget;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.util.ByteUtil;

/**
 * pastes a frame (from clipboard) over the current dmd image. planes to use are depending on the dmd draw mask
 * @author stefan rinke
 */
public class PasteTool extends DrawTool {

	private Frame frameToPaste;
	private boolean maskOnly;
	private int planeSize;
	private int width;
	private int height;
	
	public PasteTool(int actualColor, int w, int h) {
		super(actualColor);
		planeSize = w*h/8;
		height = h;
		width = w;
	}

	@Override
	public boolean mouseMove(int ix, int iy) {
		if( pressedButton >0 ) {
			int x = ix - x1;
			int y = iy - y1;
			dmd.copyLastBuffer();
			if( maskOnly ) {
				byte[] plane = copyShiftedPlane(x, y, frameToPaste.mask, true);
				dmd.ensureMask(plane);
			} else {
				int mask = dmd.getDrawMask()>>1;
				Frame f = dmd.getFrame();
				for( int j = 0; j < f.planes.size(); j++) {
					if (((1 << j) & mask) != 0) {
						byte[] plane = copyShiftedPlane(x, y, frameToPaste.planes.get(j), false);
						System.arraycopy(plane, 0, f.planes.get(j).plane, 0, planeSize);
					}
				}
			}
			return true;
		}
		return false;
	}
	
	private byte[] copyShiftedPlane(int x, int y, Plane p, boolean filling) {
		byte[] plane = new byte[planeSize];
		Arrays.fill(plane, filling?(byte)0xFF:0);
		byte[] rowBytes = new byte[width/8];
		int bytesPerRow = dmd.getBytesPerRow();
		for( int row = height-1; row>= 0; row--) {
			int destRow = row+y;
			if( destRow >= 0 && destRow < height) {
				// copy row to shift
				System.arraycopy(p.plane, bytesPerRow*row, rowBytes, 0, bytesPerRow);
				ByteUtil.shift(rowBytes, x, filling);
				System.arraycopy(rowBytes, 0, plane, bytesPerRow*destRow, bytesPerRow);
			} 
		}
		return plane;
	}

	@Override
	public boolean mouseUp(int x, int y) {
		return super.mouseUp(x, y);
	}

	@Override
	public boolean mouseDown(int x, int y) {
		dmd.addUndoBuffer();
		return true;
	}

	public void setMaskOnly(boolean val) {
		this.maskOnly = val;
	}

	public void setFrameToPaste(Frame frameToPaste) {
		 this.frameToPaste = frameToPaste;
	}

}
