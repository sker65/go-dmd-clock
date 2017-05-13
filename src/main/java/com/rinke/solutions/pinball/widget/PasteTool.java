package com.rinke.solutions.pinball.widget;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.renderer.ImageUtil;
import com.rinke.solutions.pinball.util.ByteUtil;
import com.rinke.solutions.pinball.view.model.ViewModel.PasteData;

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
	
	public PasteTool(int actualColor, PasteData d) {
		super(actualColor);
		planeSize = d.width*d.height/8;
		height = d.height;
		width = d.width;
		this.dx = d.dx;
		this.dy = d.dy;
		this.maskOnly = d.maskOnly;
	}


	@Override
	public boolean mouseMove(int ix, int iy) {
		if( pressedButton >0 ) {
			int x = ix - dx;
			int y = iy - dy;
			dmd.copyLastBuffer();
			if( maskOnly ) {
				byte[] plane = copyShiftedPlane(x, y, frameToPaste.mask, true);
				dmd.setMask(plane);
			} else {
				int planeMask = dmd.getDrawMask()>>1;
				Frame dest = dmd.getFrame();
				byte[] maskPlane = null;
				if( frameToPaste.hasMask() ) {
					maskPlane = copyShiftedPlane(x, y, new Plane(Plane.xMASK,frameToPaste.mask.data), false);
					//ImageUtil.dumpPlane(maskPlane, 16);
				}
				for( int j = 0; j < dest.planes.size(); j++) {
					if (((1 << j) & planeMask) != 0) {
						byte[] plane = copyShiftedPlane(x, y, frameToPaste.planes.get(j), false);
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
				System.arraycopy(p.data, bytesPerRow*row, rowBytes, 0, bytesPerRow);
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
