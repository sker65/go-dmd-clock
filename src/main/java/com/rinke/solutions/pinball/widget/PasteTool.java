package com.rinke.solutions.pinball.widget;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
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
	private int dx, dy;
	
	public PasteTool(int actualColor, int w, int h, int dx, int dy) {
		super(actualColor);
		planeSize = w*h/8;
		height = h;
		width = w;
		this.dx = dx;
		this.dy = dy;
	}
	
	public void pastePos (int ix, int iy) {
		int x = ix - dx;
		int y = iy - dy;
		dmd.copyLastBuffer();
		if( maskOnly ) {
			byte[] plane = copyShiftedPlane(x, y, frameToPaste.mask.data, true);
			dmd.setMask(new Mask(plane,false));
		} else {
			int planeMask = dmd.getDrawMask()>>1;
			Frame dest = dmd.getFrame();
			byte[] maskPlane = null;
			if( frameToPaste.hasMask() ) {
				maskPlane = copyShiftedPlane(x, y, frameToPaste.mask.data, false);
				//ImageUtil.dumpPlane(maskPlane, 16);
			}
			for( int j = 0; j < dest.planes.size(); j++) {
				if (((1 << j) & planeMask) != 0) {
					byte[] plane = copyShiftedPlane(x, y, frameToPaste.planes.get(j).data, false);
					if( frameToPaste.hasMask() ) {
						for( int i = 0; i < planeSize; i++) {
							dest.planes.get(j).data[i] = (byte) ((plane[i] & maskPlane[i]) | ( dest.planes.get(j).data[i] & ~maskPlane[i] ));
						}
					} else {
						System.arraycopy(plane, 0, dest.planes.get(j).data, 0, planeSize);
					}
				}
			}
		}
	}
	
	@Override
	public boolean mouseMove(int ix, int iy) {
		if( pressedButton >0 ) {
			pastePos(ix,iy);
			return true;
		}
		return false;
	}
	
	private byte[] copyShiftedPlane(int x, int y, byte[] planeData, boolean filling) {
		byte[] plane = new byte[planeSize];
		Arrays.fill(plane, filling?(byte)0xFF:0);
		byte[] rowBytes = new byte[width/8];
		int bytesPerRow = dmd.getBytesPerRow();
		for( int row = height-1; row>= 0; row--) {
			int destRow = row+y;
			if( destRow >= 0 && destRow < height) {
				// copy row to shift
				System.arraycopy(planeData, bytesPerRow*row, rowBytes, 0, bytesPerRow);
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
		 //System.out.println("mask");
		 //if( frameToPaste.hasMask()) ImageUtil.dumpPlane(frameToPaste.mask.data, 16);
	}

}
